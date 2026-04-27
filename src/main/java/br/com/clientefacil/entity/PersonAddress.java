package br.com.clientefacil.entity;

import br.com.clientefacil.core.entity.AbstractAuditableTenantEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "person_adress")
@Getter
@Setter
public class PersonAddress extends AbstractAuditableTenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ds_street")
    private String dsStreet;

    @Column(name = "ds_number", length = 50)
    private String dsNumber;

    @Column(name = "ds_complement")
    private String dsComplement;

    @Column(name = "ds_district")
    private String dsDistrict;

    @Column(name = "ds_city", nullable = false)
    private String dsCity;

    @Column(name = "ds_state", nullable = false)
    private String dsState;

    @Column(name = "ds_zip_code", nullable = false)
    private String dsZipCode;

    @Column(name = "fl_main")
    private Boolean flMain;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id")
    private Person person;
}
