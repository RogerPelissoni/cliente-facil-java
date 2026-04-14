package br.com.clientefacil.core.dto.search;

public record SearchField(
        String path,
        SearchFieldType type
) {
}