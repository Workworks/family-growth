package com.familygrowth.infrastructure;

import com.familygrowth.application.FamilyGrowthService;
import com.familygrowth.application.Stage21TeachingStore;
import com.familygrowth.application.Stage3Service;
import com.familygrowth.domain.Stage20Models.SchoolStage;
import com.familygrowth.domain.Stage21TeachingModels.ActivityContent;
import com.familygrowth.domain.Stage21TeachingModels.ActivityDraft;
import com.familygrowth.domain.Stage21TeachingModels.ActivityProgress;
import com.familygrowth.domain.Stage21TeachingModels.ActivityType;
import com.familygrowth.domain.Stage21TeachingModels.AssignmentFacts;
import com.familygrowth.domain.Stage21TeachingModels.AssignmentStatus;
import com.familygrowth.domain.Stage21TeachingModels.CourseVersion;
import com.familygrowth.domain.Stage21TeachingModels.CourseVersionStatus;
import com.familygrowth.domain.Stage21TeachingModels.EvidenceType;
import com.familygrowth.domain.Stage21TeachingModels.LearningAssignment;
import com.familygrowth.domain.Stage21TeachingModels.KindergartenAgeBand;
import com.familygrowth.domain.Stage21TeachingModels.KindergartenDomain;
import com.familygrowth.domain.Stage21TeachingModels.LessonContent;
import com.familygrowth.domain.Stage21TeachingModels.ParentCourseSummary;
import com.familygrowth.domain.Stage21TeachingModels.QuestionOption;
import com.familygrowth.domain.Stage21TeachingModels.UnitContent;
import com.familygrowth.domain.Stage21TeachingModels.VersionDraft;
import com.familygrowth.domain.Stage23LearningModels.AssignmentSource;
import com.familygrowth.domain.Stage23LearningModels.RewardSnapshot;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class JdbcStage21TeachingStore implements Stage21TeachingStore {
    private final JdbcTemplate jdbc;
    JdbcStage21TeachingStore(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public Optional<CourseReplay> courseByCreateKey(UUID familyId, String key) {
        return jdbc.query("SELECT id,payload_hash FROM teaching_course WHERE family_id=? AND idempotency_key=?",
            (rs, row) -> new Object[]{rs.getObject("id", UUID.class), rs.getString("payload_hash")}, familyId, key)
            .stream().findFirst().map(row -> new CourseReplay(versionByCourse((UUID) row[0], 1), (String) row[1]));
    }

    @Override
    @Transactional
    public CourseVersion createCourse(UUID familyId, SchoolStage stage, String subjectCode, String title,
                                      VersionDraft draft, UUID actorId, String key, String payloadHash, Instant now) {
        UUID courseId = UUID.randomUUID();
        jdbc.update("""
            INSERT INTO teaching_course(id,family_id,school_stage,subject_code,title,created_by,idempotency_key,payload_hash,created_at)
            VALUES (?,?,?,?,?,?,?,?,?)
            """, courseId, familyId, stage.name(), subjectCode, title, actorId, key, payloadHash, Timestamp.from(now));
        UUID versionId = insertVersion(courseId, 1, draft, actorId, now);
        return version(familyId, versionId).orElseThrow();
    }

    @Override
    @Transactional
    public CourseVersion createVersion(UUID familyId, UUID courseId, VersionDraft draft, UUID actorId,
                                       String key, String payloadHash, Instant now) {
        jdbc.query("SELECT id FROM teaching_course WHERE family_id=? AND id=? FOR UPDATE",
            (rs, row) -> 1, familyId, courseId).stream().findFirst().orElseThrow(FamilyGrowthService.NotFoundException::new);
        int versionNumber = jdbc.queryForObject("SELECT COALESCE(MAX(version_number),0)+1 FROM teaching_course_version WHERE course_id=?",
            Integer.class, courseId);
        UUID versionId = insertVersion(courseId, versionNumber, draft, actorId, now);
        action(familyId, courseId, versionId, actorId, "CREATE_VERSION", key, payloadHash, now);
        return version(familyId, versionId).orElseThrow();
    }

    private UUID insertVersion(UUID courseId, int versionNumber, VersionDraft draft, UUID actorId, Instant now) {
        UUID versionId = UUID.randomUUID();
        jdbc.update("""
            INSERT INTO teaching_course_version(id,course_id,version_number,summary,rights_basis,kindergarten_age_band,kindergarten_domains,status,created_by,created_at)
            VALUES (?,?,?,?,?,?,?,'DRAFT',?,?)
            """, versionId, courseId, versionNumber, draft.summary(), draft.rightsBasis(),
            draft.kindergartenAgeBand() == null ? null : draft.kindergartenAgeBand().name(), encodeDomains(draft.kindergartenDomains()),
            actorId, Timestamp.from(now));
        for (int unitOrder = 0; unitOrder < draft.units().size(); unitOrder++) {
            var unit = draft.units().get(unitOrder);
            UUID unitId = UUID.randomUUID();
            jdbc.update("INSERT INTO teaching_unit(id,course_version_id,title,display_order) VALUES (?,?,?,?)",
                unitId, versionId, unit.title(), unitOrder);
            for (int lessonOrder = 0; lessonOrder < unit.lessons().size(); lessonOrder++) {
                var lesson = unit.lessons().get(lessonOrder);
                UUID lessonId = UUID.randomUUID();
                jdbc.update("INSERT INTO teaching_lesson(id,unit_id,title,summary,display_order) VALUES (?,?,?,?,?)",
                    lessonId, unitId, lesson.title(), lesson.summary(), lessonOrder);
                for (int activityOrder = 0; activityOrder < lesson.activities().size(); activityOrder++) {
                    insertActivity(lessonId, lesson.activities().get(activityOrder), activityOrder);
                }
            }
        }
        return versionId;
    }

    private void insertActivity(UUID lessonId, ActivityDraft draft, int order) {
        UUID activityId = UUID.randomUUID();
        jdbc.update("""
            INSERT INTO learning_activity(id,lesson_id,activity_type,title,instruction,content_ref,expected_minutes,display_order)
            VALUES (?,?,?,?,?,?,?,?)
            """, activityId, lessonId, draft.type().name(), draft.title(), draft.instruction(), draft.contentRef(), draft.expectedMinutes(), order);
        if (draft.type().objective()) {
            UUID questionId = UUID.randomUUID();
            jdbc.update("""
                INSERT INTO learning_question(id,activity_id,prompt,hint,answer_key,content_version)
                VALUES (?,?,?,?,?,1)
                """, questionId, activityId, draft.prompt(), draft.hint(), draft.answerKey());
            for (int i = 0; i < draft.options().size(); i++) {
                var option = draft.options().get(i);
                jdbc.update("""
                    INSERT INTO learning_question_option(id,question_id,option_value,option_label,display_order)
                    VALUES (?,?,?,?,?)
                    """, UUID.randomUUID(), questionId, option.value(), option.label(), i);
            }
        }
    }

    @Override
    public Optional<ActionReplay> action(UUID familyId, String key) {
        return jdbc.query("SELECT action_type,target_id,result_id,payload_hash FROM teaching_action WHERE family_id=? AND idempotency_key=?",
            (rs, row) -> new ActionReplay(rs.getString("action_type"), rs.getObject("target_id", UUID.class),
                rs.getObject("result_id", UUID.class), rs.getString("payload_hash")), familyId, key).stream().findFirst();
    }

    @Override
    public Optional<CourseVersion> version(UUID familyId, UUID versionId) {
        return jdbc.query("""
            SELECT c.id course_id,c.family_id,c.school_stage,c.subject_code,c.title,
                   v.id version_id,v.version_number,v.summary,v.rights_basis,v.kindergarten_age_band,v.kindergarten_domains,v.status,v.published_at
            FROM teaching_course_version v JOIN teaching_course c ON c.id=v.course_id
            WHERE c.family_id=? AND v.id=?
            """, (rs, row) -> mapVersion(rs), familyId, versionId).stream().findFirst().map(this::hydrateVersion);
    }

    private CourseVersion versionByCourse(UUID courseId, int versionNumber) {
        return jdbc.query("""
            SELECT c.id course_id,c.family_id,c.school_stage,c.subject_code,c.title,
                   v.id version_id,v.version_number,v.summary,v.rights_basis,v.kindergarten_age_band,v.kindergarten_domains,v.status,v.published_at
            FROM teaching_course_version v JOIN teaching_course c ON c.id=v.course_id
            WHERE c.id=? AND v.version_number=?
            """, (rs, row) -> mapVersion(rs), courseId, versionNumber).stream().findFirst().map(this::hydrateVersion).orElseThrow();
    }

    private CourseVersion hydrateVersion(CourseVersion bare) {
        List<UnitContent> units = jdbc.query("""
            SELECT id,title FROM teaching_unit WHERE course_version_id=? ORDER BY display_order,id
            """, (rs, row) -> new UnitContent(rs.getObject("id", UUID.class), rs.getString("title"), List.of()), bare.versionId())
            .stream().map(unit -> new UnitContent(unit.id(), unit.title(), lessons(unit.id()))).toList();
        return new CourseVersion(bare.courseId(), bare.versionId(), bare.familyId(), bare.schoolStage(), bare.subjectCode(),
            bare.title(), bare.versionNumber(), bare.summary(), bare.rightsBasis(), bare.kindergartenAgeBand(),
            bare.kindergartenDomains(), bare.status(), units, bare.publishedAt());
    }

    private List<LessonContent> lessons(UUID unitId) {
        return jdbc.query("SELECT id,title,summary FROM teaching_lesson WHERE unit_id=? ORDER BY display_order,id",
            (rs, row) -> new LessonContent(rs.getObject("id", UUID.class), rs.getString("title"),
                rs.getString("summary"), List.of()), unitId).stream()
            .map(lesson -> new LessonContent(lesson.id(), lesson.title(), lesson.summary(), activities(lesson.id()))).toList();
    }

    private List<ActivityContent> activities(UUID lessonId) {
        return jdbc.query("""
            SELECT a.id,a.activity_type,a.title,a.instruction,a.content_ref,a.expected_minutes,
                   q.id question_id,q.prompt,q.hint,q.answer_key
            FROM learning_activity a LEFT JOIN learning_question q ON q.activity_id=a.id
            WHERE a.lesson_id=? ORDER BY a.display_order,a.id
            """, (rs, row) -> activity(rs), lessonId);
    }

    private ActivityContent activity(ResultSet rs) throws SQLException {
        UUID questionId = rs.getObject("question_id", UUID.class);
        List<QuestionOption> options = questionId == null ? List.of() : jdbc.query("""
            SELECT option_value,option_label FROM learning_question_option WHERE question_id=? ORDER BY display_order,id
            """, (ors, row) -> new QuestionOption(ors.getString(1), ors.getString(2)), questionId);
        return new ActivityContent(rs.getObject("id", UUID.class), ActivityType.valueOf(rs.getString("activity_type")),
            rs.getString("title"), rs.getString("instruction"), rs.getString("content_ref"), rs.getInt("expected_minutes"),
            questionId == null ? "" : rs.getString("prompt"), questionId == null ? "" : rs.getString("hint"),
            options, questionId == null ? "" : rs.getString("answer_key"));
    }

    @Override
    public List<ParentCourseSummary> courses(UUID familyId) {
        return jdbc.query("""
            SELECT c.id course_id,c.title,c.school_stage,c.subject_code,v.id version_id,v.version_number,v.status,v.published_at,
                   (SELECT COUNT(*) FROM teaching_lesson l JOIN teaching_unit u ON u.id=l.unit_id WHERE u.course_version_id=v.id) lesson_count
            FROM teaching_course c JOIN teaching_course_version v ON v.course_id=c.id
            WHERE c.family_id=? ORDER BY c.created_at,v.version_number
            """, (rs, row) -> new ParentCourseSummary(rs.getObject("course_id", UUID.class), rs.getString("title"),
                SchoolStage.valueOf(rs.getString("school_stage")), rs.getString("subject_code"),
                rs.getObject("version_id", UUID.class), rs.getInt("version_number"),
                CourseVersionStatus.valueOf(rs.getString("status")), rs.getInt("lesson_count"), instant(rs, "published_at")), familyId);
    }

    @Override
    @Transactional
    public CourseVersion publish(UUID familyId, UUID versionId, UUID actorId, String key, String payloadHash, Instant now) {
        int changed = jdbc.update("""
            UPDATE teaching_course_version SET status='PUBLISHED',published_by=?,published_at=?
            WHERE id=? AND status='DRAFT' AND course_id IN (SELECT id FROM teaching_course WHERE family_id=?)
            """, actorId, Timestamp.from(now), versionId, familyId);
        if (changed != 1) throw new Stage3Service.ConflictException("Course version cannot be published");
        action(familyId, versionId, versionId, actorId, "PUBLISH", key, payloadHash, now);
        return version(familyId, versionId).orElseThrow();
    }

    @Override
    public Optional<AssignmentFacts> assignment(UUID familyId, UUID childId, UUID assignmentId) {
        return jdbc.query("""
            SELECT a.id,a.child_id,a.course_version_id,a.lesson_id,a.status,a.version,a.updated_at,a.assignment_source,
                   a.money_reward_snapshot,a.coin_reward_snapshot,a.xp_reward_snapshot,a.reward_settled_at,
                   c.title course_title,c.school_stage,c.subject_code,u.title unit_title,
                   l.title lesson_title,l.summary lesson_summary,lc.review_note
            FROM lesson_assignment a JOIN teaching_course_version v ON v.id=a.course_version_id
            JOIN teaching_course c ON c.id=v.course_id JOIN teaching_lesson l ON l.id=a.lesson_id
            JOIN teaching_unit u ON u.id=l.unit_id JOIN learning_completion lc ON lc.assignment_id=a.id
            WHERE a.family_id=? AND a.child_id=? AND a.id=? AND v.status='PUBLISHED'
            """, (rs, row) -> assignment(rs), familyId, childId, assignmentId).stream().findFirst().map(this::facts);
    }

    @Override
    public List<LearningAssignment> assignments(UUID familyId, UUID childId, SchoolStage stage) {
        return jdbc.query("""
            SELECT a.id,a.child_id,a.course_version_id,a.lesson_id,a.status,a.version,a.updated_at,a.assignment_source,
                   a.money_reward_snapshot,a.coin_reward_snapshot,a.xp_reward_snapshot,a.reward_settled_at,
                   c.title course_title,c.school_stage,c.subject_code,u.title unit_title,
                   l.title lesson_title,l.summary lesson_summary,lc.review_note
            FROM lesson_assignment a JOIN teaching_course_version v ON v.id=a.course_version_id
            JOIN teaching_course c ON c.id=v.course_id JOIN teaching_lesson l ON l.id=a.lesson_id
            JOIN teaching_unit u ON u.id=l.unit_id JOIN learning_completion lc ON lc.assignment_id=a.id
            WHERE a.family_id=? AND a.child_id=? AND c.school_stage=? AND v.status='PUBLISHED'
              AND (a.assignment_source='PARENT' OR v.version_number=(SELECT MAX(v2.version_number)
                   FROM teaching_course_version v2 WHERE v2.course_id=c.id AND v2.status='PUBLISHED'))
            ORDER BY a.updated_at DESC,a.id
            """, (rs, row) -> assignment(rs), familyId, childId, stage.name()).stream().map(this::facts)
            .map(AssignmentFacts::projection).toList();
    }

    @Override
    @Transactional
    public LearningAssignment assign(UUID familyId, UUID childId, UUID versionId, UUID lessonId, UUID actorId,
                                     String key, String payloadHash, Instant now) {
        int exists = jdbc.queryForObject("""
            SELECT COUNT(*) FROM teaching_lesson l JOIN teaching_unit u ON u.id=l.unit_id
            JOIN teaching_course_version v ON v.id=u.course_version_id JOIN teaching_course c ON c.id=v.course_id
            WHERE c.family_id=? AND v.id=? AND l.id=? AND v.status='PUBLISHED'
            """, Integer.class, familyId, versionId, lessonId);
        if (exists != 1) throw new FamilyGrowthService.NotFoundException();
        UUID id = UUID.randomUUID();
        jdbc.update("""
            INSERT INTO lesson_assignment(id,family_id,child_id,course_version_id,lesson_id,status,assigned_by,idempotency_key,version,created_at,updated_at)
            VALUES (?,?,?,?,?,'ASSIGNED',?,?,0,?,?)
            """, id, familyId, childId, versionId, lessonId, actorId, key, Timestamp.from(now), Timestamp.from(now));
        jdbc.update("INSERT INTO learning_completion(assignment_id,status,version,updated_at) VALUES (?,'ASSIGNED',0,?)",
            id, Timestamp.from(now));
        action(familyId, lessonId, id, actorId, "ASSIGN", key, payloadHash, now);
        return assignment(familyId, childId, id).orElseThrow().projection();
    }

    @Override
    @Transactional
    public LearningAssignment attempt(UUID familyId, UUID childId, UUID assignmentId, UUID activityId, UUID actorId,
                                      String responseText, EvidenceType evidenceType, Boolean correct,
                                      String key, String payloadHash, Instant now) {
        AssignmentFacts current = lockAssignment(familyId, childId, assignmentId);
        if (!current.contentByActivity().containsKey(activityId)) throw new FamilyGrowthService.NotFoundException();
        UUID attemptId = UUID.randomUUID();
        jdbc.update("""
            INSERT INTO activity_attempt(id,family_id,assignment_id,activity_id,actor_id,response_text,evidence_type,checked_correct,idempotency_key,created_at)
            VALUES (?,?,?,?,?,?,?,?,?,?)
            """, attemptId, familyId, assignmentId, activityId, actorId, responseText, evidenceType.name(), correct,
            key, Timestamp.from(now));
        jdbc.update("""
            INSERT INTO mastery_evidence(id,assignment_id,activity_id,evidence_type,note,actor_id,source_attempt_id,created_at)
            VALUES (?,?,?,?,?,?,?,?)
            """, UUID.randomUUID(), assignmentId, activityId, evidenceType.name(), "", actorId, attemptId, Timestamp.from(now));
        jdbc.update("""
            UPDATE lesson_assignment SET status=CASE WHEN status IN ('ASSIGNED','REWORK_REQUIRED') THEN 'IN_PROGRESS' ELSE status END,
                   version=version+1,updated_at=? WHERE id=?
            """, Timestamp.from(now), assignmentId);
        jdbc.update("""
            UPDATE learning_completion SET status=CASE WHEN status IN ('ASSIGNED','REWORK_REQUIRED') THEN 'IN_PROGRESS' ELSE status END,
                   version=version+1,updated_at=? WHERE assignment_id=?
            """, Timestamp.from(now), assignmentId);
        action(familyId, assignmentId, attemptId, actorId, "ATTEMPT", key, payloadHash, now);
        return assignment(familyId, childId, assignmentId).orElseThrow().projection();
    }

    @Override
    @Transactional
    public LearningAssignment transition(UUID familyId, UUID childId, UUID assignmentId, AssignmentStatus target,
                                         UUID actorId, String note, long expectedVersion, String key,
                                         String payloadHash, Instant now) {
        AssignmentFacts locked = lockAssignment(familyId, childId, assignmentId);
        if (locked.projection().version() != expectedVersion) throw new Stage3Service.ConflictException("Assignment version conflict");
        AssignmentStatus current = locked.projection().status();
        boolean valid = (target == AssignmentStatus.SUBMITTED && (current == AssignmentStatus.IN_PROGRESS || current == AssignmentStatus.REWORK_REQUIRED))
            || ((target == AssignmentStatus.COMPLETED || target == AssignmentStatus.REWORK_REQUIRED) && current == AssignmentStatus.SUBMITTED);
        if (!valid) throw new Stage3Service.ConflictException("Invalid assignment transition");
        if (target == AssignmentStatus.SUBMITTED) {
            Instant reworkRequestedAt = jdbc.query("SELECT rework_requested_at FROM learning_completion WHERE assignment_id=?",
                (rs, row) -> {
                    Timestamp value = rs.getTimestamp(1);
                    return value == null ? null : value.toInstant();
                }, assignmentId).get(0);
            if (reworkRequestedAt != null) {
                int freshAttempts = jdbc.queryForObject("SELECT COUNT(*) FROM activity_attempt WHERE assignment_id=? AND created_at>=?",
                    Integer.class, assignmentId, Timestamp.from(reworkRequestedAt));
                if (freshAttempts == 0) throw new Stage3Service.ConflictException("A new attempt is required after rework");
            }
        }
        if (target == AssignmentStatus.COMPLETED) {
            for (ActivityProgress activity : locked.projection().activities()) {
                if (activity.requiredEvidence() == EvidenceType.PARENT_CONFIRMED) {
                    jdbc.update("""
                        INSERT INTO mastery_evidence(id,assignment_id,activity_id,evidence_type,note,actor_id,created_at)
                        VALUES (?,?,?,'PARENT_CONFIRMED',?,?,?)
                        """, UUID.randomUUID(), assignmentId, activity.id(), note, actorId, Timestamp.from(now));
                }
            }
            jdbc.update("""
                INSERT INTO mastery_evidence(id,assignment_id,activity_id,evidence_type,note,actor_id,created_at)
                VALUES (?,?,NULL,'MASTERED',?,?,?)
                """, UUID.randomUUID(), assignmentId, note, actorId, Timestamp.from(now));
        }
        jdbc.update("UPDATE lesson_assignment SET status=?,version=version+1,updated_at=? WHERE id=?",
            target.name(), Timestamp.from(now), assignmentId);
        if (target == AssignmentStatus.REWORK_REQUIRED) {
            jdbc.update("UPDATE learning_completion SET rework_requested_at=? WHERE assignment_id=?", Timestamp.from(now), assignmentId);
        } else if (target == AssignmentStatus.SUBMITTED) {
            jdbc.update("UPDATE learning_completion SET rework_requested_at=NULL WHERE assignment_id=?", assignmentId);
        }
        jdbc.update("""
            UPDATE learning_completion SET status=?,submitted_at=CASE WHEN ?='SUBMITTED' THEN ? ELSE submitted_at END,
                completed_at=CASE WHEN ?='COMPLETED' THEN ? ELSE completed_at END,review_note=?,
                reviewed_by=CASE WHEN ? IN ('COMPLETED','REWORK_REQUIRED') THEN ? ELSE reviewed_by END,
                version=version+1,updated_at=? WHERE assignment_id=?
            """, target.name(), target.name(), Timestamp.from(now), target.name(), Timestamp.from(now), note,
            target.name(), actorId, Timestamp.from(now), assignmentId);
        String actionType = target == AssignmentStatus.SUBMITTED ? "SUBMIT"
            : target == AssignmentStatus.COMPLETED ? "APPROVE" : "REWORK";
        action(familyId, assignmentId, assignmentId, actorId, actionType, key, payloadHash, now);
        return assignment(familyId, childId, assignmentId).orElseThrow().projection();
    }

    private AssignmentFacts lockAssignment(UUID familyId, UUID childId, UUID assignmentId) {
        jdbc.query("SELECT id FROM lesson_assignment WHERE family_id=? AND child_id=? AND id=? FOR UPDATE",
            (rs, row) -> rs.getObject(1, UUID.class), familyId, childId, assignmentId).stream().findFirst()
            .orElseThrow(FamilyGrowthService.NotFoundException::new);
        return assignment(familyId, childId, assignmentId).orElseThrow();
    }

    private LearningAssignment assignment(ResultSet rs) throws SQLException {
        return new LearningAssignment(rs.getObject("id", UUID.class), rs.getObject("child_id", UUID.class),
            rs.getObject("course_version_id", UUID.class), rs.getObject("lesson_id", UUID.class),
            rs.getString("course_title"), rs.getString("unit_title"), rs.getString("lesson_title"),
            rs.getString("lesson_summary"), SchoolStage.valueOf(rs.getString("school_stage")),
            rs.getString("subject_code"), AssignmentStatus.valueOf(rs.getString("status")), rs.getLong("version"),
            List.of(), rs.getString("review_note"), rs.getTimestamp("updated_at").toInstant(),
            AssignmentSource.valueOf(rs.getString("assignment_source")), new RewardSnapshot(
                rs.getBigDecimal("money_reward_snapshot"), rs.getLong("coin_reward_snapshot"),
                rs.getLong("xp_reward_snapshot"), instant(rs, "reward_settled_at")));
    }

    private AssignmentFacts facts(LearningAssignment bare) {
        List<ActivityContent> contents = activities(bare.lessonId());
        List<ActivityProgress> progress = contents.stream().map(content -> {
            Set<EvidenceType> evidence = new LinkedHashSet<>(jdbc.query("""
                SELECT DISTINCT evidence_type FROM mastery_evidence WHERE assignment_id=? AND activity_id=?
                """, (rs, row) -> EvidenceType.valueOf(rs.getString(1)), bare.id(), content.id()));
            Boolean correct = jdbc.query("""
                SELECT checked_correct FROM activity_attempt WHERE assignment_id=? AND activity_id=? AND evidence_type='CHECKED'
                ORDER BY created_at DESC,id DESC LIMIT 1
                """, (rs, row) -> (Boolean) rs.getObject(1), bare.id(), content.id()).stream().findFirst().orElse(null);
            return new ActivityProgress(content.id(), content.type(), content.title(), content.instruction(),
                content.contentRef(), content.expectedMinutes(), content.prompt(), content.hint(), content.options(),
                content.type().requiredEvidence(), Set.copyOf(evidence), correct);
        }).toList();
        LearningAssignment projection = new LearningAssignment(bare.id(), bare.childId(), bare.courseVersionId(),
            bare.lessonId(), bare.courseTitle(), bare.unitTitle(), bare.lessonTitle(), bare.lessonSummary(),
            bare.schoolStage(), bare.subjectCode(), bare.status(), bare.version(), progress, bare.reviewNote(), bare.updatedAt(),
            bare.assignmentSource(), bare.reward());
        Map<UUID, ActivityContent> contentMap = new LinkedHashMap<>();
        contents.forEach(content -> contentMap.put(content.id(), content));
        return new AssignmentFacts(projection, Map.copyOf(contentMap));
    }

    private void action(UUID familyId, UUID targetId, UUID resultId, UUID actorId, String type,
                        String key, String payloadHash, Instant now) {
        jdbc.update("""
            INSERT INTO teaching_action(id,family_id,target_id,result_id,actor_id,action_type,payload_hash,idempotency_key,created_at)
            VALUES (?,?,?,?,?,?,?,?,?)
            """, UUID.randomUUID(), familyId, targetId, resultId, actorId, type, payloadHash, key, Timestamp.from(now));
    }

    private static CourseVersion mapVersion(ResultSet rs) throws SQLException {
        return new CourseVersion(rs.getObject("course_id", UUID.class), rs.getObject("version_id", UUID.class),
            rs.getObject("family_id", UUID.class), SchoolStage.valueOf(rs.getString("school_stage")),
            rs.getString("subject_code"), rs.getString("title"), rs.getInt("version_number"),
            rs.getString("summary"), rs.getString("rights_basis"), ageBand(rs.getString("kindergarten_age_band")),
            decodeDomains(rs.getString("kindergarten_domains")), CourseVersionStatus.valueOf(rs.getString("status")),
            List.of(), instant(rs, "published_at"));
    }

    private static String encodeDomains(List<KindergartenDomain> domains) {
        return domains.stream().map(Enum::name).sorted().collect(java.util.stream.Collectors.joining(","));
    }

    private static List<KindergartenDomain> decodeDomains(String value) {
        if (value == null || value.isBlank()) return List.of();
        return java.util.Arrays.stream(value.split(",")).map(KindergartenDomain::valueOf).toList();
    }

    private static KindergartenAgeBand ageBand(String value) {
        return value == null || value.isBlank() ? null : KindergartenAgeBand.valueOf(value);
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }
}
