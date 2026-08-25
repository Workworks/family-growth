package com.familygrowth.application;
import com.familygrowth.domain.Stage7Models.*;
import java.math.BigDecimal;import java.time.Instant;import java.time.LocalDate;import java.util.List;import java.util.Optional;import java.util.UUID;
public interface Stage7Store{
 VirtualFund createFund(UUID familyId,String name,String riskLabel,UUID actorId,Instant now);
 List<VirtualFund> funds(UUID familyId);
 FundNav addNav(UUID familyId,UUID fundId,LocalDate date,BigDecimal nav,UUID actorId,Instant now);
 FundFeeRule createFeeRule(UUID familyId,UUID fundId,BigDecimal buyFee,BigDecimal sellFee,UUID actorId,Instant now);
 boolean childMayTrade(UUID familyId,UUID childId);
 TradePreview createPreview(UUID familyId,UUID childId,UUID fundId,TradeSide side,BigDecimal input,Instant expiresAt,Instant now);
 Optional<TradePreview> preview(UUID familyId,UUID previewId);
 Optional<FundTradeOrder> orderByKey(UUID familyId,String key);
 FundTradeOrder confirm(UUID familyId,UUID previewId,String key,UUID actorId,Instant now);
 FundPosition position(UUID familyId,UUID childId,UUID fundId);
}
