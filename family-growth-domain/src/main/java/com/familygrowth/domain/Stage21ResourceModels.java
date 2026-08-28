package com.familygrowth.domain;

import com.familygrowth.domain.Stage20Models.SchoolStage;
import java.net.IDN;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public final class Stage21ResourceModels {
    private Stage21ResourceModels() {
    }

    public enum ResourceSourceStatus { DRAFT, APPROVED, WITHDRAWN }
    public enum ResourceRefreshStatus { NEVER, READY, FAILED }

    public record ResourceCategory(UUID id, String title, String categoryUrl, int displayOrder) {
        public ResourceCategory {
            Objects.requireNonNull(id);
            title = requireText(title, "title", 120);
            categoryUrl = normalizePublicHttpsUrl(categoryUrl);
            if (displayOrder < 0 || displayOrder > 29) {
                throw new IllegalArgumentException("displayOrder must be between 0 and 29");
            }
        }
    }

    public record EducationResourceSource(
        UUID id,
        UUID familyId,
        String title,
        String sourceUrl,
        List<SchoolStage> schoolStages,
        String usageNote,
        ResourceSourceStatus status,
        ResourceRefreshStatus refreshStatus,
        String refreshError,
        Instant lastRefreshedAt,
        List<ResourceCategory> categories,
        UUID createdBy,
        UUID updatedBy,
        long version,
        Instant createdAt,
        Instant updatedAt
    ) {
        public EducationResourceSource {
            Objects.requireNonNull(id);
            Objects.requireNonNull(familyId);
            title = requireText(title, "title", 160);
            sourceUrl = normalizePublicHttpsUrl(sourceUrl);
            Objects.requireNonNull(schoolStages);
            schoolStages = schoolStages.stream().distinct().toList();
            if (schoolStages.isEmpty() || schoolStages.contains(SchoolStage.PARENT_ONLY)) {
                throw new IllegalArgumentException("At least one child school stage is required");
            }
            usageNote = requireText(usageNote, "usageNote", 500);
            Objects.requireNonNull(status);
            Objects.requireNonNull(refreshStatus);
            refreshError = normalize(refreshError, 240);
            categories = categories == null ? List.of() : List.copyOf(categories);
            if (categories.size() > 30) throw new IllegalArgumentException("At most 30 categories are allowed");
            URI root = URI.create(sourceUrl);
            if (categories.stream().anyMatch(category -> !sameOrigin(root, URI.create(category.categoryUrl())))) {
                throw new IllegalArgumentException("Categories must stay on the configured source origin");
            }
            Objects.requireNonNull(createdBy);
            Objects.requireNonNull(updatedBy);
            Objects.requireNonNull(createdAt);
            Objects.requireNonNull(updatedAt);
        }
    }

    public record ParentResourceListing(
        UUID id,
        String title,
        String sourceUrl,
        List<SchoolStage> schoolStages,
        String usageNote,
        ResourceSourceStatus status,
        ResourceRefreshStatus refreshStatus,
        String refreshError,
        Instant lastRefreshedAt,
        List<ResourceCategory> categories,
        long version
    ) {
    }

    public record ChildResourceCategory(UUID id, String title, int displayOrder) {
    }

    public record ChildResourceListing(
        UUID id,
        String title,
        List<ChildResourceCategory> categories,
        Instant lastRefreshedAt,
        boolean parentActionRequired
    ) {
    }

    public static String normalizePublicHttpsUrl(String value) {
        String raw = requireText(value, "sourceUrl", 1000);
        URI uri;
        try {
            uri = URI.create(raw);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("A valid public HTTPS URL is required");
        }
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null
            || uri.getQuery() != null || uri.getFragment() != null || (uri.getPort() != -1 && uri.getPort() != 443)) {
            throw new IllegalArgumentException("A credential-free public HTTPS URL on port 443 is required");
        }
        String host = IDN.toASCII(uri.getHost().toLowerCase(Locale.ROOT));
        if (isIpLiteral(host) || host.equals("localhost") || !host.contains(".")) {
            throw new IllegalArgumentException("IP literals and local host names are not allowed");
        }
        try {
            String path = uri.getRawPath() == null || uri.getRawPath().isBlank() ? "/" : uri.getRawPath();
            return new URI("https", null, host, -1, path, null, null).normalize().toASCIIString();
        } catch (Exception ex) {
            throw new IllegalArgumentException("A valid public HTTPS URL is required");
        }
    }

    public static boolean sameOrigin(URI left, URI right) {
        return "https".equalsIgnoreCase(left.getScheme()) && "https".equalsIgnoreCase(right.getScheme())
            && left.getHost() != null && right.getHost() != null
            && left.getHost().equalsIgnoreCase(right.getHost())
            && normalizedPort(left) == normalizedPort(right);
    }

    private static int normalizedPort(URI uri) {
        return uri.getPort() == -1 ? 443 : uri.getPort();
    }

    private static boolean isIpLiteral(String host) {
        return host.indexOf(':') >= 0 || host.matches("^[0-9.]+$");
    }

    private static String requireText(String value, String field, int max) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        String result = value.trim();
        if (result.length() > max) throw new IllegalArgumentException(field + " is too long");
        return result;
    }

    private static String normalize(String value, int max) {
        String result = value == null ? "" : value.trim();
        return result.length() <= max ? result : result.substring(0, max);
    }
}
