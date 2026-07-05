package br.com.clientefacil.entity;

import br.com.clientefacil.core.entity.AbstractAuditableTenantEntity;
import br.com.clientefacil.entity.enums.AccountReceivableStatusEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "account_receivable")
@Getter
@Setter
public class AccountReceivable extends AbstractAuditableTenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id")
    private Person person;

    @Column(name = "ds_code", nullable = false)
    private String dsCode;

    @Column(name = "nr_installment")
    private Integer nrInstallment;

    @Column(name = "vl_total")
    private Double vlTotal;

    @Column(name = "vl_balance")
    private Double vlBalance;

    @Column(name = "da_due")
    private LocalDate daDue;

    @Column(name = "dt_paid")
    private LocalDateTime dtPaid;

    @Column(name = "tp_status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private AccountReceivableStatusEnum tpStatus;

    @Column(name = "ds_observation")
    private LocalDateTime dsObservation;
}
