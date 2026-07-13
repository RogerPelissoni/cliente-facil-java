package br.com.clientefacil.entity;

import br.com.clientefacil.core.entity.AbstractAuditableTenantEntity;
import br.com.clientefacil.entity.enums.EventStatusEnum;
import br.com.clientefacil.entity.enums.EventTypeEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "event")
@Getter
@Setter
public class Event extends AbstractAuditableTenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ds_title", nullable = false, length = 100)
    private String dsTitle;

    @Column(name = "ds_description")
    private String dsDocument;

    @Column(name = "dt_start")
    private LocalDateTime dtStart;

    @Column(name = "dt_end")
    private LocalDateTime dtEnd;

    @Column(name = "tp_status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EventStatusEnum tpStatus;

    @Column(name = "tp_event")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EventTypeEnum tpEvent;

    @OneToOne(mappedBy = "event")
    private EventService service;
}
