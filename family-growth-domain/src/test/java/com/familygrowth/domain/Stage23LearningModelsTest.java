package com.familygrowth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.familygrowth.domain.Stage23LearningModels.RewardPolicy;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class Stage23LearningModelsTest {
    @Test
    void fixedRewardUsesCentsAndRejectsNegativeOrManipulativeRanges() {
        RewardPolicy policy = new RewardPolicy(UUID.randomUUID(), UUID.randomUUID(),
            new BigDecimal("1.255"), 2, 3, 0, Instant.EPOCH);
        assertThat(policy.moneyReward()).isEqualByComparingTo("1.26");
        assertThatThrownBy(() -> new RewardPolicy(UUID.randomUUID(), UUID.randomUUID(),
            new BigDecimal("-0.01"), 0, 0, 0, Instant.EPOCH)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RewardPolicy(UUID.randomUUID(), UUID.randomUUID(),
            BigDecimal.ZERO, 1_000_001, 0, 0, Instant.EPOCH)).isInstanceOf(IllegalArgumentException.class);
    }
}
