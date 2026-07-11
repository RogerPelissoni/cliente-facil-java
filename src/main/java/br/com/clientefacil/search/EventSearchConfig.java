package br.com.clientefacil.search;

import br.com.clientefacil.core.dto.search.FilterRequest;
import br.com.clientefacil.core.dto.search.SearchField;
import br.com.clientefacil.core.dto.search.SearchFieldType;
import br.com.clientefacil.core.support.SpecificationBuilder;
import br.com.clientefacil.entity.Event;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Map;

public final class EventSearchConfig {

    private EventSearchConfig() {
    }

    public static final Map<String, SearchField> FILTER_FIELDS = Map.of(
            "id", new SearchField("id", SearchFieldType.LONG)
    );

    public static final Map<String, String> SORT_FIELDS = Map.of(
            "id", "id"
    );

    public static Specification<Event> byFilters(List<FilterRequest> filters) {
        return SpecificationBuilder.fromFilters(filters, FILTER_FIELDS);
    }
}