package br.com.clientefacil.core.dto;

import java.util.List;

public record AuthenticatedUserResponse(
        Long id,
        String email,
        List<String> authorities
) {
}
