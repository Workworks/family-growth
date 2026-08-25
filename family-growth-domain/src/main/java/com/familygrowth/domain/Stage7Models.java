package com.familygrowth.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public final class Stage7Models {
    public static final String NOTICE="纯模拟，价格可能上涨或下跌，不构成投资建议";
    private Stage7Models(){}
    public enum TradeSide{BUY,SELL} public enum PreviewStatus{OPEN,CONFIRMED}
    public record VirtualFund(UUID id,UUID familyId,String name,String riskLabel,boolean active,long version,String notice,Instant createdAt){}
    public record FundNav(UUID id,UUID fundId,LocalDate navDate,BigDecimal nav,BigDecimal changePercent,Instant createdAt){public FundNav{nav=Stage7Models.nav(nav);changePercent=changePercent.setScale(4,RoundingMode.HALF_UP);if(nav.signum()<=0)throw new IllegalArgumentException("NAV must be positive");}}
    public record FundFeeRule(UUID id,UUID familyId,UUID fundId,long version,BigDecimal buyFeeRate,BigDecimal sellFeeRate,boolean active,Instant createdAt){public FundFeeRule{buyFeeRate=fee(buyFeeRate);sellFeeRate=fee(sellFeeRate);}}
    public record TradeQuote(BigDecimal inputAmount,BigDecimal grossMoney,BigDecimal feeAmount,BigDecimal netMoney,BigDecimal shares){}
    public record TradePreview(UUID id,UUID familyId,UUID childId,UUID fundId,TradeSide side,BigDecimal inputAmount,
        BigDecimal grossMoney,BigDecimal feeAmount,BigDecimal netMoney,BigDecimal shares,UUID navId,BigDecimal nav,
        UUID feeRuleId,long feeRuleVersion,PreviewStatus status,Instant expiresAt,UUID orderId,String notice,Instant createdAt){}
    public record FundPosition(UUID familyId,UUID childId,UUID fundId,BigDecimal shares,BigDecimal totalCost,
        BigDecimal averageCost,BigDecimal nav,BigDecimal marketValue,BigDecimal unrealizedPnl,BigDecimal realizedPnl,long version){ }
    public record FundTradeOrder(UUID id,UUID previewId,UUID familyId,UUID childId,UUID fundId,TradeSide side,
        BigDecimal grossMoney,BigDecimal feeAmount,BigDecimal netMoney,BigDecimal shares,BigDecimal nav,
        BigDecimal realizedPnl,UUID ledgerGroupId,String idempotencyKey,UUID actorId,Instant createdAt){}

    public static TradeQuote quote(TradeSide side,BigDecimal rawInput,BigDecimal rawNav,BigDecimal rawFee){
        Objects.requireNonNull(side);BigDecimal nav=nav(rawNav),rate=fee(rawFee);
        if(side==TradeSide.BUY){
            BigDecimal input=money(rawInput);if(input.signum()<=0)throw new IllegalArgumentException("Buy amount must be positive");
            BigDecimal fee=input.multiply(rate).setScale(2,RoundingMode.HALF_UP),net=input.subtract(fee);
            BigDecimal shares=net.divide(nav,8,RoundingMode.DOWN);if(shares.signum()<=0)throw new IllegalArgumentException("Buy shares are zero");
            return new TradeQuote(input,input,fee,net,shares);
        }
        BigDecimal shares=shares(rawInput);if(shares.signum()<=0)throw new IllegalArgumentException("Sell shares must be positive");
        BigDecimal gross=shares.multiply(nav).setScale(2,RoundingMode.DOWN),fee=gross.multiply(rate).setScale(2,RoundingMode.HALF_UP);
        BigDecimal net=gross.subtract(fee);if(net.signum()<=0)throw new IllegalArgumentException("Sell proceeds are zero");
        return new TradeQuote(shares,gross,fee,net,shares);
    }
    public static BigDecimal money(BigDecimal v){return Objects.requireNonNull(v).setScale(2,RoundingMode.UNNECESSARY);}
    public static BigDecimal nav(BigDecimal v){return Objects.requireNonNull(v).setScale(6,RoundingMode.UNNECESSARY);}
    public static BigDecimal shares(BigDecimal v){return Objects.requireNonNull(v).setScale(8,RoundingMode.UNNECESSARY);}
    private static BigDecimal fee(BigDecimal v){BigDecimal r=Objects.requireNonNull(v).setScale(6,RoundingMode.HALF_UP);if(r.signum()<0||r.compareTo(BigDecimal.ONE)>=0)throw new IllegalArgumentException("Fee rate must be in [0,1)");return r;}
}
