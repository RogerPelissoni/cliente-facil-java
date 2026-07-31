package br.com.clientefacil.search;

import br.com.clientefacil.core.dto.search.FilterRequest;
import br.com.clientefacil.core.dto.search.SearchField;
import br.com.clientefacil.core.dto.search.SearchFieldType;
import br.com.clientefacil.core.support.SpecificationBuilder;
import br.com.clientefacil.entity.NotificationDeadLetter;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Map;

public final class NotificationDeadLetterSearchConfig {

    private NotificationDeadLetterSearchConfig() {
    }

    public static final Map<String, SearchField> FILTER_FIELDS = Map.of(
            "id", new SearchField("id", SearchFieldType.LONG),
            "dsErrorReason", new SearchField("dsErrorReason", SearchFieldType.STRING),
            // status não é uma coluna própria: dtResolved nulo = pendente. O front usa
            // IS_NULL/IS_NOT_NULL nesse campo em vez de comparar valor.
            "dtResolved", new SearchField("dtResolved", SearchFieldType.STRING)
    );

    public static final Map<String, String> SORT_FIELDS = Map.of(
            "id", "id",
            "createdAt", "createdAt",
            "dtFailedAt", "dtFailedAt",
            "nrDeathCount", "nrDeathCount"
    );

    public static Specification<NotificationDeadLetter> byFilters(List<FilterRequest> filters) {
        return SpecificationBuilder.fromFilters(filters, FILTER_FIELDS);
    }
}
