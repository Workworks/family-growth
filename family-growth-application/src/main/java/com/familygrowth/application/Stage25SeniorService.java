package com.familygrowth.application;

import com.familygrowth.domain.Stage20Models.SchoolStage;
import com.familygrowth.domain.Stage25SeniorModels.*;
import com.familygrowth.domain.Stage3Models.Actor;
import com.familygrowth.domain.Stage3Models.ActorRole;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional
public class Stage25SeniorService {
    private final Stage3Service auth;private final Stage20Service experience;private final Stage25SeniorStore store;private final Clock clock;
    public Stage25SeniorService(Stage3Service auth,Stage20Service experience,Stage25SeniorStore store,Clock clock){this.auth=auth;this.experience=experience;this.store=store;this.clock=clock;}
    public ModuleConfiguration modules(Actor actor,UUID familyId,UUID childId){authorize(actor,familyId,childId);return store.modules(familyId,childId,clock.instant());}
    public ModuleConfiguration updateModules(Actor actor,UUID familyId,UUID childId,List<ModuleSelection> selections,long expectedRevision,String reason,String rawKey){
        auth.requireParent(actor,familyId);auth.requireChildOrParent(actor,familyId,childId);requireSenior(actor,familyId,childId);
        List<ModuleSelection> normalized=selections==null?List.of():selections.stream().distinct().sorted(java.util.Comparator.comparing(ModuleSelection::subjectCode).thenComparing(v->v.moduleType().name())).toList();
        if(normalized.isEmpty()||normalized.size()>12)throw new IllegalArgumentException("One to twelve senior modules are required");
        String why=text(reason,"reason",500),key=key(rawKey),payload=hash(childId+"|"+normalized+"|"+expectedRevision+"|"+why);
        var replay=store.moduleReplay(familyId,key);if(replay.isPresent()){if(!replay.get().childId().equals(childId)||!replay.get().payloadHash().equals(payload))throw new Stage3Service.ConflictException("Idempotency key payload mismatch");return store.modules(familyId,childId,clock.instant());}
        return store.updateModules(familyId,childId,normalized,expectedRevision,why,actor.actorId(),key,payload,clock.instant());
    }
    public List<WeeklyGoal> goals(Actor actor,UUID familyId,UUID childId){authorize(actor,familyId,childId);return store.goals(familyId,childId);}
    public WeeklyGoal createGoal(Actor actor,UUID familyId,UUID childId,UUID assignmentId,ModuleSelection module,LocalDate weekStart,String title,String evidence,String next,String rawKey){
        childWrite(actor,familyId,childId);validWeek(weekStart);if(!store.moduleEnabled(familyId,childId,module))throw new Stage3Service.ConflictException("Senior module is not enabled by the family");
        String key=key(rawKey),payload=hash(childId+"|"+assignmentId+"|"+module+"|"+weekStart+"|"+title+"|"+evidence+"|"+next);
        var replay=store.goalReplay(familyId,key);if(replay.isPresent()){var v=replay.get();if(!v.childId().equals(childId)||!v.actionType().equals("CREATE")||!v.payloadHash().equals(payload))throw new Stage3Service.ConflictException("Idempotency key payload mismatch");return store.goals(familyId,childId).stream().filter(g->g.id().equals(v.goalId())).findFirst().orElseThrow(FamilyGrowthService.NotFoundException::new);}
        return store.createGoal(familyId,childId,assignmentId,module,weekStart,text(title,"title",160),text(evidence,"evidenceTarget",500),text(next,"nextAction",500),actor.actorId(),key,payload,clock.instant());
    }
    public WeeklyGoal updateGoal(Actor actor,UUID familyId,UUID childId,UUID goalId,String title,String evidence,String next,long revision,String rawKey){childWrite(actor,familyId,childId);String key=key(rawKey),payload=hash(childId+"|"+goalId+"|UPDATE|"+title+"|"+evidence+"|"+next+"|"+revision);var replay=store.goalReplay(familyId,key);if(replay.isPresent())return replayGoal(replay.get(),childId,goalId,"UPDATE",payload,familyId);return store.updateGoal(familyId,childId,goalId,text(title,"title",160),text(evidence,"evidenceTarget",500),text(next,"nextAction",500),revision,actor.actorId(),key,payload,clock.instant());}
    public WeeklyGoal archiveGoal(Actor actor,UUID familyId,UUID childId,UUID goalId,long revision,String rawKey){childWrite(actor,familyId,childId);String key=key(rawKey),payload=hash(childId+"|"+goalId+"|ARCHIVE|"+revision);var replay=store.goalReplay(familyId,key);if(replay.isPresent())return replayGoal(replay.get(),childId,goalId,"ARCHIVE",payload,familyId);return store.archiveGoal(familyId,childId,goalId,revision,actor.actorId(),key,payload,clock.instant());}
    public List<Reflection> reflections(Actor actor,UUID familyId,UUID childId){authorize(actor,familyId,childId);return store.reflections(familyId,childId);}
    public Reflection reflect(Actor actor,UUID familyId,UUID childId,UUID goalId,UUID assignmentId,String evidence,ReflectionStrategy strategy,String next,boolean support,String rawKey){childWrite(actor,familyId,childId);String key=key(rawKey),payload=hash(childId+"|"+goalId+"|"+assignmentId+"|"+evidence+"|"+strategy+"|"+next+"|"+support);var replay=store.reflectionReplay(familyId,key);if(replay.isPresent()){var v=replay.get();if(!v.childId().equals(childId)||!v.payloadHash().equals(payload))throw new Stage3Service.ConflictException("Idempotency key payload mismatch");return store.reflections(familyId,childId).stream().filter(r->r.id().equals(v.reflectionId())).findFirst().orElseThrow(FamilyGrowthService.NotFoundException::new);}return store.reflect(familyId,childId,goalId,assignmentId,text(evidence,"evidenceSummary",1000),strategy,text(next,"nextAction",500),support,actor.actorId(),key,payload,clock.instant());}
    public SeniorLearningReport report(Actor actor,UUID familyId,UUID childId){auth.requireParent(actor,familyId);auth.requireChildOrParent(actor,familyId,childId);requireSenior(actor,familyId,childId);Instant end=clock.instant(),start=end.minus(Duration.ofDays(7));return new SeniorLearningReport(childId,start,end,store.recordedLearningMinutes(familyId,childId,start,end),store.facts(familyId,childId,end),store.countGoals(familyId,childId,GoalStatus.ACTIVE),store.countGoals(familyId,childId,GoalStatus.ARCHIVED),store.countReflections(familyId,childId,start,false),store.countReflections(familyId,childId,start,true),end);}
    private WeeklyGoal replayGoal(Stage25SeniorStore.GoalReplay v,UUID childId,UUID goalId,String type,String payload,UUID familyId){if(!v.childId().equals(childId)||!v.goalId().equals(goalId)||!v.actionType().equals(type)||!v.payloadHash().equals(payload))throw new Stage3Service.ConflictException("Idempotency key payload mismatch");return store.goals(familyId,childId).stream().filter(g->g.id().equals(goalId)).findFirst().orElseThrow(FamilyGrowthService.NotFoundException::new);}
    private void authorize(Actor actor,UUID familyId,UUID childId){auth.requireChildOrParent(actor,familyId,childId);requireSenior(actor,familyId,childId);}
    private void childWrite(Actor actor,UUID familyId,UUID childId){authorize(actor,familyId,childId);if(actor.role()!=ActorRole.CHILD)throw new Stage3Service.ForbiddenException();}
    private void requireSenior(Actor actor,UUID familyId,UUID childId){if(experience.experience(actor,familyId,childId).effectiveStage()!=SchoolStage.SENIOR_HIGH)throw new Stage3Service.ConflictException("Senior study planning requires the senior-high stage");}
    private void validWeek(LocalDate value){if(value==null||value.getDayOfWeek()!=DayOfWeek.MONDAY)throw new IllegalArgumentException("weekStart must be a Monday");LocalDate today=LocalDate.now(clock),min=today.minusWeeks(1),max=today.plusWeeks(8);if(value.isBefore(min)||value.isAfter(max))throw new IllegalArgumentException("weekStart is outside the supported planning window");}
    private static String text(String value,String name,int max){String v=value==null?"":value.trim();if(v.isBlank()||v.length()>max)throw new IllegalArgumentException(name+" is required and must be at most "+max);return v;}
    private static String key(String value){return text(value,"Idempotency-Key",100);}
    private static String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception ex){throw new IllegalStateException(ex);}}
}
