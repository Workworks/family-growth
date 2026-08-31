package com.familygrowth.domain;
import com.familygrowth.domain.Stage3Models.Wallet;import java.math.BigDecimal;import java.time.*;import java.util.UUID;
public final class Stage9Models{private Stage9Models(){}public enum UsageEventType{APP_ACTIVE,LEARNING}
 public record UsagePolicy(UUID familyId,UUID childId,String zoneId,int dailyLimitMinutes,int sessionLimitMinutes,int restMinutes,LocalTime quietStart,LocalTime quietEnd,long version,Instant updatedAt){
  public UsagePolicy(UUID familyId,UUID childId,String zoneId,int dailyLimitMinutes,int sessionLimitMinutes,long version,Instant updatedAt){this(familyId,childId,zoneId,dailyLimitMinutes,sessionLimitMinutes,10,LocalTime.of(21,30),LocalTime.of(6,30),version,updatedAt);}
  public UsagePolicy(UUID familyId,UUID childId,String zoneId,int dailyLimitMinutes,int sessionLimitMinutes,LocalTime quietStart,LocalTime quietEnd,long version,Instant updatedAt){this(familyId,childId,zoneId,dailyLimitMinutes,sessionLimitMinutes,10,quietStart,quietEnd,version,updatedAt);}
 }
 public record TemporaryAllowance(UUID id,UUID familyId,UUID childId,String reason,Instant startsAt,Instant expiresAt,UUID actorId,Instant createdAt){}
 public record UsageSessionState(int sessionMinutes,Instant lastActivityAt,Instant restUntil){}
 public static UsageSessionState nextSession(UsageSessionState old,UsagePolicy policy,int minutes,Instant occurredAt,Instant now){
  if(minutes<1||occurredAt.isBefore(now.minusSeconds(300)))return old;
  if(old.restUntil()!=null&&old.restUntil().isAfter(now))return old;
  boolean reset=old.lastActivityAt()==null||!old.lastActivityAt().plusSeconds(policy.restMinutes()*60L).isAfter(occurredAt);
  int total=(reset?0:old.sessionMinutes())+minutes;
  Instant rest=total>=policy.sessionLimitMinutes()?occurredAt.plusSeconds(policy.restMinutes()*60L):null;
  return new UsageSessionState(total,occurredAt,rest);
 }
 public record UsageAccessState(boolean allowed,String reasonCode,String message,int usedTodayMinutes,int dailyLimitMinutes,int sessionUsedMinutes,int sessionLimitMinutes,int restMinutes,Instant restUntil,Instant allowanceExpiresAt,Instant evaluatedAt){}
 public record UsageEvent(UUID id,UUID familyId,UUID childId,UsageEventType type,int minutes,Instant occurredAt,String idempotencyKey,UUID actorId,Instant createdAt){}
 public record TodayReport(UUID familyId,UUID childId,LocalDate date,String zoneId,int appMinutes,int learningMinutes,int submittedTasks,int approvedTasks,int pendingReviews,Wallet wallet){}
 public record MonthlyReport(UUID familyId,UUID childId,String month,String zoneId,int appMinutes,int learningMinutes,int submittedTasks,int approvedTasks,long xpGranted,
  BigDecimal moneyIncome,BigDecimal moneyExpense,long coinIncome,long coinExpense,BigDecimal giftMoney,BigDecimal exchangeFees,BigDecimal savingBalance,
  BigDecimal fundMarketValue,BigDecimal fundFees,BigDecimal fundRealizedPnl,BigDecimal fundUnrealizedPnl,boolean walletLedgerBalanced){}
}
