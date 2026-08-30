package com.familygrowth.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

public final class Stage23LearningModels {
    private Stage23LearningModels() { }

    public enum AssignmentSource { PARENT, AUTONOMOUS }

    public record RewardPolicy(UUID familyId, UUID childId, BigDecimal moneyReward,
                               long coinReward, long xpReward, long version, Instant updatedAt) {
        public RewardPolicy {
            moneyReward = normalize(moneyReward);
            if (coinReward < 0 || coinReward > 1_000_000 || xpReward < 0 || xpReward > 1_000_000) {
                throw new IllegalArgumentException("Reward is outside the supported range");
            }
        }
    }

    public record RewardSnapshot(BigDecimal money, long coin, long xp, Instant settledAt) {
        public RewardSnapshot {
            money = normalize(money);
            if (coin < 0 || xp < 0) throw new IllegalArgumentException("Reward cannot be negative");
        }
        public boolean settled() { return settledAt != null; }
    }

    public static BigDecimal normalize(BigDecimal value) {
        BigDecimal result = value == null ? BigDecimal.ZERO.setScale(2) : value.setScale(2, RoundingMode.HALF_UP);
        if (result.signum() < 0 || result.compareTo(new BigDecimal("10000.00")) > 0) {
            throw new IllegalArgumentException("Money reward is outside the supported range");
        }
        return result;
    }
}
