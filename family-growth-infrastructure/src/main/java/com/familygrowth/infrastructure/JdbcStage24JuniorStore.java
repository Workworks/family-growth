package com.familygrowth.infrastructure;

import com.familygrowth.application.FamilyGrowthService;
import com.familygrowth.application.Stage24JuniorStore;
import com.familygrowth.application.Stage3Service;
import com.familygrowth.domain.Stage21TeachingModels.AssignmentStatus;
import com.familygrowth.domain.Stage24JuniorModels.JuniorLearningPlan;
import com.familygrowth.domain.Stage24JuniorModels.MoveDirection;
import com.familygrowth.domain.Stage24JuniorModels.PlanItem;
import com.familygrowth.domain.Stage23LearningModels.SubjectLearningFacts;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcStage24JuniorStore implements Stage24JuniorStore {
    private final JdbcTemplate jdbc;
    JdbcStage24JuniorStore(JdbcTemplate jdbc){this.jdbc=jdbc;}

    @Override
    public JuniorLearningPlan plan(UUID familyId,UUID childId,Instant now){
        requireChild(familyId,childId);
        ensureHeader(familyId,childId,now);
        lockHeader(familyId,childId);
        Integer next=jdbc.queryForObject("SELECT COALESCE(MAX(position),-1)+1 FROM junior_learning_plan_item WHERE family_id=? AND child_id=?",Integer.class,familyId,childId);
        List<UUID> missing=jdbc.query("""
            SELECT a.id FROM lesson_assignment a
            JOIN teaching_course_version v ON v.id=a.course_version_id JOIN teaching_course c ON c.id=v.course_id
            WHERE a.family_id=? AND a.child_id=? AND c.school_stage='JUNIOR_MIDDLE'
              AND a.assignment_source='AUTONOMOUS' AND a.status='ASSIGNED'
              AND NOT EXISTS(SELECT 1 FROM junior_learning_plan_item p WHERE p.assignment_id=a.id)
            ORDER BY a.created_at,a.id
            """,(rs,row)->rs.getObject(1,UUID.class),familyId,childId);
        int position=next==null?0:next;
        for(UUID assignment:missing) jdbc.update("INSERT INTO junior_learning_plan_item(assignment_id,family_id,child_id,position,created_at) VALUES(?,?,?,?,?)",
            assignment,familyId,childId,position++,Timestamp.from(now));
        if(!missing.isEmpty()) jdbc.update("UPDATE junior_learning_plan SET revision=revision+1,updated_at=? WHERE family_id=? AND child_id=?",Timestamp.from(now),familyId,childId);
        return read(familyId,childId);
    }

    @Override
    public JuniorLearningPlan move(UUID familyId,UUID childId,UUID assignmentId,MoveDirection direction,long expectedRevision,
                                   UUID actorId,String key,String payloadHash,Instant now){
        plan(familyId,childId,now); long revision=lockHeader(familyId,childId);
        if(revision!=expectedRevision)throw new Stage3Service.ConflictException("Junior plan revision conflict");
        Object[] current=jdbc.query("""
            SELECT p.position,a.status,a.assignment_source FROM junior_learning_plan_item p
            JOIN lesson_assignment a ON a.id=p.assignment_id
            WHERE p.family_id=? AND p.child_id=? AND p.assignment_id=? FOR UPDATE
            """,(rs,row)->new Object[]{rs.getInt(1),rs.getString(2),rs.getString(3)},familyId,childId,assignmentId)
            .stream().findFirst().orElseThrow(FamilyGrowthService.NotFoundException::new);
        if(AssignmentStatus.valueOf((String)current[1])!=AssignmentStatus.ASSIGNED||!"AUTONOMOUS".equals(current[2]))
            throw new Stage3Service.ConflictException("Only unstarted autonomous assignments can be reordered");
        int oldPosition=(Integer)current[0];
        Object[] neighbor=jdbc.query("""
            SELECT p.assignment_id,p.position FROM junior_learning_plan_item p
            JOIN lesson_assignment a ON a.id=p.assignment_id
            WHERE p.family_id=? AND p.child_id=? AND a.status='ASSIGNED' AND a.assignment_source='AUTONOMOUS'
              AND p.position %s ? ORDER BY p.position %s LIMIT 1 FOR UPDATE
            """.formatted(direction==MoveDirection.UP?"<":">",direction==MoveDirection.UP?"DESC":"ASC"),
            (rs,row)->new Object[]{rs.getObject(1,UUID.class),rs.getInt(2)},familyId,childId,oldPosition).stream().findFirst()
            .orElseThrow(()->new Stage3Service.ConflictException("Assignment is already at the plan boundary"));
        UUID neighborAssignment=(UUID)neighbor[0]; int wanted=(Integer)neighbor[1];
        int temporary=jdbc.queryForObject("SELECT COALESCE(MAX(position),0)+1 FROM junior_learning_plan_item WHERE family_id=? AND child_id=?",Integer.class,familyId,childId);
        jdbc.update("UPDATE junior_learning_plan_item SET position=? WHERE assignment_id=?",temporary,assignmentId);
        jdbc.update("UPDATE junior_learning_plan_item SET position=? WHERE assignment_id=?",oldPosition,neighborAssignment);
        jdbc.update("UPDATE junior_learning_plan_item SET position=? WHERE assignment_id=?",wanted,assignmentId);
        jdbc.update("UPDATE junior_learning_plan SET revision=revision+1,updated_at=? WHERE family_id=? AND child_id=?",Timestamp.from(now),familyId,childId);
        jdbc.update("""
            INSERT INTO junior_learning_plan_action(id,family_id,child_id,assignment_id,actor_id,direction,old_position,new_position,
              old_revision,new_revision,idempotency_key,payload_hash,created_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)
            """,UUID.randomUUID(),familyId,childId,assignmentId,actorId,direction.name(),oldPosition,wanted,revision,revision+1,key,payloadHash,Timestamp.from(now));
        return read(familyId,childId);
    }

    @Override public Optional<Replay> replay(UUID familyId,String key){return jdbc.query("SELECT child_id,assignment_id,direction,payload_hash FROM junior_learning_plan_action WHERE family_id=? AND idempotency_key=?",
        (rs,row)->new Replay(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class),MoveDirection.valueOf(rs.getString(3)),rs.getString(4)),familyId,key).stream().findFirst();}

    @Override public List<SubjectLearningFacts> facts(UUID familyId,UUID childId,Instant now) {
        requireChild(familyId,childId); Map<String,long[]> facts=new LinkedHashMap<>();
        jdbc.query("""
            SELECT c.subject_code,a.status,COUNT(*) fact_count FROM lesson_assignment a
            JOIN teaching_course_version v ON v.id=a.course_version_id JOIN teaching_course c ON c.id=v.course_id
            WHERE a.family_id=? AND a.child_id=? AND c.school_stage='JUNIOR_MIDDLE'
            GROUP BY c.subject_code,a.status ORDER BY c.subject_code,a.status
            """,rs->{long[] v=facts.computeIfAbsent(rs.getString(1),ignored->new long[8]);long n=rs.getLong(3);
                switch(AssignmentStatus.valueOf(rs.getString(2))){case ASSIGNED->v[0]+=n;case IN_PROGRESS->v[1]+=n;case SUBMITTED->v[2]+=n;case COMPLETED->v[3]+=n;case REWORK_REQUIRED->v[4]+=n;}},familyId,childId);
        jdbc.query("""
            SELECT c.subject_code,
              SUM(CASE WHEN s.event_type IN ('HELP_REQUESTED','INCORRECT_OBSERVED') AND NOT EXISTS
                (SELECT 1 FROM learning_support_event x WHERE x.parent_event_id=s.id AND x.event_type='MISCONCEPTION_CLASSIFIED') THEN 1 ELSE 0 END),
              SUM(CASE WHEN s.event_type='REVISIT_SCHEDULED' AND NOT EXISTS
                (SELECT 1 FROM learning_support_event x WHERE x.parent_event_id=s.id AND x.event_type='REVISIT_COMPLETED') THEN 1 ELSE 0 END),
              SUM(CASE WHEN s.event_type='REVISIT_SCHEDULED' AND s.revisit_at<=? AND NOT EXISTS
                (SELECT 1 FROM learning_support_event x WHERE x.parent_event_id=s.id AND x.event_type='REVISIT_COMPLETED') THEN 1 ELSE 0 END)
            FROM learning_support_event s JOIN lesson_assignment a ON a.id=s.assignment_id
            JOIN teaching_course_version v ON v.id=a.course_version_id JOIN teaching_course c ON c.id=v.course_id
            WHERE s.family_id=? AND s.child_id=? AND c.school_stage='JUNIOR_MIDDLE'
            GROUP BY c.subject_code ORDER BY c.subject_code
            """,rs->{long[] v=facts.computeIfAbsent(rs.getString(1),ignored->new long[8]);v[5]=rs.getLong(2);v[6]=rs.getLong(3);v[7]=rs.getLong(4);},Timestamp.from(now),familyId,childId);
        return facts.entrySet().stream().map(e->{long[] v=e.getValue();return new SubjectLearningFacts(e.getKey(),v[0],v[1],v[2],v[3],v[4],v[5],v[6],v[7]);}).toList();
    }

    @Override public long recordedLearningMinutes(UUID familyId,UUID childId,Instant from,Instant to) {
        requireChild(familyId,childId); Long value=jdbc.queryForObject("SELECT COALESCE(SUM(minutes),0) FROM usage_event WHERE family_id=? AND child_id=? AND event_type='LEARNING' AND occurred_at>=? AND occurred_at<?",Long.class,familyId,childId,Timestamp.from(from),Timestamp.from(to));return value==null?0:value;
    }

    private JuniorLearningPlan read(UUID familyId,UUID childId){
        Object[] header=jdbc.query("SELECT revision,updated_at FROM junior_learning_plan WHERE family_id=? AND child_id=?",
            (rs,row)->new Object[]{rs.getLong(1),rs.getTimestamp(2).toInstant()},familyId,childId).stream().findFirst().orElseThrow(FamilyGrowthService.NotFoundException::new);
        List<PlanItem> items=jdbc.query("""
            SELECT a.id,c.subject_code,c.title,l.title,a.status,p.position FROM junior_learning_plan_item p
            JOIN lesson_assignment a ON a.id=p.assignment_id JOIN teaching_course_version v ON v.id=a.course_version_id
            JOIN teaching_course c ON c.id=v.course_id JOIN teaching_lesson l ON l.id=a.lesson_id
            WHERE p.family_id=? AND p.child_id=? AND a.status='ASSIGNED' AND a.assignment_source='AUTONOMOUS'
            ORDER BY p.position
            """,(rs,row)->new PlanItem(rs.getObject(1,UUID.class),rs.getString(2),rs.getString(3),rs.getString(4),AssignmentStatus.valueOf(rs.getString(5)),rs.getInt(6)),familyId,childId);
        return new JuniorLearningPlan(childId,(Long)header[0],items,(Instant)header[1]);
    }
    private void ensureHeader(UUID familyId,UUID childId,Instant now){
        if(jdbc.queryForObject("SELECT COUNT(*) FROM junior_learning_plan WHERE family_id=? AND child_id=?",Integer.class,familyId,childId)>0)return;
        try{jdbc.update("INSERT INTO junior_learning_plan(child_id,family_id,revision,updated_at) VALUES(?,?,0,?)",childId,familyId,Timestamp.from(now));}catch(DuplicateKeyException ignored){ }
    }
    private long lockHeader(UUID familyId,UUID childId){return jdbc.query("SELECT revision FROM junior_learning_plan WHERE family_id=? AND child_id=? FOR UPDATE",(rs,row)->rs.getLong(1),familyId,childId).stream().findFirst().orElseThrow(FamilyGrowthService.NotFoundException::new);}
    private void requireChild(UUID familyId,UUID childId){if(jdbc.queryForObject("SELECT COUNT(*) FROM child_profile WHERE family_id=? AND id=?",Integer.class,familyId,childId)!=1)throw new FamilyGrowthService.NotFoundException();}
}
