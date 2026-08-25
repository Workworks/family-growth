package com.familygrowth.domain;
import java.time.Instant; import java.util.Objects; import java.util.UUID;
public record ParentProfile(UUID id, UUID familyId, String displayName, Instant createdAt) {
 public ParentProfile { Objects.requireNonNull(id); Objects.requireNonNull(familyId); Objects.requireNonNull(createdAt); displayName=Family.requireText(displayName,"displayName"); }
 public static ParentProfile create(UUID familyId,String name,Instant now){return new ParentProfile(UUID.randomUUID(),familyId,name,now);}
}
