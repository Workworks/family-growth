package com.familygrowth.application;

import com.familygrowth.domain.Stage3Models.Actor;
import com.familygrowth.domain.Stage6Models;
import com.familygrowth.domain.Stage6Models.*;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class Stage6Service {
    private final Stage3Service authorization; private final Stage6Store store; private final Stage29CollaborationStore collaboration; private final Clock clock;
    public Stage6Service(Stage3Service authorization,Stage6Store store,Stage29CollaborationStore collaboration,Clock clock){this.authorization=authorization;this.store=store;this.collaboration=collaboration;this.clock=clock;}

    public RewardProduct createProduct(Actor actor,UUID familyId,String title,long cost,int stock,boolean active){
        authorization.requireParent(actor,familyId); return store.createProduct(familyId,title.trim(),cost,stock,active,actor.actorId(),clock.instant());
    }
    @Transactional(readOnly=true)
    public List<RewardProduct> products(Actor actor,UUID familyId,boolean activeOnly){ requireMember(actor,familyId); return store.products(familyId,activeOnly); }
    public RewardOrder createOrder(Actor actor,UUID familyId,UUID childId,UUID productId,String key){
        authorization.requireChildOrParent(actor,familyId,childId); requireKey(key);
        var existing=store.orderBySubmitKey(familyId,key);
        if(existing.isPresent()){
            if(!existing.get().childId().equals(childId)||!existing.get().productId().equals(productId)) throw new Stage3Service.ConflictException("Idempotency-Key was used for another order");
            return existing.get();
        }
        RewardOrder order=store.createOrder(familyId,childId,productId,key,actor.actorId(),clock.instant());collaboration.emitToParents(familyId,childId,com.familygrowth.domain.Stage29CollaborationModels.NotificationType.REWARD_REVIEW,"有一份奖励申请等待回应","批准后才会扣除 Coin","REWARD_ORDER",order.id(),clock.instant());return order;
    }
    public RewardOrder reviewOrder(Actor actor,UUID familyId,UUID orderId,boolean approve,String key){
        authorization.requireParent(actor,familyId); requireKey(key); RewardOrder order=store.reviewOrder(familyId,orderId,approve,key,actor.actorId(),clock.instant());if(approve)collaboration.emitToParents(familyId,order.childId(),com.familygrowth.domain.Stage29CollaborationModels.NotificationType.REWARD_FULFILL,"有一份家庭约定等待兑现","现实里做到后再确认兑现，不会再次扣 Coin","REWARD_ORDER",order.id(),clock.instant());return order;
    }
    @Transactional(readOnly=true)
    public List<RewardOrder> orders(Actor actor,UUID familyId,UUID childId){authorization.requireChildOrParent(actor,familyId,childId);return store.orders(familyId,childId);}
    public RewardOrder cancelOrder(Actor actor,UUID familyId,UUID orderId,String key){
        requireKey(key); RewardOrder order=store.order(familyId,orderId).orElseThrow(FamilyGrowthService.NotFoundException::new);
        authorization.requireChildOrParent(actor,familyId,order.childId()); return store.cancelOrder(familyId,orderId,key,actor.actorId(),clock.instant());
    }
    public SavingTransaction transferSaving(Actor actor,UUID familyId,UUID childId,SavingDirection direction,BigDecimal amount,String key){
        authorization.requireChildOrParent(actor,familyId,childId); requireKey(key); BigDecimal normalized=Stage6Models.money(amount);
        if(normalized.signum()<=0) throw new IllegalArgumentException("Saving amount must be positive");
        var existing=store.savingTransaction(familyId,key);
        if(existing.isPresent()){
            if(!existing.get().childId().equals(childId)||existing.get().direction()!=direction||existing.get().amount().compareTo(normalized)!=0) throw new Stage3Service.ConflictException("Idempotency-Key was used for another saving transfer");
            return existing.get();
        }
        return store.transferSaving(familyId,childId,direction,normalized,key,actor.actorId(),clock.instant());
    }
    @Transactional(readOnly=true)
    public SavingAccount saving(Actor actor,UUID familyId,UUID childId){authorization.requireChildOrParent(actor,familyId,childId);return store.saving(familyId,childId);}
    public Wish createWish(Actor actor,UUID familyId,UUID childId,String title,BigDecimal target){
        authorization.requireChildOrParent(actor,familyId,childId); return store.createWish(familyId,childId,title.trim(),Stage6Models.money(target),actor.actorId(),clock.instant());
    }
    public Wish allocateWish(Actor actor,UUID familyId,UUID wishId,BigDecimal amount,String key){
        requireKey(key); Wish wish=store.wish(familyId,wishId).orElseThrow(FamilyGrowthService.NotFoundException::new);
        authorization.requireChildOrParent(actor,familyId,wish.childId()); return store.allocateWish(familyId,wishId,Stage6Models.money(amount),key,actor.actorId(),clock.instant());
    }
    @Transactional(readOnly=true)
    public List<Wish> wishes(Actor actor,UUID familyId,UUID childId){authorization.requireChildOrParent(actor,familyId,childId);return store.wishes(familyId,childId);}
    private void requireMember(Actor actor,UUID familyId){if(actor!=null&&actor.childId()!=null)authorization.requireChildOrParent(actor,familyId,actor.childId());else authorization.requireParent(actor,familyId);}
    private static void requireKey(String key){if(key==null||key.isBlank()||key.length()>100)throw new IllegalArgumentException("Valid Idempotency-Key is required");}
}
