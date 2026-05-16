package br.com.clientefacil.search;

import br.com.clientefacil.core.dto.search.FilterRequest;
import br.com.clientefacil.core.dto.search.SearchField;
import br.com.clientefacil.core.dto.search.SearchFieldType;
import br.com.clientefacil.core.support.SpecificationBuilder;
import br.com.clientefacil.entity.Client;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Map;

public final class ClientSearchConfig {

    private ClientSearchConfig() {
    }

    public static final Map<String, SearchField> FILTER_FIELDS = Map.of(
            "id", new SearchField("id", SearchFieldType.LONG),
            "personId", new SearchField("person.id", SearchFieldType.LONG),
            "personName", new SearchField("person.name", SearchFieldType.STRING)
    );

    public static final Map<String, String> SORT_FIELDS = Map.of(
            "id", "id",
            "personId", "person.id",
            "personName", "person.name"
    );

    public static Specification<Client> byFilters(List<FilterRequest> filters) {
        return SpecificationBuilder.fromFilters(filters, FILTER_FIELDS);
    }
}