package br.com.clientefacil.mapper;

import br.com.clientefacil.core.mapper.CoreMapper;
import br.com.clientefacil.dto.UserResponse;
import br.com.clientefacil.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper extends CoreMapper<User, UserResponse> {

    @Override
    @Mapping(source = "person.id", target = "personId")
    @Mapping(source = "profile.id", target = "profileId")
    @Mapping(source = "company.id", target = "companyId")
    @Mapping(source = "person.name", target = "personName")
    @Mapping(source = "profile.name", target = "profileName")
    @Mapping(source = "company.name", target = "companyName")
    UserResponse toResponse(User user);

    @Override
    List<UserResponse> toResponseList(List<User> users);
}