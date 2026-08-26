package com.familygrowth.application;

import com.familygrowth.domain.Stage3Models.Actor;
import com.familygrowth.domain.Stage3Models.ActorRole;
import com.familygrowth.domain.Stage17Models;
import com.familygrowth.domain.Stage17Models.WithdrawalAction;
import com.familygrowth.domain.Stage17Models.WithdrawalQuote;
import com.familygrowth.domain.Stage17Models.WithdrawalRequest;
import com.familygrowth.domain.Stage17Models.WithdrawalRule;
import com.familygrowth.domain.Stage17Models.WithdrawalStatus;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class Stage17Service {
    private static final Duration QUOTE_LIFETIME = Duration.ofMinutes(10);

    private final Stage3Service authorization;
    private final Stage17Store store;
    private final Clock clock;

    public Stage17Service(Stage3Service authorization, Stage17Store store, Clock clock) {
        this.authorization = authorization;
        this.store = store;
        this.clock = clock;
    }

    public WithdrawalRule createRule(Actor actor, UUID familyId, BigDecimal payoutRate,
                                     BigDecimal feeRate, BigDecimal fixedFee, String key) {
        authorization.requireParent(actor, familyId);
        requireKey(key);
        WithdrawalRule candidate = new WithdrawalRule(UUID.randomUUID(), familyId, 1,
            payoutRate, feeRate, fixedFee, true, key, actor.actorId(), clock.instant());
        var existing = store.ruleByKey(familyId, key);
        if (existing.isPresent()) {
            if (existing.get().payoutRate().compareTo(candidate.payoutRate()) != 0
                || existing.get().feeRate().compareTo(candidate.feeRate()) != 0
                || existing.get().fixedFee().compareTo(candidate.fixedFee()) != 0) {
                throw new Stage3Service.ConflictException("Idempotency-Key was used for another rule");
            }
            return existing.get();
        }
        return store.createRule(familyId, candidate.payoutRate(), candidate.feeRate(),
            candidate.fixedFee(), key, actor.actorId(), clock.instant());
    }

    public WithdrawalRule activeRule(Actor actor, UUID familyId) {
        requireMember(actor, familyId);
        return store.activeRuleOrDefault(familyId, actor.actorId(), clock.instant());
    }

    public WithdrawalQuote quote(Actor actor, UUID familyId, UUID childId,
                                 BigDecimal moneyAmount, String key) {
        authorization.requireChildOrParent(actor, familyId, childId);
        requireKey(key);
        BigDecimal normalized = Stage17Models.exactMoney(moneyAmount);
        var existing = store.quoteByKey(familyId, key);
        if (existing.isPresent()) {
            if (!existing.get().childId().equals(childId)
                || existing.get().moneyAmount().compareTo(normalized) != 0) {
                throw new Stage3Service.ConflictException("Idempotency-Key was used for another quote");
            }
            return existing.get();
        }
        var rule = store.activeRuleOrDefault(familyId, actor.actorId(), clock.instant());
        var amounts = Stage17Models.quote(rule, normalized);
        var now = clock.instant();
        return store.createQuote(familyId, childId, amounts, rule, key,
            now.plus(QUOTE_LIFETIME), now);
    }

    public WithdrawalRequest request(Actor actor, UUID familyId, UUID childId,
                                     UUID quoteId, String key) {
        authorization.requireChildOrParent(actor, familyId, childId);
        requireKey(key);
        var existing = store.requestByKey(familyId, key);
        if (existing.isPresent()) {
            if (!existing.get().childId().equals(childId)
                || !existing.get().quoteId().equals(quoteId)) {
                throw new Stage3Service.ConflictException("Idempotency-Key was used for another request");
            }
            return existing.get();
        }
        var quote = store.quote(familyId, quoteId)
            .orElseThrow(FamilyGrowthService.NotFoundException::new);
        if (!quote.childId().equals(childId)) {
            throw new FamilyGrowthService.NotFoundException();
        }
        if (!quote.expiresAt().isAfter(clock.instant())) {
            throw new Stage3Service.ConflictException("Withdrawal quote expired");
        }
        return store.createRequest(familyId, quote, key, actor.actorId(), clock.instant());
    }

    @Transactional(readOnly = true)
    public List<WithdrawalRequest> requests(Actor actor, UUID familyId, UUID childId) {
        authorization.requireChildOrParent(actor, familyId, childId);
        return store.requests(familyId, childId);
    }

    public WithdrawalRequest approve(Actor actor, UUID familyId, UUID requestId, String key) {
        authorization.requireParent(actor, familyId);
        return transition(actor, familyId, requestId, WithdrawalAction.APPROVE, key);
    }

    public WithdrawalRequest reject(Actor actor, UUID familyId, UUID requestId, String key) {
        authorization.requireParent(actor, familyId);
        return transition(actor, familyId, requestId, WithdrawalAction.REJECT, key);
    }

    public WithdrawalRequest markPaid(Actor actor, UUID familyId, UUID requestId, String key) {
        authorization.requireParent(actor, familyId);
        return transition(actor, familyId, requestId, WithdrawalAction.MARK_PAID, key);
    }

    public WithdrawalRequest cancel(Actor actor, UUID familyId, UUID requestId, String key) {
        var request = store.request(familyId, requestId)
            .orElseThrow(FamilyGrowthService.NotFoundException::new);
        if (request.status() == WithdrawalStatus.APPROVED) {
            authorization.requireParent(actor, familyId);
        } else {
            authorization.requireChildOrParent(actor, familyId, request.childId());
        }
        return transition(actor, familyId, requestId, WithdrawalAction.CANCEL, key);
    }

    private WithdrawalRequest transition(Actor actor, UUID familyId, UUID requestId,
                                         WithdrawalAction action, String key) {
        requireKey(key);
        var replay = store.actionByKey(familyId, action, key);
        if (replay.isPresent()) {
            if (!replay.get().requestId().equals(requestId)) {
                throw new Stage3Service.ConflictException("Idempotency-Key was used for another action");
            }
            return store.request(familyId, requestId).orElseThrow();
        }
        return store.transition(familyId, requestId, action, key, actor.actorId(), clock.instant());
    }

    private void requireMember(Actor actor, UUID familyId) {
        if (actor != null && actor.role() == ActorRole.CHILD) {
            authorization.requireChildOrParent(actor, familyId, actor.childId());
        } else {
            authorization.requireParent(actor, familyId);
        }
    }

    private static void requireKey(String key) {
        if (key == null || key.isBlank() || key.length() > 100) {
            throw new IllegalArgumentException("Valid Idempotency-Key is required");
        }
    }

}
