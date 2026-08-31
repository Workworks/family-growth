package com.familygrowth.application;

import com.familygrowth.domain.Stage3Models.Actor;
import com.familygrowth.domain.Stage5Models.ExchangeDirection;
import com.familygrowth.domain.Stage5Models.ExchangeOrder;
import com.familygrowth.domain.Stage5Models.ExchangePreview;
import com.familygrowth.domain.Stage5Models.ExchangeRule;
import com.familygrowth.domain.Stage5Models.GiftMoney;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class Stage5Service {
    private static final Duration PREVIEW_LIFETIME = Duration.ofMinutes(10);
    private final Stage3Service authorization;
    private final Stage5Store store;
    private final Stage28RewardStore governance;
    private final Clock clock;

    public Stage5Service(Stage3Service authorization, Stage5Store store, Stage28RewardStore governance, Clock clock) {
        this.authorization = authorization; this.store = store; this.governance = governance; this.clock = clock;
    }

    public GiftMoney depositGift(Actor actor, UUID familyId, UUID childId, BigDecimal amount,
                                 String note, String key) {
        authorization.requireParent(actor, familyId);
        authorization.requireChildOrParent(actor, familyId, childId);
        requireKey(key);
        BigDecimal normalized = amount == null ? null : amount.setScale(2, java.math.RoundingMode.UNNECESSARY);
        if (normalized == null || normalized.signum() <= 0 || normalized.compareTo(new BigDecimal("100000.00")) > 0) {
            throw new IllegalArgumentException("Gift money amount is invalid");
        }
        var existing = store.findGift(familyId, key);
        if (existing.isPresent()) {
            if (!existing.get().childId().equals(childId) || existing.get().amount().compareTo(normalized) != 0) {
                throw new Stage3Service.ConflictException("Idempotency-Key was used for another gift");
            }
            return existing.get();
        }
        return store.depositGift(familyId, childId, normalized, normalize(note), key, actor.actorId(), clock.instant());
    }

    public ExchangeRule createRule(Actor actor, UUID familyId, BigDecimal buyRate, BigDecimal sellRate,
                                   BigDecimal buyFee, BigDecimal sellFee, BigDecimal maxSource) {
        authorization.requireParent(actor, familyId);
        return store.createRule(familyId, buyRate, sellRate, buyFee, sellFee, maxSource,
            actor.actorId(), clock.instant());
    }

    @Transactional(readOnly = true)
    public ExchangeRule activeRule(Actor actor, UUID familyId) {
        if (actor != null && actor.childId() != null) {
            authorization.requireChildOrParent(actor, familyId, actor.childId());
        } else {
            authorization.requireParent(actor, familyId);
        }
        return store.activeRule(familyId).orElseThrow(() -> new Stage3Service.ConflictException("No active exchange rule"));
    }

    public ExchangePreview preview(Actor actor, UUID familyId, UUID childId,
                                   ExchangeDirection direction, BigDecimal sourceAmount) {
        authorization.requireChildOrParent(actor, familyId, childId);
        var rule = store.activeRule(familyId)
            .orElseThrow(() -> new Stage3Service.ConflictException("No active exchange rule"));
        var quote = com.familygrowth.domain.Stage5Models.quote(rule, direction, sourceAmount);
        var now = clock.instant();
        var preview = store.savePreview(familyId, childId, direction, quote, rule, now.plus(PREVIEW_LIFETIME), now);
        governance.validateAndBindPreview(familyId, childId, direction, quote.sourceAmount(), preview.id(), now);
        return preview;
    }

    public ExchangeOrder confirm(Actor actor, UUID familyId, UUID previewId, String key) {
        requireKey(key);
        var preview = store.preview(familyId, previewId)
            .orElseThrow(FamilyGrowthService.NotFoundException::new);
        authorization.requireChildOrParent(actor, familyId, preview.childId());
        var existing = store.findOrder(familyId, key);
        if (existing.isPresent()) {
            if (!existing.get().previewId().equals(previewId)) {
                throw new Stage3Service.ConflictException("Idempotency-Key was used for another exchange");
            }
            return existing.get();
        }
        if (preview.expiresAt().isBefore(clock.instant())) {
            throw new Stage3Service.ConflictException("Exchange preview expired");
        }
        var active = store.activeRule(familyId).orElseThrow();
        if (!active.id().equals(preview.ruleId()) || active.version() != preview.ruleVersion()) {
            throw new Stage3Service.ConflictException("Exchange rule changed; preview again");
        }
        governance.validateExchangeConfirm(familyId, previewId,
            actor.role() == com.familygrowth.domain.Stage3Models.ActorRole.CHILD, clock.instant());
        return store.confirm(familyId, previewId, key, actor.actorId(), clock.instant());
    }

    private static void requireKey(String key) {
        if (key == null || key.isBlank() || key.length() > 100) throw new IllegalArgumentException("Valid Idempotency-Key is required");
    }
    private static String normalize(String value) { return value == null ? "" : value.trim(); }
}
