package br.com.clientefacil.entity;

import br.com.clientefacil.core.entity.AbstractSoftDeletableTenantEntity;
import br.com.clientefacil.core.entity.SoftDelete;
import br.com.clientefacil.entity.enums.PersonGenderEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "person")
@Getter
@Setter
@SQLRestriction(SoftDelete.NOT_DELETED)
public class Person extends AbstractSoftDeletableTenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "ds_document", length = 20)
    private String dsDocument;

    @Column(name = "tp_gender")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private PersonGenderEnum tpGender;

    @Column(name = "fl_active", nullable = false)
    private Boolean flActive = true;

    @OneToMany(mappedBy = "person")
    private Set<PersonAddress> personAddresses = new HashSet<>();

    @OneToMany(mappedBy = "person")
    private Set<PersonPhone> personPhones = new HashSet<>();

    @OneToMany(mappedBy = "person")
    private Set<PersonMail> personMails = new HashSet<>();
}
