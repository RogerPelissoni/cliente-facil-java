package br.com.clientefacil.dto;

import br.com.clientefacil.core.dto.search.FilterRequest;
import br.com.clientefacil.core.dto.search.SearchRequest;
import br.com.clientefacil.core.dto.search.SortRequest;

import java.util.List;

public record UserSearchRequest(
        Integer page,
        Integer size,
        List<SortRequest> sorts,
        List<FilterRequest> filters
) implements SearchRequest {

    public static UserSearchRequest defaultRequest() {
        return new UserSearchRequest(
                0,
                10,
                List.of(),
                List.of()
        );
    }
}