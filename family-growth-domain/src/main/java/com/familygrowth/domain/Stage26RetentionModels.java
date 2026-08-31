package com.familygrowth.domain;
import java.time.Instant;import java.util.UUID;
public final class Stage26RetentionModels {private Stage26RetentionModels(){}
 public record RetentionPolicy(UUID familyId,UUID childId,int usageDetailDays,long version,Instant updatedAt){}
 public record RetentionTarget(UUID familyId,UUID childId,int usageDetailDays){}
 public record RetentionRun(UUID id,UUID familyId,UUID childId,String triggerType,int usageEventsDeleted,int allowancesRedacted,int expiredTokensCleared,Instant cutoffAt,Instant createdAt){}
}
