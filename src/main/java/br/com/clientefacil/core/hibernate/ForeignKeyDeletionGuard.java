package br.com.clientefacil.core.hibernate;

import br.com.clientefacil.exception.BusinessException;
import org.hibernate.event.spi.PreDeleteEvent;
import org.hibernate.event.spi.PreDeleteEventListener;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

// Substitui os validators manuais da Fase A do soft delete (PersonDeletionValidator e afins) e o
// @SQLDelete de cada entidade por uma lógica genérica: o schema do banco (information_schema) já
// declara, via ON DELETE RESTRICT/CASCADE de cada FK e via a presença da coluna "deleted_at", o que
// deveria acontecer numa exclusão — isso vira a fonte única de verdade em vez de código repetido
// entidade a entidade. Ver docs/guides/5_soft-delete.md pra contexto completo.
//
// Registrado como PreDeleteEventListener em core/config/HibernateListenerConfig.java — roda antes de
// QUALQUER exclusão do sistema (não só as 8 entidades soft-deletáveis), com três papéis:
//   1. RESTRICT/NO ACTION: bloqueia (BusinessException) se existir linha ativa referenciando a
//      entidade sendo excluída — mesmo papel que o banco fazia sozinho antes do soft delete existir.
//   2. CASCADE: processa a árvore de filhos recursivamente — soft-deleta (coluna "deleted_at") quem
//      tem esse mecanismo, remove fisicamente quem não tem (exatamente o que o CASCADE do banco
//      faria, e que parou de disparar pelo mesmo motivo).
//   3. A entidade sendo excluída em si: se a tabela tem "deleted_at", faz o UPDATE aqui mesmo (via
//      JdbcTemplate, mesma técnica já usada pra cascata) e VETA o DELETE físico que o Hibernate ia
//      gerar — dispensa o @SQLDelete em cada entidade. Vetar (`onPreDelete` retornando `true`) só
//      pula a chamada a `persister.delete(...)` (confirmado lendo `EntityDeleteAction.execute()` no
//      source do Hibernate) — o resto do ciclo de vida (remoção da persistence context, eviction de
//      cache, @PostDelete) roda igual, vetado ou não, igual já acontecia com @SQLDelete. E como isso
//      não usa a anotação `@SoftDelete` nativa do Hibernate, não temos a trava dela (nenhuma
//      associação LAZY apontando pra uma entidade soft-deletável pode existir com @SoftDelete —
//      ver docs/guides/5_soft-delete.md) — aqui `entityMappingType.getSoftDeleteMapping()` nunca é
//      preenchido, a trava nunca dispara.
@Component
public class ForeignKeyDeletionGuard implements PreDeleteEventListener {

    // Só pra deixar a mensagem de bloqueio legível — fallback é o próprio nome da tabela.
    private static final Map<String, String> TABLE_LABELS = Map.ofEntries(
            Map.entry("client", "cliente"),
            Map.entry("professional", "profissional"),
            Map.entry("company", "empresa"),
            Map.entry("person", "pessoa"),
            Map.entry("users", "usuário"),
            Map.entry("account_receivable", "título financeiro"),
            Map.entry("account_receivable_movement", "movimentação financeira"),
            Map.entry("event", "evento"),
            Map.entry("event_service", "agendamento"),
            Map.entry("event_owner", "responsável por evento")
    );

    private final JdbcTemplate jdbcTemplate;

    // Carregado uma vez só (schema não muda em runtime) — nenhuma query de metadata por exclusão.
    private volatile Map<String, List<FkRef>> restrictFksByTable;
    private volatile Map<String, List<FkRef>> cascadeFksByTable;
    private volatile Set<String> tablesWithDeletedAt;

