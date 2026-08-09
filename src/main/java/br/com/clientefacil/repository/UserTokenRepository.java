package br.com.clientefacil.repository;

import br.com.clientefacil.entity.User;
import br.com.clientefacil.entity.UserToken;
import br.com.clientefacil.entity.enums.UserTokenTypeEnum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserTokenRepository extends JpaRepository<UserToken, Long> {

    Optional<UserToken> findByDsTokenHashAndDtUsedAtIsNull(String dsTokenHash);

    // Usado por UserTokenService.issue pra invalidar tokens anteriores do mesmo tipo antes de emitir
    // um novo — pedir recuperação de senha duas vezes não pode deixar dois links simultaneamente
    // válidos.
    List<UserToken> findAllByUserAndTpTypeAndDtUsedAtIsNull(User user, UserTokenTypeEnum tpType);
}
