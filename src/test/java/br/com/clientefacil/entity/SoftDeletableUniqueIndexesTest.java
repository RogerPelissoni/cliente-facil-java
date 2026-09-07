package br.com.clientefacil.entity;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link SoftDeletableEntitiesTest} garante a declaração (`@SQLRestriction`) e
 * {@link SoftDeleteBehaviorIntegrationTest} garante o comportamento do delete em si — nenhum dos dois
 * cobre a terceira forma de errar soft delete: uma constraint `UNIQUE` "cheia" numa tabela que tem
 * `deleted_at`, que bloquearia pra sempre recriar um registro com a mesma chave depois de um soft
 * delete (ver `docs/guides/5_soft-delete.md`, seção "Migrations").
 * <p>
 * Diferente do `ForeignKeyDeletionGuard` (que resolve RESTRICT/CASCADE genericamente em runtime, lendo
 * o schema), aqui não existe automação equivalente: quais colunas formam a chave de unicidade de cada
 * tabela é decisão de negócio, não algo derivável do schema. Este teste não gera nem substitui a SQL
 * da migration — só confirma, lendo o catálogo do Postgres (`pg_index`), que a convenção documentada
 * (índice único PARCIAL, `WHERE deleted_at IS NULL`, em vez de constraint/índice único "cheio") foi
 * seguida em toda tabela soft-deletável. Mesmo espírito do `SoftDeletableEntitiesTest`: não prova que
 * a regra de negócio está certa, só que ninguém quebra a convenção sem o build avisar.
 */
@SpringBootTest
class SoftDeletableUniqueIndexesTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void noSoftDeletableTableHasANonPartialUniqueIndex() {
        // PK entra como índice único também (indisunique = true) mas não representa uma regra de
        // negócio pra converter em parcial — excluída via indisprimary = false.
        List<Map<String, Object>> violations = jdbcTemplate.queryForList("""
                SELECT t.relname AS table_name, i.relname AS index_name,
                       pg_get_indexdef(idx.indexrelid) AS index_def
                FROM pg_index idx
                JOIN pg_class i ON i.oid = idx.indexrelid
                JOIN pg_class t ON t.oid = idx.indrelid
                JOIN pg_namespace n ON n.oid = t.relnamespace
                WHERE n.nspname = 'public'
                  AND idx.indisunique = true
                  AND idx.indisprimary = false
                  AND idx.indpred IS NULL
                  AND t.relname IN (
                      SELECT DISTINCT table_name FROM information_schema.columns
                      WHERE column_name = 'deleted_at' AND table_schema = 'public'
                  )
                """);

        assertThat(violations)
                .as("índice/constraint único sem WHERE deleted_at IS NULL numa tabela soft-deletável — "
                        + "um registro soft-deletado bloquearia pra sempre recriar outro com a mesma chave. "
                        + "Recrie como índice único PARCIAL: CREATE UNIQUE INDEX ... WHERE deleted_at IS NULL "
                        + "(ver docs/guides/5_soft-delete.md, seção Migrations). Encontrado(s): " + violations)
                .isEmpty();
    }

    @Test
    void everyPartialUniqueIndexOnASoftDeletableTableExcludesOnlyDeletedRows() {
        List<Map<String, Object>> partialIndexes = jdbcTemplate.queryForList("""
                SELECT t.relname AS table_name, i.relname AS index_name,
                       pg_get_expr(idx.indpred, idx.indrelid) AS predicate
                FROM pg_index idx
                JOIN pg_class i ON i.oid = idx.indexrelid
                JOIN pg_class t ON t.oid = idx.indrelid
                JOIN pg_namespace n ON n.oid = t.relnamespace
                WHERE n.nspname = 'public'
                  AND idx.indisunique = true
                  AND idx.indpred IS NOT NULL
                  AND t.relname IN (
                      SELECT DISTINCT table_name FROM information_schema.columns
                      WHERE column_name = 'deleted_at' AND table_schema = 'public'
                  )
                """);

        assertThat(partialIndexes)
                .as("nenhum índice único parcial encontrado nas tabelas soft-deletáveis — se isto falhar "
                        + "depois que uma migration adicionar um, é sinal de que a query acima parou de achar "
                        + "índices parciais (ajustar a query, não remover a checagem)")
                .isNotEmpty();

        for (Map<String, Object> row : partialIndexes) {
            assertThat(row.get("predicate"))
                    .as("índice " + row.get("index_name") + " (" + row.get("table_name") + ") é parcial mas o "
                            + "predicado não é exatamente 'deleted_at IS NULL' — condição errada exclui "
                            + "as linhas certas na hora errada")
                    .isEqualTo("(deleted_at IS NULL)");
        }
    }
}
