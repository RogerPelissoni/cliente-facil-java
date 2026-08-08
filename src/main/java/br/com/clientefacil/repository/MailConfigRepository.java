package br.com.clientefacil.repository;

import br.com.clientefacil.entity.MailConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MailConfigRepository extends JpaRepository<MailConfig, Long> {

    Optional<MailConfig> findByCompanyId(Long companyId);

    Optional<MailConfig> findByCompanyIdIsNull();
}
