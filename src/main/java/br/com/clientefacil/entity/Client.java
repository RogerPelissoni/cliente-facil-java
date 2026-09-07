package br.com.clientefacil.entity;

import br.com.clientefacil.core.entity.AbstractSoftDeletableTenantEntity;
import br.com.clientefacil.core.entity.SoftDelete;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "client")
@Getter
@Setter
@SQLRestriction(SoftDelete.NOT_DELETED)
public class Client extends AbstractSoftDeletableTenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id")
    private Person person;
}
