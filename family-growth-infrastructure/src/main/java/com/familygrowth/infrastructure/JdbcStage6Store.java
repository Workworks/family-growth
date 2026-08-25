package com.familygrowth.infrastructure;

import com.familygrowth.application.FamilyGrowthService;
import com.familygrowth.application.Stage3Service;
import com.familygrowth.application.Stage6Store;
import com.familygrowth.domain.Stage6Models.*;
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
class JdbcStage6Store implements Stage6Store {
    private final JdbcTemplate jdbc;
    JdbcStage6Store(JdbcTemplate jdbc){this.jdbc=jdbc;}

    @Override public RewardProduct createProduct(UUID familyId,String title,long cost,int stock,boolean active,UUID actorId,Instant now){
        UUID id=UUID.randomUUID(); jdbc.update("INSERT INTO reward_product(id,family_id,title,coin_cost,stock_count,active,version,actor_id,created_at) VALUES(?,?,?,?,?,?,0,?,?)",
            id,familyId,title,cost,stock,active,actorId,ts(now)); return product(familyId,id);
    }
    @Override public List<RewardProduct> products(UUID familyId,boolean activeOnly){
        return jdbc.query("SELECT * FROM reward_product WHERE family_id=? AND (?=FALSE OR active=TRUE) ORDER BY created_at,id",this::productRow,familyId,activeOnly);
    }
    @Override public Optional<RewardOrder> orderBySubmitKey(UUID familyId,String key){return jdbc.query("SELECT * FROM reward_order WHERE family_id=? AND submit_key=?",this::orderRow,familyId,key).stream().findFirst();}
    @Override public Optional<RewardOrder> order(UUID familyId,UUID id){return jdbc.query("SELECT * FROM reward_order WHERE family_id=? AND id=?",this::orderRow,familyId,id).stream().findFirst();}
    @Override public RewardOrder createOrder(UUID familyId,UUID childId,UUID productId,String key,UUID actorId,Instant now){
        RewardProduct product=product(familyId,productId); if(!product.active()||product.stockCount()==0)throw new Stage3Service.ConflictException("Reward product is unavailable");
        UUID id=UUID.randomUUID(); jdbc.update("INSERT INTO reward_order(id,family_id,child_id,product_id,product_title,coin_cost,status,submit_key,created_at) VALUES(?,?,?,?,?,?,'CREATED',?,?)",
            id,familyId,childId,productId,product.title(),product.coinCost(),key,ts(now)); return order(familyId,id).orElseThrow();
    }
    @Override public RewardOrder reviewOrder(UUID familyId,UUID orderId,boolean approve,String key,UUID actorId,Instant now){
        RewardOrder order=lockOrder(familyId,orderId); RewardOrderStatus desired=approve?RewardOrderStatus.APPROVED:RewardOrderStatus.REJECTED;
        if(order.status()!=RewardOrderStatus.CREATED){
            if(order.status()==desired&&key.equals(order.reviewKey()))return order;
            throw new Stage3Service.ConflictException("Reward order is already decided");
        }
        UUID group=approve?UUID.randomUUID():null;
        if(approve){
            RewardProduct product=lockProduct(familyId,order.productId());
            if(!product.active()||product.stockCount()<=0)throw new Stage3Service.ConflictException("Reward product is unavailable");
            WalletRow wallet=lockWallet(familyId,order.childId());
            long after;
            try{after=Math.subtractExact(wallet.coin(),order.coinCost());}catch(ArithmeticException ex){throw new Stage3Service.ConflictException("Coin balance is insufficient");}
            if(after<0)throw new Stage3Service.ConflictException("Coin balance is insufficient");
            insertLedger(familyId,order.childId(),"COIN",BigDecimal.valueOf(-order.coinCost()),BigDecimal.valueOf(wallet.coin()),
                BigDecimal.valueOf(after),"REWARD_ORDER","REWARD_ORDER",order.id(),group,key,actorId,"Reward approved: "+order.productTitle(),now);
            jdbc.update("UPDATE wallet SET coin_balance=?,version=version+1,updated_at=? WHERE family_id=? AND child_id=?",after,ts(now),familyId,order.childId());
            jdbc.update("UPDATE reward_product SET stock_count=stock_count-1,version=version+1 WHERE id=? AND stock_count>0",order.productId());
        }
        jdbc.update("UPDATE reward_order SET status=?,review_key=?,reviewed_by=?,ledger_group_id=?,reviewed_at=? WHERE id=? AND status='CREATED'",
            desired.name(),key,actorId,group,ts(now),orderId); return order(familyId,orderId).orElseThrow();
    }
    @Override public RewardOrder cancelOrder(UUID familyId,UUID orderId,String key,UUID actorId,Instant now){
        RewardOrder order=lockOrder(familyId,orderId);
        if(order.status()==RewardOrderStatus.CANCELED&&key.equals(order.reviewKey()))return order;
        if(order.status()!=RewardOrderStatus.CREATED)throw new Stage3Service.ConflictException("Only a created order can be canceled");
        jdbc.update("UPDATE reward_order SET status='CANCELED',review_key=?,reviewed_by=?,reviewed_at=? WHERE id=? AND status='CREATED'",key,actorId,ts(now),orderId);
        return order(familyId,orderId).orElseThrow();
    }
    @Override public Optional<SavingTransaction> savingTransaction(UUID familyId,String key){return jdbc.query("SELECT * FROM saving_transaction WHERE family_id=? AND idempotency_key=?",this::savingTxRow,familyId,key).stream().findFirst();}
    @Override public SavingTransaction transferSaving(UUID familyId,UUID childId,SavingDirection direction,BigDecimal amount,String key,UUID actorId,Instant now){
        ensureSaving(familyId,childId,now); WalletRow wallet=lockWallet(familyId,childId); SavingAccount saving=lockSaving(familyId,childId);
        BigDecimal walletAfter=direction==SavingDirection.DEPOSIT?wallet.money().subtract(amount):wallet.money().add(amount);
        BigDecimal savingAfter=direction==SavingDirection.DEPOSIT?saving.balance().add(amount):saving.balance().subtract(amount);
        if(walletAfter.signum()<0||savingAfter.signum()<0)throw new Stage3Service.ConflictException("Money balance is insufficient");
        UUID id=UUID.randomUUID(),group=UUID.randomUUID(); BigDecimal delta=direction==SavingDirection.DEPOSIT?amount.negate():amount;
        jdbc.update("""
            INSERT INTO saving_transaction(id,family_id,child_id,direction,amount,wallet_before,wallet_after,saving_before,saving_after,ledger_group_id,idempotency_key,actor_id,created_at)
            VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)
            """,id,familyId,childId,direction.name(),amount,wallet.money(),walletAfter,saving.balance(),savingAfter,group,key,actorId,ts(now));
        insertLedger(familyId,childId,"MONEY",delta,wallet.money(),walletAfter,"SAVING_"+direction.name(),"SAVING_TRANSFER",id,group,key,actorId,"Internal saving transfer",now);
        jdbc.update("UPDATE wallet SET money_balance=?,version=version+1,updated_at=? WHERE family_id=? AND child_id=?",walletAfter,ts(now),familyId,childId);
        jdbc.update("UPDATE saving_account SET balance=?,version=version+1,updated_at=? WHERE family_id=? AND child_id=?",savingAfter,ts(now),familyId,childId);
        return savingTransaction(familyId,key).orElseThrow();
    }
    @Override public SavingAccount saving(UUID familyId,UUID childId){
        return jdbc.query("SELECT * FROM saving_account WHERE family_id=? AND child_id=?",this::savingRow,familyId,childId).stream().findFirst()
            .orElse(new SavingAccount(familyId,childId,new BigDecimal("0.00"),0));
    }
    @Override public Wish createWish(UUID familyId,UUID childId,String title,BigDecimal target,UUID actorId,Instant now){
        if(title.isBlank()||title.length()>120||target.signum()<=0)throw new IllegalArgumentException("Wish is invalid");
        ensureSaving(familyId,childId,now);
        UUID id=UUID.randomUUID();jdbc.update("INSERT INTO wish(id,family_id,child_id,title,target_amount,allocated_amount,version,actor_id,created_at) VALUES(?,?,?,?,?,0.00,0,?,?)",
            id,familyId,childId,title,target,actorId,ts(now));return wish(familyId,id).orElseThrow();
    }
    @Override public Optional<Wish> wish(UUID familyId,UUID wishId){return jdbc.query("SELECT * FROM wish WHERE family_id=? AND id=?",this::wishRow,familyId,wishId).stream().findFirst();}
    @Override public Wish allocateWish(UUID familyId,UUID wishId,BigDecimal amount,String key,UUID actorId,Instant now){
        var replay=jdbc.query("SELECT wish_id,after_amount FROM wish_allocation WHERE family_id=? AND idempotency_key=?",
            (rs,row)->new AllocationReplay(uuid(rs,"wish_id"),rs.getBigDecimal("after_amount")),familyId,key).stream().findFirst();
        if(replay.isPresent()){
            if(!replay.get().wishId().equals(wishId)||replay.get().after().compareTo(amount)!=0)throw new Stage3Service.ConflictException("Idempotency-Key was used for another wish allocation");
            return wish(familyId,wishId).orElseThrow();
        }
        Wish current=jdbc.query("SELECT * FROM wish WHERE family_id=? AND id=? FOR UPDATE",this::wishRow,familyId,wishId).stream().findFirst().orElseThrow(FamilyGrowthService.NotFoundException::new);
        SavingAccount saving=lockSaving(familyId,current.childId());
        BigDecimal other=jdbc.queryForObject("SELECT COALESCE(SUM(allocated_amount),0.00) FROM wish WHERE family_id=? AND child_id=? AND id<>?",BigDecimal.class,familyId,current.childId(),wishId);
        if(amount.signum()<0||other.add(amount).compareTo(saving.balance())>0)throw new Stage3Service.ConflictException("Wish allocation exceeds saving balance");
        jdbc.update("INSERT INTO wish_allocation(id,family_id,wish_id,amount,before_amount,after_amount,idempotency_key,actor_id,created_at) VALUES(?,?,?,?,?,?,?,?,?)",
            UUID.randomUUID(),familyId,wishId,amount,current.allocatedAmount(),amount,key,actorId,ts(now));
        jdbc.update("UPDATE wish SET allocated_amount=?,version=version+1 WHERE id=?",amount,wishId);return wish(familyId,wishId).orElseThrow();
    }
    @Override public List<Wish> wishes(UUID familyId,UUID childId){return jdbc.query("SELECT * FROM wish WHERE family_id=? AND child_id=? ORDER BY created_at,id",this::wishRow,familyId,childId);}

