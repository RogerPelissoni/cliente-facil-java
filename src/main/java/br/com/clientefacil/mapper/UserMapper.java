package br.com.clientefacil.mapper;

import br.com.clientefacil.dto.UserResponse;
import br.com.clientefacil.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(source = "person.name", target = "personName")
    UserResponse toResponse(User user);

    List<UserResponse> toResponseList(List<User> users);
}