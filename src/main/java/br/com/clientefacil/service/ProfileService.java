package br.com.clientefacil.service;

import br.com.clientefacil.core.exception.ResourceNotFoundException;
import br.com.clientefacil.core.support.SortBuilder;
import br.com.clientefacil.domain.config.ResourceEnum;
import br.com.clientefacil.dto.*;
import br.com.clientefacil.entity.Module;
import br.com.clientefacil.entity.Profile;
import br.com.clientefacil.entity.ProfilePermission;
import br.com.clientefacil.entity.Resource;
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
                .orElse(Collections.emptyList())
                .stream()
                .filter(ProfilePermissionRequest::hasPermission)
                .toList();

        Map<Long, ProfilePermission> existingByResourceId = profilePermissionRepository.findAllByProfileId(profile.getId())
                .stream()
                .collect(Collectors.toMap(
                        permission -> permission.getResource().getId(),
                        Function.identity()
                ));

        Set<Long> requestedResourceIds = requested.stream()
                .map(ProfilePermissionRequest::resourceId)
                .collect(Collectors.toSet());

        List<ProfilePermission> toInsert = requested.stream()
                .map(ProfilePermissionRequest::resourceId)
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
                .map(ProfilePermissionRequest::resourceId)
                .anyMatch(resourceId -> !uniqueResourceIds.add(resourceId));

        if (hasDuplicate) {
            throw new IllegalArgumentException("Não é permitido repetir recurso nas permissões do perfil.");
        }
    }

    public List<ProfilePermissionResponse> findPermissionsByProfile(Long profileId) {

        List<ProfilePermission> profilePermissions = profilePermissionRepository.findAllByProfileId(profileId);
        List<Resource> resources = resourceRepository.findAllWithRelations();

        Map<Long, ProfilePermission> profilePermissionByResourceId = profilePermissions.stream()
                .collect(Collectors.toMap(
                        permission -> permission.getResource().getId(),
                        Function.identity()
                ));

        Map<String, Resource> resourceBySignature = resources.stream()
                .collect(Collectors.toMap(
                        Resource::getSignature,
                        Function.identity()
                ));

        return Arrays.stream(ResourceEnum.values())
                .map(resourceEnum -> {
                    Resource resource = resourceBySignature.get(resourceEnum.getSignature());
                    Module module = resource.getModule();
                    ProfilePermission profilePermission = profilePermissionByResourceId.get(resource.getId());

                    boolean hasPermission = profilePermission != null;

                    return new ProfilePermissionResponse(
                            hasPermission ? profilePermission.getId() : null,
                            resource.getId(),
                            resource.getName(),
                            resource.getSignature(),
                            module.getId(),
                            module.getName(),
                            hasPermission
                    );
                })
                .toList();
    }
}