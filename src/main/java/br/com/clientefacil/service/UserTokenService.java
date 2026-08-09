package br.com.clientefacil.service;

import br.com.clientefacil.entity.User;
import br.com.clientefacil.entity.UserToken;
import br.com.clientefacil.entity.enums.UserTokenTypeEnum;
import br.com.clientefacil.repository.UserTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

// Mecânica de token de uso único, compartilhada entre confirmação de e-mail e recuperação de senha
// (AuthService/UserService) — só gera/valida/expira, quem decide o que fazer com o User devolvido é
// o chamador.
//
// O token que trafega no link do e-mail NUNCA é persistido: só o hash SHA-256 dele vai pro banco
// (mesma cautela já aplicada à senha de SMTP em MailConfig, mutatis mutandis — aqui não precisa ser
// reversível, então hash simples resolve, diferente da senha do SMTP que precisa ser recuperada em
// claro pelo EmailListener).
@Service
@RequiredArgsConstructor
public class UserTokenService {

    private final UserTokenRepository repository;

    public String issue(User user, UserTokenTypeEnum type, Duration ttl) {
        // Pedir duas vezes (ex: "esqueci minha senha" clicado de novo antes de usar o primeiro link)
        // não pode deixar dois links simultaneamente válidos — invalida qualquer token do mesmo tipo
        // ainda não usado antes de emitir o novo. Reaproveita dtUsedAt em vez de um estado novo
        // ("invalidado" x "usado") — consume() já filtra por dtUsedAt IS NULL, então pra quem valida
        // um link a diferença não importa: os dois significam "esse token não serve mais".
        invalidateExistingTokens(user, type);

        String rawToken = generateRawToken();

        UserToken token = new UserToken();
        token.setUser(user);
        token.setTpType(type);
        token.setDsTokenHash(hash(rawToken));
        token.setDtExpiresAt(LocalDateTime.now().plus(ttl));
        repository.save(token);

        return rawToken;
    }

    private void invalidateExistingTokens(User user, UserTokenTypeEnum type) {
        List<UserToken> existing = repository.findAllByUserAndTpTypeAndDtUsedAtIsNull(user, type);

        if (existing.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        existing.forEach(token -> token.setDtUsedAt(now));
        repository.saveAll(existing);
    }

    // Erro único e genérico pros dois motivos de falha (não existe / já usado / expirado / tipo
    // errado) — não vale a pena diferenciar pro chamador, e evita dar pista de qual é o problema.
    public User consume(String rawToken, UserTokenTypeEnum type) {
        UserToken token = repository.findByDsTokenHashAndDtUsedAtIsNull(hash(rawToken))
                .filter(t -> t.getTpType() == type)
                .filter(t -> t.getDtExpiresAt().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new RuntimeException("Link inválido ou expirado"));

        token.setDtUsedAt(LocalDateTime.now());
        repository.save(token);

        return token.getUser();
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 não disponível", e);
        }
    }
}
