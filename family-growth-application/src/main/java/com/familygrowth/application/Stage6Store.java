package com.familygrowth.application;

import com.familygrowth.domain.Stage6Models.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface Stage6Store {
    RewardProduct createProduct(UUID familyId,String title,long coinCost,int stockCount,boolean active,UUID actorId,Instant now);
    List<RewardProduct> products(UUID familyId,boolean activeOnly);
    Optional<RewardOrder> orderBySubmitKey(UUID familyId,String key);
    Optional<RewardOrder> order(UUID familyId,UUID orderId);
    List<RewardOrder> orders(UUID familyId,UUID childId);
    RewardOrder createOrder(UUID familyId,UUID childId,UUID productId,String key,UUID actorId,Instant now);
    RewardOrder reviewOrder(UUID familyId,UUID orderId,boolean approve,String key,UUID actorId,Instant now);
    RewardOrder cancelOrder(UUID familyId,UUID orderId,String key,UUID actorId,Instant now);
    Optional<SavingTransaction> savingTransaction(UUID familyId,String key);
    SavingTransaction transferSaving(UUID familyId,UUID childId,SavingDirection direction,BigDecimal amount,String key,UUID actorId,Instant now);
    SavingAccount saving(UUID familyId,UUID childId);
    Wish createWish(UUID familyId,UUID childId,String title,BigDecimal target,UUID actorId,Instant now);
    Optional<Wish> wish(UUID familyId,UUID wishId);
    Wish allocateWish(UUID familyId,UUID wishId,BigDecimal amount,String key,UUID actorId,Instant now);
    List<Wish> wishes(UUID familyId,UUID childId);
}
