package br.com.clientefacil.core.support;

import br.com.clientefacil.core.dto.search.SearchRequest;
import br.com.clientefacil.core.dto.search.SortRequest;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class SortBuilder {

    private static final String DEFAULT_SORT_FIELD = "id";

    private SortBuilder() {
    }

    public static Sort fromRequest(SearchRequest request, Map<String, String> sortFields) {
        if (request == null || request.sorts() == null || request.sorts().isEmpty()) {
            return defaultSort();
        }

        List<Sort.Order> orders = new ArrayList<>();

        for (SortRequest sortRequest : request.sorts()) {
            if (sortRequest == null || sortRequest.field() == null || sortRequest.field().isBlank()) {
                continue;
            }

            String mappedField = sortFields.get(sortRequest.field());

            if (mappedField == null || mappedField.isBlank()) {
                continue;
            }

            Sort.Direction direction = resolveDirection(sortRequest);
            orders.add(new Sort.Order(direction, mappedField));
        }

        return orders.isEmpty() ? defaultSort() : Sort.by(orders);
    }

    private static Sort.Direction resolveDirection(SortRequest sortRequest) {
        if (sortRequest.direction() != null
                && "DESC".equalsIgnoreCase(sortRequest.direction().name())) {
            return Sort.Direction.DESC;
        }

        return Sort.Direction.ASC;
    }

    private static Sort defaultSort() {
        return Sort.by(Sort.Direction.ASC, DEFAULT_SORT_FIELD);
    }
}