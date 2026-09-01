package com.familygrowth.application;
import static com.familygrowth.domain.Stage31EconomyModels.*;import java.math.BigDecimal;import java.time.*;import java.util.*;
public interface Stage31EconomyStore{
 SavingRewardRule configureSaving(UUID family,BigDecimal rate,BigDecimal minimum,UUID actor,Instant now);SavingRewardAward settleSaving(UUID family,UUID child,String cycle,String key,UUID actor,Instant now);
 SimulatedMarketRule configureMarket(UUID family,UUID fund,String seed,int bps,UUID actor,Instant now);SimulatedMarketTick tick(UUID family,UUID fund,LocalDate date,String key,UUID actor,Instant now);
 HoldingFeeRule configureHolding(UUID family,UUID fund,int days,BigDecimal rate,UUID actor,Instant now);EconomyLabSnapshot snapshot(UUID family);UUID createCosmetic(UUID family,String title,long coinCost,int stock,UUID actor,Instant now);
}
