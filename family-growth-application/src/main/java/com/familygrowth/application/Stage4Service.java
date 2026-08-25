package com.familygrowth.application;

import com.familygrowth.domain.Stage3Models.Actor;
import com.familygrowth.domain.Stage3Models.AssetType;
import com.familygrowth.domain.Stage3Models.LedgerEntry;
import com.familygrowth.domain.Stage3Models.WalletReconciliation;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class Stage4Service {
    private final Stage3Service authorization;
    private final Stage3Store store;
    private final Clock clock;

    public Stage4Service(Stage3Service authorization, Stage3Store store, Clock clock) {
        this.authorization = authorization;
        this.store = store;
        this.clock = clock;
    }

    public LedgerEntry adjust(
        Actor actor, UUID familyId, UUID childId, AssetType assetType,
        BigDecimal requestedDelta, String reason, String idempotencyKey
    ) {
        authorization.requireParent(actor, familyId);
        authorization.requireChildOrParent(actor, familyId, childId);
        if (assetType == null || requestedDelta == null || requestedDelta.signum() == 0) {
            throw new IllegalArgumentException("Non-zero adjustment is required");
        }
        BigDecimal delta = com.familygrowth.domain.Stage3Models.money(requestedDelta);
        if (assetType == AssetType.COIN && delta.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException("Coin adjustment must be an integer");
        }
        if (reason == null || reason.isBlank() || reason.length() > 500) {
            throw new IllegalArgumentException("Adjustment reason is required");
        }
        requireKey(idempotencyKey);
        var existing = store.findAdjustment(familyId, idempotencyKey);
        if (existing.isPresent()) {
            if (existing.get().assetType() != assetType || existing.get().delta().compareTo(delta) != 0
                || !existing.get().childId().equals(childId)) {
                throw new Stage3Service.ConflictException("Idempotency-Key was used for another adjustment");
            }
            return existing.get();
        }
        return store.adjustWallet(
            familyId, childId, actor.actorId(), assetType, delta, reason.trim(), idempotencyKey, clock.instant());
    }

    @Transactional(readOnly = true)
    public WalletReconciliation reconcile(Actor actor, UUID familyId, UUID childId) {
        authorization.requireParent(actor, familyId);
        authorization.requireChildOrParent(actor, familyId, childId);
        return store.reconcile(familyId, childId);
    }

    private static void requireKey(String key) {
        if (key == null || key.isBlank() || key.length() > 100) {
            throw new IllegalArgumentException("Valid Idempotency-Key is required");
        }
    }
}
