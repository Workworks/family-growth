package com.familygrowth.domain;
import java.math.BigDecimal;import java.time.Instant;import java.time.LocalDate;import java.util.List;import java.util.UUID;
public final class Stage31EconomyModels{private Stage31EconomyModels(){}
 public record SavingRewardRule(UUID id,long version,BigDecimal periodicRate,BigDecimal minimumBalance,Instant createdAt){}
 public record SavingRewardAward(UUID id,UUID childId,String cycleKey,BigDecimal baseBalance,BigDecimal rewardAmount,UUID ledgerGroupId,Instant createdAt){}
 public record SimulatedMarketRule(UUID id,UUID fundId,long version,String seedLabel,int maximumDailyBps,Instant createdAt){}
 public record SimulatedMarketTick(UUID id,UUID fundId,LocalDate tickDate,int changeBps,BigDecimal navBefore,BigDecimal navAfter,UUID fundNavId,Instant createdAt){}
 public record HoldingFeeRule(UUID id,UUID fundId,long version,int minimumHoldingDays,BigDecimal earlyFeeRate,Instant createdAt){}
 public record EconomyLabSnapshot(SavingRewardRule savingRule,List<SimulatedMarketRule> marketRules,List<HoldingFeeRule> holdingRules,String notice){}
 public static final String NOTICE="全部金额与涨跌仅用于家庭教育模拟，不连接真实资金、行情或投资建议";
}