    public ForeignKeyDeletionGuard(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private record FkRef(String table, String column) {
    }

    @Override
    public boolean onPreDelete(PreDeleteEvent event) {
        // getTableNames()[0] = tabela primária — todas as entidades do projeto são single-table
        // (sem herança joined-table), então isso é sempre a tabela declarada em @Table(name = ...).
        String table = ((AbstractEntityPersister) event.getPersister()).getTableNames()[0];
        Long id = ((Number) event.getId()).longValue();

        return handleDelete(table, id);
    }

    // Package-private (não private) só pra ForeignKeyDeletionGuardTest chamar direto, sem precisar
    // montar um PreDeleteEvent/EntityPersister reais — o resto da classe permanece encapsulado.
    boolean handleDelete(String table, Long id) {
        ensureMetadataLoaded();

        checkAndCascade(table, id, new HashSet<>());

        if (tablesWithDeletedAt.contains(table)) {
            applyDelete(table, id); // UPDATE ... SET deleted_at = now() — feito aqui, não via @SQLDelete
            return true; // veta o DELETE físico: já fizemos o soft delete nós mesmos.
        }

        return false; // tabela sem deleted_at: deixa o DELETE físico normal do Hibernate acontecer.
    }

    void checkAndCascade(String table, Long id, Set<String> visited) {
        if (!visited.add(table + ":" + id)) {
            return; // já processado nesta mesma operação (evita reprocessar/ciclo).
        }

        for (FkRef ref : restrictFksByTable.getOrDefault(table, List.of())) {
            if (existsActiveReference(ref, id)) {
                throw new BusinessException(
                        "Operação bloqueada, existe um registro vinculado (" + label(ref.table()) + ")."
                );
            }
        }

        for (FkRef ref : cascadeFksByTable.getOrDefault(table, List.of())) {
            for (Long childId : findChildIds(ref, id)) {
                checkAndCascade(ref.table(), childId, visited); // valida/cascateia o filho primeiro
                applyDelete(ref.table(), childId);
            }
        }
    }

    private boolean existsActiveReference(FkRef ref, Long id) {
        String sql = "SELECT EXISTS(SELECT 1 FROM " + ref.table() + " WHERE " + ref.column() + " = ?"
                + (tablesWithDeletedAt.contains(ref.table()) ? " AND deleted_at IS NULL" : "")
                + ")";
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(sql, Boolean.class, id));
    }

    private List<Long> findChildIds(FkRef ref, Long id) {
        String sql = "SELECT id FROM " + ref.table() + " WHERE " + ref.column() + " = ?"
                + (tablesWithDeletedAt.contains(ref.table()) ? " AND deleted_at IS NULL" : "");
        return jdbcTemplate.queryForList(sql, Long.class, id);
    }

    private void applyDelete(String table, Long id) {
        if (tablesWithDeletedAt.contains(table)) {
            jdbcTemplate.update("UPDATE " + table + " SET deleted_at = now() WHERE id = ?", id);
        } else {
            jdbcTemplate.update("DELETE FROM " + table + " WHERE id = ?", id);
        }
    }

    private String label(String table) {
        return TABLE_LABELS.getOrDefault(table, table);
    }

    void ensureMetadataLoaded() {
        if (restrictFksByTable != null) {
            return;
        }
        synchronized (this) {
            if (restrictFksByTable != null) {
                return;
            }
            loadMetadata();
        }
    }

    private void loadMetadata() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT
                    tc.table_name AS referencing_table,
                    kcu.column_name AS referencing_column,
                    ccu.table_name AS referenced_table,
                    rc.delete_rule AS delete_rule
                FROM information_schema.table_constraints tc
                JOIN information_schema.key_column_usage kcu
                    ON tc.constraint_name = kcu.constraint_name AND tc.table_schema = kcu.table_schema
                JOIN information_schema.constraint_column_usage ccu
                    ON tc.constraint_name = ccu.constraint_name AND tc.table_schema = ccu.table_schema
                JOIN information_schema.referential_constraints rc
                    ON tc.constraint_name = rc.constraint_name AND tc.table_schema = rc.constraint_schema
                WHERE tc.constraint_type = 'FOREIGN KEY' AND tc.table_schema = 'public'
                """);

        Map<String, List<FkRef>> restrict = new ConcurrentHashMap<>();
        Map<String, List<FkRef>> cascade = new ConcurrentHashMap<>();

        for (Map<String, Object> row : rows) {
            String referencedTable = (String) row.get("referenced_table");
            FkRef ref = new FkRef((String) row.get("referencing_table"), (String) row.get("referencing_column"));
            String deleteRule = (String) row.get("delete_rule");

            Map<String, List<FkRef>> target = "CASCADE".equals(deleteRule) ? cascade : restrict;
            target.computeIfAbsent(referencedTable, k -> new java.util.ArrayList<>()).add(ref);
        }

        this.tablesWithDeletedAt = jdbcTemplate.queryForList("""
                        SELECT DISTINCT table_name FROM information_schema.columns
                        WHERE column_name = 'deleted_at' AND table_schema = 'public'
                        """, String.class)
                .stream().collect(Collectors.toUnmodifiableSet());

        this.restrictFksByTable = Map.copyOf(restrict);
        this.cascadeFksByTable = Map.copyOf(cascade);
    }
}
