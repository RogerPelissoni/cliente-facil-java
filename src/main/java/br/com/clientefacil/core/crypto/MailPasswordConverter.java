package br.com.clientefacil.core.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Base64;

/**
 * Criptografa/descriptografa a senha SMTP guardada em mail_config (ds_password) antes de
 * gravar/ler do banco. Precisa ser reversível (diferente de uma senha de usuário, com hash) porque
 * o EmailListener precisa da senha em texto claro para autenticar no servidor SMTP.
 * <p>
 * AES-256/GCM: cada valor criptografado carrega seu próprio IV aleatório (12 bytes) prefixado ao
 * ciphertext, então o mesmo valor em claro nunca gera o mesmo resultado duas vezes. A chave vem de
 * "mail.config.encryption-key" (env MAIL_CONFIG_ENCRYPTION_KEY), mesmo padrão de configuração do
 * jwt.secret (JwtService) — precisa ter 32 bytes (AES-256).
 * <p>
 * "autoApply = false": aplicado explicitamente via @Convert em MailConfig, não em toda String do
 * projeto. Registrado como @Component para que o Spring injete a chave via @Value (Hibernate
 * resolve @Converter pelo SpringBeanContainer quando ele também é um bean gerenciado).
 */
@Slf4j
@Component
@Converter(autoApply = false)
public class MailPasswordConverter implements AttributeConverter<String, String> {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecretKeySpec key;

    public MailPasswordConverter(@Value("${mail.config.encryption-key}") String encryptionKey) {
        this.key = new SecretKeySpec(encryptionKey.getBytes(StandardCharsets.UTF_8), "AES");
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (!StringUtils.hasText(attribute)) {
            return null;
        }

        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            AlgorithmParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);

            byte[] cipherText = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));

            byte[] result = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(cipherText, 0, result, iv.length, cipherText.length);

            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            log.error("Falha ao criptografar a senha SMTP", e);
            throw new IllegalStateException("Falha ao criptografar a senha SMTP", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (!StringUtils.hasText(dbData)) {
            return null;
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(dbData);

            byte[] iv = new byte[IV_LENGTH_BYTES];
            System.arraycopy(decoded, 0, iv, 0, IV_LENGTH_BYTES);

            byte[] cipherText = new byte[decoded.length - IV_LENGTH_BYTES];
            System.arraycopy(decoded, IV_LENGTH_BYTES, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            AlgorithmParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);

            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Falha ao descriptografar a senha SMTP", e);
            throw new IllegalStateException("Falha ao descriptografar a senha SMTP", e);
        }
    }
}
