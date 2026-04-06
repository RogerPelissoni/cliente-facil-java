package br.com.clientefacil.service;

import br.com.clientefacil.repository.UserRepository;
import br.com.clientefacil.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.List;

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
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                ))
                .orElseThrow(() -> new RuntimeException("Usuario nao encontrado"));
    }
}
