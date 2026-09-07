package br.com.clientefacil.core.hibernate;

import br.com.clientefacil.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Testa checkAndCascade/ensureMetadataLoaded diretamente (package-private, ver comentário na classe)
// em vez de montar um PreDeleteEvent/EntityPersister reais do Hibernate — a metadata (quais FKs
// existem, quais tabelas têm a coluna "deleted_at") é simulada via JdbcTemplate mockado, não contra
// Postgres.
@ExtendWith(MockitoExtension.class)
class ForeignKeyDeletionGuardTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private ForeignKeyDeletionGuard guard;

    @BeforeEach
    void setUp() {
        // Fixture: client.person_id -> person (RESTRICT); event_message.event_id -> event (CASCADE,
        // sem coluna "deleted_at" -> remoção física); account_receivable_movement.account_receivable_id
        // -> account_receivable (CASCADE, movement TEM "deleted_at" -> soft delete em cascata);
        // blocker.arm_id -> account_receivable_movement (RESTRICT, usado no teste de bloqueio profundo).
        List<Map<String, Object>> fkRows = List.of(
                fkRow("client", "person_id", "person", "RESTRICT"),
                fkRow("event_message", "event_id", "event", "CASCADE"),
                fkRow("account_receivable_movement", "account_receivable_id", "account_receivable", "CASCADE"),
                fkRow("blocker", "arm_id", "account_receivable_movement", "RESTRICT")
        );
        when(jdbcTemplate.queryForList(contains("information_schema.table_constraints"))).thenReturn(fkRows);
        when(jdbcTemplate.queryForList(contains("information_schema.columns"), eq(String.class)))
                .thenReturn(List.of("client", "event", "account_receivable", "account_receivable_movement"));

        guard = new ForeignKeyDeletionGuard(jdbcTemplate);
        guard.ensureMetadataLoaded();
    }

    private static Map<String, Object> fkRow(String referencingTable, String referencingColumn,
                                              String referencedTable, String deleteRule) {
        return Map.of(
                "referencing_table", referencingTable,
                "referencing_column", referencingColumn,
                "referenced_table", referencedTable,
                "delete_rule", deleteRule
        );
    }

    @Test
    void blocks_whenActiveRestrictReferenceExists() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), eq(10L))).thenReturn(true);

        assertThatThrownBy(() -> guard.checkAndCascade("person", 10L, new HashSet<>()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cliente");
    }

    @Test
    void allows_whenNoRestrictReferenceExists() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), eq(11L))).thenReturn(false);

        assertThatCode(() -> guard.checkAndCascade("person", 11L, new HashSet<>()))
                .doesNotThrowAnyException();
    }

    @Test
    void cascadesPhysicalDelete_forChildTableWithoutDeletedAt() {
        when(jdbcTemplate.queryForList(anyString(), eq(Long.class), eq(20L))).thenReturn(List.of(77L));

        guard.checkAndCascade("event", 20L, new HashSet<>());

        verify(jdbcTemplate).update(contains("DELETE FROM event_message"), eq(77L));
    }

    @Test
    void cascadesSoftDelete_forChildTableWithDeletedAt() {
        when(jdbcTemplate.queryForList(anyString(), eq(Long.class), eq(30L))).thenReturn(List.of(88L));
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), eq(88L))).thenReturn(false);

        guard.checkAndCascade("account_receivable", 30L, new HashSet<>());

        verify(jdbcTemplate).update(contains("UPDATE account_receivable_movement SET deleted_at"), eq(88L));
    }

    @Test
    void abortsEntireOperation_whenCascadeChildIsRestrictBlockedDeeper() {
        when(jdbcTemplate.queryForList(anyString(), eq(Long.class), eq(40L))).thenReturn(List.of(99L));
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), eq(99L))).thenReturn(true);

        assertThatThrownBy(() -> guard.checkAndCascade("account_receivable", 40L, new HashSet<>()))
                .isInstanceOf(BusinessException.class);

        verify(jdbcTemplate, never()).update(anyString(), any(Object[].class));
    }

    // handleDelete = o que onPreDelete delega pra depois de extrair table/id do PreDeleteEvent —
    // testado direto aqui pelo mesmo motivo de checkAndCascade (evita montar um PreDeleteEvent real).
    // É a peça que substitui @SQLDelete: faz o UPDATE ela mesma e veta o DELETE físico do Hibernate.

    @Test
    void handleDelete_softDeletesAndVetoes_whenTableHasDeletedAt() {
        boolean veto = guard.handleDelete("client", 50L);

        assertThat(veto).isTrue();
        verify(jdbcTemplate).update(contains("UPDATE client SET deleted_at = now() WHERE id = ?"), eq(50L));
    }

    @Test
    void handleDelete_doesNotVeto_whenTableHasNoDeletedAt() {
        boolean veto = guard.handleDelete("blocker", 51L);

        assertThat(veto).isFalse();
        verify(jdbcTemplate, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void handleDelete_throwsAndNeverUpdates_whenRestrictBlocked() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), eq(52L))).thenReturn(true);

        assertThatThrownBy(() -> guard.handleDelete("person", 52L))
                .isInstanceOf(BusinessException.class);

        verify(jdbcTemplate, never()).update(anyString(), any(Object[].class));
    }
}
