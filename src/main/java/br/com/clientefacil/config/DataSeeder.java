package br.com.clientefacil.config;

import br.com.clientefacil.entity.Company;
import br.com.clientefacil.entity.Person;
import br.com.clientefacil.entity.Profile;
import br.com.clientefacil.entity.User;
import br.com.clientefacil.entity.enums.PersonGenderEnum;
import br.com.clientefacil.repository.CompanyRepository;
import br.com.clientefacil.repository.PersonRepository;
import br.com.clientefacil.repository.ProfileRepository;
import br.com.clientefacil.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataSeeder {

    @Bean
    public CommandLineRunner seedData(
            CompanyRepository companyRepository,
            PersonRepository personRepository,
            ProfileRepository profileRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {

            // ========================================
            // 1. COMPANY (SEM PERSON)
            // ========================================
            Company company = companyRepository.findByName("Cliente Fácil").orElseGet(() -> {
                Company c = new Company();
                c.setName("Cliente Fácil");
                return companyRepository.save(c);
            });

            // ========================================
            // 2. PERSON (AGORA COM COMPANY VIA TENANT)
            // ========================================
            Person person = personRepository.findByDsDocument("000.000.000-00").orElseGet(() -> {
                Person p = new Person();
                p.setName("Administrador Pessoa");
                p.setDsDocument("000.000.000-00");
                p.setTpGender(PersonGenderEnum.M);

                // 🔥 ESSENCIAL
                p.getTenant().setCompany(company);

                return personRepository.save(p);
            });

            // ========================================
            // 3. ATUALIZA COMPANY COM PERSON
            // ========================================
            if (company.getPerson() == null) {
                company.setPerson(person);
                companyRepository.save(company);
            }

            // ========================================
            // 4. PROFILE
            // ========================================
            Profile profile = profileRepository.findByName("Admin").orElseGet(() -> {
                Profile p = new Profile();
                p.setName("Admin");

                p.getTenant().setCompany(company);

                return profileRepository.save(p);
            });

            // ========================================
            // 5. USER
            // ========================================
            if (userRepository.findByEmail("admin@admin.com").isEmpty()) {
                User userAdmin = new User();
                userAdmin.setName("Administrador");
                userAdmin.setEmail("admin@admin.com");
                userAdmin.setPassword(passwordEncoder.encode("123456"));
                userAdmin.setPerson(person);
                userAdmin.setProfile(profile);

                userAdmin.getTenant().setCompany(company);

                userRepository.save(userAdmin);

                System.out.println("✅ Usuário admin criado");
            }
        };
    }
}