package com.familygrowth.infrastructure;

import com.familygrowth.application.FamilyGrowthService;
import com.familygrowth.application.Stage3Service;
import com.familygrowth.application.Stage5Store;
import com.familygrowth.domain.Stage5Models.ExchangeDirection;
import com.familygrowth.domain.Stage5Models.ExchangeOrder;
import com.familygrowth.domain.Stage5Models.ExchangePreview;
import com.familygrowth.domain.Stage5Models.ExchangeQuote;
import com.familygrowth.domain.Stage5Models.ExchangeRule;
import com.familygrowth.domain.Stage5Models.GiftMoney;
import com.familygrowth.domain.Stage5Models.PreviewStatus;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcStage5Store implements Stage5Store {
    private final JdbcTemplate jdbc;

    JdbcStage5Store(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public Optional<GiftMoney> findGift(UUID familyId, String key) {
        return jdbc.query("SELECT * FROM gift_money WHERE family_id = ? AND idempotency_key = ?",
            this::gift, familyId, key).stream().findFirst();
    }

    @Override
    public GiftMoney depositGift(UUID familyId, UUID childId, BigDecimal amount, String note,
                                 String key, UUID actorId, Instant now) {
        WalletRow wallet = lockWallet(familyId, childId);
        UUID id = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        BigDecimal after = wallet.money().add(amount);
        jdbc.update("""
            INSERT INTO gift_money
                (id,family_id,child_id,amount,note,ledger_group_id,idempotency_key,actor_id,created_at)
            VALUES (?,?,?,?,?,?,?,?,?)
            """, id, familyId, childId, amount, note, groupId, key, actorId, ts(now));
        insertLedger(familyId, childId, "MONEY", amount, wallet.money(), after,
            "GIFT_MONEY", "GIFT_MONEY", id, groupId, key, actorId, note, now);
        jdbc.update("UPDATE wallet SET money_balance=?,version=version+1,updated_at=? WHERE family_id=? AND child_id=?",
            after, ts(now), familyId, childId);
        return findGift(familyId, key).orElseThrow();
    }

    @Override
    public ExchangeRule createRule(UUID familyId, BigDecimal buyRate, BigDecimal sellRate,
                                   BigDecimal buyFee, BigDecimal sellFee, BigDecimal maxSource,
                                   UUID actorId, Instant now) {
        jdbc.queryForObject("SELECT id FROM family WHERE id = ? FOR UPDATE", UUID.class, familyId);
        Long version = jdbc.queryForObject(
            "SELECT COALESCE(MAX(rule_version),0)+1 FROM exchange_rule WHERE family_id=?", Long.class, familyId);
        jdbc.update("UPDATE exchange_rule SET active=FALSE WHERE family_id=? AND active=TRUE", familyId);
        UUID id = UUID.randomUUID();
        jdbc.update("""
            INSERT INTO exchange_rule
                (id,family_id,rule_version,money_to_coin_rate,coin_to_money_rate,
                 money_to_coin_fee_rate,coin_to_money_fee_rate,max_source_amount,active,actor_id,created_at)
            VALUES (?,?,?,?,?,?,?,?,TRUE,?,?)
            """, id, familyId, version, buyRate, sellRate, buyFee, sellFee, maxSource, actorId, ts(now));
        return activeRule(familyId).orElseThrow();
    }

    @Override
    public Optional<ExchangeRule> activeRule(UUID familyId) {
        return jdbc.query("SELECT * FROM exchange_rule WHERE family_id=? AND active=TRUE",
            this::rule, familyId).stream().findFirst();
    }

    @Override
    public ExchangePreview savePreview(UUID familyId, UUID childId, ExchangeDirection direction,
                                       ExchangeQuote quote, ExchangeRule rule, Instant expiresAt, Instant now) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
            INSERT INTO exchange_preview
                (id,family_id,child_id,direction,source_amount,source_fee,net_source,target_amount,
                 applied_rate,applied_fee_rate,education_notice,rule_id,rule_version,status,expires_at,created_at)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,'OPEN',?,?)
            """, id, familyId, childId, direction.name(), quote.sourceAmount(), quote.sourceFee(),
            quote.netSource(), quote.targetAmount(),
            direction == ExchangeDirection.MONEY_TO_COIN ? rule.moneyToCoinRate() : rule.coinToMoneyRate(),
            direction == ExchangeDirection.MONEY_TO_COIN ? rule.moneyToCoinFeeRate() : rule.coinToMoneyFeeRate(),
            com.familygrowth.domain.Stage5Models.EDUCATION_NOTICE, rule.id(), rule.version(), ts(expiresAt), ts(now));
        return preview(familyId, id).orElseThrow();
    }

    @Override
    public Optional<ExchangePreview> preview(UUID familyId, UUID previewId) {
        return jdbc.query("SELECT * FROM exchange_preview WHERE family_id=? AND id=?",
            this::previewRow, familyId, previewId).stream().findFirst();
    }

    @Override
    public Optional<ExchangeOrder> findOrder(UUID familyId, String key) {
        return jdbc.query("SELECT * FROM exchange_order WHERE family_id=? AND idempotency_key=?",
            this::order, familyId, key).stream().findFirst();
    }

    @Override
    public ExchangeOrder confirm(UUID familyId, UUID previewId, String key, UUID actorId, Instant now) {
        ExchangePreview preview = jdbc.query(
            "SELECT * FROM exchange_preview WHERE family_id=? AND id=? FOR UPDATE",
            this::previewRow, familyId, previewId).stream().findFirst()
            .orElseThrow(FamilyGrowthService.NotFoundException::new);
        if (preview.status() == PreviewStatus.CONFIRMED) {
            ExchangeOrder existing = jdbc.query("SELECT * FROM exchange_order WHERE id=?", this::order,
                preview.confirmedOrderId()).stream().findFirst().orElseThrow();
            if (!existing.idempotencyKey().equals(key)) {
                throw new Stage3Service.ConflictException("Exchange preview is already confirmed");
            }
            return existing;
        }
        WalletRow wallet = lockWallet(familyId, preview.childId());
        UUID orderId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        BigDecimal moneyBefore = wallet.money();
        BigDecimal coinBefore = BigDecimal.valueOf(wallet.coin());
        BigDecimal moneyAfter;
        BigDecimal coinAfter;
        if (preview.direction() == ExchangeDirection.MONEY_TO_COIN) {
            moneyAfter = moneyBefore.subtract(preview.sourceAmount());
            coinAfter = coinBefore.add(preview.targetAmount());
        } else {
            coinAfter = coinBefore.subtract(preview.sourceAmount());
            moneyAfter = moneyBefore.add(preview.targetAmount());
        }
        if (moneyAfter.compareTo(wallet.reserved()) < 0 || coinAfter.signum() < 0) {
            throw new Stage3Service.ConflictException("Wallet balance is insufficient");
        }
        long checkedCoin = coinAfter.longValueExact();
        jdbc.update("""
            INSERT INTO exchange_order
                (id,preview_id,family_id,child_id,direction,source_amount,source_fee,target_amount,
                 rule_id,rule_version,ledger_group_id,idempotency_key,actor_id,created_at)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """, orderId, previewId, familyId, preview.childId(), preview.direction().name(),
            preview.sourceAmount(), preview.sourceFee(), preview.targetAmount(), preview.ruleId(),
            preview.ruleVersion(), groupId, key, actorId, ts(now));
        if (preview.direction() == ExchangeDirection.MONEY_TO_COIN) {
            insertLedger(familyId, preview.childId(), "MONEY", preview.sourceAmount().negate(),
                moneyBefore, moneyAfter, "EXCHANGE_SOURCE", "EXCHANGE", orderId, groupId, key, actorId,
                "Money exchanged to Coin; fee=" + preview.sourceFee(), now);
            insertLedger(familyId, preview.childId(), "COIN", preview.targetAmount(),
                coinBefore, coinAfter, "EXCHANGE_TARGET", "EXCHANGE", orderId, groupId, key, actorId,
                "Coin received", now);
        } else {
            insertLedger(familyId, preview.childId(), "COIN", preview.sourceAmount().negate(),
                coinBefore, coinAfter, "EXCHANGE_SOURCE", "EXCHANGE", orderId, groupId, key, actorId,
                "Coin exchanged to Money; fee=" + preview.sourceFee(), now);
            insertLedger(familyId, preview.childId(), "MONEY", preview.targetAmount(),
                moneyBefore, moneyAfter, "EXCHANGE_TARGET", "EXCHANGE", orderId, groupId, key, actorId,
                "Money received", now);
        }
        jdbc.update("UPDATE wallet SET money_balance=?,coin_balance=?,version=version+1,updated_at=? WHERE family_id=? AND child_id=?",
            moneyAfter, checkedCoin, ts(now), familyId, preview.childId());
        jdbc.update("UPDATE exchange_preview SET status='CONFIRMED',confirmed_order_id=? WHERE id=? AND status='OPEN'",
            orderId, previewId);
        return jdbc.query("SELECT * FROM exchange_order WHERE id=?", this::order, orderId).stream().findFirst().orElseThrow();
    }

    private WalletRow lockWallet(UUID familyId, UUID childId) {
        return jdbc.query("SELECT money_balance,reserved_money,coin_balance FROM wallet WHERE family_id=? AND child_id=? FOR UPDATE",
            (rs, row) -> new WalletRow(rs.getBigDecimal(1), rs.getBigDecimal(2), rs.getLong(3)), familyId, childId)
            .stream().findFirst().orElseThrow(FamilyGrowthService.NotFoundException::new);
    }

    private void insertLedger(UUID familyId, UUID childId, String asset, BigDecimal delta,
                              BigDecimal before, BigDecimal after, String entryType, String businessType,
                              UUID businessId, UUID groupId, String key, UUID actorId, String reason, Instant now) {
        jdbc.update("""
            INSERT INTO ledger_entry
                (id,family_id,child_id,asset_type,delta,before_balance,after_balance,entry_type,
                 business_type,business_id,group_id,idempotency_key,actor_id,reason,created_at)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """, UUID.randomUUID(), familyId, childId, asset, delta, before, after, entryType,
            businessType, businessId, groupId, key, actorId, reason, ts(now));
    }

    private GiftMoney gift(ResultSet rs, int row) throws SQLException {
        return new GiftMoney(uuid(rs,"id"),uuid(rs,"family_id"),uuid(rs,"child_id"),rs.getBigDecimal("amount"),
            rs.getString("note"),uuid(rs,"ledger_group_id"),rs.getString("idempotency_key"),
            uuid(rs,"actor_id"),instant(rs,"created_at"));
    }
    private ExchangeRule rule(ResultSet rs, int row) throws SQLException {
        return new ExchangeRule(uuid(rs,"id"),uuid(rs,"family_id"),rs.getLong("rule_version"),
            rs.getBigDecimal("money_to_coin_rate"),rs.getBigDecimal("coin_to_money_rate"),
            rs.getBigDecimal("money_to_coin_fee_rate"),rs.getBigDecimal("coin_to_money_fee_rate"),
            rs.getBigDecimal("max_source_amount"),rs.getBoolean("active"),uuid(rs,"actor_id"),instant(rs,"created_at"));
    }
    private ExchangePreview previewRow(ResultSet rs, int row) throws SQLException {
        return new ExchangePreview(uuid(rs,"id"),uuid(rs,"family_id"),uuid(rs,"child_id"),
            ExchangeDirection.valueOf(rs.getString("direction")),rs.getBigDecimal("source_amount"),
            rs.getBigDecimal("source_fee"),rs.getBigDecimal("net_source"),rs.getBigDecimal("target_amount"),
            rs.getBigDecimal("applied_rate"),rs.getBigDecimal("applied_fee_rate"),rs.getString("education_notice"),
            uuid(rs,"rule_id"),rs.getLong("rule_version"),PreviewStatus.valueOf(rs.getString("status")),
            instant(rs,"expires_at"),uuid(rs,"confirmed_order_id"),instant(rs,"created_at"));
    }
    private ExchangeOrder order(ResultSet rs, int row) throws SQLException {
        return new ExchangeOrder(uuid(rs,"id"),uuid(rs,"preview_id"),uuid(rs,"family_id"),uuid(rs,"child_id"),
            ExchangeDirection.valueOf(rs.getString("direction")),rs.getBigDecimal("source_amount"),
            rs.getBigDecimal("source_fee"),rs.getBigDecimal("target_amount"),uuid(rs,"rule_id"),
            rs.getLong("rule_version"),uuid(rs,"ledger_group_id"),rs.getString("idempotency_key"),
            uuid(rs,"actor_id"),instant(rs,"created_at"));
    }
    private static UUID uuid(ResultSet rs,String name) throws SQLException { return rs.getObject(name,UUID.class); }
    private static Instant instant(ResultSet rs,String name) throws SQLException {
        Timestamp value=rs.getTimestamp(name); return value==null?null:value.toInstant();
    }
    private static Timestamp ts(Instant value) { return Timestamp.from(value); }
    private record WalletRow(BigDecimal money, BigDecimal reserved, long coin) {}
}
