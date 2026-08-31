package com.familygrowth.application;
import com.familygrowth.domain.Stage28RewardModels.*;import com.familygrowth.domain.Stage3Models.RewardGrant;import com.familygrowth.domain.Stage5Models.ExchangeDirection;import java.math.BigDecimal;import java.time.Instant;import java.util.*;
public interface Stage28RewardStore{
 BudgetRule createBudgetRule(UUID family,UUID child,String zone,BigDecimal daily,BigDecimal weekly,BigDecimal monthly,OverflowPolicy policy,BigDecimal coinRate,BigDecimal xpRate,UUID actor,String key,String hash,Instant now);
 Optional<BudgetRule> budgetRuleReplay(UUID family,String key);Optional<BudgetRule> activeBudgetRule(UUID family,UUID child);BudgetSummary budgetSummary(UUID family,UUID child,Instant now);
 Optional<BudgetOverride> overrideReplay(UUID family,String key);BudgetOverride createOverride(UUID family,UUID child,UUID completion,RewardGrant proposed,String reason,UUID actor,String key,String hash,Instant now);
 RewardGrant governReward(UUID family,UUID child,UUID completion,RewardGrant proposed,UUID actor,String reviewKey,Instant now);
 ExchangeControlRule createExchangeControl(UUID family,boolean m2c,boolean c2m,BigDecimal m2cDaily,BigDecimal m2cMonthly,BigDecimal c2mDaily,BigDecimal c2mMonthly,boolean approval,String zone,UUID actor,String key,String hash,Instant now);
 Optional<ExchangeControlRule> exchangeControlReplay(UUID family,String key);Optional<ExchangeControlRule> activeExchangeControl(UUID family);
 void validateAndBindPreview(UUID family,UUID child,ExchangeDirection direction,BigDecimal source,UUID preview,Instant now);void validateExchangeConfirm(UUID family,UUID preview,boolean childActor,Instant now);
 Optional<ExchangeApproval> approvalReplay(UUID family,String key);Optional<ExchangeApproval> approval(UUID family,UUID id);List<ExchangeApproval> approvals(UUID family,UUID child);ExchangeApproval submitApproval(UUID family,UUID child,UUID preview,UUID actor,String key,Instant now);ExchangeApproval finishApproval(UUID family,UUID id,boolean approved,String note,UUID order,UUID actor,String key,Instant now);
 Optional<Fulfillment> fulfillmentReplay(UUID family,String key);Fulfillment fulfill(UUID family,UUID order,String note,UUID actor,String key,Instant now);
 void redactChildFreeText(UUID family,UUID child);
}
