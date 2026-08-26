package com.familygrowth.web;

import com.familygrowth.application.Stage17Service;
import com.familygrowth.domain.Stage3Models;
import com.familygrowth.domain.Stage3Models.Actor;
import com.familygrowth.domain.Stage17Models.WithdrawalQuote;
import com.familygrowth.domain.Stage17Models.WithdrawalRequest;
import com.familygrowth.domain.Stage17Models.WithdrawalRule;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/v1/families/{familyId}")
class Stage17Controller {
    private final Stage17Service service;

    Stage17Controller(Stage17Service service) {
        this.service = service;
    }

    @PostMapping("/withdrawal-rules")
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<WithdrawalRule> createRule(
        @RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,
        @PathVariable UUID familyId,
        @RequestHeader("Idempotency-Key") String key,
        @Valid @RequestBody RuleRequest request
    ) {
        return ApiResponse.ok(service.createRule(actor, familyId, request.payoutRate(),
            request.feeRate(), request.fixedFee(), key));
    }

    @GetMapping("/withdrawal-rules/active")
    ApiResponse<WithdrawalRule> activeRule(
        @RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,
        @PathVariable UUID familyId
    ) {
        return ApiResponse.ok(service.activeRule(actor, familyId));
    }

    @PostMapping("/children/{childId}/withdrawal-quotes")
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<WithdrawalQuote> quote(
        @RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,
        @PathVariable UUID familyId,
        @PathVariable UUID childId,
        @RequestHeader("Idempotency-Key") String key,
        @Valid @RequestBody QuoteRequest request
    ) {
        return ApiResponse.ok(service.quote(actor, familyId, childId, request.moneyAmount(), key));
    }

    @PostMapping("/children/{childId}/withdrawal-requests")
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<WithdrawalRequest> request(
        @RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,
        @PathVariable UUID familyId,
        @PathVariable UUID childId,
        @RequestHeader("Idempotency-Key") String key,
        @Valid @RequestBody CreateRequest request
    ) {
        return ApiResponse.ok(service.request(actor, familyId, childId, request.quoteId(), key));
    }

    @GetMapping("/children/{childId}/withdrawal-requests")
    ApiResponse<List<WithdrawalRequest>> requests(
        @RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,
        @PathVariable UUID familyId,
        @PathVariable UUID childId
    ) {
        return ApiResponse.ok(service.requests(actor, familyId, childId));
    }

    @PostMapping("/withdrawal-requests/{requestId}/approve")
    ApiResponse<WithdrawalRequest> approve(
        @RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,
        @PathVariable UUID familyId,
        @PathVariable UUID requestId,
        @RequestHeader("Idempotency-Key") String key
    ) {
        return ApiResponse.ok(service.approve(actor, familyId, requestId, key));
    }

    @PostMapping("/withdrawal-requests/{requestId}/reject")
    ApiResponse<WithdrawalRequest> reject(
        @RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,
        @PathVariable UUID familyId,
        @PathVariable UUID requestId,
        @RequestHeader("Idempotency-Key") String key
    ) {
        return ApiResponse.ok(service.reject(actor, familyId, requestId, key));
    }

    @PostMapping("/withdrawal-requests/{requestId}/cancel")
    ApiResponse<WithdrawalRequest> cancel(
        @RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,
        @PathVariable UUID familyId,
        @PathVariable UUID requestId,
        @RequestHeader("Idempotency-Key") String key
    ) {
        return ApiResponse.ok(service.cancel(actor, familyId, requestId, key));
    }

    @PostMapping("/withdrawal-requests/{requestId}/paid")
    ApiResponse<WithdrawalRequest> paid(
        @RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,
        @PathVariable UUID familyId,
        @PathVariable UUID requestId,
        @RequestHeader("Idempotency-Key") String key
    ) {
        return ApiResponse.ok(service.markPaid(actor, familyId, requestId, key));
    }

    record RuleRequest(
        @NotNull @DecimalMin(value = "0.000001") @Digits(integer = 13, fraction = 6)
        BigDecimal payoutRate,
        @NotNull @DecimalMin("0.000000") @DecimalMax(value = "0.999999")
        @Digits(integer = 1, fraction = 6) BigDecimal feeRate,
        @NotNull @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal fixedFee
    ) {
    }

    record QuoteRequest(
        @NotNull @DecimalMin("0.01") @DecimalMax("100000.00")
        @Digits(integer = 6, fraction = 2) BigDecimal moneyAmount
    ) {
    }

    record CreateRequest(@NotNull UUID quoteId) {
    }
}
