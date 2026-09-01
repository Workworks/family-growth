CREATE TABLE saving_reward_rule (
 id UUID PRIMARY KEY,family_id UUID NOT NULL REFERENCES family(id),rule_version BIGINT NOT NULL,periodic_rate NUMERIC(9,6) NOT NULL,
 minimum_balance NUMERIC(19,2) NOT NULL,active BOOLEAN NOT NULL,actor_id UUID NOT NULL,created_at TIMESTAMP WITH TIME ZONE NOT NULL,
 CONSTRAINT uq_saving_reward_rule UNIQUE(family_id,rule_version),CONSTRAINT ck_saving_reward_rate CHECK(periodic_rate BETWEEN 0 AND 0.100000),
 CONSTRAINT ck_saving_reward_minimum CHECK(minimum_balance>=0)
);
CREATE INDEX idx_saving_reward_active ON saving_reward_rule(family_id,active);
CREATE TABLE saving_reward_award (
 id UUID PRIMARY KEY,family_id UUID NOT NULL REFERENCES family(id),child_id UUID NOT NULL REFERENCES child_profile(id),rule_id UUID NOT NULL REFERENCES saving_reward_rule(id),
 cycle_key VARCHAR(40) NOT NULL,base_balance NUMERIC(19,2) NOT NULL,reward_amount NUMERIC(19,2) NOT NULL,ledger_group_id UUID NOT NULL,
 idempotency_key VARCHAR(100) NOT NULL,actor_id UUID NOT NULL,created_at TIMESTAMP WITH TIME ZONE NOT NULL,
 CONSTRAINT uq_saving_reward_cycle UNIQUE(family_id,child_id,cycle_key),CONSTRAINT uq_saving_reward_key UNIQUE(family_id,idempotency_key)
);
CREATE TABLE simulated_market_rule (
 id UUID PRIMARY KEY,family_id UUID NOT NULL REFERENCES family(id),fund_id UUID NOT NULL REFERENCES virtual_fund(id),rule_version BIGINT NOT NULL,
 deterministic_seed VARCHAR(80) NOT NULL,maximum_daily_bps INTEGER NOT NULL,active BOOLEAN NOT NULL,actor_id UUID NOT NULL,created_at TIMESTAMP WITH TIME ZONE NOT NULL,
 CONSTRAINT uq_simulated_market_version UNIQUE(fund_id,rule_version),CONSTRAINT ck_simulated_market_bps CHECK(maximum_daily_bps BETWEEN 0 AND 2000)
);
CREATE INDEX idx_simulated_market_active ON simulated_market_rule(fund_id,active);
CREATE TABLE simulated_market_tick (
 id UUID PRIMARY KEY,family_id UUID NOT NULL REFERENCES family(id),fund_id UUID NOT NULL REFERENCES virtual_fund(id),rule_id UUID NOT NULL REFERENCES simulated_market_rule(id),
 tick_date DATE NOT NULL,change_bps INTEGER NOT NULL,nav_before NUMERIC(19,6) NOT NULL,nav_after NUMERIC(19,6) NOT NULL,fund_nav_id UUID NOT NULL REFERENCES fund_nav(id),
 idempotency_key VARCHAR(100) NOT NULL,actor_id UUID NOT NULL,created_at TIMESTAMP WITH TIME ZONE NOT NULL,
 CONSTRAINT uq_simulated_tick_date UNIQUE(fund_id,tick_date),CONSTRAINT uq_simulated_tick_key UNIQUE(family_id,idempotency_key)
);
CREATE TABLE fund_holding_fee_rule (
 id UUID PRIMARY KEY,family_id UUID NOT NULL REFERENCES family(id),fund_id UUID NOT NULL REFERENCES virtual_fund(id),rule_version BIGINT NOT NULL,
 minimum_holding_days INTEGER NOT NULL,early_fee_rate NUMERIC(9,6) NOT NULL,active BOOLEAN NOT NULL,actor_id UUID NOT NULL,created_at TIMESTAMP WITH TIME ZONE NOT NULL,
 CONSTRAINT uq_holding_fee_version UNIQUE(fund_id,rule_version),CONSTRAINT ck_holding_days CHECK(minimum_holding_days BETWEEN 0 AND 3650),
 CONSTRAINT ck_early_fee_rate CHECK(early_fee_rate BETWEEN 0 AND 0.500000)
);
CREATE INDEX idx_holding_fee_active ON fund_holding_fee_rule(fund_id,active);
ALTER TABLE fund_trade_preview ADD COLUMN holding_days INTEGER NOT NULL DEFAULT 0;
ALTER TABLE fund_trade_preview ADD COLUMN early_fee_rate NUMERIC(9,6) NOT NULL DEFAULT 0;
ALTER TABLE fund_trade_preview ADD COLUMN early_fee_amount NUMERIC(19,2) NOT NULL DEFAULT 0;
ALTER TABLE fund_trade_order ADD COLUMN holding_days INTEGER NOT NULL DEFAULT 0;
ALTER TABLE fund_trade_order ADD COLUMN early_fee_rate NUMERIC(9,6) NOT NULL DEFAULT 0;
ALTER TABLE fund_trade_order ADD COLUMN early_fee_amount NUMERIC(19,2) NOT NULL DEFAULT 0;
ALTER TABLE reward_product ADD COLUMN product_kind VARCHAR(20) NOT NULL DEFAULT 'REAL_WORLD';
ALTER TABLE reward_product ADD CONSTRAINT ck_reward_product_kind CHECK(product_kind IN('REAL_WORLD','COSMETIC'));
CREATE TABLE economy_lab_action(id UUID PRIMARY KEY,family_id UUID NOT NULL REFERENCES family(id),actor_id UUID NOT NULL,action_type VARCHAR(40) NOT NULL,target_id UUID NOT NULL,detail VARCHAR(300) NOT NULL,created_at TIMESTAMP WITH TIME ZONE NOT NULL);
