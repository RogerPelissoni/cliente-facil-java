package br.com.clientefacil.service;

import br.com.clientefacil.dto.UserRequest;
import br.com.clientefacil.entity.User;
import br.com.clientefacil.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository repository,
                       PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public User findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
    }

    public User create(UserRequest request) {

        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());

        // 🔐 senha criptografada (ESSENCIAL)
        user.setPassword(passwordEncoder.encode(request.password()));

        return repository.save(user);
    }
}