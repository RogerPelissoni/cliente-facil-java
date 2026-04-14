package br.com.clientefacil.core.support;

import br.com.clientefacil.core.dto.search.FilterOperator;
import br.com.clientefacil.core.dto.search.FilterRequest;
import br.com.clientefacil.core.dto.search.SearchField;
import br.com.clientefacil.core.dto.search.SearchFieldType;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class SpecificationBuilder {

    private SpecificationBuilder() {
    }

    public static <T> Specification<T> fromFilters(
            List<FilterRequest> filters,
            Map<String, SearchField> allowedFields
    ) {
        return (root, query, cb) -> {
            if (filters == null || filters.isEmpty()) {
                return cb.conjunction();
            }

            List<Predicate> predicates = new ArrayList<>();

            for (FilterRequest filter : filters) {
                if (filter == null || filter.field() == null || filter.operator() == null) {
                    continue;
                }

                SearchField searchField = allowedFields.get(filter.field());
                if (searchField == null) {
                    continue;
                }

                Path<?> path = resolvePath(root, searchField.path());
                Predicate predicate = buildPredicate(cb, path, searchField, filter);

                if (predicate != null) {
                    predicates.add(predicate);
                }
            }

            if (predicates.isEmpty()) {
                return cb.conjunction();
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static <T> Path<?> resolvePath(Root<T> root, String path) {
        String[] parts = path.split("\\.");
        Path<?> current = root;

        for (String part : parts) {
            current = current.get(part);
        }

        return current;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Predicate buildPredicate(
            CriteriaBuilder cb,
            Path<?> path,
            SearchField searchField,
            FilterRequest filter
    ) {
        SearchFieldType fieldType = searchField.type();
        FilterOperator operator = filter.operator();

        Object value = castSingleValue(filter.value(), fieldType);

        return switch (operator) {
            case EQ -> value != null ? cb.equal(path, value) : null;
            case NE -> value != null ? cb.notEqual(path, value) : null;

            case GT -> buildGreaterThan(cb, path, value, fieldType);
            case GTE -> buildGreaterThanOrEqualTo(cb, path, value, fieldType);
            case LT -> buildLessThan(cb, path, value, fieldType);
            case LTE -> buildLessThanOrEqualTo(cb, path, value, fieldType);

            case LIKE -> {
                if (fieldType == SearchFieldType.STRING && value != null) {
                    yield cb.like(cb.lower((Expression<String>) path), value.toString().toLowerCase());
                }
                yield null;
            }

            case CONTAINS -> {
                if (fieldType == SearchFieldType.STRING && value != null) {
                    yield cb.like(cb.lower((Expression<String>) path), "%" + value.toString().toLowerCase() + "%");
                }
                yield null;
            }

            case STARTS_WITH -> {
                if (fieldType == SearchFieldType.STRING && value != null) {
                    yield cb.like(cb.lower((Expression<String>) path), value.toString().toLowerCase() + "%");
                }
                yield null;
            }

            case ENDS_WITH -> {
                if (fieldType == SearchFieldType.STRING && value != null) {
                    yield cb.like(cb.lower((Expression<String>) path), "%" + value.toString().toLowerCase());
                }
                yield null;
            }

            case IN -> {
                if (filter.values() == null || filter.values().isEmpty()) {
                    yield null;
                }

                List<Object> castedValues = filter.values().stream()
                        .map(v -> castSingleValue(v, fieldType))
                        .filter(Objects::nonNull)
                        .toList();

                if (castedValues.isEmpty()) {
                    yield null;
                }

                yield path.in(castedValues);
            }

            case BETWEEN -> {
                if (filter.values() == null || filter.values().size() != 2) {
                    yield null;
                }

                Object start = castSingleValue(filter.values().get(0), fieldType);
                Object end = castSingleValue(filter.values().get(1), fieldType);

                if (start == null || end == null) {
                    yield null;
                }

                if (!(start instanceof Comparable) || !(end instanceof Comparable)) {
                    yield null;
                }

                yield cb.between(
                        (Expression<? extends Comparable>) path,
                        (Comparable) start,
                        (Comparable) end
                );
            }

            case IS_NULL -> cb.isNull(path);
            case IS_NOT_NULL -> cb.isNotNull(path);
        };
    }

    @SuppressWarnings({"unchecked"})
    private static Predicate buildGreaterThan(
            CriteriaBuilder cb,
            Path<?> path,
            Object value,
            SearchFieldType fieldType
    ) {
        if (value == null) return null;

        return switch (fieldType) {
            case STRING -> cb.greaterThan((Expression<String>) path, value.toString());
            case LONG, INTEGER -> cb.gt((Expression<? extends Number>) path, (Number) value);
            case BOOLEAN -> null;
        };
    }

    @SuppressWarnings({"unchecked"})
    private static Predicate buildGreaterThanOrEqualTo(
            CriteriaBuilder cb,
            Path<?> path,
            Object value,
            SearchFieldType fieldType
    ) {
        if (value == null) return null;

        return switch (fieldType) {
            case STRING -> cb.greaterThanOrEqualTo((Expression<String>) path, value.toString());
            case LONG, INTEGER -> cb.ge((Expression<? extends Number>) path, (Number) value);
            case BOOLEAN -> null;
        };
    }

    @SuppressWarnings({"unchecked"})
    private static Predicate buildLessThan(
            CriteriaBuilder cb,
            Path<?> path,
            Object value,
            SearchFieldType fieldType
    ) {
        if (value == null) return null;

        return switch (fieldType) {
            case STRING -> cb.lessThan((Expression<String>) path, value.toString());
            case LONG, INTEGER -> cb.lt((Expression<? extends Number>) path, (Number) value);
            case BOOLEAN -> null;
        };
    }

    @SuppressWarnings({"unchecked"})
    private static Predicate buildLessThanOrEqualTo(
            CriteriaBuilder cb,
            Path<?> path,
            Object value,
            SearchFieldType fieldType
    ) {
        if (value == null) return null;

        return switch (fieldType) {
            case STRING -> cb.lessThanOrEqualTo((Expression<String>) path, value.toString());
            case LONG, INTEGER -> cb.le((Expression<? extends Number>) path, (Number) value);
            case BOOLEAN -> null;
        };
    }

    private static Object castSingleValue(String value, SearchFieldType type) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return switch (type) {
                case STRING -> value;
                case LONG -> Long.valueOf(value);
                case INTEGER -> Integer.valueOf(value);
                case BOOLEAN -> Boolean.valueOf(value);
            };
        } catch (Exception ex) {
            return null;
        }
    }
}