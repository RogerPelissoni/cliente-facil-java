package br.com.clientefacil.mapper;

import br.com.clientefacil.core.mapper.CoreMapper;
import br.com.clientefacil.dto.ProfileResponse;
import br.com.clientefacil.entity.Profile;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProfileMapper extends CoreMapper<Profile, ProfileResponse> {

    @Override
    ProfileResponse toResponse(Profile company);

    @Override
    List<ProfileResponse> toResponseList(List<Profile> profiles);

}