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

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repository;
    private final PersonRepository personRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper mapper;

    public List<UserResponse> findAll() {
        return mapper.toResponseList(repository.findAll());
    }

    public UserResponse findById(Long id) {
        return mapper.toResponse(findUserById(id));
    }

    public UserResponse create(UserRequest request) {
        var person = personRepository.getReferenceById(request.personId());
        var profile = profileRepository.getReferenceById(request.profileId());

        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setPerson(person);
        user.setProfile(profile);

        User savedUser = repository.save(user);

        return mapper.toResponse(savedUser);
    }

    public UserResponse update(Long id, UserRequest request) {
        User user = findUserById(id);

        var person = personRepository.getReferenceById(request.personId());
        var profile = profileRepository.getReferenceById(request.profileId());

        user.setName(request.name());
        user.setEmail(request.email());

        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }

        user.setPerson(person);
        user.setProfile(profile);

        User updatedUser = repository.save(user);

        return mapper.toResponse(updatedUser);
    }

    public void delete(Long id) {
        repository.delete(findUserById(id));
    }

    private User findUserById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario nao encontrado"));
    }
}
