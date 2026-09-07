package br.com.clientefacil.entity;

import br.com.clientefacil.core.entity.AbstractSoftDeletableEntity;
import br.com.clientefacil.core.entity.AbstractSoftDeletableTenantEntity;
import br.com.clientefacil.core.entity.SoftDelete;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Não dá pra eliminar {@code @SQLRestriction} de cada entidade soft-deletável — quatro tentativas
 * nesta mesma sessão esbarraram em limitações reais do Hibernate 6.4.4 (meta-anotação não é resolvida
 * nesta versão apesar do @Target permitir; @SoftDelete nativo bloqueia qualquer associação
 * @ManyToOne/@OneToOne LAZY apontando pra uma entidade soft-deletável — decisão permanente do time do
 * Hibernate, não um bug, confirmada até na versão mais recente do Hibernate; trigger no banco
 * duplicaria a fonte de verdade). {@code @SQLDelete} em si já foi eliminado: o
 * {@code ForeignKeyDeletionGuard} (core/hibernate/) faz o UPDATE ele mesmo e veta o DELETE físico do
 * Hibernate, sem usar a anotação nativa (por isso escapa da trava de LAZY). Ver
 * docs/guides/5_soft-delete.md pro relato completo.
 * <p>
 * O que dá pra fazer com o que sobra ({@code @SQLRestriction}, e a ausência de {@code @SQLDelete}):
 * não deixar a regra "silenciosamente errada" passar despercebida. Este teste não prova que soft
 * delete funciona (isso é validado ao vivo contra Postgres, ver o guia) — garante que toda entidade
 * que estende {@link AbstractSoftDeletableEntity}/{@link AbstractSoftDeletableTenantEntity} declara
 * {@code @SQLRestriction} com a condição correta e não reintroduziu {@code @SQLDelete} (redundante
 * agora — o guard sempre veta antes dele rodar, mas confundiria quem for ler o código depois).
 */
class SoftDeletableEntitiesTest {

    private static final String ENTITY_PACKAGE = "br.com.clientefacil.entity";

    @Test
    void everySoftDeletableEntityDeclaresSqlRestrictionAndNotSqlDelete() {
        List<Class<?>> entities = softDeletableEntityClasses();

        // Se isto falhar, o scan quebrou (ou as 8 entidades pararam de estender as superclasses) —
        // não confundir com "nenhuma entidade soft-deletável existe", que nunca deveria ser o caso.
        assertThat(entities).as("nenhuma entidade soft-deletável encontrada em " + ENTITY_PACKAGE).isNotEmpty();

        for (Class<?> entityClass : entities) {
            SQLRestriction sqlRestriction = entityClass.getAnnotation(SQLRestriction.class);
            assertThat(sqlRestriction)
                    .as(entityClass.getSimpleName() + " estende uma superclasse soft-deletável mas não "
                            + "declara @SQLRestriction — registros excluídos continuariam aparecendo em toda leitura")
                    .isNotNull();
            assertThat(sqlRestriction.value())
                    .as(entityClass.getSimpleName() + ": @SQLRestriction devia usar a constante SoftDelete.NOT_DELETED")
                    .isEqualTo(SoftDelete.NOT_DELETED);

            assertThat(entityClass.getAnnotation(SQLDelete.class))
                    .as(entityClass.getSimpleName() + ": @SQLDelete não devia mais existir aqui — o "
                            + "ForeignKeyDeletionGuard já faz o UPDATE e veta o DELETE físico; a anotação "
                            + "ficaria redundante (o guard veta antes dela rodar) e confundiria quem ler depois")
                    .isNull();
        }
    }

    // ClassPathScanningCandidateComponentProvider com AssignableTypeFilter em vez de uma lista
    // hard-coded das 8 entidades — uma entidade nova que estenda a superclasse certa já entra
    // automaticamente na checagem, sem precisar lembrar de atualizar este teste também. As duas
    // superclasses em si não entram no resultado: elas moram em core.entity, fora de ENTITY_PACKAGE.
    private static List<Class<?>> softDeletableEntityClasses() {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(AbstractSoftDeletableEntity.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(AbstractSoftDeletableTenantEntity.class));

        return scanner.findCandidateComponents(ENTITY_PACKAGE).stream()
                .map(SoftDeletableEntitiesTest::loadClass)
                .toList();
    }

    private static Class<?> loadClass(org.springframework.beans.factory.config.BeanDefinition beanDefinition) {
        try {
            return Class.forName(beanDefinition.getBeanClassName());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }
}
