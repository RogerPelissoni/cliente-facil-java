package br.com.clientefacil.service;

import br.com.clientefacil.core.CoreService;
import br.com.clientefacil.dto.UserRequest;
import br.com.clientefacil.dto.UserResponse;
import br.com.clientefacil.entity.User;
import br.com.clientefacil.mapper.UserMapper;
import br.com.clientefacil.repository.PersonRepository;
import br.com.clientefacil.repository.ProfileRepository;
import br.com.clientefacil.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService extends CoreService<User, UserResponse, Long> {

    private final UserRepository repository;
    private final PersonRepository personRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper mapper;

    @Override
    protected JpaRepository<User, Long> getRepository() {
        return repository;
    }

    @Override
    protected UserResponse toResponse(User entity) {
        return mapper.toResponse(entity);
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
        User user = findEntityById(id);

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
}
