package br.com.clientefacil.entity;

import br.com.clientefacil.core.crypto.MailPasswordConverter;
import br.com.clientefacil.core.entity.AbstractAuditableTenantEntity;
import br.com.clientefacil.entity.enums.MailEncryptionType;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Configuração de SMTP usada pelo EmailListener para montar um JavaMailSender dinâmico.
 * <p>
 * companyId (herdado de AbstractAuditableTenantEntity) NULL = "config base" do sistema, usada por
 * e-mails que não pertencem a uma empresa específica (ex: alerta de dead-letter, recuperação de
 * senha). companyId preenchido = config própria de uma empresa, que tem prioridade sobre a base
 * quando existir — ver MailConfigService.resolveFor(companyId).
 * <p>
 * Não é coincidência que company_id aceite NULL aqui: é o mesmo mecanismo de fallback que o
 * tenantFilter do Hibernate já usa (company_id = :companyId OR company_id IS NULL), então uma
 * config base fica automaticamente visível para qualquer empresa autenticada, sem lógica extra de
 * query — só a prioridade explícita continua sendo responsabilidade do service.
 */
@Entity
@Table(name = "mail_config")
@Getter
@Setter
public class MailConfig extends AbstractAuditableTenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ds_host", nullable = false)
    private String dsHost;

    @Column(name = "nr_port", nullable = false)
    private Integer nrPort;

    @Column(name = "ds_username")
    private String dsUsername;

    // Nunca serializado de volta pro front (WRITE_ONLY): a API aceita a senha na criação/edição,
    // mas nunca a devolve, mesmo criptografada — mesmo cuidado que User.password já tem.
    @Column(name = "ds_password")
    @Convert(converter = MailPasswordConverter.class)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String dsPassword;

    @Column(name = "tp_encryption", nullable = false)
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private MailEncryptionType tpEncryption = MailEncryptionType.TLS;

    @Column(name = "ds_from_name", nullable = false)
    private String dsFromName;

    @Column(name = "ds_from_address", nullable = false)
    private String dsFromAddress;

    @Column(name = "fl_active", nullable = false)
    private boolean flActive = true;
}
