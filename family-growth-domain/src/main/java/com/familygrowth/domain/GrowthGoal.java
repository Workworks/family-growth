package com.familygrowth.domain;
import java.time.Instant; import java.util.Objects; import java.util.UUID;
public record GrowthGoal(UUID id,UUID familyId,UUID planId,String title,String description,Instant createdAt){public GrowthGoal{Objects.requireNonNull(id);Objects.requireNonNull(familyId);Objects.requireNonNull(planId);Objects.requireNonNull(createdAt);title=Family.requireText(title,"title");description=description==null?"":description.trim();}}
