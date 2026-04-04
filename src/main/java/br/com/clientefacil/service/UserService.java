package br.com.clientefacil.service;

import br.com.clientefacil.dto.UserRequest;
import br.com.clientefacil.dto.UserResponse;
import br.com.clientefacil.entity.User;
import br.com.clientefacil.mapper.UserMapper;
import br.com.clientefacil.repository.PersonRepository;
import br.com.clientefacil.repository.ProfileRepository;
import br.com.clientefacil.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repository;
    private final PersonRepository personRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper mapper;

    public UserResponse findById(Long id) {
        User user = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario nao encontrado"));

        return mapper.toResponse(user);
    }

    public UserResponse create(UserRequest request) {
        var person = personRepository.findById(request.personId())
                .orElseThrow(() -> new RuntimeException("Person nao encontrada"));

        var profile = profileRepository.findById(request.profileId())
                .orElseThrow(() -> new RuntimeException("Profile nao encontrado"));

        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setPerson(person);
        user.setProfile(profile);

        User savedUser = repository.save(user);

        return mapper.toResponse(savedUser);
    }
}
