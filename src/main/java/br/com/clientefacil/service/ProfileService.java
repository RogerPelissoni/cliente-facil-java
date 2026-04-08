package br.com.clientefacil.service;

import br.com.clientefacil.core.CoreService;
import br.com.clientefacil.dto.ProfilePermissionRequest;
import br.com.clientefacil.dto.ProfileRequest;
import br.com.clientefacil.dto.ProfileResponse;
import br.com.clientefacil.entity.Profile;
import br.com.clientefacil.entity.ProfilePermission;
import br.com.clientefacil.mapper.ProfileMapper;
import br.com.clientefacil.repository.ProfilePermissionRepository;
import br.com.clientefacil.repository.ProfileRepository;
import br.com.clientefacil.repository.ResourceRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfileService extends CoreService<Profile, ProfileResponse, ProfileMapper, Long> {

    private final ProfileRepository repository;
    private final ResourceRepository resourceRepository;
    private final ProfilePermissionRepository profilePermissionRepository;
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

    @Transactional
    public ProfileResponse create(ProfileRequest request) {
        validateDuplicateResources(request);

        Profile entity = new Profile();
        fillEntityByRequest(entity, request);

        Profile savedProfile = repository.save(entity);

        syncPermissions(savedProfile, request);

        return mapper.toResponse(savedProfile);
    }

    @Transactional
    public ProfileResponse update(Long id, ProfileRequest request) {
        validateDuplicateResources(request);

        Profile entity = findEntityById(id);
        fillEntityByRequest(entity, request);

        Profile savedProfile = repository.save(entity);

        syncPermissions(savedProfile, request);

        return mapper.toResponse(savedProfile);
    }

    private void fillEntityByRequest(Profile entity, ProfileRequest request) {
        entity.setName(request.name());
    }

    private void syncPermissions(Profile profile, ProfileRequest request) {
        List<ProfilePermissionRequest> reqPermissions = Optional.ofNullable(request.profilePermissions()).orElse(Collections.emptyList());

        Map<Long, ProfilePermission> permissionByResourceId = profilePermissionRepository.findAllByProfileId(profile.getId())
                .stream()
                .collect(Collectors.toMap(
                        permission -> permission.getResource().getId(),
                        Function.identity()
                ));

        List<ProfilePermission> permissionsToSave = new ArrayList<>();
        Set<Long> idsToDelete = new HashSet<>();

        for (ProfilePermissionRequest item : reqPermissions) {
            Long idResource = item.idResource();
            idsToDelete.add(idResource);

            ProfilePermission permission = permissionByResourceId.get(idResource);

            if (permission == null) {
                permission = new ProfilePermission();
                permission.setProfile(profile);
                permission.setResource(resourceRepository.getReferenceById(idResource));
            }

            permission.setNrPermissionLevel(item.nrPermissionLevel());
            permissionsToSave.add(permission);
        }

        List<ProfilePermission> permissionToDelete = permissionByResourceId.values().stream()
                .filter(permission -> !idsToDelete.contains(permission.getResource().getId()))
                .toList();

        profilePermissionRepository.deleteAll(permissionToDelete);
        profilePermissionRepository.saveAll(permissionsToSave);
    }

    private void validateDuplicateResources(ProfileRequest request) {
        List<ProfilePermissionRequest> permissions = Optional.ofNullable(request.profilePermissions()).orElse(Collections.emptyList());

        Set<Long> uniqueResourceIds = new HashSet<>();

        boolean hasDuplicate = permissions.stream()
                .map(ProfilePermissionRequest::idResource)
                .anyMatch(resourceId -> !uniqueResourceIds.add(resourceId));

        if (hasDuplicate) {
            throw new IllegalArgumentException("Não é permitido repetir recurso nas permissões do perfil.");
        }
    }
}