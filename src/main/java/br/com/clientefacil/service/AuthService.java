package br.com.clientefacil.service;

import br.com.clientefacil.dto.AuthRequest;
import br.com.clientefacil.dto.AuthResponse;
import br.com.clientefacil.entity.User;
import br.com.clientefacil.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse login(AuthRequest request) {

        // 1. Buscar usuário
        User user = repository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        // 2. Validar senha
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new RuntimeException("Credenciais inválidas");
        }

        // 3. Gerar token
        String token = jwtService.generateToken(user.getEmail());

        return new AuthResponse(token);
    }
}