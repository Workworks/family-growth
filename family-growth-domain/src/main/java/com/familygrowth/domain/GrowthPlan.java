package com.familygrowth.domain;
import java.time.*; import java.util.Objects; import java.util.UUID;
public record GrowthPlan(UUID id,UUID familyId,UUID childId,String title,String description,LocalDate startDate,LocalDate endDate,boolean active,Instant createdAt){
 public GrowthPlan{Objects.requireNonNull(id);Objects.requireNonNull(familyId);Objects.requireNonNull(childId);Objects.requireNonNull(startDate);Objects.requireNonNull(createdAt);title=Family.requireText(title,"title");description=description==null?"":description.trim();if(endDate!=null&&endDate.isBefore(startDate))throw new IllegalArgumentException("endDate cannot precede startDate");}
}
