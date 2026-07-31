package br.com.clientefacil.entity;

import br.com.clientefacil.core.entity.AbstractAuditableTenantEntity;
import br.com.clientefacil.entity.enums.NotificationStatusEnum;
import br.com.clientefacil.entity.enums.NotificationTypeEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification")
@Getter
@Setter
public class Notification extends AbstractAuditableTenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "tp_type", nullable = false)
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private NotificationTypeEnum tpType;

    @Column(name = "tp_status", nullable = false)
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private NotificationStatusEnum tpStatus;

    @Column(name = "ds_title", nullable = false)
    private String dsTitle;

    @Column(name = "ds_message", nullable = false)
    private String dsMessage;

    @Column(name = "dt_read")
    private LocalDateTime dtRead;
}
