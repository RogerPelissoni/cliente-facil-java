package br.com.clientefacil.entity;

import br.com.clientefacil.entity.base.AbstractAuditableTenantEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "profile_permission")
@Getter
@Setter
public class ProfilePermission extends AbstractAuditableTenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nr_permission_level", nullable = false)
    private Short nrPermissionLevel;

    @ManyToOne
    @JoinColumn(name = "profile_id")
    private Profile profile;

    @ManyToOne
    @JoinColumn(name = "resource_id")
    private Resource resource;
}
