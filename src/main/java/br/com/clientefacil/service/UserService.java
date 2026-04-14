package br.com.clientefacil.service;

import br.com.clientefacil.core.exception.ResourceNotFoundException;
import br.com.clientefacil.dto.UserRequest;
import br.com.clientefacil.dto.UserResponse;
import br.com.clientefacil.entity.User;
import br.com.clientefacil.mapper.UserMapper;
import br.com.clientefacil.repository.PersonRepository;
import br.com.clientefacil.repository.ProfileRepository;
import br.com.clientefacil.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    public Page<UserResponse> findAll(
            int page,
            int size,
            String sort,
            String direction
    ) {
        Sort.Direction dir = Sort.Direction.fromOptionalString(direction)
                .orElse(Sort.Direction.ASC);

        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, sort));

        return repository.findAllWithRelations(pageable)
                .map(mapper::toResponse);
    }

    public UserResponse findById(Long id) {
        return mapper.toResponse(findEntityById(id));
    }

    public UserResponse create(UserRequest request) {
        User user = new User();
        fillEntityByRequest(user, request, true);
        return mapper.toResponse(repository.save(user));
    }

    public UserResponse update(Long id, UserRequest request) {
        User user = findEntityById(id);
        fillEntityByRequest(user, request, false);
        return mapper.toResponse(repository.save(user));
    }

    public void delete(Long id) {
        repository.delete(findEntityById(id));
    }

    private User findEntityById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
    }

    private void fillEntityByRequest(
            User user,
            UserRequest request,
            boolean isCreate
    ) {
        var person = personRepository.getReferenceById(request.personId());
        var profile = profileRepository.getReferenceById(request.profileId());

        user.setName(request.name());
        user.setEmail(request.email());
        user.setPerson(person);
        user.setProfile(profile);

        if (isCreate) {
            user.setPassword(passwordEncoder.encode(request.password()));
            return;
        }

        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }
    }
}