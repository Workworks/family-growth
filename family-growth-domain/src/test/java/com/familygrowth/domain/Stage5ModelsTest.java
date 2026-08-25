package com.familygrowth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class Stage5ModelsTest {
    private final Stage5Models.ExchangeRule rule = new Stage5Models.ExchangeRule(
        UUID.randomUUID(), UUID.randomUUID(), 1, new BigDecimal("10"), new BigDecimal("12"),
        BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("1000"), true,
        UUID.randomUUID(), Instant.now());

    @Test void quotesBothDirectionsWithDocumentedSpread() {
        var buy = Stage5Models.quote(rule, Stage5Models.ExchangeDirection.MONEY_TO_COIN, new BigDecimal("10.00"));
        assertThat(buy.targetAmount()).isEqualByComparingTo("100.00");
        var sell = Stage5Models.quote(rule, Stage5Models.ExchangeDirection.COIN_TO_MONEY, new BigDecimal("120.00"));
        assertThat(sell.targetAmount()).isEqualByComparingTo("10.00");
    }

    @Test void rejectsFractionalCoinAndBudgetOverflow() {
        assertThatThrownBy(() -> Stage5Models.quote(rule, Stage5Models.ExchangeDirection.COIN_TO_MONEY,
            new BigDecimal("1.50"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Stage5Models.quote(rule, Stage5Models.ExchangeDirection.MONEY_TO_COIN,
            new BigDecimal("1000.01"))).isInstanceOf(IllegalArgumentException.class);
    }
}
