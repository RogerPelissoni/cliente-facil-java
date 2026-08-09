package br.com.clientefacil.core.service;

import br.com.clientefacil.core.dto.AuthRequest;
import br.com.clientefacil.core.dto.AuthResponse;
import br.com.clientefacil.core.dto.ResetPasswordRequest;
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

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserTokenService userTokenService;
    private final EmailService emailService;
    private final String frontendUrl;

    public AuthService(
            UserRepository repository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            UserTokenService userTokenService,
            EmailService emailService,
            @Value("${app.frontend-url}") String frontendUrl
    ) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userTokenService = userTokenService;
        this.emailService = emailService;
        this.frontendUrl = frontendUrl;
    }

    public AuthResponse login(AuthRequest request) {

        // 1. Buscar usuário
        User user = repository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        // 2. Validar senha
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new RuntimeException("Credenciais inválidas");
        }

        // 3. E-mail precisa estar confirmado (ver UserService.sendConfirmationEmail, disparado na
        // criação do usuário)
        if (user.getDtEmailConfirmedAt() == null) {
            throw new RuntimeException(
                    "E-mail ainda não confirmado. Verifique sua caixa de entrada ou peça um reenvio ao administrador.");
        }

        // 4. Gerar token
        String token = jwtService.generateToken(user.getEmail());

        return new AuthResponse(token);
    }

    public void confirmEmail(String token) {
        User user = userTokenService.consume(token, UserTokenTypeEnum.EMAIL_CONFIRMATION);
        user.setDtEmailConfirmedAt(LocalDateTime.now());
        repository.save(user);
    }

    // Sempre "sucesso" do ponto de vista do chamador, exista ou não o e-mail — evita que esse
    // endpoint sirva pra descobrir quais e-mails têm conta no sistema (enumeração de usuários).
    public void forgotPassword(String email) {
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
