package com.familygrowth.application;

import com.familygrowth.domain.Stage17Models.WithdrawalAction;
import com.familygrowth.domain.Stage17Models.WithdrawalActionReplay;
import com.familygrowth.domain.Stage17Models.WithdrawalAmounts;
import com.familygrowth.domain.Stage17Models.WithdrawalQuote;
import com.familygrowth.domain.Stage17Models.WithdrawalRequest;
import com.familygrowth.domain.Stage17Models.WithdrawalRule;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface Stage17Store {
    Optional<WithdrawalRule> ruleByKey(UUID familyId, String key);
    WithdrawalRule createRule(UUID familyId, BigDecimal payoutRate, BigDecimal feeRate,
                              BigDecimal fixedFee, String key, UUID actorId, Instant now);
    WithdrawalRule activeRuleOrDefault(UUID familyId, UUID actorId, Instant now);

    Optional<WithdrawalQuote> quoteByKey(UUID familyId, String key);
    Optional<WithdrawalQuote> quote(UUID familyId, UUID quoteId);
    WithdrawalQuote createQuote(UUID familyId, UUID childId, WithdrawalAmounts amounts,
                                WithdrawalRule rule, String key, Instant expiresAt, Instant now);

    Optional<WithdrawalRequest> requestByKey(UUID familyId, String key);
    Optional<WithdrawalRequest> request(UUID familyId, UUID requestId);
    List<WithdrawalRequest> requests(UUID familyId, UUID childId);
    WithdrawalRequest createRequest(UUID familyId, WithdrawalQuote quote, String key,
                                    UUID actorId, Instant now);

    Optional<WithdrawalActionReplay> actionByKey(UUID familyId, WithdrawalAction action, String key);
    WithdrawalRequest transition(UUID familyId, UUID requestId, WithdrawalAction action,
                                 String key, UUID actorId, Instant now);
}
