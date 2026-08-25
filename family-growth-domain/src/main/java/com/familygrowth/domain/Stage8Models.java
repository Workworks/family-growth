package com.familygrowth.domain;
import com.familygrowth.domain.Stage3Models.Wallet;import java.time.Instant;import java.util.List;import java.util.UUID;
public final class Stage8Models{private Stage8Models(){}public record SyncTask(UUID id,String title,int expectedMinutes,String status,UUID latestCompletionId){}public record ChildSyncSnapshot(UUID familyId,UUID childId,String childName,Wallet wallet,List<SyncTask> tasks,int pendingReviews,int approvedToday,Instant serverTime){} }
