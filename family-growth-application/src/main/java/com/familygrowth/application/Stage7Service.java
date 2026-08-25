package com.familygrowth.application;
import com.familygrowth.domain.Stage3Models.Actor;import com.familygrowth.domain.Stage3Models.ActorRole;import com.familygrowth.domain.Stage7Models;import com.familygrowth.domain.Stage7Models.*;
import java.math.BigDecimal;import java.time.Clock;import java.time.Duration;import java.time.LocalDate;import java.util.List;import java.util.UUID;
import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;
@Service @Transactional public class Stage7Service{
 private static final Duration PREVIEW_TTL=Duration.ofMinutes(10);private final Stage3Service auth;private final Stage7Store store;private final Clock clock;
 public Stage7Service(Stage3Service auth,Stage7Store store,Clock clock){this.auth=auth;this.store=store;this.clock=clock;}
 public VirtualFund createFund(Actor actor,UUID family,String name,String risk){auth.requireParent(actor,family);return store.createFund(family,name.trim(),risk.trim(),actor.actorId(),clock.instant());}
 @Transactional(readOnly=true) public List<VirtualFund> funds(Actor actor,UUID family){member(actor,family);return store.funds(family);}
 public FundNav addNav(Actor actor,UUID family,UUID fund,LocalDate date,BigDecimal nav){auth.requireParent(actor,family);return store.addNav(family,fund,date,Stage7Models.nav(nav),actor.actorId(),clock.instant());}
 public FundFeeRule createRule(Actor actor,UUID family,UUID fund,BigDecimal buy,BigDecimal sell){auth.requireParent(actor,family);return store.createFeeRule(family,fund,buy,sell,actor.actorId(),clock.instant());}
 public TradePreview preview(Actor actor,UUID family,UUID child,UUID fund,TradeSide side,BigDecimal input){tradeAccess(actor,family,child);return store.createPreview(family,child,fund,side,input,clock.instant().plus(PREVIEW_TTL),clock.instant());}
 public FundTradeOrder confirm(Actor actor,UUID family,UUID previewId,String key){key(key);TradePreview p=store.preview(family,previewId).orElseThrow(FamilyGrowthService.NotFoundException::new);tradeAccess(actor,family,p.childId());var existing=store.orderByKey(family,key);if(existing.isPresent()){if(!existing.get().previewId().equals(previewId))throw new Stage3Service.ConflictException("Idempotency-Key was used for another fund order");return existing.get();}if(p.expiresAt().isBefore(clock.instant()))throw new Stage3Service.ConflictException("Fund preview expired");return store.confirm(family,previewId,key,actor.actorId(),clock.instant());}
 @Transactional(readOnly=true) public FundPosition position(Actor actor,UUID family,UUID child,UUID fund){auth.requireChildOrParent(actor,family,child);return store.position(family,child,fund);}
 private void tradeAccess(Actor actor,UUID family,UUID child){auth.requireChildOrParent(actor,family,child);if(actor.role()==ActorRole.CHILD&&!store.childMayTrade(family,child))throw new Stage3Service.ForbiddenException();}
 private void member(Actor actor,UUID family){if(actor!=null&&actor.childId()!=null)auth.requireChildOrParent(actor,family,actor.childId());else auth.requireParent(actor,family);}
 private static void key(String key){if(key==null||key.isBlank()||key.length()>100)throw new IllegalArgumentException("Valid Idempotency-Key is required");}
}
