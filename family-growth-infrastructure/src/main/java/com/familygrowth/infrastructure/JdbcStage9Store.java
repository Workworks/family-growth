package com.familygrowth.infrastructure;

import com.familygrowth.application.FamilyGrowthService;
import com.familygrowth.application.Stage3Service;
import com.familygrowth.application.Stage9Store;
import com.familygrowth.domain.Stage3Models.Wallet;
import com.familygrowth.domain.Stage9Models.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcStage9Store implements Stage9Store {
    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private final JdbcTemplate jdbc;

    JdbcStage9Store(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public UsagePolicy upsertPolicy(UUID family, UUID child, String zone, int daily, int session, LocalTime quietStart, LocalTime quietEnd, UUID actor, Instant now) {
        requireChild(family, child);
        int updated = jdbc.update("""
            UPDATE usage_policy SET zone_id=?,daily_limit_minutes=?,session_limit_minutes=?,quiet_start=?,quiet_end=?,version=version+1,
            actor_id=?,updated_at=? WHERE family_id=? AND child_id=?
            """, zone, daily, session, java.sql.Time.valueOf(quietStart),java.sql.Time.valueOf(quietEnd),actor, ts(now), family, child);
        if (updated == 0) {
            try {
                jdbc.update("""
                    INSERT INTO usage_policy(child_id,family_id,zone_id,daily_limit_minutes,session_limit_minutes,quiet_start,quiet_end,version,actor_id,created_at,updated_at)
                    VALUES(?,?,?,?,?,?,?,0,?,?,?)
                    """, child, family, zone, daily, session,java.sql.Time.valueOf(quietStart),java.sql.Time.valueOf(quietEnd),actor, ts(now), ts(now));
            } catch (DuplicateKeyException conflict) {
                throw new Stage3Service.ConflictException("Usage policy was changed concurrently");
            }
        }
        jdbc.update("INSERT INTO usage_policy_action(id,family_id,child_id,actor_id,daily_limit_minutes,session_limit_minutes,zone_id,quiet_start,quiet_end,created_at) VALUES(?,?,?,?,?,?,?,?,?,?)",
            UUID.randomUUID(),family,child,actor,daily,session,zone,java.sql.Time.valueOf(quietStart),java.sql.Time.valueOf(quietEnd),ts(now));
        return policy(family, child);
    }

    @Override
    public UsagePolicy policy(UUID family, UUID child) {
        requireChild(family, child);
        return jdbc.query("SELECT * FROM usage_policy WHERE family_id=? AND child_id=?", this::policyRow, family, child)
            .stream().findFirst().orElse(new UsagePolicy(family, child, "Asia/Shanghai", 20, 10,LocalTime.of(21,30),LocalTime.of(6,30),0, Instant.EPOCH));
    }

    @Override
    public Optional<UsageEvent> eventByKey(UUID family, String key) {
        return jdbc.query("SELECT * FROM usage_event WHERE family_id=? AND idempotency_key=?", this::eventRow, family, key)
            .stream().findFirst();
    }

    @Override
    public UsageEvent addEvent(UUID family, UUID child, UsageEventType type, int minutes, Instant occurred, String key, UUID actor, Instant now) {
        requireChild(family, child);
        UUID id = UUID.randomUUID();
        try {
            jdbc.update("""
                INSERT INTO usage_event(id,family_id,child_id,event_type,minutes,occurred_at,idempotency_key,actor_id,created_at)
                VALUES(?,?,?,?,?,?,?,?,?)
                """, id, family, child, type.name(), minutes, ts(occurred), key, actor, ts(now));
        } catch (DuplicateKeyException conflict) {
            return eventByKey(family, key).orElseThrow(() -> conflict);
        }
        return eventByKey(family, key).orElseThrow();
    }

    @Override
    public TodayReport today(UUID family, UUID child, UsagePolicy policy, LocalDate date, Instant start, Instant end) {
        int app = usageMinutes(family, child, UsageEventType.APP_ACTIVE, start, end);
        int learning = usageMinutes(family, child, UsageEventType.LEARNING, start, end);
        int submitted = count("SELECT COUNT(*) FROM task_completion WHERE family_id=? AND child_id=? AND submitted_at>=? AND submitted_at<?", family, child, ts(start), ts(end));
        int approved = count("SELECT COUNT(*) FROM task_completion WHERE family_id=? AND child_id=? AND status='APPROVED' AND reviewed_at>=? AND reviewed_at<?", family, child, ts(start), ts(end));
        int pending = count("SELECT COUNT(*) FROM task_completion WHERE family_id=? AND child_id=? AND status='SUBMITTED'", family, child);
        return new TodayReport(family, child, date, policy.zoneId(), app, learning, submitted, approved, pending, wallet(family, child));
    }

    @Override
    public MonthlyReport month(UUID family, UUID child, UsagePolicy policy, String month, Instant start, Instant end) {
        int app = usageMinutes(family, child, UsageEventType.APP_ACTIVE, start, end);
        int learning = usageMinutes(family, child, UsageEventType.LEARNING, start, end);
        int submitted = count("SELECT COUNT(*) FROM task_completion WHERE family_id=? AND child_id=? AND submitted_at>=? AND submitted_at<?", family, child, ts(start), ts(end));
        int approved = count("SELECT COUNT(*) FROM task_completion WHERE family_id=? AND child_id=? AND status='APPROVED' AND reviewed_at>=? AND reviewed_at<?", family, child, ts(start), ts(end));
        long xp = number("SELECT COALESCE(SUM(xp_reward),0) FROM task_completion WHERE family_id=? AND child_id=? AND status='APPROVED' AND reviewed_at>=? AND reviewed_at<?", family, child, ts(start), ts(end)).longValue();
        BigDecimal income = decimal("SELECT COALESCE(SUM(delta),0) FROM ledger_entry WHERE family_id=? AND child_id=? AND asset_type='MONEY' AND delta>0 AND created_at>=? AND created_at<?", family, child, ts(start), ts(end));
        BigDecimal expense = decimal("SELECT COALESCE(-SUM(delta),0) FROM ledger_entry WHERE family_id=? AND child_id=? AND asset_type='MONEY' AND delta<0 AND created_at>=? AND created_at<?", family, child, ts(start), ts(end));
        long coinIncome = number("SELECT COALESCE(SUM(delta),0) FROM ledger_entry WHERE family_id=? AND child_id=? AND asset_type='COIN' AND delta>0 AND created_at>=? AND created_at<?", family, child, ts(start), ts(end)).longValue();
        long coinExpense = number("SELECT COALESCE(-SUM(delta),0) FROM ledger_entry WHERE family_id=? AND child_id=? AND asset_type='COIN' AND delta<0 AND created_at>=? AND created_at<?", family, child, ts(start), ts(end)).longValue();
        BigDecimal gifts = decimal("SELECT COALESCE(SUM(amount),0) FROM gift_money WHERE family_id=? AND child_id=? AND created_at>=? AND created_at<?", family, child, ts(start), ts(end));
        BigDecimal exchangeFees = decimal("SELECT COALESCE(SUM(source_fee),0) FROM exchange_order WHERE family_id=? AND child_id=? AND created_at>=? AND created_at<?", family, child, ts(start), ts(end));
        BigDecimal saving = decimal("SELECT COALESCE(MAX(balance),0) FROM saving_account WHERE family_id=? AND child_id=?", family, child);
        BigDecimal market = decimal("""
            SELECT COALESCE(SUM(p.shares*(SELECT n.nav FROM fund_nav n WHERE n.fund_id=p.fund_id ORDER BY n.nav_date DESC,n.created_at DESC LIMIT 1)),0)
            FROM fund_position p WHERE p.family_id=? AND p.child_id=?
            """, family, child);
        BigDecimal fundFees = decimal("SELECT COALESCE(SUM(fee_amount),0) FROM fund_trade_order WHERE family_id=? AND child_id=? AND created_at>=? AND created_at<?", family, child, ts(start), ts(end));
        BigDecimal realized = decimal("SELECT COALESCE(SUM(realized_pnl),0) FROM fund_trade_order WHERE family_id=? AND child_id=? AND created_at>=? AND created_at<?", family, child, ts(start), ts(end));
        BigDecimal cost = decimal("SELECT COALESCE(SUM(total_cost),0) FROM fund_position WHERE family_id=? AND child_id=?", family, child);
        Wallet wallet = wallet(family, child);
        BigDecimal ledgerMoney = decimal("SELECT COALESCE(SUM(delta),0) FROM ledger_entry WHERE family_id=? AND child_id=? AND asset_type='MONEY'", family, child);
        long ledgerCoin = number("SELECT COALESCE(SUM(delta),0) FROM ledger_entry WHERE family_id=? AND child_id=? AND asset_type='COIN'", family, child).longValue();
        return new MonthlyReport(family, child, month, policy.zoneId(), app, learning, submitted, approved, xp,
            income, expense, coinIncome, coinExpense, gifts, exchangeFees, saving, market, fundFees, realized,
            market.subtract(cost), wallet.moneyBalance().compareTo(ledgerMoney) == 0 && wallet.coinBalance() == ledgerCoin);
    }

    @Override public Optional<TemporaryAllowance> allowanceByKey(UUID family,String key){return jdbc.query("SELECT * FROM usage_temporary_allowance WHERE family_id=? AND idempotency_key=?",this::allowanceRow,family,key).stream().findFirst();}
    @Override public Optional<TemporaryAllowance> activeAllowance(UUID family,UUID child,Instant now){return jdbc.query("SELECT * FROM usage_temporary_allowance WHERE family_id=? AND child_id=? AND starts_at<=? AND expires_at>? ORDER BY expires_at DESC LIMIT 1",this::allowanceRow,family,child,ts(now),ts(now)).stream().findFirst();}
    @Override public TemporaryAllowance addAllowance(UUID family,UUID child,String reason,Instant starts,Instant expires,String key,String payloadHash,UUID actor,Instant now){requireChild(family,child);UUID id=UUID.randomUUID();try{jdbc.update("INSERT INTO usage_temporary_allowance(id,family_id,child_id,actor_id,reason,starts_at,expires_at,idempotency_key,payload_hash,created_at) VALUES(?,?,?,?,?,?,?,?,?,?)",id,family,child,actor,reason,ts(starts),ts(expires),key,payloadHash,ts(now));}catch(DuplicateKeyException conflict){return allowanceByKey(family,key).orElseThrow(()->conflict);}return allowanceByKey(family,key).orElseThrow();}
    @Override public int appMinutes(UUID family,UUID child,Instant start,Instant end){return usageMinutes(family,child,UsageEventType.APP_ACTIVE,start,end);}

    private int usageMinutes(UUID family, UUID child, UsageEventType type, Instant start, Instant end) {
        return number("SELECT COALESCE(SUM(minutes),0) FROM usage_event WHERE family_id=? AND child_id=? AND event_type=? AND occurred_at>=? AND occurred_at<?", family, child, type.name(), ts(start), ts(end)).intValue();
    }

    private Wallet wallet(UUID family, UUID child) {
        return jdbc.query("SELECT * FROM wallet WHERE family_id=? AND child_id=?", (r, n) -> {
            BigDecimal money = r.getBigDecimal("money_balance");
            BigDecimal reserved = r.getBigDecimal("reserved_money");
            return new Wallet(r.getObject("child_id", UUID.class), r.getObject("family_id", UUID.class),
                money, reserved, money.subtract(reserved), r.getLong("coin_balance"), r.getLong("version"));
        }, family, child)
            .stream().findFirst().orElseThrow(FamilyGrowthService.NotFoundException::new);
    }

    private void requireChild(UUID family, UUID child) {
        if (count("SELECT COUNT(*) FROM child_profile WHERE family_id=? AND id=?", family, child) != 1) throw new FamilyGrowthService.NotFoundException();
    }

    private int count(String sql, Object... args) { return number(sql, args).intValue(); }
    private Number number(String sql, Object... args) { Number value = jdbc.queryForObject(sql, Number.class, args); return value == null ? 0 : value; }
    private BigDecimal decimal(String sql, Object... args) { BigDecimal value = jdbc.queryForObject(sql, BigDecimal.class, args); return value == null ? ZERO : value.setScale(2, RoundingMode.HALF_UP); }
    private UsagePolicy policyRow(ResultSet r, int n) throws SQLException { return new UsagePolicy(uuid(r, "family_id"), uuid(r, "child_id"), r.getString("zone_id"), r.getInt("daily_limit_minutes"), r.getInt("session_limit_minutes"),r.getTime("quiet_start").toLocalTime(),r.getTime("quiet_end").toLocalTime(), r.getLong("version"), instant(r, "updated_at")); }
    private UsageEvent eventRow(ResultSet r, int n) throws SQLException { return new UsageEvent(uuid(r, "id"), uuid(r, "family_id"), uuid(r, "child_id"), UsageEventType.valueOf(r.getString("event_type")), r.getInt("minutes"), instant(r, "occurred_at"), r.getString("idempotency_key"), uuid(r, "actor_id"), instant(r, "created_at")); }
    private TemporaryAllowance allowanceRow(ResultSet r,int n)throws SQLException{return new TemporaryAllowance(uuid(r,"id"),uuid(r,"family_id"),uuid(r,"child_id"),r.getString("reason"),instant(r,"starts_at"),instant(r,"expires_at"),uuid(r,"actor_id"),instant(r,"created_at"));}
    private static UUID uuid(ResultSet r, String column) throws SQLException { return r.getObject(column, UUID.class); }
    private static Instant instant(ResultSet r, String column) throws SQLException { Timestamp value = r.getTimestamp(column); return value == null ? null : value.toInstant(); }
    private static Timestamp ts(Instant value) { return Timestamp.from(value); }
}
