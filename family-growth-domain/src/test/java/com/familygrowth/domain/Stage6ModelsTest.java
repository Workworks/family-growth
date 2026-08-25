package com.familygrowth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.familygrowth.domain.Stage6Models.SavingDirection;
import com.familygrowth.domain.Stage6Models.SavingTransaction;
import com.familygrowth.domain.Stage6Models.Wish;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class Stage6ModelsTest {
    @Test void savingTransferConservesTotalMoney() {
        var tx = new SavingTransaction(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),SavingDirection.DEPOSIT,
            new BigDecimal("20.00"),new BigDecimal("80.00"),new BigDecimal("60.00"),
            new BigDecimal("10.00"),new BigDecimal("30.00"),UUID.randomUUID(),"k",UUID.randomUUID(),Instant.now());
        assertThat(tx.walletAfter().add(tx.savingAfter())).isEqualByComparingTo("90.00");
    }
    @Test void rejectsNonConservingTransferAndComputesWishProgress() {
        assertThatThrownBy(() -> new SavingTransaction(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),SavingDirection.DEPOSIT,
            new BigDecimal("20.00"),new BigDecimal("80.00"),new BigDecimal("60.00"),new BigDecimal("10.00"),
            new BigDecimal("31.00"),UUID.randomUUID(),"k",UUID.randomUUID(),Instant.now())).isInstanceOf(IllegalArgumentException.class);
        var wish = new Wish(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),"积木",new BigDecimal("50.00"),
            new BigDecimal("20.00"),BigDecimal.ZERO,false,1,Instant.now());
        assertThat(wish.progressPercent()).isEqualByComparingTo("40.00");
    }
}
