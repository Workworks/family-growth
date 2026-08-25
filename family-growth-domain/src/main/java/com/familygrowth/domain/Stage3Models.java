package com.familygrowth.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Stage3Models {
    public static final String ACTOR_REQUEST_ATTRIBUTE = "familyGrowthActor";
    public static final BigDecimal ZERO_MONEY = new BigDecimal("0.00");

    private Stage3Models() {
    }

    public enum ActorRole { PARENT, CHILD }
    public enum CompletionStatus { SUBMITTED, APPROVED, REJECTED }
    public enum AssetType { MONEY, COIN }

    public record Actor(UUID familyId, UUID actorId, ActorRole role, UUID childId) {
        public Actor {
            Objects.requireNonNull(familyId);
            Objects.requireNonNull(actorId);
            Objects.requireNonNull(role);
            if (role == ActorRole.CHILD && childId == null) {
                throw new IllegalArgumentException("Child actor requires childId");
            }
        }
    }

    public record AuthSession(String token, Actor actor, Instant expiresAt) {
        public AuthSession {
            Objects.requireNonNull(token);
            Objects.requireNonNull(actor);
            Objects.requireNonNull(expiresAt);
        }
    }

    public record TaskCompletion(
        UUID id,
        UUID familyId,
        UUID childId,
        UUID taskId,
        CompletionStatus status,
        String evidenceNote,
        UUID submittedBy,
        UUID reviewedBy,
        String reviewNote,
        long xpReward,
        long coinReward,
        BigDecimal moneyReward,
        UUID ledgerGroupId,
        Instant submittedAt,
        Instant reviewedAt
    ) {
        public TaskCompletion {
            moneyReward = money(moneyReward);
        }
    }

    public record Wallet(UUID childId, UUID familyId, BigDecimal moneyBalance, long coinBalance, long version) {
        public Wallet {
            moneyBalance = money(moneyBalance);
            if (moneyBalance.signum() < 0 || coinBalance < 0) {
                throw new IllegalArgumentException("Wallet balance cannot be negative");
            }
        }
    }

    public record LedgerEntry(
        UUID id,
        UUID familyId,
        UUID childId,
        AssetType assetType,
        BigDecimal delta,
        BigDecimal beforeBalance,
        BigDecimal afterBalance,
        String entryType,
        String businessType,
        UUID businessId,
        UUID groupId,
        UUID actorId,
        String reason,
        Instant createdAt
    ) {
        public LedgerEntry {
            delta = money(delta);
            beforeBalance = money(beforeBalance);
            afterBalance = money(afterBalance);
            if (afterBalance.compareTo(beforeBalance.add(delta)) != 0) {
                throw new IllegalArgumentException("Ledger arithmetic mismatch");
            }
        }
    }

    public record WalletReconciliation(
        UUID familyId,
        UUID childId,
        BigDecimal walletMoney,
        BigDecimal ledgerMoney,
        long walletCoin,
        long ledgerCoin,
        boolean balanced
    ) {
        public WalletReconciliation {
            walletMoney = money(walletMoney);
            ledgerMoney = money(ledgerMoney);
            balanced = walletMoney.compareTo(ledgerMoney) == 0 && walletCoin == ledgerCoin;
        }
    }

    public record RewardGrant(long xp, long coin, BigDecimal money) {
        public RewardGrant {
            money = Stage3Models.money(money);
            if (xp < 0 || coin < 0 || money.signum() < 0) {
                throw new IllegalArgumentException("Reward cannot be negative");
            }
        }
    }

    public static BigDecimal money(BigDecimal value) {
        Objects.requireNonNull(value, "money");
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
