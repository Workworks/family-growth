package com.familygrowth.domain;
import java.time.Instant;import java.util.List;import java.util.Map;import java.util.UUID;
public final class Stage26DataRightsModels {private Stage26DataRightsModels(){}
 public record ChildDataExport(String schemaVersion,UUID familyId,UUID childId,Instant generatedAt,Map<String,Object> profile,Map<String,Object> learning,Map<String,Object> usage,Map<String,Object> finance,List<String> exclusions){}
 public record ErasurePreview(UUID requestId,UUID childId,List<String> deletedOrRedacted,List<String> retained,Instant confirmationExpiresAt,String confirmationToken){}
 public record ErasureResult(UUID requestId,UUID childId,String status,Instant completedAt,List<String> retained){}
 public record StoredRequest(UUID id,UUID familyId,UUID childId,UUID actorId,String type,String status,String tokenHash,Instant tokenExpiresAt,String idempotencyKey,String payloadHash,Instant createdAt,Instant completedAt){}
}
