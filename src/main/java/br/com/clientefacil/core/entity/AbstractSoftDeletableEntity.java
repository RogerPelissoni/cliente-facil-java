package br.com.clientefacil.core.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// Base pra entidades de negócio não tenant-aware que passam a ser soft-deletáveis (hoje só Company,
// que é a própria raiz do tenant, então não estende AbstractAuditableTenantEntity). Para as tenant-
// aware, ver AbstractSoftDeletableTenantEntity.
//
// @SQLDelete e @SQLRestriction ficam em cada entidade CONCRETA, não aqui — testado ao vivo nesta
// versão do Hibernate (6.4.4.Final) e confirmado que @SQLRestriction declarado só no
// @MappedSuperclass NÃO é herdado pelas subclasses (a query gerada ignora a condição
// silenciosamente, sem erro nenhum). @SQLDelete já teria essa limitação de qualquer forma (a SQL
// literal precisa do nome da tabela). Ver entity/Company.java pra um exemplo dos dois juntos.
@MappedSuperclass
@Getter
@Setter
public abstract class AbstractSoftDeletableEntity extends AbstractAuditableEntity {

    @Column(name = "deleted_at")
    @JsonIgnore
    private LocalDateTime deletedAt;

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
