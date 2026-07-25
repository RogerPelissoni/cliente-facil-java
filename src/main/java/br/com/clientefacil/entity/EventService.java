package br.com.clientefacil.entity;

import br.com.clientefacil.core.entity.AbstractAuditableTenantEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "event_service")
@Getter
@Setter
public class EventService extends AbstractAuditableTenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professional_id")
    private Professional professional;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_receivable_id")
    private AccountReceivable accountReceivable;
}
