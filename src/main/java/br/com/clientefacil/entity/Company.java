package br.com.clientefacil.entity;

import br.com.clientefacil.core.entity.AbstractSoftDeletableEntity;
import br.com.clientefacil.core.entity.SoftDelete;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "company")
@Getter
@Setter
@SQLRestriction(SoftDelete.NOT_DELETED)
public class Company extends AbstractSoftDeletableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id")
    private Person person;

    @Column(name = "fl_active", nullable = false)
    private Boolean flActive = true;
}
