package br.com.clientefacil.entity;

import br.com.clientefacil.listener.BaseEntityListener;
import br.com.clientefacil.listener.HasAuditable;
import br.com.clientefacil.listener.HasTenant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "profile")
@EntityListeners(BaseEntityListener.class)
@Getter
@Setter
public class Profile implements HasAuditable, HasTenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Embedded
    private Tenant tenant = new Tenant();

    @Embedded
    private Auditable auditable = new Auditable();
}