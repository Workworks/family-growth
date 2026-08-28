package com.familygrowth.domain;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class Stage20Models {
    private Stage20Models() {
    }

    public enum SchoolStage {
        PARENT_ONLY,
        KINDERGARTEN,
        PRIMARY,
        JUNIOR_MIDDLE,
        SENIOR_HIGH;

        public static SchoolStage recommended(LocalDate birthDate, LocalDate today) {
            Objects.requireNonNull(birthDate);
            Objects.requireNonNull(today);
            if (birthDate.isAfter(today)) {
                throw new IllegalArgumentException("birthDate cannot be in the future");
            }
            int age = Period.between(birthDate, today).getYears();
            if (age < 3) return PARENT_ONLY;
            if (age < 6) return KINDERGARTEN;
            if (age < 12) return PRIMARY;
            if (age < 15) return JUNIOR_MIDDLE;
            return SENIOR_HIGH;
        }
    }

    public record FeedbackProfile(
        String visualStyle,
        int maxAnimationMs,
        int hapticPulseCount,
        double primaryPressScale,
        boolean hapticsEnabled
    ) {
        public FeedbackProfile {
            visualStyle = requireText(visualStyle, "visualStyle");
            if (maxAnimationMs < 0 || maxAnimationMs > 320) {
                throw new IllegalArgumentException("maxAnimationMs is outside the child safety range");
            }
            if (hapticPulseCount < 0 || hapticPulseCount > 2) {
                throw new IllegalArgumentException("hapticPulseCount is outside the child safety range");
            }
            if (primaryPressScale < 1.0 || primaryPressScale > 1.10) {
                throw new IllegalArgumentException("primaryPressScale is outside the child safety range");
            }
        }
    }

    public record ExperienceProfile(
        UUID familyId,
        UUID childId,
        LocalDate birthDate,
        int ageYears,
        SchoolStage recommendedStage,
        SchoolStage stageOverride,
        SchoolStage effectiveStage,
        String overrideReason,
        boolean hapticsEnabled,
        FeedbackProfile feedbackProfile,
        List<String> visibleCapabilities,
        long version,
        Instant updatedAt
    ) {
        public ExperienceProfile {
            Objects.requireNonNull(familyId);
            Objects.requireNonNull(childId);
            Objects.requireNonNull(birthDate);
            Objects.requireNonNull(recommendedStage);
            Objects.requireNonNull(effectiveStage);
            Objects.requireNonNull(feedbackProfile);
            Objects.requireNonNull(visibleCapabilities);
            Objects.requireNonNull(updatedAt);
            visibleCapabilities = List.copyOf(visibleCapabilities);
        }
    }

    public record ExperienceAudit(
        UUID id,
        UUID familyId,
        UUID childId,
        UUID actorId,
        LocalDate oldBirthDate,
        LocalDate newBirthDate,
        SchoolStage oldStageOverride,
        SchoolStage newStageOverride,
        boolean oldHapticsEnabled,
        boolean newHapticsEnabled,
        String reason,
        Instant createdAt
    ) {
        public ExperienceAudit {
            Objects.requireNonNull(id);
            Objects.requireNonNull(familyId);
            Objects.requireNonNull(childId);
            Objects.requireNonNull(actorId);
            Objects.requireNonNull(oldBirthDate);
            Objects.requireNonNull(newBirthDate);
            reason = normalize(reason);
            Objects.requireNonNull(createdAt);
        }
    }

    public enum DocumentaryAccessMode { ORIGINAL_OFFLINE, LICENSED_OFFLINE, OFFICIAL_LINK }
    public enum DocumentaryStatus { DRAFT, APPROVED, WITHDRAWN }

    public record DocumentarySource(
        UUID id,
        UUID familyId,
        SchoolStage schoolStage,
        String title,
        String description,
        String languageTag,
        Integer durationSeconds,
        DocumentaryAccessMode accessMode,
        String sourceReference,
        String rightsHolder,
        String rightsReference,
        LocalDate licenseExpiresOn,
        DocumentaryStatus status,
        UUID createdBy,
        UUID updatedBy,
        long version,
        Instant createdAt,
        Instant updatedAt
    ) {
        public DocumentarySource {
            Objects.requireNonNull(id);
            Objects.requireNonNull(familyId);
            Objects.requireNonNull(schoolStage);
            if (schoolStage == SchoolStage.PARENT_ONLY) {
                throw new IllegalArgumentException("Documentary sources require a school stage");
            }
            title = requireText(title, "title");
            description = normalize(description);
            languageTag = requireText(languageTag, "languageTag");
            if (durationSeconds != null && (durationSeconds < 10 || durationSeconds > 14_400)) {
                throw new IllegalArgumentException("durationSeconds must be between 10 and 14400");
            }
            Objects.requireNonNull(accessMode);
            sourceReference = requireText(sourceReference, "sourceReference");
            rightsHolder = requireText(rightsHolder, "rightsHolder");
            rightsReference = requireText(rightsReference, "rightsReference");
            Objects.requireNonNull(status);
            Objects.requireNonNull(createdBy);
            Objects.requireNonNull(updatedBy);
            Objects.requireNonNull(createdAt);
            Objects.requireNonNull(updatedAt);
            validateReference(accessMode, sourceReference);
        }

        private static void validateReference(DocumentaryAccessMode mode, String reference) {
            if (mode == DocumentaryAccessMode.OFFICIAL_LINK) {
                URI uri;
                try {
                    uri = URI.create(reference);
                } catch (IllegalArgumentException ex) {
                    throw new IllegalArgumentException("OFFICIAL_LINK requires a valid HTTPS URL");
                }
                if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null) {
                    throw new IllegalArgumentException("OFFICIAL_LINK requires a credential-free HTTPS URL");
                }
            } else if (mode == DocumentaryAccessMode.ORIGINAL_OFFLINE && !reference.startsWith("asset://")) {
                throw new IllegalArgumentException("ORIGINAL_OFFLINE requires an asset:// reference");
            } else if (mode == DocumentaryAccessMode.LICENSED_OFFLINE && !reference.startsWith("content-package://")) {
                throw new IllegalArgumentException("LICENSED_OFFLINE requires a content-package:// reference");
            }
        }
    }

    public record DocumentaryListing(
        UUID id,
        SchoolStage schoolStage,
        String title,
        String description,
        String languageTag,
        Integer durationSeconds,
        DocumentaryAccessMode accessMode,
        DocumentaryStatus status,
        String sourceReference,
        String rightsHolder,
        String rightsReference,
        LocalDate licenseExpiresOn,
        boolean parentActionRequired
    ) {
    }

    public static FeedbackProfile feedbackFor(SchoolStage stage, boolean hapticsEnabled) {
        return switch (stage) {
            case PARENT_ONLY -> new FeedbackProfile("parent-records", 0, 0, 1.0, false);
            case KINDERGARTEN -> new FeedbackProfile("storybook-stage", 320, hapticsEnabled ? 2 : 0, 1.10, hapticsEnabled);
            case PRIMARY -> new FeedbackProfile("exploration-notebook", 220, hapticsEnabled ? 1 : 0, 1.04, hapticsEnabled);
            case JUNIOR_MIDDLE -> new FeedbackProfile("subject-lab", 160, hapticsEnabled ? 1 : 0, 1.02, hapticsEnabled);
            case SENIOR_HIGH -> new FeedbackProfile("study-studio", 120, hapticsEnabled ? 1 : 0, 1.01, hapticsEnabled);
        };
    }

    public static List<String> capabilitiesFor(SchoolStage stage) {
        return switch (stage) {
            case PARENT_ONLY -> List.of("PARENT_RECORDS");
            case KINDERGARTEN -> List.of("TODAY", "DISCOVER", "MY", "PARENT_CO_USE");
            case PRIMARY -> List.of("TODAY", "LEARN", "MY", "SUBJECTS");
            case JUNIOR_MIDDLE -> List.of("PLAN", "SUBJECTS", "REVIEW", "REWORK");
            case SENIOR_HIGH -> List.of("GOALS", "COURSES", "REVIEW", "SELF_PLAN");
        };
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.trim();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
