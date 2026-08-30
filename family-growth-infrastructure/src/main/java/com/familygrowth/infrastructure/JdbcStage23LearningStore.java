package com.familygrowth.infrastructure;

import com.familygrowth.application.FamilyGrowthService;
import com.familygrowth.application.Stage23LearningStore;
import com.familygrowth.application.Stage3Service;
import com.familygrowth.application.Stage3Store;
import com.familygrowth.domain.Stage20Models.SchoolStage;
import com.familygrowth.domain.Stage21TeachingModels.AssignmentStatus;
import com.familygrowth.domain.Stage23LearningModels.RewardPolicy;
import com.familygrowth.domain.Stage23LearningModels.MisconceptionCategory;
import com.familygrowth.domain.Stage23LearningModels.SupportEvent;
import com.familygrowth.domain.Stage23LearningModels.SupportEventType;
import com.familygrowth.domain.Stage3Models.AssetType;
import com.familygrowth.domain.Stage3Models.Wallet;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcStage23LearningStore implements Stage23LearningStore {
    private final JdbcTemplate jdbc;
    private final Stage3Store accounts;

    JdbcStage23LearningStore(JdbcTemplate jdbc, Stage3Store accounts) {
        this.jdbc = jdbc; this.accounts = accounts;
    }

    @Override
    public RewardPolicy policy(UUID familyId, UUID childId) {
        requireChild(familyId, childId, false);
        return jdbc.query("SELECT * FROM autonomous_learning_reward_policy WHERE family_id=? AND child_id=?",
            (rs, row) -> new RewardPolicy(familyId, childId, rs.getBigDecimal("money_reward"),
                rs.getLong("coin_reward"), rs.getLong("xp_reward"), rs.getLong("version"),
                rs.getTimestamp("updated_at").toInstant()), familyId, childId).stream().findFirst()
            .orElse(new RewardPolicy(familyId, childId, BigDecimal.ZERO, 0, 0, 0, null));
    }

    @Override
    public RewardPolicy updatePolicy(UUID familyId, UUID childId, UUID actorId, BigDecimal money, long coin, long xp,
                                     long expectedVersion, String reason, Instant now) {
        requireChild(familyId, childId, true);
        RewardPolicy next = new RewardPolicy(familyId, childId, money, coin, xp, expectedVersion, now);
        RewardPolicy old = policy(familyId, childId);
        if (old.version() != expectedVersion) throw new Stage3Service.ConflictException("Reward policy version conflict");
        int changed;
        if (old.updatedAt() == null) {
            changed = jdbc.update("""
                INSERT INTO autonomous_learning_reward_policy
                (child_id,family_id,money_reward,coin_reward,xp_reward,version,updated_by,created_at,updated_at)
                VALUES (?,?,?,?,?,1,?,?,?)
                """, childId, familyId, next.moneyReward(), coin, xp, actorId, ts(now), ts(now));
        } else {
            changed = jdbc.update("""
                UPDATE autonomous_learning_reward_policy SET money_reward=?,coin_reward=?,xp_reward=?,
                version=version+1,updated_by=?,updated_at=? WHERE family_id=? AND child_id=? AND version=?
                """, next.moneyReward(), coin, xp, actorId, ts(now), familyId, childId, expectedVersion);
        }
        if (changed != 1) throw new Stage3Service.ConflictException("Reward policy version conflict");
        jdbc.update("""
            INSERT INTO autonomous_learning_reward_audit
            (id,family_id,child_id,old_money_reward,new_money_reward,old_coin_reward,new_coin_reward,
             old_xp_reward,new_xp_reward,reason,actor_id,created_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
            """, UUID.randomUUID(), familyId, childId, old.moneyReward(), next.moneyReward(), old.coinReward(), coin,
            old.xpReward(), xp, reason, actorId, ts(now));
        return policy(familyId, childId);
    }

    @Override
    public Optional<SyncReplay> syncReplay(UUID familyId, String key) {
        return jdbc.query("SELECT child_id,school_stage,payload_hash,created_count FROM autonomous_enrollment_action WHERE family_id=? AND idempotency_key=?",
            (rs, row) -> new SyncReplay(rs.getObject(1, UUID.class), SchoolStage.valueOf(rs.getString(2)),
                rs.getString(3), rs.getInt(4)), familyId, key).stream().findFirst();
    }

    @Override
    public int syncAssignments(UUID familyId, UUID childId, SchoolStage stage, UUID actorId, String key,
                               String payloadHash, Instant now) {
        requireChild(familyId, childId, true);
        RewardPolicy reward = policy(familyId, childId);
        List<Object[]> lessons = jdbc.query("""
            SELECT v.id,l.id,COALESCE(v.published_by,v.created_by)
            FROM teaching_course c JOIN teaching_course_version v ON v.course_id=c.id
            JOIN teaching_unit u ON u.course_version_id=v.id JOIN teaching_lesson l ON l.unit_id=u.id
            WHERE c.family_id=? AND c.school_stage=? AND v.status='PUBLISHED'
              AND NOT EXISTS (SELECT 1 FROM teaching_course_withdrawal w WHERE w.course_version_id=v.id)
              AND v.version_number=(SELECT MAX(v2.version_number) FROM teaching_course_version v2
                                    WHERE v2.course_id=c.id AND v2.status='PUBLISHED')
            ORDER BY c.created_at,u.display_order,l.display_order
            """, (rs, row) -> new Object[]{rs.getObject(1, UUID.class), rs.getObject(2, UUID.class),
                rs.getObject(3, UUID.class)}, familyId, stage.name());
        int created = 0;
        for (Object[] lesson : lessons) {
            Integer exists = jdbc.queryForObject("SELECT COUNT(*) FROM lesson_assignment WHERE child_id=? AND lesson_id=?",
                Integer.class, childId, lesson[1]);
            if (exists != null && exists > 0) continue;
            UUID assignmentId = UUID.randomUUID();
            jdbc.update("""
                INSERT INTO lesson_assignment
                (id,family_id,child_id,course_version_id,lesson_id,status,assigned_by,idempotency_key,version,
                 created_at,updated_at,assignment_source,money_reward_snapshot,coin_reward_snapshot,xp_reward_snapshot)
                VALUES (?,?,?,?,?,'ASSIGNED',?,?,0,?,?,'AUTONOMOUS',?,?,?)
                """, assignmentId, familyId, childId, lesson[0], lesson[1], lesson[2],
                "auto:" + childId + ":" + lesson[1], ts(now), ts(now), reward.moneyReward(), reward.coinReward(), reward.xpReward());
            jdbc.update("INSERT INTO learning_completion(assignment_id,status,version,updated_at) VALUES (?,'ASSIGNED',0,?)",
                assignmentId, ts(now));
            created++;
        }
        jdbc.update("""
            INSERT INTO autonomous_enrollment_action
            (id,family_id,child_id,school_stage,actor_id,payload_hash,idempotency_key,created_count,created_at)
            VALUES (?,?,?,?,?,?,?,?,?)
            """, UUID.randomUUID(), familyId, childId, stage.name(), actorId, payloadHash, key, created, ts(now));
        return created;
    }

    @Override
    public void snapshotAssignment(UUID familyId, UUID childId, UUID assignmentId, Instant now) {
        RewardPolicy reward = policy(familyId, childId);
        int changed = jdbc.update("""
            UPDATE lesson_assignment SET money_reward_snapshot=?,coin_reward_snapshot=?,xp_reward_snapshot=?
            WHERE family_id=? AND child_id=? AND id=? AND reward_settled_at IS NULL
            """, reward.moneyReward(), reward.coinReward(), reward.xpReward(), familyId, childId, assignmentId);
        if (changed != 1) throw new FamilyGrowthService.NotFoundException();
    }

    @Override
    public void settleReward(UUID familyId, UUID childId, UUID assignmentId, UUID actorId, Instant now) {
        Object[] reward = jdbc.query("""
            SELECT status,money_reward_snapshot,coin_reward_snapshot,xp_reward_snapshot,reward_settled_at
            FROM lesson_assignment WHERE family_id=? AND child_id=? AND id=? FOR UPDATE
            """, (rs, row) -> new Object[]{rs.getString(1),rs.getBigDecimal(2),rs.getLong(3),rs.getLong(4),rs.getTimestamp(5)},
            familyId, childId, assignmentId).stream().findFirst().orElseThrow(FamilyGrowthService.NotFoundException::new);
        if (reward[4] != null) return;
        if (AssignmentStatus.valueOf((String) reward[0]) != AssignmentStatus.COMPLETED) {
            throw new Stage3Service.ConflictException("Learning reward requires a completed assignment");
        }
        BigDecimal money = (BigDecimal) reward[1]; long coin = (Long) reward[2]; long xp = (Long) reward[3];
        accounts.ensureChildAccounts(familyId, childId, now);
        Wallet wallet = jdbc.query("SELECT child_id,family_id,money_balance,reserved_money,coin_balance,version FROM wallet WHERE family_id=? AND child_id=? FOR UPDATE",
            (rs, row) -> new Wallet(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class),rs.getBigDecimal(3),
                rs.getBigDecimal(4),rs.getBigDecimal(3).subtract(rs.getBigDecimal(4)),rs.getLong(5),rs.getLong(6)),
            familyId, childId).stream().findFirst().orElseThrow(FamilyGrowthService.NotFoundException::new);
        BigDecimal moneyAfter = wallet.moneyBalance().add(money); long coinAfter = Math.addExact(wallet.coinBalance(), coin);
        UUID groupId = UUID.randomUUID(); String idem = "learning:" + assignmentId;
        if (money.signum() > 0) insertLedger(familyId, childId, assignmentId, groupId, actorId, AssetType.MONEY,
            money, wallet.moneyBalance(), moneyAfter, idem, now);
        if (coin > 0) insertLedger(familyId, childId, assignmentId, groupId, actorId, AssetType.COIN,
            BigDecimal.valueOf(coin), BigDecimal.valueOf(wallet.coinBalance()), BigDecimal.valueOf(coinAfter), idem, now);
        if (xp > 0) jdbc.update("UPDATE child_progress SET xp_balance=xp_balance+?,version=version+1,updated_at=? WHERE family_id=? AND child_id=?",
            xp, ts(now), familyId, childId);
        jdbc.update("UPDATE wallet SET money_balance=?,coin_balance=?,version=version+1,updated_at=? WHERE family_id=? AND child_id=?",
            moneyAfter, coinAfter, ts(now), familyId, childId);
        int changed = jdbc.update("UPDATE lesson_assignment SET reward_settled_at=?,reward_group_id=? WHERE id=? AND reward_settled_at IS NULL",
            ts(now), groupId, assignmentId);
        if (changed != 1) throw new Stage3Service.ConflictException("Learning reward was already settled");
    }

    @Override
    public List<SupportEvent> supportEvents(UUID familyId, UUID childId, UUID assignmentId) {
        requireAssignment(familyId, childId, assignmentId, null, false);
        return jdbc.query("SELECT * FROM learning_support_event WHERE family_id=? AND child_id=? AND assignment_id=? ORDER BY created_at,id",
            (rs,row) -> new SupportEvent(rs.getObject("id",UUID.class), rs.getObject("assignment_id",UUID.class),
                rs.getObject("activity_id",UUID.class), SupportEventType.valueOf(rs.getString("event_type")),
                rs.getString("category") == null ? null : MisconceptionCategory.valueOf(rs.getString("category")),
                rs.getString("child_message"), rs.getString("private_note"), instant(rs.getTimestamp("revisit_at")),
                rs.getObject("parent_event_id",UUID.class), rs.getTimestamp("created_at").toInstant()),
            familyId, childId, assignmentId);
    }

    @Override
    public void requestHelp(UUID familyId, UUID childId, UUID assignmentId, UUID activityId, UUID actorId,
                            String message, String key, String payloadHash, Instant now) {
        requireAssignment(familyId, childId, assignmentId, activityId, true);
        if (replay(familyId, key, payloadHash)) return;
        insertSupport(UUID.randomUUID(), familyId, childId, assignmentId, activityId, SupportEventType.HELP_REQUESTED,
            null, message, "", null, null, actorId, key, payloadHash, now);
    }

    @Override
    public void classifySupport(UUID familyId, UUID childId, UUID assignmentId, UUID sourceEventId, UUID actorId,
                                MisconceptionCategory category, String privateNote, Instant revisitAt,
                                String key, String payloadHash, Instant now) {
        requireAssignment(familyId, childId, assignmentId, null, false);
        if (replay(familyId, key, payloadHash)) return;
        SupportEvent source = supportEvents(familyId, childId, assignmentId).stream().filter(e -> e.id().equals(sourceEventId))
            .findFirst().orElseThrow(FamilyGrowthService.NotFoundException::new);
        if (source.type() != SupportEventType.HELP_REQUESTED && source.type() != SupportEventType.INCORRECT_OBSERVED) {
            throw new Stage3Service.ConflictException("Only a help or incorrect fact can be classified");
        }
        UUID classified = UUID.randomUUID();
        insertSupport(classified, familyId, childId, assignmentId, source.activityId(),
            SupportEventType.MISCONCEPTION_CLASSIFIED, category, "家长已经看到，会一起找办法。", privateNote,
            null, sourceEventId, actorId, key, payloadHash, now);
        if (revisitAt != null) insertSupport(UUID.randomUUID(), familyId, childId, assignmentId, source.activityId(),
            SupportEventType.REVISIT_SCHEDULED, category, "稍后再试一次。", "", revisitAt, classified,
            actorId, key + ":revisit", payloadHash, now);
    }

    @Override
    public void recordAttemptSupport(UUID familyId, UUID childId, UUID assignmentId, UUID activityId,
                                     UUID actorId, Boolean correct, String attemptKey, Instant now) {
        if (correct == null) return;
        if (!correct) {
            String key = "incorrect:" + attemptKey;
            if (!replay(familyId, key, key)) insertSupport(UUID.randomUUID(), familyId, childId, assignmentId, activityId,
                SupportEventType.INCORRECT_OBSERVED, null, "这次答案还没对上，可以慢慢找一找。", "", null, null,
                actorId, key, key, now);
            return;
        }
        List<UUID> due = jdbc.query("""
            SELECT s.id FROM learning_support_event s
            WHERE s.family_id=? AND s.child_id=? AND s.assignment_id=? AND s.activity_id=?
              AND s.event_type='REVISIT_SCHEDULED' AND s.revisit_at<=?
              AND NOT EXISTS (SELECT 1 FROM learning_support_event c WHERE c.parent_event_id=s.id AND c.event_type='REVISIT_COMPLETED')
            ORDER BY s.revisit_at
            """, (rs,row)->rs.getObject(1,UUID.class), familyId, childId, assignmentId, activityId, ts(now));
        for (UUID scheduleId : due) {
            String key = "revisit-complete:" + scheduleId;
            if (!replay(familyId, key, key)) insertSupport(UUID.randomUUID(), familyId, childId, assignmentId, activityId,
                SupportEventType.REVISIT_COMPLETED, null, "按计划又试了一次，并答对了。", "", null, scheduleId,
                actorId, key, key, now);
        }
    }

    private boolean replay(UUID familyId, String key, String payloadHash) {
        List<String> hashes = jdbc.query("SELECT payload_hash FROM learning_support_event WHERE family_id=? AND idempotency_key=?",
            (rs,row)->rs.getString(1), familyId, key);
        if (hashes.isEmpty()) return false;
        if (!hashes.get(0).equals(payloadHash)) throw new Stage3Service.ConflictException("Idempotency key payload mismatch");
        return true;
    }

    private void insertSupport(UUID id, UUID familyId, UUID childId, UUID assignmentId, UUID activityId,
                               SupportEventType type, MisconceptionCategory category, String childMessage,
                               String privateNote, Instant revisitAt, UUID parentEventId, UUID actorId,
                               String key, String payloadHash, Instant now) {
        jdbc.update("""
            INSERT INTO learning_support_event
            (id,family_id,child_id,assignment_id,activity_id,event_type,category,child_message,private_note,
             revisit_at,parent_event_id,actor_id,idempotency_key,payload_hash,created_at)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """, id, familyId, childId, assignmentId, activityId, type.name(), category == null ? null : category.name(),
            childMessage, privateNote, revisitAt == null ? null : ts(revisitAt), parentEventId, actorId, key, payloadHash, ts(now));
    }

    private void requireAssignment(UUID familyId, UUID childId, UUID assignmentId, UUID activityId, boolean open) {
        String sql = """
            SELECT COUNT(*) FROM lesson_assignment a
            WHERE a.family_id=? AND a.child_id=? AND a.id=?
            """ + (activityId == null ? "" : " AND EXISTS (SELECT 1 FROM learning_activity x JOIN teaching_lesson l ON l.id=a.lesson_id WHERE x.lesson_id=l.id AND x.id=?)")
            + (open ? " AND a.status IN ('ASSIGNED','IN_PROGRESS','REWORK_REQUIRED')" : "");
        Object[] args = activityId == null ? new Object[]{familyId,childId,assignmentId} : new Object[]{familyId,childId,assignmentId,activityId};
        Integer count = jdbc.queryForObject(sql, Integer.class, args);
        if (count == null || count != 1) throw new FamilyGrowthService.NotFoundException();
    }

    private void insertLedger(UUID familyId, UUID childId, UUID assignmentId, UUID groupId, UUID actorId,
                              AssetType asset, BigDecimal delta, BigDecimal before, BigDecimal after,
                              String key, Instant now) {
        jdbc.update("""
            INSERT INTO ledger_entry(id,family_id,child_id,asset_type,delta,before_balance,after_balance,
            entry_type,business_type,business_id,group_id,idempotency_key,actor_id,reason,created_at)
            VALUES (?,?,?,?,?,?,?,'LEARNING_REWARD','LEARNING_ASSIGNMENT',?,?,?,?,?,?)
            """, UUID.randomUUID(), familyId, childId, asset.name(), delta, before, after,
            assignmentId, groupId, key, actorId, "自主课程经家长确认完成", ts(now));
    }

    private void requireChild(UUID familyId, UUID childId, boolean lock) {
        String sql = "SELECT id FROM child_profile WHERE family_id=? AND id=?" + (lock ? " FOR UPDATE" : "");
        if (jdbc.query(sql, (rs,row)->rs.getObject(1,UUID.class), familyId, childId).isEmpty()) {
            throw new FamilyGrowthService.NotFoundException();
        }
    }
    private static Timestamp ts(Instant value) { return Timestamp.from(value); }
    private static Instant instant(Timestamp value) { return value == null ? null : value.toInstant(); }
}
