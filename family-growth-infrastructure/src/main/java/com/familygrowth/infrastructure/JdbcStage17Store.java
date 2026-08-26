package com.familygrowth.infrastructure;

import com.familygrowth.application.FamilyGrowthService;
import com.familygrowth.application.Stage17Store;
import com.familygrowth.application.Stage3Service;
import com.familygrowth.domain.Stage17Models;
import com.familygrowth.domain.Stage17Models.WithdrawalAction;
import com.familygrowth.domain.Stage17Models.WithdrawalActionReplay;
import com.familygrowth.domain.Stage17Models.WithdrawalAmounts;
import com.familygrowth.domain.Stage17Models.WithdrawalQuote;
import com.familygrowth.domain.Stage17Models.WithdrawalRequest;
import com.familygrowth.domain.Stage17Models.WithdrawalRule;
import com.familygrowth.domain.Stage17Models.WithdrawalStatus;
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
class JdbcStage17Store implements Stage17Store {
    private static final String SYSTEM_DEFAULT_KEY = "SYSTEM_DEFAULT_WITHDRAWAL_RULE";

    private final JdbcTemplate jdbc;

    JdbcStage17Store(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<WithdrawalRule> ruleByKey(UUID familyId, String key) {
        return jdbc.query("SELECT * FROM withdrawal_rule WHERE family_id=? AND idempotency_key=?",
            this::ruleRow, familyId, key).stream().findFirst();
    }

    @Override
    public WithdrawalRule createRule(UUID familyId, BigDecimal payoutRate, BigDecimal feeRate,
                                     BigDecimal fixedFee, String key, UUID actorId, Instant now) {
        lockFamily(familyId);
        Optional<WithdrawalRule> replay = ruleByKey(familyId, key);
        if (replay.isPresent()) {
            if (replay.get().payoutRate().compareTo(payoutRate) != 0
                || replay.get().feeRate().compareTo(feeRate) != 0
                || replay.get().fixedFee().compareTo(fixedFee) != 0) {
                throw new Stage3Service.ConflictException(
                    "Idempotency-Key was used for another rule");
            }
            return replay.get();
        }
        Long version = jdbc.queryForObject(
            "SELECT COALESCE(MAX(rule_version),0)+1 FROM withdrawal_rule WHERE family_id=?",
            Long.class, familyId);
        jdbc.update("UPDATE withdrawal_rule SET active=FALSE WHERE family_id=? AND active=TRUE", familyId);
        UUID id = UUID.randomUUID();
        jdbc.update("""
            INSERT INTO withdrawal_rule
                (id,family_id,rule_version,payout_rate,fee_rate,fixed_fee,active,
                 idempotency_key,actor_id,created_at)
            VALUES (?,?,?,?,?,?,TRUE,?,?,?)
            """, id, familyId, version, payoutRate, feeRate, fixedFee, key, actorId, ts(now));
        return rule(familyId, id);
    }

    @Override
    public WithdrawalRule activeRuleOrDefault(UUID familyId, UUID actorId, Instant now) {
        Optional<WithdrawalRule> active = activeRule(familyId);
        if (active.isPresent()) {
            return active.get();
        }
        lockFamily(familyId);
        active = activeRule(familyId);
        if (active.isPresent()) {
            return active.get();
        }
        return createRule(familyId, new BigDecimal("1.000000"), new BigDecimal("0.000000"),
            new BigDecimal("0.00"), SYSTEM_DEFAULT_KEY, actorId, now);
    }

    @Override
    public Optional<WithdrawalQuote> quoteByKey(UUID familyId, String key) {
        return jdbc.query("SELECT * FROM withdrawal_quote WHERE family_id=? AND idempotency_key=?",
            this::quoteRow, familyId, key).stream().findFirst();
    }

    @Override
    public Optional<WithdrawalQuote> quote(UUID familyId, UUID quoteId) {
        return jdbc.query("SELECT * FROM withdrawal_quote WHERE family_id=? AND id=?",
            this::quoteRow, familyId, quoteId).stream().findFirst();
    }

    @Override
    public WithdrawalQuote createQuote(UUID familyId, UUID childId, WithdrawalAmounts amounts,
                                       WithdrawalRule rule, String key, Instant expiresAt, Instant now) {
        lockFamily(familyId);
        Optional<WithdrawalQuote> replay = quoteByKey(familyId, key);
        if (replay.isPresent()) {
            if (!replay.get().childId().equals(childId)
                || replay.get().moneyAmount().compareTo(amounts.moneyAmount()) != 0) {
                throw new Stage3Service.ConflictException(
                    "Idempotency-Key was used for another quote");
            }
            return replay.get();
        }
        UUID id = UUID.randomUUID();
        jdbc.update("""
            INSERT INTO withdrawal_quote
                (id,family_id,child_id,money_amount,payout_rate,gross_payout,fee_rate,
                 fixed_fee,fee_amount,net_payout,rule_id,rule_version,idempotency_key,
                 notice,expires_at,created_at)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """, id, familyId, childId, amounts.moneyAmount(), rule.payoutRate(),
            amounts.grossPayout(), rule.feeRate(), rule.fixedFee(), amounts.feeAmount(),
            amounts.netPayout(), rule.id(), rule.version(), key, Stage17Models.EDUCATION_NOTICE,
            ts(expiresAt), ts(now));
        return quote(familyId, id).orElseThrow();
    }

    @Override
    public Optional<WithdrawalRequest> requestByKey(UUID familyId, String key) {
        return jdbc.query("SELECT * FROM withdrawal_request WHERE family_id=? AND request_key=?",
            this::requestRow, familyId, key).stream().findFirst();
    }

    @Override
    public Optional<WithdrawalRequest> request(UUID familyId, UUID requestId) {
        return jdbc.query("SELECT * FROM withdrawal_request WHERE family_id=? AND id=?",
            this::requestRow, familyId, requestId).stream().findFirst();
    }

    @Override
    public List<WithdrawalRequest> requests(UUID familyId, UUID childId) {
        return jdbc.query("""
            SELECT * FROM withdrawal_request WHERE family_id=? AND child_id=?
            ORDER BY requested_at DESC,id DESC
            """, this::requestRow, familyId, childId);
    }

    @Override
    public WithdrawalRequest createRequest(UUID familyId, WithdrawalQuote quote, String key,
                                           UUID actorId, Instant now) {
        lockFamily(familyId);
        Optional<WithdrawalRequest> replay = requestByKey(familyId, key);
        if (replay.isPresent()) {
            if (!replay.get().childId().equals(quote.childId())
                || !replay.get().quoteId().equals(quote.id())) {
                throw new Stage3Service.ConflictException(
                    "Idempotency-Key was used for another request");
            }
            return replay.get();
        }
        UUID id = UUID.randomUUID();
        jdbc.update("""
            INSERT INTO withdrawal_request
                (id,quote_id,family_id,child_id,money_amount,payout_rate,gross_payout,
                 fee_rate,fixed_fee,fee_amount,net_payout,rule_id,rule_version,status,
                 request_key,requested_by,requested_at)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,'REQUESTED',?,?,?)
            """, id, quote.id(), familyId, quote.childId(), quote.moneyAmount(), quote.payoutRate(),
            quote.grossPayout(), quote.feeRate(), quote.fixedFee(), quote.feeAmount(),
            quote.netPayout(), quote.ruleId(), quote.ruleVersion(), key, actorId, ts(now));
        return request(familyId, id).orElseThrow();
    }

    @Override
    public Optional<WithdrawalActionReplay> actionByKey(UUID familyId, WithdrawalAction action,
                                                        String key) {
        return jdbc.query("""
            SELECT request_id,action,idempotency_key FROM withdrawal_action
            WHERE family_id=? AND action=? AND idempotency_key=?
            """, (rs, row) -> new WithdrawalActionReplay(uuid(rs, "request_id"),
                WithdrawalAction.valueOf(rs.getString("action")), rs.getString("idempotency_key")),
            familyId, action.name(), key).stream().findFirst();
    }

    @Override
    public WithdrawalRequest transition(UUID familyId, UUID requestId, WithdrawalAction action,
                                        String key, UUID actorId, Instant now) {
        WithdrawalRequest current = lockRequest(familyId, requestId);
        Optional<WithdrawalActionReplay> replay = actionByKey(familyId, action, key);
        if (replay.isPresent()) {
            if (!replay.get().requestId().equals(requestId)) {
                throw new Stage3Service.ConflictException("Idempotency-Key was used for another action");
            }
            return current;
        }

        WithdrawalStatus next;
        UUID ledgerGroup = current.ledgerGroupId();
        switch (action) {
            case APPROVE -> {
                requireStatus(current, WithdrawalStatus.REQUESTED);
                WalletRow wallet = lockWallet(familyId, current.childId());
                if (wallet.available().compareTo(current.moneyAmount()) < 0) {
                    throw new Stage3Service.ConflictException("Available Money is insufficient");
                }
                BigDecimal reservedAfter = wallet.reserved().add(current.moneyAmount());
                jdbc.update("""
                    UPDATE wallet SET reserved_money=?,version=version+1,updated_at=?
                    WHERE family_id=? AND child_id=?
                    """, reservedAfter, ts(now), familyId, current.childId());
                jdbc.update("""
                    UPDATE withdrawal_request SET status='APPROVED',decided_by=?,decided_at=?
                    WHERE family_id=? AND id=? AND status='REQUESTED'
                    """, actorId, ts(now), familyId, requestId);
                next = WithdrawalStatus.APPROVED;
            }
            case REJECT -> {
                requireStatus(current, WithdrawalStatus.REQUESTED);
                jdbc.update("""
                    UPDATE withdrawal_request SET status='REJECTED',decided_by=?,decided_at=?
                    WHERE family_id=? AND id=? AND status='REQUESTED'
                    """, actorId, ts(now), familyId, requestId);
                next = WithdrawalStatus.REJECTED;
            }
            case CANCEL -> {
                if (current.status() != WithdrawalStatus.REQUESTED
                    && current.status() != WithdrawalStatus.APPROVED) {
                    throw new Stage3Service.ConflictException("Withdrawal cannot be cancelled");
                }
                if (current.status() == WithdrawalStatus.APPROVED) {
                    WalletRow wallet = lockWallet(familyId, current.childId());
                    BigDecimal reservedAfter = wallet.reserved().subtract(current.moneyAmount());
                    if (reservedAfter.signum() < 0) {
                        throw new Stage3Service.ConflictException("Reserved Money is inconsistent");
                    }
                    jdbc.update("""
                        UPDATE wallet SET reserved_money=?,version=version+1,updated_at=?
                        WHERE family_id=? AND child_id=?
                        """, reservedAfter, ts(now), familyId, current.childId());
                }
                jdbc.update("""
                    UPDATE withdrawal_request SET status='CANCELLED',cancelled_by=?,cancelled_at=?
                    WHERE family_id=? AND id=? AND status IN ('REQUESTED','APPROVED')
                    """, actorId, ts(now), familyId, requestId);
                next = WithdrawalStatus.CANCELLED;
            }
            case MARK_PAID -> {
                requireStatus(current, WithdrawalStatus.APPROVED);
                WalletRow wallet = lockWallet(familyId, current.childId());
                BigDecimal balanceAfter = wallet.money().subtract(current.moneyAmount());
                BigDecimal reservedAfter = wallet.reserved().subtract(current.moneyAmount());
                if (balanceAfter.signum() < 0 || reservedAfter.signum() < 0
                    || reservedAfter.compareTo(balanceAfter) > 0) {
                    throw new Stage3Service.ConflictException("Reserved Money is inconsistent");
                }
                ledgerGroup = UUID.randomUUID();
                jdbc.update("""
                    INSERT INTO ledger_entry
                        (id,family_id,child_id,asset_type,delta,before_balance,after_balance,
                         entry_type,business_type,business_id,group_id,idempotency_key,
                         actor_id,reason,created_at)
                    VALUES (?,?,?,'MONEY',?,?,?,'WITHDRAWAL_PAID','WITHDRAWAL',?,?,?,?,?,?)
                    """, UUID.randomUUID(), familyId, current.childId(), current.moneyAmount().negate(),
                    wallet.money(), balanceAfter, requestId, ledgerGroup, key, actorId,
                    "Offline payout confirmed; gross=" + current.grossPayout()
                        + "; fee=" + current.feeAmount() + "; net=" + current.netPayout(), ts(now));
                jdbc.update("""
                    UPDATE wallet SET money_balance=?,reserved_money=?,version=version+1,updated_at=?
                    WHERE family_id=? AND child_id=?
                    """, balanceAfter, reservedAfter, ts(now), familyId, current.childId());
                jdbc.update("""
                    UPDATE withdrawal_request SET status='PAID',ledger_group_id=?,paid_by=?,paid_at=?
                    WHERE family_id=? AND id=? AND status='APPROVED'
                    """, ledgerGroup, actorId, ts(now), familyId, requestId);
                next = WithdrawalStatus.PAID;
            }
            default -> throw new IllegalStateException("Unsupported withdrawal action");
        }

        jdbc.update("""
            INSERT INTO withdrawal_action
                (id,family_id,request_id,action,status_after,idempotency_key,actor_id,created_at)
            VALUES (?,?,?,?,?,?,?,?)
            """, UUID.randomUUID(), familyId, requestId, action.name(), next.name(), key,
            actorId, ts(now));
        return request(familyId, requestId).orElseThrow();
    }

    private Optional<WithdrawalRule> activeRule(UUID familyId) {
        return jdbc.query("SELECT * FROM withdrawal_rule WHERE family_id=? AND active=TRUE",
            this::ruleRow, familyId).stream().findFirst();
    }

    private WithdrawalRule rule(UUID familyId, UUID id) {
        return jdbc.query("SELECT * FROM withdrawal_rule WHERE family_id=? AND id=?",
            this::ruleRow, familyId, id).stream().findFirst().orElseThrow();
    }

    private WithdrawalRequest lockRequest(UUID familyId, UUID requestId) {
        return jdbc.query("SELECT * FROM withdrawal_request WHERE family_id=? AND id=? FOR UPDATE",
            this::requestRow, familyId, requestId).stream().findFirst()
            .orElseThrow(FamilyGrowthService.NotFoundException::new);
    }

    private WalletRow lockWallet(UUID familyId, UUID childId) {
        return jdbc.query("""
            SELECT money_balance,reserved_money FROM wallet
            WHERE family_id=? AND child_id=? FOR UPDATE
            """, (rs, row) -> new WalletRow(rs.getBigDecimal("money_balance"),
                rs.getBigDecimal("reserved_money")), familyId, childId).stream().findFirst()
            .orElseThrow(FamilyGrowthService.NotFoundException::new);
    }

    private void lockFamily(UUID familyId) {
        jdbc.queryForObject("SELECT id FROM family WHERE id=? FOR UPDATE", UUID.class, familyId);
    }

    private static void requireStatus(WithdrawalRequest request, WithdrawalStatus expected) {
        if (request.status() != expected) {
            throw new Stage3Service.ConflictException("Withdrawal state transition is invalid");
        }
    }

    private WithdrawalRule ruleRow(ResultSet rs, int row) throws SQLException {
        return new WithdrawalRule(uuid(rs, "id"), uuid(rs, "family_id"),
            rs.getLong("rule_version"), rs.getBigDecimal("payout_rate"),
            rs.getBigDecimal("fee_rate"), rs.getBigDecimal("fixed_fee"),
            rs.getBoolean("active"), rs.getString("idempotency_key"),
            uuid(rs, "actor_id"), instant(rs, "created_at"));
    }

    private WithdrawalQuote quoteRow(ResultSet rs, int row) throws SQLException {
        return new WithdrawalQuote(uuid(rs, "id"), uuid(rs, "family_id"),
            uuid(rs, "child_id"), rs.getBigDecimal("money_amount"),
            rs.getBigDecimal("payout_rate"), rs.getBigDecimal("gross_payout"),
            rs.getBigDecimal("fee_rate"), rs.getBigDecimal("fixed_fee"),
            rs.getBigDecimal("fee_amount"), rs.getBigDecimal("net_payout"),
            uuid(rs, "rule_id"), rs.getLong("rule_version"),
            rs.getString("idempotency_key"), rs.getString("notice"),
            instant(rs, "expires_at"), instant(rs, "created_at"));
    }

    private WithdrawalRequest requestRow(ResultSet rs, int row) throws SQLException {
        return new WithdrawalRequest(uuid(rs, "id"), uuid(rs, "quote_id"),
            uuid(rs, "family_id"), uuid(rs, "child_id"), rs.getBigDecimal("money_amount"),
            rs.getBigDecimal("payout_rate"), rs.getBigDecimal("gross_payout"),
            rs.getBigDecimal("fee_rate"), rs.getBigDecimal("fixed_fee"),
            rs.getBigDecimal("fee_amount"), rs.getBigDecimal("net_payout"),
            uuid(rs, "rule_id"), rs.getLong("rule_version"),
            WithdrawalStatus.valueOf(rs.getString("status")), uuid(rs, "ledger_group_id"),
            rs.getString("request_key"), uuid(rs, "requested_by"), uuid(rs, "decided_by"),
            uuid(rs, "paid_by"), uuid(rs, "cancelled_by"), instant(rs, "requested_at"),
            instant(rs, "decided_at"), instant(rs, "paid_at"), instant(rs, "cancelled_at"));
    }

    private static UUID uuid(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, UUID.class);
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private static Timestamp ts(Instant value) {
        return Timestamp.from(value);
    }

    private record WalletRow(BigDecimal money, BigDecimal reserved) {
        BigDecimal available() {
            return money.subtract(reserved);
        }
    }
}
