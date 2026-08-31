package com.familygrowth.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Stage6Models {
    private Stage6Models() {}
    public enum RewardOrderStatus { CREATED, APPROVED, REJECTED, CANCELED, FULFILLED }
    public enum SavingDirection { DEPOSIT, WITHDRAW }

    public record RewardProduct(UUID id, UUID familyId, String title, long coinCost,
                                int stockCount, boolean active, long version, Instant createdAt) {
        public RewardProduct {
            if (title == null || title.isBlank() || coinCost <= 0 || stockCount < 0) {
                throw new IllegalArgumentException("Reward product is invalid");
            }
        }
    }
    public record RewardOrder(UUID id, UUID familyId, UUID childId, UUID productId,
                              String productTitle, long coinCost, RewardOrderStatus status,
                              String submitKey, String reviewKey, UUID reviewedBy,
                              UUID ledgerGroupId, Instant createdAt, Instant reviewedAt,
                              String fulfillmentNote,String fulfillKey,UUID fulfilledBy,Instant fulfilledAt) {
        public RewardOrder(UUID id,UUID familyId,UUID childId,UUID productId,String productTitle,long coinCost,RewardOrderStatus status,String submitKey,String reviewKey,UUID reviewedBy,UUID ledgerGroupId,Instant createdAt,Instant reviewedAt){this(id,familyId,childId,productId,productTitle,coinCost,status,submitKey,reviewKey,reviewedBy,ledgerGroupId,createdAt,reviewedAt,"",null,null,null);}
    }
    public record SavingAccount(UUID familyId, UUID childId, BigDecimal balance, long version) {
        public SavingAccount { balance = money(balance); if (balance.signum() < 0) throw new IllegalArgumentException("Saving balance cannot be negative"); }
    }
    public record SavingTransaction(UUID id, UUID familyId, UUID childId, SavingDirection direction,
                                    BigDecimal amount, BigDecimal walletBefore, BigDecimal walletAfter,
                                    BigDecimal savingBefore, BigDecimal savingAfter, UUID ledgerGroupId,
                                    String idempotencyKey, UUID actorId, Instant createdAt) {
        public SavingTransaction {
            amount=money(amount); walletBefore=money(walletBefore); walletAfter=money(walletAfter);
            savingBefore=money(savingBefore); savingAfter=money(savingAfter);
            BigDecimal signed = direction == SavingDirection.DEPOSIT ? amount.negate() : amount;
            if (walletAfter.compareTo(walletBefore.add(signed)) != 0 ||
                walletBefore.add(savingBefore).compareTo(walletAfter.add(savingAfter)) != 0) {
                throw new IllegalArgumentException("Saving transfer must conserve Money");
            }
        }
    }
    public record Wish(UUID id, UUID familyId, UUID childId, String title, BigDecimal targetAmount,
                       BigDecimal allocatedAmount, BigDecimal progressPercent, boolean achieved,
                       long version, Instant createdAt) {
        public Wish {
            targetAmount=money(targetAmount); allocatedAmount=money(allocatedAmount);
            if (targetAmount.signum() <= 0 || allocatedAmount.signum() < 0) throw new IllegalArgumentException("Wish amounts are invalid");
            progressPercent = allocatedAmount.multiply(new BigDecimal("100"))
                .divide(targetAmount, 2, RoundingMode.DOWN).min(new BigDecimal("100.00"));
            achieved = allocatedAmount.compareTo(targetAmount) >= 0;
        }
    }
    public static BigDecimal money(BigDecimal value) {
        return Objects.requireNonNull(value).setScale(2, RoundingMode.UNNECESSARY);
    }
}
