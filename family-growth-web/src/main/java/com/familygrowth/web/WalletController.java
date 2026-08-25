package com.familygrowth.web;

import com.familygrowth.application.Stage4Service;
import com.familygrowth.domain.Stage3Models;
import com.familygrowth.domain.Stage3Models.Actor;
import com.familygrowth.domain.Stage3Models.AssetType;
import com.familygrowth.domain.Stage3Models.LedgerEntry;
import com.familygrowth.domain.Stage3Models.WalletReconciliation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/families/{familyId}/children/{childId}/wallet")
class WalletController {
    private final Stage4Service service;

    WalletController(Stage4Service service) {
        this.service = service;
    }

    @PostMapping("/adjustments")
    ApiResponse<LedgerEntry> adjust(
        @RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,
        @PathVariable UUID familyId,
        @PathVariable UUID childId,
        @RequestHeader("Idempotency-Key") String idempotencyKey,
        @Valid @RequestBody AdjustmentRequest request
    ) {
        return ApiResponse.ok(service.adjust(
            actor, familyId, childId, request.assetType(), request.delta(), request.reason(), idempotencyKey));
    }

    @GetMapping("/reconciliation")
    ApiResponse<WalletReconciliation> reconcile(
        @RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,
        @PathVariable UUID familyId,
        @PathVariable UUID childId
    ) {
        return ApiResponse.ok(service.reconcile(actor, familyId, childId));
    }

    record AdjustmentRequest(
        @NotNull AssetType assetType,
        @NotNull BigDecimal delta,
        @NotBlank @Size(max = 500) String reason
    ) {}
}
