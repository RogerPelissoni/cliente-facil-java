package br.com.clientefacil.entity;

import br.com.clientefacil.entity.base.AbstractAuditableTenantEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "module")
@Getter
@Setter
public class Module extends AbstractAuditableTenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;
}
