package com.familygrowth.web;

import com.familygrowth.application.Stage23LearningService;
import com.familygrowth.domain.Stage21TeachingModels.LearningAssignment;
import com.familygrowth.domain.Stage23LearningModels.RewardPolicy;
import com.familygrowth.domain.Stage23LearningModels.MisconceptionCategory;
import com.familygrowth.domain.Stage23LearningModels.SupportEvent;
import com.familygrowth.domain.Stage3Models;
import com.familygrowth.domain.Stage3Models.Actor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/families/{familyId}/children/{childId}/autonomous-learning")
class Stage23LearningController {
    private final Stage23LearningService service;
    Stage23LearningController(Stage23LearningService service) { this.service = service; }

    @GetMapping("/reward-policy")
    ApiResponse<RewardPolicy> policy(@RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,
                                     @PathVariable UUID familyId, @PathVariable UUID childId) {
        return ApiResponse.ok(service.policy(actor, familyId, childId));
    }

    @PutMapping("/reward-policy")
    ApiResponse<RewardPolicy> updatePolicy(@RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,
                                           @PathVariable UUID familyId, @PathVariable UUID childId,
                                           @Valid @RequestBody RewardPolicyRequest request) {
        return ApiResponse.ok(service.updatePolicy(actor, familyId, childId, request.moneyReward(),
            request.coinReward(), request.xpReward(), request.expectedVersion(), request.auditReason()));
    }

    @PostMapping("/sync")
    ApiResponse<List<LearningAssignment>> sync(@RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,
                                               @PathVariable UUID familyId, @PathVariable UUID childId,
                                               @RequestHeader("Idempotency-Key") String key) {
        return ApiResponse.ok(service.sync(actor, familyId, childId, key));
    }

    @GetMapping("/assignments/{assignmentId}/support-events")
    ApiResponse<List<SupportEvent>> supportEvents(@RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,
                                                   @PathVariable UUID familyId, @PathVariable UUID childId,
                                                   @PathVariable UUID assignmentId) {
        return ApiResponse.ok(service.supportEvents(actor, familyId, childId, assignmentId));
    }

    @PostMapping("/assignments/{assignmentId}/activities/{activityId}/help")
    ApiResponse<LearningAssignment> requestHelp(@RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,
                                                 @PathVariable UUID familyId, @PathVariable UUID childId,
                                                 @PathVariable UUID assignmentId, @PathVariable UUID activityId,
                                                 @RequestHeader("Idempotency-Key") String key,
                                                 @Valid @RequestBody HelpRequest request) {
        return ApiResponse.ok(service.requestHelp(actor, familyId, childId, assignmentId, activityId, request.message(), key));
    }

    @PostMapping("/assignments/{assignmentId}/support-events/classify")
    ApiResponse<List<SupportEvent>> classify(@RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,
                                              @PathVariable UUID familyId, @PathVariable UUID childId,
                                              @PathVariable UUID assignmentId,
                                              @RequestHeader("Idempotency-Key") String key,
                                              @Valid @RequestBody ClassifyRequest request) {
        return ApiResponse.ok(service.classifySupport(actor, familyId, childId, assignmentId, request.sourceEventId(),
            request.category(), request.privateNote(), request.revisitAt(), key));
    }

    record RewardPolicyRequest(
        @NotNull @DecimalMin("0.00") @DecimalMax("10000.00") BigDecimal moneyReward,
        @Min(0) @Max(1_000_000) long coinReward,
        @Min(0) @Max(1_000_000) long xpReward,
        @Min(0) long expectedVersion,
        @NotBlank @Size(max=500) String auditReason
    ) { }
    record HelpRequest(@NotBlank @Size(max=160) String message) { }
    record ClassifyRequest(@NotNull UUID sourceEventId, @NotNull MisconceptionCategory category,
                           @Size(max=500) String privateNote, Instant revisitAt) { }
}
