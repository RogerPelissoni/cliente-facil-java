package br.com.clientefacil.core.service;

import br.com.clientefacil.core.dto.AuthRequest;
import br.com.clientefacil.core.dto.AuthResponse;
import br.com.clientefacil.core.dto.ResetPasswordRequest;
import br.com.clientefacil.core.exception.TooManyRequestsException;
import br.com.clientefacil.core.security.RateLimiter;
import br.com.clientefacil.entity.User;
import br.com.clientefacil.entity.enums.UserTokenTypeEnum;
import br.com.clientefacil.messaging.template.PasswordResetTemplate;
import br.com.clientefacil.repository.UserRepository;
import br.com.clientefacil.service.EmailService;
import br.com.clientefacil.service.UserTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Service
public class AuthService {

    // Reset de senha é mais urgente/sensível que confirmação de e-mail — janela bem mais curta.
    private static final Duration PASSWORD_RESET_TTL = Duration.ofHours(1);

    // Rate limit: throttla a TAXA de tentativas por e-mail (não por IP — o backend só enxerga o
    // proxy same-origin do Next.js como origem de rede hoje; rate-limitar por IP juntaria todo
    // mundo que passa por ele no mesmo balde). Reseta sozinho ao sair da janela — não precisa de
    // ação nenhuma pra "destravar".
    private static final int LOGIN_RATE_LIMIT_MAX_ATTEMPTS = 5;
    private static final Duration LOGIN_RATE_LIMIT_WINDOW = Duration.ofMinutes(1);
    private static final int FORGOT_PASSWORD_RATE_LIMIT_MAX_ATTEMPTS = 3;
    private static final Duration FORGOT_PASSWORD_RATE_LIMIT_WINDOW = Duration.ofMinutes(15);

    // Bloqueio de conta: diferente do rate limit acima — conta especificamente FALHAS de senha
    // (login certo zera o contador), persiste no próprio User (sobrevive a restart da aplicação,
    // ao contrário do rate limit, que é só em memória) e complementa o rate limit contra quem
    // distribui as tentativas ao longo de várias janelas de tempo.
    private static final int LOGIN_LOCKOUT_MAX_ATTEMPTS = 5;
    private static final Duration LOGIN_LOCKOUT_DURATION = Duration.ofMinutes(15);

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserTokenService userTokenService;
    private final EmailService emailService;
    private final RateLimiter rateLimiter;
    private final String frontendUrl;

    public AuthService(
            UserRepository repository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            UserTokenService userTokenService,
            EmailService emailService,
            RateLimiter rateLimiter,
            @Value("${app.frontend-url}") String frontendUrl
    ) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userTokenService = userTokenService;
        this.emailService = emailService;
        this.rateLimiter = rateLimiter;
        this.frontendUrl = frontendUrl;
    }

    public AuthResponse login(AuthRequest request) {
        String email = request.email();

        // 1. Rate limit — antes até de olhar se o usuário existe, senão um e-mail que não existe
        // vira um jeito de descobrir isso (só ele nunca levaria 429 depois de N tentativas).
        if (!rateLimiter.tryConsume(rateLimitKey("login", email), LOGIN_RATE_LIMIT_MAX_ATTEMPTS, LOGIN_RATE_LIMIT_WINDOW)) {
            throw new TooManyRequestsException("Muitas tentativas de login. Aguarde um momento e tente novamente.");
        }

        // 2. Buscar usuário — mesma mensagem genérica de senha errada (ver abaixo); mensagens
        // diferentes pra "não existe" e "senha errada" permitiam descobrir quais e-mails têm conta.
        User user = repository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Credenciais inválidas"));

        // 3. Conta bloqueada por excesso de tentativas (ver registerFailedLoginAttempt) — checa
        // antes de validar a senha, senão o tempo de resposta ainda vazaria se a senha bateria.
        if (user.getDtLockedUntil() != null && user.getDtLockedUntil().isAfter(LocalDateTime.now())) {
            throw new RuntimeException(
                    "Conta temporariamente bloqueada por excesso de tentativas de login. Tente novamente mais tarde.");
        }

        // 4. Validar senha
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            registerFailedLoginAttempt(user);
            throw new RuntimeException("Credenciais inválidas");
        }

        if (user.getNrFailedLoginAttempts() > 0) {
            user.setNrFailedLoginAttempts(0);
            repository.save(user);
        }

        // 5. E-mail precisa estar confirmado (ver UserService.sendConfirmationEmail, disparado na
        // criação do usuário)
        if (user.getDtEmailConfirmedAt() == null) {
            throw new RuntimeException(
                    "E-mail ainda não confirmado. Verifique sua caixa de entrada ou peça um reenvio ao administrador.");
        }

        // 6. Gerar token
        String token = jwtService.generateToken(user.getEmail());

        return new AuthResponse(token);
    }

    private void registerFailedLoginAttempt(User user) {
        int attempts = user.getNrFailedLoginAttempts() + 1;

        if (attempts >= LOGIN_LOCKOUT_MAX_ATTEMPTS) {
            // zera o contador junto — quando o bloqueio expirar, a próxima sequência de tentativas
            // começa do zero, não já "quase bloqueada" de novo.
            user.setNrFailedLoginAttempts(0);
            user.setDtLockedUntil(LocalDateTime.now().plus(LOGIN_LOCKOUT_DURATION));
        } else {
            user.setNrFailedLoginAttempts(attempts);
        }

        repository.save(user);
    }

    private String rateLimitKey(String scope, String email) {
        return scope + ":" + email.trim().toLowerCase();
    }

    public void confirmEmail(String token) {
        User user = userTokenService.consume(token, UserTokenTypeEnum.EMAIL_CONFIRMATION);
        user.setDtEmailConfirmedAt(LocalDateTime.now());
        repository.save(user);
    }

    // Sempre "sucesso" do ponto de vista do chamador, exista ou não o e-mail — evita que esse
    // endpoint sirva pra descobrir quais e-mails têm conta no sistema (enumeração de usuários).
    public void forgotPassword(String email) {
        // Antes do lookup, pelo mesmo motivo do rate limit de login: aplicar por igual a e-mails
        // que existem ou não é o que mantém as duas respostas indistinguíveis.
        if (!rateLimiter.tryConsume(rateLimitKey("forgot-password", email), FORGOT_PASSWORD_RATE_LIMIT_MAX_ATTEMPTS, FORGOT_PASSWORD_RATE_LIMIT_WINDOW)) {
            throw new TooManyRequestsException("Muitos pedidos de recuperação de senha. Aguarde um momento e tente novamente.");
        }

        repository.findByEmail(email).ifPresentOrElse(user -> {
            String token = userTokenService.issue(user, UserTokenTypeEnum.PASSWORD_RESET, PASSWORD_RESET_TTL);
            var template = new PasswordResetTemplate(frontendUrl + "/auth/reset-password?token=" + token);

            emailService.sendTemplated(user.getCompanyId(), user.getEmail(), "Cliente Fácil — Recuperação de senha", template);
        }, () -> log.info("Pedido de recuperação de senha para e-mail não cadastrado: {}", email));
    }

    public void resetPassword(ResetPasswordRequest request) {
        User user = userTokenService.consume(request.token(), UserTokenTypeEnum.PASSWORD_RESET);
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        repository.save(user);
    }
}
