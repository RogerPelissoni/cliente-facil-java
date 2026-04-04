package br.com.clientefacil.entity;

import br.com.clientefacil.entity.enums.PersonGenderEnum;
import br.com.clientefacil.listener.BaseEntityListener;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "person")
@EntityListeners(BaseEntityListener.class)
@Getter
@Setter
public class Person {

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

    @Embedded
    private Auditable auditable = new Auditable();

    @Embedded
    private Tenant tenant = new Tenant();
}