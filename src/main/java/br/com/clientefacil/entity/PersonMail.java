package br.com.clientefacil.entity;

import br.com.clientefacil.core.entity.AbstractAuditableTenantEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "person_mail")
@Getter
@Setter
public class PersonMail extends AbstractAuditableTenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ds_mail")
    private String dsMail;

    @Column(name = "fl_main")
    private Boolean flMain;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id")
    private Person person;
}
