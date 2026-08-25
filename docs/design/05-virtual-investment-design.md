# 05 虚拟投资设计

## 边界

系统只创建虚拟名称与纯模拟 NAV，不接真实行情、真实基金代码、证券账户、支付或投资推荐。所有页面和订单确认显示“模拟投资，不代表真实证券产品，可能上涨或下跌”。基金盈亏只改变家庭账本内的 Money；孩子申请线下兑现时，才进入独立的零钱回收流程。

## 基金与 NAV

VirtualFund 保存名称、说明、风险级别、启用状态和展示参数；可交易事实由 `VirtualFundNav` 与版本化 `FundFeeRule` 决定。风险等级 LOW/MEDIUM/HIGH/VERY_HIGH 对应不同期望漂移与波动率范围，但不承诺收益。

首版 MarketEngine 使用可注入随机源，按周期生成：

`nextNav = max(minNav, currentNav × (1 + drift + volatility × shock))`

实现时使用高精度十进制；随机 shock 可正可负，测试使用固定 seed/脚本化 shock。配置必须允许下跌，且设单期变化护栏防止负 NAV 或异常跳变。引擎执行以 `(fund_id, nav_date)` 唯一约束保证幂等。

## 买入

- 输入 `grossAmount`。
- `buyFee = round(grossAmount × buyFeeRate, 2)`。
- `netInvestment = grossAmount - buyFee`。
- `shares = netInvestment / nav`，保留 8 位。
- 确认前展示四项与使用的 NAV、规则版本；执行时扣 Money、增加份额、更新加权平均成本并写 Ledger。

示例验收：NAV 1.00，投入 20.00，费率 5%，手续费 1.00，净投入 19.00，份额 19.00000000。

## 卖出

- `grossProceeds = shares × nav`。
- `sellFee = grossProceeds × sellFeeRate`。
- 根据持有批次计算提前赎回费；V1 建议按 FIFO lot，而非仅存平均日期。
- `netProceeds = grossProceeds - sellFee - earlyRedemptionFee`。
- 原子减少持仓、增加 Money，并分别记录市值、费用、到账和已实现损益。

## 持仓与损益

FundPosition 是投影：shares、averageCost、marketValue、profitLoss、profitLossRate。市场价值 = shares × 最新 NAV；未实现损益须扣除已发生买入费，是否估算未来卖出费必须明确标记，V1 报告分开显示，避免混淆。

## 费率与教育反馈

费率由家长配置并版本化，持有 `<7 天`、`7–30 天`、长期的赎回档位可配置。高频交易报告分开显示市场损益、交易费与零钱回收手续费，不能用惩罚性隐藏扣费。家长可以通过透明手续费让孩子理解交易成本，但平台不从中取得真实收入。至少提供三只虚拟基金：稳稳成长、成长基金、冒险基金，不使用真实产品标识。
