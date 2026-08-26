package com.familygrowth.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Stage17Models {
    public static final String EDUCATION_NOTICE =
        "这是家庭内部的线下零钱约定，不是银行提现或平台支付。";

    private Stage17Models() {
    }

    public enum WithdrawalStatus { REQUESTED, APPROVED, PAID, REJECTED, CANCELLED }
    public enum WithdrawalAction { APPROVE, REJECT, CANCEL, MARK_PAID }

    public record WithdrawalRule(
        UUID id, UUID familyId, long version, BigDecimal payoutRate, BigDecimal feeRate,
        BigDecimal fixedFee, boolean active, String idempotencyKey, UUID actorId, Instant createdAt
    ) {
        public WithdrawalRule {
            Objects.requireNonNull(id);
            Objects.requireNonNull(familyId);
            payoutRate = rate(payoutRate);
            feeRate = normalizedFeeRate(feeRate);
            fixedFee = money(fixedFee);
            if (version < 1 || payoutRate.signum() <= 0 || feeRate.signum() < 0
                || feeRate.compareTo(BigDecimal.ONE) >= 0 || fixedFee.signum() < 0) {
                throw new IllegalArgumentException("Withdrawal rule is invalid");
            }
        }
    }

    public record WithdrawalAmounts(
        BigDecimal moneyAmount, BigDecimal grossPayout, BigDecimal feeAmount, BigDecimal netPayout
    ) {
        public WithdrawalAmounts {
            moneyAmount = exactMoney(moneyAmount);
            grossPayout = money(grossPayout);
            feeAmount = money(feeAmount);
            netPayout = money(netPayout);
            if (moneyAmount.signum() <= 0 || grossPayout.signum() <= 0 || feeAmount.signum() < 0
                || netPayout.signum() <= 0 || grossPayout.subtract(feeAmount).compareTo(netPayout) != 0) {
                throw new IllegalArgumentException("Withdrawal quote is invalid");
            }
        }
    }

    public record WithdrawalQuote(
        UUID id, UUID familyId, UUID childId, BigDecimal moneyAmount, BigDecimal payoutRate,
        BigDecimal grossPayout, BigDecimal feeRate, BigDecimal fixedFee, BigDecimal feeAmount,
        BigDecimal netPayout, UUID ruleId, long ruleVersion, String idempotencyKey,
        String notice, Instant expiresAt, Instant createdAt
    ) {
        public WithdrawalQuote {
            moneyAmount = exactMoney(moneyAmount);
            payoutRate = rate(payoutRate);
            grossPayout = money(grossPayout);
            feeRate = normalizedFeeRate(feeRate);
            fixedFee = money(fixedFee);
            feeAmount = money(feeAmount);
            netPayout = money(netPayout);
        }
    }

    public record WithdrawalRequest(
        UUID id, UUID quoteId, UUID familyId, UUID childId, BigDecimal moneyAmount,
        BigDecimal payoutRate, BigDecimal grossPayout, BigDecimal feeRate, BigDecimal fixedFee,
        BigDecimal feeAmount, BigDecimal netPayout, UUID ruleId, long ruleVersion,
        WithdrawalStatus status, UUID ledgerGroupId, String requestKey, UUID requestedBy,
        UUID decidedBy, UUID paidBy, UUID cancelledBy, Instant requestedAt, Instant decidedAt,
        Instant paidAt, Instant cancelledAt
    ) {
        public WithdrawalRequest {
            moneyAmount = exactMoney(moneyAmount);
            payoutRate = rate(payoutRate);
            grossPayout = money(grossPayout);
            feeRate = normalizedFeeRate(feeRate);
            fixedFee = money(fixedFee);
            feeAmount = money(feeAmount);
            netPayout = money(netPayout);
        }
    }

    public record WithdrawalActionReplay(
        UUID requestId, WithdrawalAction action, String idempotencyKey
    ) {
    }

    public static WithdrawalAmounts quote(WithdrawalRule rule, BigDecimal requestedMoney) {
        Objects.requireNonNull(rule);
        BigDecimal amount = exactMoney(requestedMoney);
        if (amount.signum() <= 0 || amount.compareTo(new BigDecimal("100000.00")) > 0) {
            throw new IllegalArgumentException("Withdrawal amount is invalid");
        }
        BigDecimal gross = amount.multiply(rule.payoutRate()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal fee = gross.multiply(rule.feeRate()).setScale(2, RoundingMode.HALF_UP)
            .add(rule.fixedFee()).setScale(2, RoundingMode.HALF_UP);
        return new WithdrawalAmounts(amount, gross, fee, gross.subtract(fee));
    }

    public static BigDecimal exactMoney(BigDecimal value) {
        Objects.requireNonNull(value, "money");
        return value.setScale(2, RoundingMode.UNNECESSARY);
    }

    private static BigDecimal money(BigDecimal value) {
        Objects.requireNonNull(value, "money");
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal rate(BigDecimal value) {
        Objects.requireNonNull(value, "rate");
        return value.setScale(6, RoundingMode.UNNECESSARY);
    }

    private static BigDecimal normalizedFeeRate(BigDecimal value) {
        Objects.requireNonNull(value, "feeRate");
        return value.setScale(6, RoundingMode.UNNECESSARY);
    }
}
