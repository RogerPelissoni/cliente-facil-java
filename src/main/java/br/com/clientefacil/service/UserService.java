package br.com.clientefacil.service;

import br.com.clientefacil.core.dto.UserRoleEnum;
import br.com.clientefacil.core.exception.ResourceNotFoundException;
import br.com.clientefacil.core.support.SortBuilder;
import br.com.clientefacil.dto.ChangePasswordRequest;
import br.com.clientefacil.dto.DefaultSearchRequest;
import br.com.clientefacil.dto.UserRequest;
import br.com.clientefacil.dto.UserResponse;
import br.com.clientefacil.entity.User;
import br.com.clientefacil.entity.enums.UserTokenTypeEnum;
import br.com.clientefacil.mapper.UserMapper;
import br.com.clientefacil.messaging.template.EmailConfirmationTemplate;
import br.com.clientefacil.repository.PersonRepository;
import br.com.clientefacil.repository.ProfileRepository;
import br.com.clientefacil.repository.UserRepository;
import br.com.clientefacil.search.UserSearchConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserService {

    // Convite pode demorar a ser aberto (diferente do reset de senha, que é mais urgente) — ver
    // UserTokenService.
    private static final Duration EMAIL_CONFIRMATION_TTL = Duration.ofDays(7);

    private final UserRepository repository;
    private final PersonRepository personRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper mapper;
    private final UserTokenService userTokenService;
    private final EmailService emailService;
    private final String frontendUrl;

    public UserService(
            UserRepository repository,
            PersonRepository personRepository,
            ProfileRepository profileRepository,
            PasswordEncoder passwordEncoder,
            UserMapper mapper,
            UserTokenService userTokenService,
            EmailService emailService,
            @Value("${app.frontend-url}") String frontendUrl
    ) {
        this.repository = repository;
        this.personRepository = personRepository;
        this.profileRepository = profileRepository;
        this.passwordEncoder = passwordEncoder;
        this.mapper = mapper;
        this.userTokenService = userTokenService;
        this.emailService = emailService;
        this.frontendUrl = frontendUrl;
    }

    public Map<Long, String> keyValue() {
        return repository.keyValue()
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (String) row[1]
                ));
    }

    public Page<UserResponse> search(DefaultSearchRequest request) {
        Pageable pageable = PageRequest.of(
                request.pageOrDefault(),
                request.sizeOrDefault(),
                SortBuilder.fromRequest(request, UserSearchConfig.SORT_FIELDS)
        );

        Specification<User> specification = UserSearchConfig.byFilters(request.filters());

        return repository.findAll(specification, pageable)
                .map(mapper::toResponse);
    }

    public Page<UserResponse> findAll(
            int page,
            int size,
            String sort,
            String direction
    ) {
        Sort.Direction dir = Sort.Direction.fromOptionalString(direction)
                .orElse(Sort.Direction.ASC);

        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, sort));

        return repository.findAllWithRelations(pageable)
                .map(mapper::toResponse);
    }

    public UserResponse findById(Long id) {
        return mapper.toResponse(findEntityById(id));
    }

    public UserResponse create(UserRequest request) {
        User user = new User();
        fillEntityByRequest(user, request, true);
        User saved = repository.save(user);

        sendConfirmationEmail(saved);

        return mapper.toResponse(saved);
    }

    public UserResponse update(Long id, UserRequest request) {
        User user = findEntityById(id);
        fillEntityByRequest(user, request, false);
        return mapper.toResponse(repository.save(user));
    }

    public void delete(Long id) {
        repository.delete(findEntityById(id));
    }

    // Self-service: diferente de update(), aqui a senha atual precisa ser confirmada antes de
    // aceitar a troca — não passa por fillEntityByRequest porque essa é a única mudança permitida
    // (não altera nome/e-mail/perfil).
    public void changeMyPassword(Long userId, ChangePasswordRequest request) {
        User user = findEntityById(userId);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new RuntimeException("Senha atual incorreta");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        repository.save(user);
    }

    // Reenvia o e-mail de confirmação (ex: usuário perdeu o primeiro, ou o e-mail nunca chegou) —
    // usado pelo botão na tela de usuários (ver UserController), visível só quando ainda não
    // confirmado.
    public void resendConfirmation(Long id) {
        User user = findEntityById(id);

        if (user.getDtEmailConfirmedAt() != null) {
            throw new RuntimeException("E-mail já confirmado");
        }

        sendConfirmationEmail(user);
    }

    // Best-effort, igual ao resto do projeto que dispara e-mail a partir de um evento de negócio
    // (ver NotificationDeadLetterListener.notifyAdmins): uma falha ao enfileirar não pode impedir a
    // criação do usuário.
    private void sendConfirmationEmail(User user) {
        try {
            String token = userTokenService.issue(user, UserTokenTypeEnum.EMAIL_CONFIRMATION, EMAIL_CONFIRMATION_TTL);
            var template = new EmailConfirmationTemplate(frontendUrl + "/auth/confirm-email?token=" + token);

            emailService.sendTemplated(user.getCompanyId(), user.getEmail(), "Cliente Fácil — Confirme seu e-mail", template);
        } catch (Exception e) {
            log.warn("Não foi possível enviar o e-mail de confirmação para {}", user.getEmail(), e);
        }
    }

    private User findEntityById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
    }

    private void fillEntityByRequest(
            User user,
            UserRequest request,
            boolean isCreate
    ) {
        var person = personRepository.getReferenceById(request.personId());
        var profile = profileRepository.getReferenceById(request.profileId());

        user.setName(request.name());
        user.setEmail(request.email());
        user.setRole(UserRoleEnum.valueOf(request.role()));
        user.setPerson(person);
        user.setProfile(profile);
        user.setCompanyId(request.companyId());

        if (isCreate) {
            user.setPassword(passwordEncoder.encode(request.password()));
            return;
        }

        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }
    }
}