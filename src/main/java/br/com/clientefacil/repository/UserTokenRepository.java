package br.com.clientefacil.repository;

import br.com.clientefacil.entity.UserToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserTokenRepository extends JpaRepository<UserToken, Long> {

    Optional<UserToken> findByDsTokenHashAndDtUsedAtIsNull(String dsTokenHash);
}
