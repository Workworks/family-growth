package com.familygrowth.web;

import com.familygrowth.application.Stage21ResourceService;
import com.familygrowth.domain.Stage20Models.SchoolStage;
import com.familygrowth.domain.Stage21ResourceModels.ChildResourceListing;
import com.familygrowth.domain.Stage21ResourceModels.ParentResourceListing;
import com.familygrowth.domain.Stage3Models;
import com.familygrowth.domain.Stage3Models.Actor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/families/{familyId}")
class Stage21ResourceController {
    private final Stage21ResourceService service;

    Stage21ResourceController(Stage21ResourceService service) {
        this.service = service;
    }

    @PostMapping("/education-resource-sources")
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<ParentResourceListing> create(
        @RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,
        @PathVariable UUID familyId,
        @RequestHeader("Idempotency-Key") String key,
        @Valid @RequestBody ResourceSourceRequest request
    ) {
        return ApiResponse.ok(service.create(actor, familyId, request.title(), request.sourceUrl(),
            request.schoolStages(), request.usageNote(), key));
    }

    @GetMapping("/education-resource-sources")
    ApiResponse<List<ParentResourceListing>> list(
        @RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,
        @PathVariable UUID familyId
    ) {
        return ApiResponse.ok(service.parentSources(actor, familyId));
    }

    @PostMapping("/education-resource-sources/{sourceId}/refresh")
    ApiResponse<ParentResourceListing> refresh(
        @RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,
        @PathVariable UUID familyId,
        @PathVariable UUID sourceId,
        @RequestHeader("Idempotency-Key") String key
    ) {
        return ApiResponse.ok(service.refresh(actor, familyId, sourceId, key));
    }

    @PostMapping("/education-resource-sources/{sourceId}/approve")
    ApiResponse<ParentResourceListing> approve(
        @RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,
        @PathVariable UUID familyId,
        @PathVariable UUID sourceId,
        @RequestHeader("Idempotency-Key") String key
    ) {
        return ApiResponse.ok(service.approve(actor, familyId, sourceId, key));
    }

    @PostMapping("/education-resource-sources/{sourceId}/withdraw")
    ApiResponse<ParentResourceListing> withdraw(
        @RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,
        @PathVariable UUID familyId,
        @PathVariable UUID sourceId,
        @RequestHeader("Idempotency-Key") String key
    ) {
        return ApiResponse.ok(service.withdraw(actor, familyId, sourceId, key));
    }

    @GetMapping("/children/{childId}/education-resource-catalog")
    ApiResponse<List<ChildResourceListing>> childCatalog(
        @RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,
        @PathVariable UUID familyId,
        @PathVariable UUID childId
    ) {
        return ApiResponse.ok(service.childCatalog(actor, familyId, childId));
    }

    record ResourceSourceRequest(
        @NotBlank @Size(max = 160) String title,
        @NotBlank @Size(max = 1000) String sourceUrl,
        @NotEmpty @Size(max = 4) List<SchoolStage> schoolStages,
        @NotBlank @Size(max = 500) String usageNote
    ) {
    }
}
