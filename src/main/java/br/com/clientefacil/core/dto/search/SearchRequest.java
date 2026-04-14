package br.com.clientefacil.core.dto.search;

import java.util.List;

public interface SearchRequest {
    Integer page();

    Integer size();

    List<SortRequest> sorts();

    List<FilterRequest> filters();

    default int pageOrDefault() {
        return page() != null && page() >= 0 ? page() : 0;
    }

    default int sizeOrDefault() {
        return size() != null && size() > 0 ? size() : 10;
    }
}