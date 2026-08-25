package com.familygrowth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class Stage3ModelsTest {
    @Test
    void moneyUsesHalfUpCentsAndRejectsNegativeReward() {
        assertThat(Stage3Models.money(new BigDecimal("2.345"))).isEqualByComparingTo("2.35");
        assertThatThrownBy(() -> new Stage3Models.RewardGrant(0, -1, BigDecimal.ZERO))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ledgerRequiresExactBeforeDeltaAfterArithmetic() {
        var id = UUID.randomUUID();
        assertThatThrownBy(() -> new Stage3Models.LedgerEntry(
            id, id, id, Stage3Models.AssetType.MONEY,
            new BigDecimal("2.00"), new BigDecimal("1.00"), new BigDecimal("4.00"),
            "TASK_REWARD", "TASK_COMPLETION", id, id, id, "reward", Instant.now()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void childActorRequiresChildScope() {
        var id = UUID.randomUUID();
        assertThatThrownBy(() -> new Stage3Models.Actor(id, id, Stage3Models.ActorRole.CHILD, null))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
