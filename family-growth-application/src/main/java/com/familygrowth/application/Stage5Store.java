package com.familygrowth.application;

import com.familygrowth.domain.Stage5Models.ExchangeDirection;
import com.familygrowth.domain.Stage5Models.ExchangeOrder;
import com.familygrowth.domain.Stage5Models.ExchangePreview;
import com.familygrowth.domain.Stage5Models.ExchangeQuote;
import com.familygrowth.domain.Stage5Models.ExchangeRule;
import com.familygrowth.domain.Stage5Models.GiftMoney;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface Stage5Store {
    Optional<GiftMoney> findGift(UUID familyId, String idempotencyKey);
    GiftMoney depositGift(UUID familyId, UUID childId, BigDecimal amount, String note,
                          String idempotencyKey, UUID actorId, Instant now);
    ExchangeRule createRule(UUID familyId, BigDecimal moneyToCoinRate, BigDecimal coinToMoneyRate,
                            BigDecimal moneyToCoinFeeRate, BigDecimal coinToMoneyFeeRate,
                            BigDecimal maxSourceAmount, UUID actorId, Instant now);
    Optional<ExchangeRule> activeRule(UUID familyId);
    ExchangePreview savePreview(UUID familyId, UUID childId, ExchangeDirection direction,
                                ExchangeQuote quote, ExchangeRule rule, Instant expiresAt, Instant now);
    Optional<ExchangePreview> preview(UUID familyId, UUID previewId);
    Optional<ExchangeOrder> findOrder(UUID familyId, String idempotencyKey);
    ExchangeOrder confirm(UUID familyId, UUID previewId, String idempotencyKey, UUID actorId, Instant now);
}
