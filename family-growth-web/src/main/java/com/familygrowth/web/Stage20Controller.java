package com.familygrowth.web;

import com.familygrowth.application.Stage20Service;
import com.familygrowth.domain.Stage20Models.DocumentaryAccessMode;
import com.familygrowth.domain.Stage20Models.DocumentaryListing;
import com.familygrowth.domain.Stage20Models.DocumentarySource;
import com.familygrowth.domain.Stage20Models.ExperienceAudit;
import com.familygrowth.domain.Stage20Models.ExperienceProfile;
import com.familygrowth.domain.Stage20Models.SchoolStage;
import com.familygrowth.domain.Stage20Models.PrimaryGradeBand;
import com.familygrowth.domain.Stage20Models.StageTransitionPreview;
import com.familygrowth.domain.Stage3Models;
import com.familygrowth.domain.Stage3Models.Actor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/families/{familyId}")
class Stage20Controller {
    private final Stage20Service service;

    Stage20Controller(Stage20Service service) {
        this.service = service;
    }

    @GetMapping("/children/{childId}/experience-profile")
    ApiResponse<ExperienceProfile> experience(
        @RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,
        @PathVariable UUID familyId,
        @PathVariable UUID childId
    ) {
        return ApiResponse.ok(service.experience(actor, familyId, childId));
    }

    @PutMapping("/children/{childId}/experience-profile")
    ApiResponse<ExperienceProfile> updateExperience(
        @RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,
        @PathVariable UUID familyId,
        @PathVariable UUID childId,
        @Valid @RequestBody ExperienceRequest request
    ) {
        return ApiResponse.ok(service.updateExperience(actor, familyId, childId, request.birthDate(),
            request.stageOverride(), request.primaryBandOverride(), request.overrideReason(), request.hapticsEnabled(), request.expectedVersion(),
            request.auditReason()));
    }

    @GetMapping("/children/{childId}/experience-profile/audit")
    ApiResponse<List<ExperienceAudit>> audit(
        @RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,
        @PathVariable UUID familyId,
        @PathVariable UUID childId,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit
    ) {
        return ApiResponse.ok(service.audit(actor, familyId, childId, limit));
    }

    @GetMapping("/children/{childId}/experience-profile/transition-preview")
    ApiResponse<StageTransitionPreview> transitionPreview(@RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE)Actor actor,@PathVariable UUID familyId,@PathVariable UUID childId,@RequestParam @PastOrPresent LocalDate birthDate,@RequestParam(required=false)SchoolStage stageOverride){return ApiResponse.ok(service.transitionPreview(actor,familyId,childId,birthDate,stageOverride));}

    @PostMapping("/documentary-sources")
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<DocumentarySource> createDocumentary(
        @RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,
        @PathVariable UUID familyId,
        @RequestHeader("Idempotency-Key") String key,
        @Valid @RequestBody DocumentaryRequest request
    ) {
        return ApiResponse.ok(service.createDocumentary(actor, familyId, request.schoolStage(), request.title(),
            request.description(), request.languageTag(), request.durationSeconds(), request.accessMode(),
            request.sourceReference(), request.rightsHolder(), request.rightsReference(),
            request.licenseExpiresOn(), key));
    }

    @GetMapping("/documentary-sources")
    ApiResponse<List<DocumentaryListing>> documentaries(
        @RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,
        @PathVariable UUID familyId
    ) {
        return ApiResponse.ok(service.parentDocumentaries(actor, familyId));
    }

    @GetMapping("/children/{childId}/documentaries")
    ApiResponse<List<DocumentaryListing>> childDocumentaries(
        @RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,
        @PathVariable UUID familyId,
        @PathVariable UUID childId
    ) {
        return ApiResponse.ok(service.childDocumentaries(actor, familyId, childId));
    }

    @PostMapping("/documentary-sources/{sourceId}/approve")
    ApiResponse<DocumentarySource> approveDocumentary(
        @RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,
        @PathVariable UUID familyId,
        @PathVariable UUID sourceId,
        @RequestHeader("Idempotency-Key") String key
    ) {
        return ApiResponse.ok(service.approveDocumentary(actor, familyId, sourceId, key));
    }

    @PostMapping("/documentary-sources/{sourceId}/withdraw")
    ApiResponse<DocumentarySource> withdrawDocumentary(
        @RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,
        @PathVariable UUID familyId,
        @PathVariable UUID sourceId,
        @RequestHeader("Idempotency-Key") String key
    ) {
        return ApiResponse.ok(service.withdrawDocumentary(actor, familyId, sourceId, key));
    }

    record ExperienceRequest(
        @NotNull @PastOrPresent LocalDate birthDate,
        SchoolStage stageOverride,
        PrimaryGradeBand primaryBandOverride,
        @Size(max = 240) String overrideReason,
        @NotNull Boolean hapticsEnabled,
        @Min(0) long expectedVersion,
        @NotBlank @Size(max = 240) String auditReason
    ) {
    }

    record DocumentaryRequest(
        @NotNull SchoolStage schoolStage,
        @NotBlank @Size(max = 160) String title,
        @Size(max = 1000) String description,
        @NotBlank @Pattern(regexp = "^[A-Za-z]{2,8}(-[A-Za-z0-9]{1,8})*$") String languageTag,
        @Min(10) @Max(14_400) Integer durationSeconds,
        @NotNull DocumentaryAccessMode accessMode,
        @NotBlank @Size(max = 1000) String sourceReference,
        @NotBlank @Size(max = 240) String rightsHolder,
        @NotBlank @Size(max = 1000) String rightsReference,
        LocalDate licenseExpiresOn
    ) {
    }
}
