package br.com.clientefacil.dto;

import org.springframework.data.domain.Page;

import java.util.Map;

public record ClientScreenResponse(
        Page<ClientResponse> obClients,
        Map<Long, String> kvPerson
) {
}
