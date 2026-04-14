package br.com.clientefacil.core.dto.search;

public record SortRequest(
        String field,
        SortDirection direction
) {
}
