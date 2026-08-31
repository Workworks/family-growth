package com.familygrowth.application;
import com.familygrowth.domain.Stage26RetentionModels.*;import java.time.Instant;import java.util.List;import java.util.UUID;
public interface Stage26RetentionStore {RetentionPolicy policy(UUID family,UUID child);RetentionPolicy updatePolicy(UUID family,UUID child,UUID actor,int days,long expectedVersion,String reason,Instant now);RetentionRun run(UUID family,UUID child,int days,String trigger,UUID actor,Instant now);List<RetentionTarget> targets();}
