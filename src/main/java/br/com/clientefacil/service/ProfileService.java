package br.com.clientefacil.service;

import br.com.clientefacil.core.exception.ResourceNotFoundException;
import br.com.clientefacil.core.support.SortBuilder;
import br.com.clientefacil.dto.DefaultSearchRequest;
import br.com.clientefacil.dto.ProfilePermissionRequest;
import br.com.clientefacil.dto.ProfileRequest;
import br.com.clientefacil.dto.ProfileResponse;
import br.com.clientefacil.entity.Profile;
import br.com.clientefacil.entity.ProfilePermission;
import br.com.clientefacil.mapper.ProfileMapper;
import br.com.clientefacil.repository.ProfilePermissionRepository;
import br.com.clientefacil.repository.ProfileRepository;
import br.com.clientefacil.repository.ResourceRepository;
import br.com.clientefacil.search.ProfileSearchConfig;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository repository;
    private final ResourceRepository resourceRepository;
    private final ProfilePermissionRepository profilePermissionRepository;
    private final ProfileMapper mapper;

    public Page<ProfileResponse> search(DefaultSearchRequest request) {
        Pageable pageable = PageRequest.of(
                request.pageOrDefault(),
                request.sizeOrDefault(),
                SortBuilder.fromRequest(request, ProfileSearchConfig.SORT_FIELDS)
        );

        Specification<Profile> specification = ProfileSearchConfig.byFilters(request.filters());

        return repository.findAll(specification, pageable)
                .map(mapper::toResponse);
    }

    public Page<ProfileResponse> findAll(
            int page,
            int size,
            String sort,
            String direction
    ) {
        Sort.Direction dir = Sort.Direction.fromOptionalString(direction)
                .orElse(Sort.Direction.ASC);

        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, sort));

        return repository.findAll(pageable)
                .map(mapper::toResponse);
    }

    public ProfileResponse findById(Long id) {
        return mapper.toResponse(findEntityById(id));
    }

    public Map<Long, String> keyValue() {
        return repository.keyValue()
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (String) row[1]
                ));
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

    @Transactional
    public void delete(Long id) {
        repository.delete(findEntityById(id));
    }

    private Profile findEntityById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil não encontrado(a)"));
    }

    private void fillEntityByRequest(Profile entity, ProfileRequest request) {
        entity.setName(request.name());
    }

    private void syncPermissions(Profile profile, ProfileRequest request) {
        List<ProfilePermissionRequest> requested = Optional.ofNullable(request.profilePermissions())
                .orElse(Collections.emptyList());

        Map<Long, ProfilePermission> existingByResourceId = profilePermissionRepository.findAllByProfileId(profile.getId())
                .stream()
                .collect(Collectors.toMap(
                        permission -> permission.getResource().getId(),
                        Function.identity()
                ));

        Set<Long> requestedResourceIds = requested.stream()
                .map(ProfilePermissionRequest::idResource)
                .collect(Collectors.toSet());

        List<ProfilePermission> toInsert = requested.stream()
                .map(ProfilePermissionRequest::idResource)
                .filter(resourceId -> !existingByResourceId.containsKey(resourceId))
                .map(resourceId -> {
                    ProfilePermission permission = new ProfilePermission();
                    permission.setProfile(profile);
                    permission.setResource(resourceRepository.getReferenceById(resourceId));
                    return permission;
                })
                .toList();

        List<ProfilePermission> toDelete = existingByResourceId.values().stream()
                .filter(permission -> !requestedResourceIds.contains(permission.getResource().getId()))
                .toList();

        if (!toDelete.isEmpty()) {
            profilePermissionRepository.deleteAll(toDelete);
        }

        if (!toInsert.isEmpty()) {
            profilePermissionRepository.saveAll(toInsert);
        }
    }

    private void validateDuplicateResources(ProfileRequest request) {
        List<ProfilePermissionRequest> permissions = Optional.ofNullable(request.profilePermissions())
                .orElse(Collections.emptyList());

        Set<Long> uniqueResourceIds = new HashSet<>();

        boolean hasDuplicate = permissions.stream()
                .map(ProfilePermissionRequest::idResource)
                .anyMatch(resourceId -> !uniqueResourceIds.add(resourceId));

        if (hasDuplicate) {
            throw new IllegalArgumentException("Não é permitido repetir recurso nas permissões do perfil.");
        }
    }
}