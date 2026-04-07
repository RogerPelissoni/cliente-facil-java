package br.com.clientefacil.service;

import br.com.clientefacil.core.CoreService;
import br.com.clientefacil.dto.ProfileRequest;
import br.com.clientefacil.dto.ProfileResponse;
import br.com.clientefacil.entity.Profile;
import br.com.clientefacil.mapper.ProfileMapper;
import br.com.clientefacil.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProfileService extends CoreService<Profile, ProfileResponse, ProfileMapper, Long> {

    private final ProfileRepository repository;
    private final ProfileMapper mapper;

    @Override
    protected JpaRepository<Profile, Long> getRepository() {
        return repository;
    }

    @Override
    protected ProfileMapper getMapper() {
        return mapper;
    }

    @Override
    protected String getEntityName() {
        return "Perfil";
    }

    public ProfileResponse create(ProfileRequest request) {
        Profile entity = new Profile();
        fillEntityByRequest(entity, request);
        return mapper.toResponse(repository.save(entity));
    }

    public ProfileResponse update(Long id, ProfileRequest request) {
        Profile entity = findEntityById(id);
        fillEntityByRequest(entity, request);
        return mapper.toResponse(repository.save(entity));
    }

    private void fillEntityByRequest(Profile entity, ProfileRequest request) {
        entity.setName(request.name());
    }
}