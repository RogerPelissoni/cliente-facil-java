package br.com.clientefacil.entity;

import br.com.clientefacil.listener.BaseEntityListener;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "company")
@EntityListeners(BaseEntityListener.class)
@Getter
@Setter
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @Embedded
    private Auditable auditable = new Auditable();
}