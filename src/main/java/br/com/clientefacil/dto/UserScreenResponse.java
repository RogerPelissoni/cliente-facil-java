package br.com.clientefacil.dto;

import org.springframework.data.domain.Page;

import java.util.Map;

public record UserScreenResponse(
        Page<UserResponse> obUser,
        Map<Long, String> kvPerson,
        Map<Long, String> kvProfile,
        Map<Long, String> kvCompany
) {
}
