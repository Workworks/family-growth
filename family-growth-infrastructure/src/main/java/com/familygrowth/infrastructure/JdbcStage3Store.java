package com.familygrowth.infrastructure;

import com.familygrowth.application.FamilyGrowthService;
import com.familygrowth.application.Stage3Service;
import com.familygrowth.application.Stage3Store;
import com.familygrowth.domain.Stage3Models.Actor;
import com.familygrowth.domain.Stage3Models.ActorRole;
import com.familygrowth.domain.Stage3Models.AssetType;
import com.familygrowth.domain.Stage3Models.CompletionStatus;
import com.familygrowth.domain.Stage3Models.LedgerEntry;
import com.familygrowth.domain.Stage3Models.RewardGrant;
import com.familygrowth.domain.Stage3Models.TaskCompletion;
import com.familygrowth.domain.Stage3Models.Wallet;
import com.familygrowth.domain.Stage3Models.WalletReconciliation;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcStage3Store implements Stage3Store {
    private final JdbcTemplate jdbc;

    JdbcStage3Store(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void createPinCredential(UUID familyId, UUID parentId, String pinHash, Instant now) {
        jdbc.update("""
            INSERT INTO parent_pin_credential
                (parent_id, family_id, pin_hash, failed_attempts, version, created_at, updated_at)
            VALUES (?, ?, ?, 0, 0, ?, ?)
            """, parentId, familyId, pinHash, ts(now), ts(now));
    }

    @Override
    public Optional<PinCredential> findPinCredential(UUID familyId, UUID parentId) {
        return jdbc.query("""
            SELECT family_id, parent_id, pin_hash, failed_attempts, locked_until
            FROM parent_pin_credential WHERE family_id = ? AND parent_id = ?
            """, this::pinCredential, familyId, parentId).stream().findFirst();
    }

    @Override
    public void recordFailedPin(UUID parentId, int failedAttempts, Instant lockedUntil, Instant now) {
        jdbc.update("""
            UPDATE parent_pin_credential SET failed_attempts = ?, locked_until = ?,
                version = version + 1, updated_at = ? WHERE parent_id = ?
            """, failedAttempts, nullableTs(lockedUntil), ts(now), parentId);
    }

    @Override
    public void clearFailedPin(UUID parentId, Instant now) {
        jdbc.update("""
            UPDATE parent_pin_credential SET failed_attempts = 0, locked_until = NULL,
                version = version + 1, updated_at = ? WHERE parent_id = ?
            """, ts(now), parentId);
    }

    @Override
    public void saveSession(String tokenHash, Actor actor, Instant expiresAt, Instant now) {
        jdbc.update("""
            INSERT INTO auth_session
                (id, token_hash, family_id, actor_id, actor_role, child_id, expires_at, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """, UUID.randomUUID(), tokenHash, actor.familyId(), actor.actorId(), actor.role().name(),
            actor.childId(), ts(expiresAt), ts(now));
    }

    @Override
    public Optional<StoredSession> findSession(String tokenHash, Instant now) {
        return jdbc.query("""
            SELECT token_hash, family_id, actor_id, actor_role, child_id, expires_at, revoked_at
            FROM auth_session
            WHERE token_hash = ? AND expires_at > ? AND revoked_at IS NULL
            """, this::session, tokenHash, ts(now)).stream().findFirst();
    }

    @Override
    public void ensureChildAccounts(UUID familyId, UUID childId, Instant now) {
        int walletCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM wallet WHERE family_id = ? AND child_id = ?",
            Integer.class, familyId, childId);
        if (walletCount == 0) {
            jdbc.update("""
                INSERT INTO wallet
                    (child_id, family_id, money_balance, coin_balance, version, created_at, updated_at)
                VALUES (?, ?, 0.00, 0, 0, ?, ?)
                """, childId, familyId, ts(now), ts(now));
        }
        int progressCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM child_progress WHERE family_id = ? AND child_id = ?",
            Integer.class, familyId, childId);
        if (progressCount == 0) {
            jdbc.update("""
                INSERT INTO child_progress
                    (child_id, family_id, xp_balance, version, created_at, updated_at)
                VALUES (?, ?, 0, 0, ?, ?)
                """, childId, familyId, ts(now), ts(now));
        }
    }

    @Override
    public boolean taskBelongsToChild(UUID familyId, UUID childId, UUID taskId) {
        Integer count = jdbc.queryForObject("""
            SELECT COUNT(*) FROM growth_task t
            JOIN growth_goal g ON g.id = t.goal_id AND g.family_id = t.family_id
            JOIN growth_plan p ON p.id = g.plan_id AND p.family_id = g.family_id
            WHERE t.id = ? AND t.family_id = ? AND p.child_id = ? AND t.active = TRUE
            """, Integer.class, taskId, familyId, childId);
        return count != null && count == 1;
    }

    @Override
    public Optional<TaskCompletion> findCompletionByIdempotency(UUID familyId, String idempotencyKey) {
        return jdbc.query("""
            SELECT * FROM task_completion WHERE family_id = ? AND submit_idempotency_key = ?
            """, this::completion, familyId, idempotencyKey).stream().findFirst();
    }

    @Override
    public TaskCompletion submitCompletion(
        UUID familyId, UUID childId, UUID taskId, UUID submittedBy,
        String evidenceNote, String idempotencyKey, Instant now
    ) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
            INSERT INTO task_completion
                (id, family_id, child_id, task_id, status, evidence_note, submitted_by,
                 review_note, xp_reward, coin_reward, money_reward, submit_idempotency_key,
                 version, submitted_at)
            VALUES (?, ?, ?, ?, 'SUBMITTED', ?, ?, '', 0, 0, 0.00, ?, 0, ?)
            """, id, familyId, childId, taskId, evidenceNote, submittedBy, idempotencyKey, ts(now));
        jdbc.update("""
            INSERT INTO idempotency_operation
                (id, family_id, operation_type, idempotency_key, result_id, created_at)
            VALUES (?, ?, 'SUBMIT_COMPLETION', ?, ?, ?)
            """, UUID.randomUUID(), familyId, idempotencyKey, id, ts(now));
        return findCompletion(familyId, id).orElseThrow();
    }

    @Override
    public Optional<TaskCompletion> findCompletion(UUID familyId, UUID completionId) {
        return jdbc.query("SELECT * FROM task_completion WHERE family_id = ? AND id = ?",
            this::completion, familyId, completionId).stream().findFirst();
    }

    @Override
    public TaskCompletion reviewCompletion(
        UUID familyId, UUID completionId, UUID reviewerId, boolean approve,
        RewardGrant rewards, String reviewNote, String idempotencyKey, Instant now
    ) {
        TaskCompletion completion = jdbc.query(
            "SELECT * FROM task_completion WHERE family_id = ? AND id = ? FOR UPDATE",
            this::completion, familyId, completionId).stream().findFirst()
            .orElseThrow(FamilyGrowthService.NotFoundException::new);
        if (completion.status() != CompletionStatus.SUBMITTED) {
            throw new Stage3Service.ConflictException("Completion is already reviewed");
        }

        UUID groupId = approve ? UUID.randomUUID() : null;
        if (approve) {
            ensureChildAccounts(familyId, completion.childId(), now);
            Wallet wallet = jdbc.query(
                "SELECT child_id, family_id, money_balance, reserved_money, coin_balance, version FROM wallet " +
                    "WHERE family_id = ? AND child_id = ? FOR UPDATE",
                this::wallet, familyId, completion.childId()).stream().findFirst()
                .orElseThrow(FamilyGrowthService.NotFoundException::new);

            BigDecimal moneyBefore = wallet.moneyBalance();
            BigDecimal moneyAfter = moneyBefore.add(rewards.money());
            long coinBefore = wallet.coinBalance();
            long coinAfter = Math.addExact(coinBefore, rewards.coin());

            jdbc.update("""
                UPDATE child_progress SET xp_balance = xp_balance + ?, version = version + 1,
                    updated_at = ? WHERE family_id = ? AND child_id = ?
                """, rewards.xp(), ts(now), familyId, completion.childId());
            if (rewards.money().signum() > 0) {
                insertLedger(familyId, completion.childId(), AssetType.MONEY, rewards.money(),
                    moneyBefore, moneyAfter, completionId, groupId, idempotencyKey, reviewerId, reviewNote, now);
            }
            if (rewards.coin() > 0) {
                insertLedger(familyId, completion.childId(), AssetType.COIN,
                    BigDecimal.valueOf(rewards.coin()), BigDecimal.valueOf(coinBefore),
                    BigDecimal.valueOf(coinAfter), completionId, groupId, idempotencyKey, reviewerId, reviewNote, now);
            }
            jdbc.update("""
                UPDATE wallet SET money_balance = ?, coin_balance = ?, version = version + 1,
                    updated_at = ? WHERE family_id = ? AND child_id = ?
                """, moneyAfter, coinAfter, ts(now), familyId, completion.childId());
        }

        String status = approve ? CompletionStatus.APPROVED.name() : CompletionStatus.REJECTED.name();
        int changed = jdbc.update("""
            UPDATE task_completion SET status = ?, reviewed_by = ?, review_note = ?,
                xp_reward = ?, coin_reward = ?, money_reward = ?, ledger_group_id = ?,
                review_idempotency_key = ?, version = version + 1, reviewed_at = ?
            WHERE family_id = ? AND id = ? AND status = 'SUBMITTED'
            """, status, reviewerId, reviewNote, rewards.xp(), rewards.coin(), rewards.money(), groupId,
            idempotencyKey, ts(now), familyId, completionId);
        if (changed != 1) {
            throw new Stage3Service.ConflictException("Completion review raced with another request");
        }
        jdbc.update("""
            INSERT INTO idempotency_operation
                (id, family_id, operation_type, idempotency_key, result_id, created_at)
            VALUES (?, ?, 'REVIEW_COMPLETION', ?, ?, ?)
            """, UUID.randomUUID(), familyId, idempotencyKey, completionId, ts(now));
        return findCompletion(familyId, completionId).orElseThrow();
    }

    @Override
    public Wallet getWallet(UUID familyId, UUID childId) {
        return jdbc.query("""
            SELECT child_id, family_id, money_balance, reserved_money, coin_balance, version
            FROM wallet WHERE family_id = ? AND child_id = ?
            """, this::wallet, familyId, childId).stream().findFirst()
            .orElseThrow(FamilyGrowthService.NotFoundException::new);
    }

    @Override
    public List<LedgerEntry> getLedger(UUID familyId, UUID childId, int limit) {
        return jdbc.query("""
            SELECT * FROM ledger_entry WHERE family_id = ? AND child_id = ?
            ORDER BY created_at DESC, id DESC LIMIT ?
            """, this::ledger, familyId, childId, limit);
    }

    @Override
    public Optional<LedgerEntry> findAdjustment(UUID familyId, String idempotencyKey) {
        return jdbc.query("""
            SELECT * FROM ledger_entry
            WHERE family_id = ? AND entry_type = 'PARENT_ADJUSTMENT' AND idempotency_key = ?
            """, this::ledger, familyId, idempotencyKey).stream().findFirst();
    }

    @Override
    public LedgerEntry adjustWallet(
        UUID familyId, UUID childId, UUID actorId, AssetType assetType,
        BigDecimal delta, String reason, String idempotencyKey, Instant now
    ) {
        Wallet wallet = jdbc.query(
            "SELECT child_id, family_id, money_balance, reserved_money, coin_balance, version FROM wallet " +
                "WHERE family_id = ? AND child_id = ? FOR UPDATE",
            this::wallet, familyId, childId).stream().findFirst()
            .orElseThrow(FamilyGrowthService.NotFoundException::new);
        BigDecimal before = assetType == AssetType.MONEY
            ? wallet.moneyBalance() : BigDecimal.valueOf(wallet.coinBalance());
        BigDecimal after = before.add(delta);
        if (after.signum() < 0
            || (assetType == AssetType.MONEY && after.compareTo(wallet.reservedMoney()) < 0)) {
            throw new Stage3Service.ConflictException("Wallet balance cannot be negative");
        }
        UUID businessId = UUID.nameUUIDFromBytes(
            ("PARENT_ADJUSTMENT:" + familyId + ":" + idempotencyKey)
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
        UUID groupId = UUID.randomUUID();
        jdbc.update("""
            INSERT INTO ledger_entry
                (id, family_id, child_id, asset_type, delta, before_balance, after_balance,
                 entry_type, business_type, business_id, group_id, idempotency_key,
                 actor_id, reason, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, 'PARENT_ADJUSTMENT', 'PARENT_ADJUSTMENT',
                    ?, ?, ?, ?, ?, ?)
            """, UUID.randomUUID(), familyId, childId, assetType.name(), delta, before, after,
            businessId, groupId, idempotencyKey, actorId, reason, ts(now));
        if (assetType == AssetType.MONEY) {
            jdbc.update("""
                UPDATE wallet SET money_balance = ?, version = version + 1, updated_at = ?
                WHERE family_id = ? AND child_id = ?
                """, after, ts(now), familyId, childId);
        } else {
            long coinAfter = after.longValueExact();
            jdbc.update("""
                UPDATE wallet SET coin_balance = ?, version = version + 1, updated_at = ?
                WHERE family_id = ? AND child_id = ?
                """, coinAfter, ts(now), familyId, childId);
        }
        jdbc.update("""
            INSERT INTO idempotency_operation
                (id, family_id, operation_type, idempotency_key, result_id, created_at)
            VALUES (?, ?, 'PARENT_ADJUSTMENT', ?, ?, ?)
            """, UUID.randomUUID(), familyId, idempotencyKey, businessId, ts(now));
        return findAdjustment(familyId, idempotencyKey).orElseThrow();
    }

    @Override
    public WalletReconciliation reconcile(UUID familyId, UUID childId) {
        Wallet wallet = getWallet(familyId, childId);
        BigDecimal ledgerMoney = jdbc.queryForObject("""
            SELECT COALESCE(SUM(delta), 0.00) FROM ledger_entry
            WHERE family_id = ? AND child_id = ? AND asset_type = 'MONEY'
            """, BigDecimal.class, familyId, childId);
        BigDecimal ledgerCoin = jdbc.queryForObject("""
            SELECT COALESCE(SUM(delta), 0.00) FROM ledger_entry
            WHERE family_id = ? AND child_id = ? AND asset_type = 'COIN'
            """, BigDecimal.class, familyId, childId);
        return new WalletReconciliation(
            familyId, childId, wallet.moneyBalance(), ledgerMoney,
            wallet.coinBalance(), ledgerCoin.longValueExact(), true);
    }

    private void insertLedger(
        UUID familyId, UUID childId, AssetType asset, BigDecimal delta,
        BigDecimal before, BigDecimal after, UUID completionId, UUID groupId,
        String idempotencyKey, UUID actorId, String reason, Instant now
    ) {
        jdbc.update("""
            INSERT INTO ledger_entry
                (id, family_id, child_id, asset_type, delta, before_balance, after_balance,
                 entry_type, business_type, business_id, group_id, idempotency_key,
                 actor_id, reason, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, 'TASK_REWARD', 'TASK_COMPLETION', ?, ?, ?, ?, ?, ?)
            """, UUID.randomUUID(), familyId, childId, asset.name(), delta, before, after,
            completionId, groupId, idempotencyKey, actorId, reason, ts(now));
    }

    private PinCredential pinCredential(ResultSet rs, int row) throws SQLException {
        return new PinCredential(
            rs.getObject("family_id", UUID.class), rs.getObject("parent_id", UUID.class),
            rs.getString("pin_hash"), rs.getInt("failed_attempts"), instant(rs, "locked_until"));
    }

    private StoredSession session(ResultSet rs, int row) throws SQLException {
        var role = ActorRole.valueOf(rs.getString("actor_role"));
        return new StoredSession(rs.getString("token_hash"), new Actor(
            rs.getObject("family_id", UUID.class), rs.getObject("actor_id", UUID.class), role,
            rs.getObject("child_id", UUID.class)), instant(rs, "expires_at"), instant(rs, "revoked_at"));
    }

    private TaskCompletion completion(ResultSet rs, int row) throws SQLException {
        return new TaskCompletion(
            rs.getObject("id", UUID.class), rs.getObject("family_id", UUID.class),
            rs.getObject("child_id", UUID.class), rs.getObject("task_id", UUID.class),
            CompletionStatus.valueOf(rs.getString("status")), rs.getString("evidence_note"),
            rs.getObject("submitted_by", UUID.class), rs.getObject("reviewed_by", UUID.class),
            rs.getString("review_note"), rs.getLong("xp_reward"), rs.getLong("coin_reward"),
            rs.getBigDecimal("money_reward"), rs.getObject("ledger_group_id", UUID.class),
            instant(rs, "submitted_at"), instant(rs, "reviewed_at"));
    }

    private Wallet wallet(ResultSet rs, int row) throws SQLException {
        BigDecimal money = rs.getBigDecimal("money_balance");
        BigDecimal reserved = rs.getBigDecimal("reserved_money");
        return new Wallet(rs.getObject("child_id", UUID.class), rs.getObject("family_id", UUID.class),
            money, reserved, money.subtract(reserved), rs.getLong("coin_balance"), rs.getLong("version"));
    }

    private LedgerEntry ledger(ResultSet rs, int row) throws SQLException {
        return new LedgerEntry(
            rs.getObject("id", UUID.class), rs.getObject("family_id", UUID.class),
            rs.getObject("child_id", UUID.class), AssetType.valueOf(rs.getString("asset_type")),
            rs.getBigDecimal("delta"), rs.getBigDecimal("before_balance"),
            rs.getBigDecimal("after_balance"), rs.getString("entry_type"),
            rs.getString("business_type"), rs.getObject("business_id", UUID.class),
            rs.getObject("group_id", UUID.class), rs.getObject("actor_id", UUID.class),
            rs.getString("reason"), instant(rs, "created_at"));
    }

    private static Timestamp ts(Instant value) {
        return Timestamp.from(value);
    }

    private static Timestamp nullableTs(Instant value) {
        return value == null ? null : ts(value);
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }
}
