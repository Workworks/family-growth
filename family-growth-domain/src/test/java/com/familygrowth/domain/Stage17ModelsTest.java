package com.familygrowth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class Stage17ModelsTest {
    @Test
    void quoteUsesTransparentFixedAndProportionalFees() {
        var rule = rule("1.000000", "0.025000", "1.00");
        var quote = Stage17Models.quote(rule, new BigDecimal("100.00"));

        assertThat(quote.grossPayout()).isEqualByComparingTo("100.00");
        assertThat(quote.feeAmount()).isEqualByComparingTo("3.50");
        assertThat(quote.netPayout()).isEqualByComparingTo("96.50");
    }

    @Test
    void quoteRoundsCnyAtCentAndRejectsNonPositiveNet() {
        var rounded = Stage17Models.quote(rule("1.234567", "0.000000", "0.00"),
            new BigDecimal("3.00"));
        assertThat(rounded.grossPayout()).isEqualByComparingTo("3.70");

        assertThatThrownBy(() -> Stage17Models.quote(
            rule("1.000000", "0.000000", "10.00"), new BigDecimal("10.00")))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Stage17Models.quote(
            rule("1.000000", "0.000000", "0.00"), new BigDecimal("1.001")))
            .isInstanceOf(ArithmeticException.class);
    }

    private static Stage17Models.WithdrawalRule rule(String rate, String feeRate, String fixed) {
        return new Stage17Models.WithdrawalRule(UUID.randomUUID(), UUID.randomUUID(), 1,
            new BigDecimal(rate), new BigDecimal(feeRate), new BigDecimal(fixed), true,
            "rule", UUID.randomUUID(), Instant.parse("2026-08-26T00:00:00Z"));
    }
}
