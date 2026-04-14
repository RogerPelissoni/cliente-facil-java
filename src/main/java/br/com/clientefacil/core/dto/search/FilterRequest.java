package br.com.clientefacil.core.dto.search;

import java.util.List;

public record FilterRequest(
        String field,
        FilterOperator operator,
        String value,
        List<String> values
) {
}