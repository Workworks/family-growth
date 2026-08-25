package com.familygrowth.web;

import com.familygrowth.application.Stage3Service;
import com.familygrowth.domain.Stage3Models;
import com.familygrowth.domain.Stage3Models.Actor;
import com.familygrowth.domain.Stage3Models.LedgerEntry;
import com.familygrowth.domain.Stage3Models.RewardGrant;
import com.familygrowth.domain.Stage3Models.TaskCompletion;
import com.familygrowth.domain.Stage3Models.Wallet;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/families/{familyId}")
class TaskCompletionController {
    private final Stage3Service service;

    TaskCompletionController(Stage3Service service) {
        this.service = service;
    }

    @PostMapping("/children/{childId}/tasks/{taskId}/completions")
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<TaskCompletion> submit(
        @RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,
        @PathVariable UUID familyId,
        @PathVariable UUID childId,
        @PathVariable UUID taskId,
        @RequestHeader("Idempotency-Key") String idempotencyKey,
        @Valid @RequestBody SubmitRequest request
    ) {
        return ApiResponse.ok(service.submit(
            actor, familyId, childId, taskId, request.evidenceNote(), idempotencyKey));
    }

    @PostMapping("/completions/{completionId}/review")
    ApiResponse<TaskCompletion> review(
        @RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,
        @PathVariable UUID familyId,
        @PathVariable UUID completionId,
        @RequestHeader("Idempotency-Key") String idempotencyKey,
        @Valid @RequestBody ReviewRequest request
    ) {
        return ApiResponse.ok(service.review(actor, familyId, completionId, request.approved(),
            new RewardGrant(request.xpReward(), request.coinReward(), request.moneyReward()),
            request.reviewNote(), idempotencyKey));
    }

    @GetMapping("/children/{childId}/wallet")
    ApiResponse<Wallet> wallet(
        @RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,
        @PathVariable UUID familyId,
        @PathVariable UUID childId
    ) {
        return ApiResponse.ok(service.wallet(actor, familyId, childId));
    }

    @GetMapping("/children/{childId}/ledger")
    ApiResponse<List<LedgerEntry>> ledger(
        @RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,
        @PathVariable UUID familyId,
        @PathVariable UUID childId,
        @RequestParam(defaultValue = "50") @Min(1) @Max(200) int limit
    ) {
        return ApiResponse.ok(service.ledger(actor, familyId, childId, limit));
    }

    record SubmitRequest(@Size(max = 1000) String evidenceNote) {}

    record ReviewRequest(
        boolean approved,
        @Min(0) long xpReward,
        @Min(0) long coinReward,
        @NotNull @DecimalMin("0.00") BigDecimal moneyReward,
        @Size(max = 1000) String reviewNote
    ) {}
}