    private void ensureSaving(UUID familyId,UUID childId,Instant now){if(jdbc.queryForObject("SELECT COUNT(*) FROM saving_account WHERE family_id=? AND child_id=?",Integer.class,familyId,childId)==0)
        jdbc.update("INSERT INTO saving_account(child_id,family_id,balance,version,created_at,updated_at) VALUES(?,?,0.00,0,?,?)",childId,familyId,ts(now),ts(now));}
    private RewardProduct product(UUID familyId,UUID id){return jdbc.query("SELECT * FROM reward_product WHERE family_id=? AND id=?",this::productRow,familyId,id).stream().findFirst().orElseThrow(FamilyGrowthService.NotFoundException::new);}
    private RewardProduct lockProduct(UUID familyId,UUID id){return jdbc.query("SELECT * FROM reward_product WHERE family_id=? AND id=? FOR UPDATE",this::productRow,familyId,id).stream().findFirst().orElseThrow(FamilyGrowthService.NotFoundException::new);}
    private RewardOrder lockOrder(UUID familyId,UUID id){return jdbc.query("SELECT * FROM reward_order WHERE family_id=? AND id=? FOR UPDATE",this::orderRow,familyId,id).stream().findFirst().orElseThrow(FamilyGrowthService.NotFoundException::new);}
    private WalletRow lockWallet(UUID familyId,UUID childId){return jdbc.query("SELECT money_balance,coin_balance FROM wallet WHERE family_id=? AND child_id=? FOR UPDATE",(rs,row)->new WalletRow(rs.getBigDecimal(1),rs.getLong(2)),familyId,childId).stream().findFirst().orElseThrow(FamilyGrowthService.NotFoundException::new);}
    private SavingAccount lockSaving(UUID familyId,UUID childId){return jdbc.query("SELECT * FROM saving_account WHERE family_id=? AND child_id=? FOR UPDATE",this::savingRow,familyId,childId).stream().findFirst().orElseThrow(FamilyGrowthService.NotFoundException::new);}
    private void insertLedger(UUID familyId,UUID childId,String asset,BigDecimal delta,BigDecimal before,BigDecimal after,String entryType,String businessType,UUID businessId,UUID group,String key,UUID actor,String reason,Instant now){
        jdbc.update("INSERT INTO ledger_entry(id,family_id,child_id,asset_type,delta,before_balance,after_balance,entry_type,business_type,business_id,group_id,idempotency_key,actor_id,reason,created_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
            UUID.randomUUID(),familyId,childId,asset,delta,before,after,entryType,businessType,businessId,group,key,actor,reason,ts(now));
    }
    private RewardProduct productRow(ResultSet rs,int row)throws SQLException{return new RewardProduct(uuid(rs,"id"),uuid(rs,"family_id"),rs.getString("title"),rs.getLong("coin_cost"),rs.getInt("stock_count"),rs.getBoolean("active"),rs.getLong("version"),instant(rs,"created_at"));}
    private RewardOrder orderRow(ResultSet rs,int row)throws SQLException{return new RewardOrder(uuid(rs,"id"),uuid(rs,"family_id"),uuid(rs,"child_id"),uuid(rs,"product_id"),rs.getString("product_title"),rs.getLong("coin_cost"),RewardOrderStatus.valueOf(rs.getString("status")),rs.getString("submit_key"),rs.getString("review_key"),uuid(rs,"reviewed_by"),uuid(rs,"ledger_group_id"),instant(rs,"created_at"),instant(rs,"reviewed_at"));}
    private SavingAccount savingRow(ResultSet rs,int row)throws SQLException{return new SavingAccount(uuid(rs,"family_id"),uuid(rs,"child_id"),rs.getBigDecimal("balance"),rs.getLong("version"));}
    private SavingTransaction savingTxRow(ResultSet rs,int row)throws SQLException{return new SavingTransaction(uuid(rs,"id"),uuid(rs,"family_id"),uuid(rs,"child_id"),SavingDirection.valueOf(rs.getString("direction")),rs.getBigDecimal("amount"),rs.getBigDecimal("wallet_before"),rs.getBigDecimal("wallet_after"),rs.getBigDecimal("saving_before"),rs.getBigDecimal("saving_after"),uuid(rs,"ledger_group_id"),rs.getString("idempotency_key"),uuid(rs,"actor_id"),instant(rs,"created_at"));}
    private Wish wishRow(ResultSet rs,int row)throws SQLException{return new Wish(uuid(rs,"id"),uuid(rs,"family_id"),uuid(rs,"child_id"),rs.getString("title"),rs.getBigDecimal("target_amount"),rs.getBigDecimal("allocated_amount"),BigDecimal.ZERO,false,rs.getLong("version"),instant(rs,"created_at"));}
    private static UUID uuid(ResultSet rs,String name)throws SQLException{return rs.getObject(name,UUID.class);} private static Instant instant(ResultSet rs,String name)throws SQLException{Timestamp value=rs.getTimestamp(name);return value==null?null:value.toInstant();} private static Timestamp ts(Instant value){return Timestamp.from(value);}
    private record WalletRow(BigDecimal money,long coin){} private record AllocationReplay(UUID wishId,BigDecimal after){}
}
