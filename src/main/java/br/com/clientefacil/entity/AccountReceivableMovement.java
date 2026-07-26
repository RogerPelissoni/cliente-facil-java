package br.com.clientefacil.entity;

import br.com.clientefacil.core.entity.AbstractAuditableTenantEntity;
import br.com.clientefacil.entity.enums.AccountReceivableMovementPaymentTypeEnum;
import br.com.clientefacil.entity.enums.AccountReceivableMovementTypeEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "account_receivable_movement")
@Getter
@Setter
public class AccountReceivableMovement extends AbstractAuditableTenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_receivable_id")
    private AccountReceivable accountReceivable;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reversal_account_receivable_movement_id")
    private AccountReceivableMovement reversalAccountReceivableMovement;

    @Column(name = "vl_movement")
    private Double vlMovement;

    @Column(name = "vl_discount")
    private Double vlDiscount;

    @Column(name = "dt_movement")
    private LocalDateTime dtMovement;

    @Column(name = "tp_payment")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private AccountReceivableMovementPaymentTypeEnum tpPayment;

    @Column(name = "tp_movement")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private AccountReceivableMovementTypeEnum tpMovement;

    @Column(name = "ds_observations", nullable = false)
    private String dsObservations;
}
