package br.com.clientefacil.seeder;

import br.com.clientefacil.core.dto.UserRoleEnum;
import br.com.clientefacil.entity.*;
import br.com.clientefacil.entity.enums.MailEncryptionType;
import br.com.clientefacil.entity.enums.PersonGenderEnum;
import br.com.clientefacil.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class MainSeeder {

    private final ResourceRepository resourceRepository;
    private final ProfilePermissionRepository profilePermissionRepository;

    private final String defaultMailHost;
    private final int defaultMailPort;
    private final String defaultMailFromName;
    private final String defaultMailFromAddress;

    public MainSeeder(
            ResourceRepository resourceRepository,
            ProfilePermissionRepository profilePermissionRepository,
            @Value("${mail.config.default.host}") String defaultMailHost,
            @Value("${mail.config.default.port}") int defaultMailPort,
            @Value("${mail.config.default.from-name}") String defaultMailFromName,
            @Value("${mail.config.default.from-address}") String defaultMailFromAddress
    ) {
        this.resourceRepository = resourceRepository;
        this.profilePermissionRepository = profilePermissionRepository;
        this.defaultMailHost = defaultMailHost;
        this.defaultMailPort = defaultMailPort;
        this.defaultMailFromName = defaultMailFromName;
        this.defaultMailFromAddress = defaultMailFromAddress;
    }

    @Bean
    public CommandLineRunner seedData(
            CompanyRepository companyRepository,
            PersonRepository personRepository,
            ProfileRepository profileRepository,
            UserRepository userRepository,
            MailConfigRepository mailConfigRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            Company company = companyRepository.findByName("Cliente Facil").orElseGet(() -> {
                Company createdCompany = new Company();
                createdCompany.setName("Cliente Facil");
                return companyRepository.save(createdCompany);
            });

            Person person = personRepository.findByDsDocument("000.000.000-00").orElseGet(() -> {
                Person createdPerson = new Person();
                createdPerson.setName("Administrador Pessoa");
                createdPerson.setDsDocument("000.000.000-00");
                createdPerson.setTpGender(PersonGenderEnum.M);
                createdPerson.setCompanyId(company.getId());
                return personRepository.save(createdPerson);
            });

            if (company.getPerson() == null) {
                company.setPerson(person);
                companyRepository.save(company);
            }

            Profile profile = profileRepository.findByName("Admin").orElseGet(() -> {
                Profile createdProfile = new Profile();
                createdProfile.setName("Admin");
                createdProfile.setCompanyId(company.getId());
                return profileRepository.save(createdProfile);
            });

            if (userRepository.findByEmail("admin@admin.com").isEmpty()) {
                User adminUser = new User();
                adminUser.setName("Administrador");
                adminUser.setEmail("admin@admin.com");
                adminUser.setPassword(passwordEncoder.encode("123456"));
                adminUser.setRole(UserRoleEnum.admin);
                adminUser.setPerson(person);
                adminUser.setProfile(profile);
                adminUser.setCompanyId(company.getId());
                // Sem isso o próprio bootstrap ficaria bloqueado no login (ver AuthService.login) —
                // não existe fluxo de confirmação pra rodar antes do primeiro usuário existir.
                adminUser.setDtEmailConfirmedAt(LocalDateTime.now());
                userRepository.save(adminUser);
            }

            createUserPermissions(profile, company);
            seedBaseMailConfig(mailConfigRepository);
        };
    }

    // Config base (company_id NULL) usada por e-mails do sistema (dead-letter, recuperação de
    // senha, etc — ver MailConfigService). Só cria se ainda não existir nenhuma, pra não sobrescrever
    // uma config que já tenha sido ajustada via PUT /api/v1/mail-configs/base. Aponta pro MailHog em
    // dev/docker por padrão (sem autenticação, sem TLS) — ver docker-compose.yml.
    private void seedBaseMailConfig(MailConfigRepository mailConfigRepository) {
        if (mailConfigRepository.findByCompanyIdIsNull().isPresent()) {
            return;
        }

        MailConfig baseConfig = new MailConfig();
        baseConfig.markAsGlobalScope();
        baseConfig.setDsHost(defaultMailHost);
        baseConfig.setNrPort(defaultMailPort);
        baseConfig.setTpEncryption(MailEncryptionType.NONE);
        baseConfig.setDsFromName(defaultMailFromName);
        baseConfig.setDsFromAddress(defaultMailFromAddress);
        baseConfig.setFlActive(true);
        mailConfigRepository.save(baseConfig);
    }

    private void createUserPermissions(Profile profile, Company company) {
        List<Resource> resources = resourceRepository.findAll();
        List<ProfilePermission> permissionsToSave = new ArrayList<>();

        for (Resource resource : resources) {
            boolean existsProfilePermission = profilePermissionRepository.existsByProfileIdAndResourceId(profile.getId(), resource.getId());

            if (!existsProfilePermission) {
                ProfilePermission profilePermission = new ProfilePermission();
                profilePermission.setProfile(profile);
                profilePermission.setResource(resource);
                profilePermission.setCompanyId(company.getId());
                permissionsToSave.add(profilePermission);
            }
        }

        if (!permissionsToSave.isEmpty()) {
            profilePermissionRepository.saveAll(permissionsToSave);
        }
    }
}
