package br.com.clientefacil.core.service;

import br.com.clientefacil.core.security.entity.AuthenticatedUser;
import br.com.clientefacil.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticatedUserService {

    private final UserRepository repository;

    public AuthenticatedUser loadByEmail(String email) {
        return repository.findByEmail(email)
                .map(user -> new AuthenticatedUser(
                        user.getId(),
                        user.getCompanyId(),
                        user.getEmail(),
                        user.getPassword(),
                        user.getProfile().getPermissions().stream()
                                .map(permission -> permission.getResource().getSignature())
                                .map(SimpleGrantedAuthority::new)
                                .toList()
                ))
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
    }
}