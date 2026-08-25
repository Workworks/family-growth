package com.familygrowth.web;

import com.familygrowth.application.Stage5Service;
import com.familygrowth.domain.Stage3Models;
import com.familygrowth.domain.Stage3Models.Actor;
import com.familygrowth.domain.Stage5Models.ExchangeDirection;
import com.familygrowth.domain.Stage5Models.ExchangeOrder;
import com.familygrowth.domain.Stage5Models.ExchangePreview;
import com.familygrowth.domain.Stage5Models.ExchangeRule;
import com.familygrowth.domain.Stage5Models.GiftMoney;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
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
class Stage5Controller {
    private final Stage5Service service;
    Stage5Controller(Stage5Service service) { this.service = service; }

    @PostMapping("/children/{childId}/gift-money")
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<GiftMoney> gift(@RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,
        @PathVariable UUID familyId, @PathVariable UUID childId,
        @RequestHeader("Idempotency-Key") String key, @Valid @RequestBody GiftRequest request) {
        return ApiResponse.ok(service.depositGift(actor, familyId, childId, request.amount(), request.note(), key));
    }

    @PostMapping("/exchange-rules")
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<ExchangeRule> rule(@RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,
        @PathVariable UUID familyId, @Valid @RequestBody RuleRequest request) {
        return ApiResponse.ok(service.createRule(actor, familyId, request.moneyToCoinRate(),
            request.coinToMoneyRate(), request.moneyToCoinFeeRate(), request.coinToMoneyFeeRate(),
            request.maxSourceAmount()));
    }

    @GetMapping("/exchange-rules/active")
    ApiResponse<ExchangeRule> active(@RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,
        @PathVariable UUID familyId) {
        return ApiResponse.ok(service.activeRule(actor, familyId));
    }

    @PostMapping("/children/{childId}/exchange-previews")
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<ExchangePreview> preview(@RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,
        @PathVariable UUID familyId, @PathVariable UUID childId, @Valid @RequestBody PreviewRequest request) {
        return ApiResponse.ok(service.preview(actor, familyId, childId, request.direction(), request.sourceAmount()));
    }

    @PostMapping("/exchange-previews/{previewId}/confirm")
    ApiResponse<ExchangeOrder> confirm(@RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,
        @PathVariable UUID familyId, @PathVariable UUID previewId,
        @RequestHeader("Idempotency-Key") String key) {
        return ApiResponse.ok(service.confirm(actor, familyId, previewId, key));
    }

    record GiftRequest(@NotNull @DecimalMin("0.01") @DecimalMax("100000.00") BigDecimal amount,
                       @Size(max=500) String note) {}
    record RuleRequest(@NotNull @DecimalMin("0.000001") BigDecimal moneyToCoinRate,
                       @NotNull @DecimalMin("0.000001") BigDecimal coinToMoneyRate,
                       @NotNull @DecimalMin("0.0") @DecimalMax(value="0.999999") BigDecimal moneyToCoinFeeRate,
                       @NotNull @DecimalMin("0.0") @DecimalMax(value="0.999999") BigDecimal coinToMoneyFeeRate,
                       @NotNull @DecimalMin("0.01") BigDecimal maxSourceAmount) {}
    record PreviewRequest(@NotNull ExchangeDirection direction,
                          @NotNull @DecimalMin("0.01") BigDecimal sourceAmount) {}
}
