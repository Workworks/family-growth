package com.familygrowth.domain;
import com.familygrowth.domain.Stage3Models.Wallet;import java.math.BigDecimal;import java.time.*;import java.util.UUID;
public final class Stage9Models{private Stage9Models(){}public enum UsageEventType{APP_ACTIVE,LEARNING}
 public record UsagePolicy(UUID familyId,UUID childId,String zoneId,int dailyLimitMinutes,int sessionLimitMinutes,LocalTime quietStart,LocalTime quietEnd,long version,Instant updatedAt){
  public UsagePolicy(UUID familyId,UUID childId,String zoneId,int dailyLimitMinutes,int sessionLimitMinutes,long version,Instant updatedAt){this(familyId,childId,zoneId,dailyLimitMinutes,sessionLimitMinutes,LocalTime.of(21,30),LocalTime.of(6,30),version,updatedAt);}
 }
 public record TemporaryAllowance(UUID id,UUID familyId,UUID childId,String reason,Instant startsAt,Instant expiresAt,UUID actorId,Instant createdAt){}
 public record UsageAccessState(boolean allowed,String reasonCode,String message,int usedTodayMinutes,int dailyLimitMinutes,Instant allowanceExpiresAt,Instant evaluatedAt){}
 public record UsageEvent(UUID id,UUID familyId,UUID childId,UsageEventType type,int minutes,Instant occurredAt,String idempotencyKey,UUID actorId,Instant createdAt){}
 public record TodayReport(UUID familyId,UUID childId,LocalDate date,String zoneId,int appMinutes,int learningMinutes,int submittedTasks,int approvedTasks,int pendingReviews,Wallet wallet){}
 public record MonthlyReport(UUID familyId,UUID childId,String month,String zoneId,int appMinutes,int learningMinutes,int submittedTasks,int approvedTasks,long xpGranted,
  BigDecimal moneyIncome,BigDecimal moneyExpense,long coinIncome,long coinExpense,BigDecimal giftMoney,BigDecimal exchangeFees,BigDecimal savingBalance,
  BigDecimal fundMarketValue,BigDecimal fundFees,BigDecimal fundRealizedPnl,BigDecimal fundUnrealizedPnl,boolean walletLedgerBalanced){}
}
