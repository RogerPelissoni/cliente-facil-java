package br.com.clientefacil.entity;

import br.com.clientefacil.core.entity.AbstractAuditableEntity;
import br.com.clientefacil.entity.enums.UserTokenTypeEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// Token de uso único para os dois fluxos públicos que precisam provar posse de um e-mail:
// confirmação de e-mail (Parte 10) e recuperação de senha. Não é tenant-aware: é infraestrutura de
// autenticação, não dado de negócio de uma empresa — mesmo espírito não-tenant de
// NotificationDeadLetter. Ver UserTokenService pra emissão/consumo (o token cru nunca é persistido,
// só o hash — ver dsTokenHash).
@Entity
@Table(name = "user_token")
@Getter
@Setter
public class UserToken extends AbstractAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "tp_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private UserTokenTypeEnum tpType;

    @Column(name = "ds_token_hash", nullable = false, unique = true)
    private String dsTokenHash;

    @Column(name = "dt_expires_at", nullable = false)
    private LocalDateTime dtExpiresAt;

    // null = ainda válido (se não expirado). Setado no consumo — cada token só pode ser usado uma vez.
    @Column(name = "dt_used_at")
    private LocalDateTime dtUsedAt;
}
