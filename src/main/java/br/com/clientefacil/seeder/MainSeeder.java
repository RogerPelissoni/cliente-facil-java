package br.com.clientefacil.seeder;

import br.com.clientefacil.core.dto.UserRoleEnum;
import br.com.clientefacil.entity.*;
import br.com.clientefacil.entity.enums.PersonGenderEnum;
import br.com.clientefacil.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class MainSeeder {

    private final ResourceRepository resourceRepository;
    private final ProfilePermissionRepository profilePermissionRepository;

    public MainSeeder(ResourceRepository resourceRepository, ProfilePermissionRepository profilePermissionRepository) {
        this.resourceRepository = resourceRepository;
        this.profilePermissionRepository = profilePermissionRepository;
    }

    @Bean
    public CommandLineRunner seedData(
            CompanyRepository companyRepository,
            PersonRepository personRepository,
            ProfileRepository profileRepository,
            UserRepository userRepository,
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
                userRepository.save(adminUser);
            }

            createUserPermissions(profile, company);
        };
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
