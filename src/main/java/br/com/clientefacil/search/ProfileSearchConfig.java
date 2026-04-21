package br.com.clientefacil.search;

import br.com.clientefacil.core.dto.search.FilterRequest;
import br.com.clientefacil.core.dto.search.SearchField;
import br.com.clientefacil.core.dto.search.SearchFieldType;
import br.com.clientefacil.core.support.SpecificationBuilder;
import br.com.clientefacil.entity.Profile;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Map;

public final class ProfileSearchConfig {

    private ProfileSearchConfig() {
    }

    public static final Map<String, SearchField> FILTER_FIELDS = Map.of(
            "id", new SearchField("id", SearchFieldType.LONG),
            "name", new SearchField("name", SearchFieldType.STRING)
    );

    public static final Map<String, String> SORT_FIELDS = Map.of(
            "id", "id",
            "name", "name"
    );

    public static Specification<Profile> byFilters(List<FilterRequest> filters) {
        return SpecificationBuilder.fromFilters(filters, FILTER_FIELDS);
    }
}