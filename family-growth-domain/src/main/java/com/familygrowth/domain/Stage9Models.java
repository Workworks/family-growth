package com.familygrowth.domain;
import com.familygrowth.domain.Stage3Models.Wallet;import java.math.BigDecimal;import java.time.*;import java.util.UUID;
public final class Stage9Models{private Stage9Models(){}public enum UsageEventType{APP_ACTIVE,LEARNING}
 public record UsagePolicy(UUID familyId,UUID childId,String zoneId,int dailyLimitMinutes,int sessionLimitMinutes,long version,Instant updatedAt){}
 public record UsageEvent(UUID id,UUID familyId,UUID childId,UsageEventType type,int minutes,Instant occurredAt,String idempotencyKey,UUID actorId,Instant createdAt){}
 public record TodayReport(UUID familyId,UUID childId,LocalDate date,String zoneId,int appMinutes,int learningMinutes,int submittedTasks,int approvedTasks,int pendingReviews,Wallet wallet){}
 public record MonthlyReport(UUID familyId,UUID childId,String month,String zoneId,int appMinutes,int learningMinutes,int submittedTasks,int approvedTasks,long xpGranted,
  BigDecimal moneyIncome,BigDecimal moneyExpense,long coinIncome,long coinExpense,BigDecimal giftMoney,BigDecimal exchangeFees,BigDecimal savingBalance,
  BigDecimal fundMarketValue,BigDecimal fundFees,BigDecimal fundRealizedPnl,BigDecimal fundUnrealizedPnl,boolean walletLedgerBalanced){}
}
