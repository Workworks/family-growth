package com.familygrowth.application;

import com.familygrowth.domain.Stage20Models.SchoolStage;
import com.familygrowth.domain.Stage24JuniorModels.JuniorLearningPlan;
import com.familygrowth.domain.Stage24JuniorModels.MoveDirection;
import com.familygrowth.domain.Stage24JuniorModels.JuniorLearningReport;
import com.familygrowth.domain.Stage3Models.Actor;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class Stage24JuniorService {
    private final Stage3Service auth; private final Stage20Service experience;
    private final Stage24JuniorStore store; private final Clock clock;
    public Stage24JuniorService(Stage3Service auth, Stage20Service experience, Stage24JuniorStore store, Clock clock) {
        this.auth=auth; this.experience=experience; this.store=store; this.clock=clock;
    }
    public JuniorLearningPlan plan(Actor actor, UUID familyId, UUID childId) {
        auth.requireChildOrParent(actor,familyId,childId); requireJunior(actor,familyId,childId);
        return store.plan(familyId,childId,clock.instant());
    }
    public JuniorLearningPlan move(Actor actor, UUID familyId, UUID childId, UUID assignmentId,
                                   MoveDirection direction, long expectedRevision, String rawKey) {
        auth.requireChildOrParent(actor,familyId,childId); requireJunior(actor,familyId,childId);
        String key=key(rawKey); String payload=hash(childId+"|"+assignmentId+"|"+direction+"|"+expectedRevision);
        var replay=store.replay(familyId,key);
        if(replay.isPresent()) {
            var value=replay.get();
            if(!value.childId().equals(childId)||!value.assignmentId().equals(assignmentId)
                ||value.direction()!=direction||!value.payloadHash().equals(payload))
                throw new Stage3Service.ConflictException("Idempotency key payload mismatch");
            return store.plan(familyId,childId,clock.instant());
        }
        return store.move(familyId,childId,assignmentId,direction,expectedRevision,actor.actorId(),key,payload,clock.instant());
    }
    public JuniorLearningReport report(Actor actor,UUID familyId,UUID childId) {
        auth.requireParent(actor,familyId); auth.requireChildOrParent(actor,familyId,childId); requireJunior(actor,familyId,childId);
        Instant end=clock.instant(),start=end.minus(Duration.ofDays(7));
        JuniorLearningPlan plan=store.plan(familyId,childId,end);
        return new JuniorLearningReport(childId,start,end,store.recordedLearningMinutes(familyId,childId,start,end),
            store.facts(familyId,childId,end),plan.revision(),end);
    }
    private void requireJunior(Actor actor,UUID familyId,UUID childId) {
        if(experience.experience(actor,familyId,childId).effectiveStage()!=SchoolStage.JUNIOR_MIDDLE)
            throw new Stage3Service.ConflictException("Junior learning plan requires the junior-middle stage");
    }
    private static String key(String value){if(value==null||value.isBlank()||value.trim().length()>100)throw new IllegalArgumentException("Valid Idempotency-Key is required");return value.trim();}
    private static String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception ex){throw new IllegalStateException(ex);}}
}
