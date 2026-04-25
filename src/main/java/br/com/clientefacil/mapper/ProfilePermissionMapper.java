package br.com.clientefacil.mapper;

import br.com.clientefacil.core.mapper.CoreMapper;
import br.com.clientefacil.dto.ProfilePermissionResponse;
import br.com.clientefacil.entity.ProfilePermission;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProfilePermissionMapper extends CoreMapper<ProfilePermission, ProfilePermissionResponse> {

    @Override
    @Mapping(target = "resourceId", source = "resource.id")
    @Mapping(target = "resourceName", source = "resource.name")
    @Mapping(target = "resourceSignature", source = "resource.signature")
    ProfilePermissionResponse toResponse(ProfilePermission profilePermission);

    @Override
    List<ProfilePermissionResponse> toResponseList(List<ProfilePermission> profilePermissions);

}