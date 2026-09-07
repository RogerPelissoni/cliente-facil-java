package br.com.clientefacil.core.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// Base pra entidades de negócio tenant-aware que passam a ser soft-deletáveis (Client, Person,
// Professional, Event, AccountReceivable, AccountReceivableMovement, User) — ver
// AbstractSoftDeletableEntity pro porquê de @SQLDelete/@SQLRestriction ficarem em cada entidade
// concreta em vez de aqui.
@MappedSuperclass
@Getter
@Setter
public abstract class AbstractSoftDeletableTenantEntity extends AbstractAuditableTenantEntity {

    @Column(name = "deleted_at")
    @JsonIgnore
    private LocalDateTime deletedAt;

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
