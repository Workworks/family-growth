package com.familygrowth.domain;
import java.time.*; import java.util.Objects; import java.util.UUID;
public record ChildProfile(UUID id, UUID familyId, String displayName, LocalDate birthDate, AgeStage ageStage, Instant createdAt) {
 public ChildProfile { Objects.requireNonNull(id); Objects.requireNonNull(familyId); Objects.requireNonNull(birthDate); Objects.requireNonNull(ageStage); Objects.requireNonNull(createdAt); displayName=Family.requireText(displayName,"displayName"); if(birthDate.isAfter(LocalDate.now())) throw new IllegalArgumentException("birthDate cannot be in the future"); }
 public static ChildProfile create(UUID familyId,String name,LocalDate birthDate,AgeStage stage,Instant now){return new ChildProfile(UUID.randomUUID(),familyId,name,birthDate,stage,now);}
}
