package com.familygrowth.domain;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.familygrowth.domain.Stage7Models.TradeSide;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
class Stage7ModelsTest{
 @Test void fixedBuyVectorIsExact(){var q=Stage7Models.quote(TradeSide.BUY,new BigDecimal("20.00"),new BigDecimal("1.000000"),new BigDecimal("0.050000"));assertThat(q.feeAmount()).isEqualByComparingTo("1.00");assertThat(q.netMoney()).isEqualByComparingTo("19.00");assertThat(q.shares()).isEqualByComparingTo("19.00000000");}
 @Test void sellRoundsDownAndRejectsInvalidFees(){var q=Stage7Models.quote(TradeSide.SELL,new BigDecimal("5.00000000"),new BigDecimal("1.100000"),new BigDecimal("0.020000"));assertThat(q.grossMoney()).isEqualByComparingTo("5.50");assertThat(q.feeAmount()).isEqualByComparingTo("0.11");assertThatThrownBy(()->Stage7Models.quote(TradeSide.BUY,new BigDecimal("1.00"),BigDecimal.ONE,BigDecimal.ONE)).isInstanceOf(IllegalArgumentException.class);}
}
