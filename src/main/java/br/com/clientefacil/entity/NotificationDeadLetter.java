package br.com.clientefacil.entity;

import br.com.clientefacil.core.entity.AbstractAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// Registro de auditoria mínimo para mensagens que esgotaram as tentativas de reprocessamento
// e caíram na DLQ (ver NotificationDeadLetterListener). Não é tenant-aware: é um log
// operacional da infraestrutura de mensageria, não um dado de negócio de uma empresa.
@Entity
@Table(name = "notification_dead_letter")
@Getter
@Setter
public class NotificationDeadLetter extends AbstractAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ds_payload", nullable = false)
    private String dsPayload;

    @Column(name = "ds_error_reason")
    private String dsErrorReason;

    @Column(name = "nr_death_count")
    private Integer nrDeathCount;

    @Column(name = "dt_failed_at")
    private LocalDateTime dtFailedAt;
}
