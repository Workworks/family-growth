package com.familygrowth.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Stage5Models {
    public static final String EDUCATION_NOTICE = "家庭内部教育账本，不代表真实货币或投资收益";
    private Stage5Models() {}

    public enum ExchangeDirection { MONEY_TO_COIN, COIN_TO_MONEY }
    public enum PreviewStatus { OPEN, CONFIRMED }

    public record GiftMoney(
        UUID id, UUID familyId, UUID childId, BigDecimal amount, String note,
        UUID ledgerGroupId, String idempotencyKey, UUID actorId, Instant createdAt
    ) {
        public GiftMoney { amount = money(amount); }
    }

    public record ExchangeRule(
        UUID id, UUID familyId, long version, BigDecimal moneyToCoinRate,
        BigDecimal coinToMoneyRate, BigDecimal moneyToCoinFeeRate,
        BigDecimal coinToMoneyFeeRate, BigDecimal maxSourceAmount,
        boolean active, UUID actorId, Instant createdAt
    ) {
        public ExchangeRule {
            moneyToCoinRate = rate(moneyToCoinRate);
            coinToMoneyRate = rate(coinToMoneyRate);
            moneyToCoinFeeRate = fee(moneyToCoinFeeRate);
            coinToMoneyFeeRate = fee(coinToMoneyFeeRate);
            maxSourceAmount = money(maxSourceAmount);
            if (moneyToCoinRate.signum() <= 0 || coinToMoneyRate.signum() <= 0
                || maxSourceAmount.signum() <= 0) {
                throw new IllegalArgumentException("Exchange rates and budget must be positive");
            }
        }
    }

    public record ExchangeQuote(
        BigDecimal sourceAmount, BigDecimal sourceFee, BigDecimal netSource,
        BigDecimal targetAmount
    ) {}

    public record ExchangePreview(
        UUID id, UUID familyId, UUID childId, ExchangeDirection direction,
        BigDecimal sourceAmount, BigDecimal sourceFee, BigDecimal netSource,
        BigDecimal targetAmount, BigDecimal appliedRate, BigDecimal appliedFeeRate,
        String educationNotice, UUID ruleId, long ruleVersion, PreviewStatus status,
        Instant expiresAt, UUID confirmedOrderId, Instant createdAt
    ) {}

    public record ExchangeOrder(
        UUID id, UUID previewId, UUID familyId, UUID childId, ExchangeDirection direction,
        BigDecimal sourceAmount, BigDecimal sourceFee, BigDecimal targetAmount,
        UUID ruleId, long ruleVersion, UUID ledgerGroupId, String idempotencyKey,
        UUID actorId, Instant createdAt
    ) {}

    public static ExchangeQuote quote(ExchangeRule rule, ExchangeDirection direction, BigDecimal rawSource) {
        Objects.requireNonNull(rule);
        Objects.requireNonNull(direction);
        Objects.requireNonNull(rawSource);
        BigDecimal source = rawSource.setScale(2, RoundingMode.UNNECESSARY);
        if (source.signum() <= 0 || source.compareTo(rule.maxSourceAmount()) > 0) {
            throw new IllegalArgumentException("Source amount is outside the configured budget");
        }
        if (direction == ExchangeDirection.COIN_TO_MONEY && source.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException("Coin source amount must be an integer");
        }
        if (direction == ExchangeDirection.MONEY_TO_COIN) {
            BigDecimal fee = source.multiply(rule.moneyToCoinFeeRate()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal net = source.subtract(fee);
            BigDecimal target = net.multiply(rule.moneyToCoinRate()).setScale(0, RoundingMode.DOWN);
            if (target.signum() <= 0) throw new IllegalArgumentException("Exchange target is zero");
            return new ExchangeQuote(source, fee, net, target.setScale(2));
        }
        BigDecimal fee = source.multiply(rule.coinToMoneyFeeRate()).setScale(0, RoundingMode.CEILING);
        BigDecimal net = source.subtract(fee);
        BigDecimal target = net.divide(rule.coinToMoneyRate(), 2, RoundingMode.DOWN);
        if (target.signum() <= 0) throw new IllegalArgumentException("Exchange target is zero");
        return new ExchangeQuote(source, fee.setScale(2), net, target);
    }

    private static BigDecimal money(BigDecimal value) {
        return Objects.requireNonNull(value).setScale(2, RoundingMode.UNNECESSARY);
    }

    private static BigDecimal rate(BigDecimal value) {
        return Objects.requireNonNull(value).setScale(6, RoundingMode.HALF_UP);
    }

    private static BigDecimal fee(BigDecimal value) {
        BigDecimal result = Objects.requireNonNull(value).setScale(6, RoundingMode.HALF_UP);
        if (result.signum() < 0 || result.compareTo(BigDecimal.ONE) >= 0) {
            throw new IllegalArgumentException("Fee rate must be in [0,1)");
        }
        return result;
    }
}
