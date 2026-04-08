package br.com.clientefacil.entity;

import br.com.clientefacil.entity.base.AbstractAuditableTenantEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "resource")
@Getter
@Setter
public class Resource extends AbstractAuditableTenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "signature", nullable = false, length = 100)
    private String signature;

    @ManyToOne
    @JoinColumn(name = "module_id")
    private Module module;
}
