package br.com.clientefacil.entity;

import br.com.clientefacil.core.dto.UserRoleEnum;
import br.com.clientefacil.core.entity.AbstractSoftDeletableTenantEntity;
import br.com.clientefacil.core.entity.SoftDelete;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@SQLRestriction(SoftDelete.NOT_DELETED)
public class User extends AbstractSoftDeletableTenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    // unique = true removido daqui: a unicidade agora é um índice PARCIAL (só entre ativos, ver
    // V1_2__create_users_table.sql/users_email_key), que a anotação de coluna não consegue expressar.
    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    // Null = e-mail ainda não confirmado (login bloqueado, ver AuthService.login). Preenchido só via
    // POST /auth/confirm-email, com o token enviado no momento da criação do usuário (ver
    // UserService.sendConfirmationEmail).
    @Column(name = "dt_email_confirmed_at")
    private LocalDateTime dtEmailConfirmedAt;

    // Bloqueio de conta após tentativas de senha errada seguidas (ver AuthService.login).
    @Column(name = "nr_failed_login_attempts", nullable = false)
    private int nrFailedLoginAttempts;

    @Column(name = "dt_locked_until")
    private LocalDateTime dtLockedUntil;

    @Column(name = "fl_active", nullable = false)
    private Boolean flActive = true;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private UserRoleEnum role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", insertable = false, updatable = false)
    private Company company;
}
