package com.worktrack.repo.specification;

import jakarta.persistence.metamodel.SingularAttribute;
import org.springframework.data.jpa.domain.Specification;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

public class Spec {

    private Spec() {
    }

    @SafeVarargs
    public static <T> Specification<T> and(Specification<T>... specs) {
        return combine(Specification::and, specs);
    }

    @SafeVarargs
    public static <T> Specification<T> or(Specification<T>... specs) {
        return combine(Specification::or, specs);
    }

    public static <T, Y> Specification<T> in(SingularAttribute<? super T, Y> attr, List<? extends Y> values) {
        return (root, q, cb) -> (values == null || values.isEmpty())
                ? cb.conjunction()
                : root.get(attr).in(values);
    }

    public static <T, Y> Specification<T> eq(SingularAttribute<? super T, Y> attr, Y value) {
        return (root, q, cb) -> value == null ? cb.conjunction() : cb.equal(root.get(attr), value);
    }

    public static <T> Specification<T> containsEscaped(SingularAttribute<? super T, String> attr, String value) {
        return (root, query, cb) -> {
            if (value == null || value.isBlank()) {
                return cb.conjunction();
            }
            String escaped = escapeForLike(value);
            String pattern = "%" + escaped.toLowerCase(Locale.ROOT) + "%";
            return cb.like(cb.lower(root.get(attr)), pattern, '\\');
        };
    }

    public static <T> Specification<T> containsRaw(SingularAttribute<? super T, String> attr, String value) {
        return (root, query, cb) -> {
            if (value == null || value.isBlank()) {
                return cb.conjunction();
            }
            String pattern = "%" + value.toLowerCase(Locale.ROOT) + "%";
            return cb.like(cb.lower(root.get(attr)), pattern);
        };
    }

    public static <T> Specification<T> not(Specification<T> spec) {
        return (root, q, cb) -> (spec == null) ? cb.conjunction() : cb.not(spec.toPredicate(root, q, cb));
    }

    public static <T, V> Specification<T> whenNotNull(V value, Function<V, Specification<T>> func) {
        return Optional.ofNullable(value).map(func).orElseGet(Specification::<T>unrestricted);
    }

    public static <T> Specification<T> whenNotBlank(String value, Function<String, Specification<T>> func) {
        return Optional.ofNullable(value).filter(v -> !v.isBlank()).map(func).orElseGet(Specification::<T>unrestricted);
    }

    @SafeVarargs
    private static <T> Specification<T> combine(
            BiFunction<Specification<T>, Specification<T>, Specification<T>> combiner,
            Specification<T>... specs) {

        Specification<T> base = Specification.unrestricted();
        return Arrays.stream(specs)
                .filter(Objects::nonNull).reduce(base, combiner::apply);
    }

    private static String escapeForLike(String input) {
        return input
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

}
