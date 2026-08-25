CREATE TABLE virtual_fund(
 id UUID PRIMARY KEY,family_id UUID NOT NULL REFERENCES family(id),name VARCHAR(120) NOT NULL,risk_label VARCHAR(80) NOT NULL,
 active BOOLEAN NOT NULL DEFAULT TRUE,version BIGINT NOT NULL DEFAULT 0,notice VARCHAR(200) NOT NULL,actor_id UUID NOT NULL,created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_virtual_fund_family ON virtual_fund(family_id,active,created_at);
CREATE TABLE fund_nav(
 id UUID PRIMARY KEY,family_id UUID NOT NULL REFERENCES family(id),fund_id UUID NOT NULL REFERENCES virtual_fund(id),nav_date DATE NOT NULL,
 nav NUMERIC(19,6) NOT NULL,change_percent NUMERIC(12,4) NOT NULL,actor_id UUID NOT NULL,created_at TIMESTAMP WITH TIME ZONE NOT NULL,
 CONSTRAINT uq_fund_nav_date UNIQUE(fund_id,nav_date),CONSTRAINT ck_fund_nav_positive CHECK(nav>0),
 CONSTRAINT ck_fund_nav_shock CHECK(change_percent>=-50.0000 AND change_percent<=50.0000)
);
CREATE TABLE fund_fee_rule(
 id UUID PRIMARY KEY,family_id UUID NOT NULL REFERENCES family(id),fund_id UUID NOT NULL REFERENCES virtual_fund(id),rule_version BIGINT NOT NULL,
 buy_fee_rate NUMERIC(8,6) NOT NULL,sell_fee_rate NUMERIC(8,6) NOT NULL,active BOOLEAN NOT NULL,actor_id UUID NOT NULL,created_at TIMESTAMP WITH TIME ZONE NOT NULL,
 CONSTRAINT uq_fund_fee_version UNIQUE(fund_id,rule_version),CONSTRAINT ck_fund_fees CHECK(buy_fee_rate>=0 AND buy_fee_rate<1 AND sell_fee_rate>=0 AND sell_fee_rate<1)
);
CREATE INDEX idx_fund_fee_active ON fund_fee_rule(fund_id,active);
CREATE TABLE fund_trade_preview(
 id UUID PRIMARY KEY,family_id UUID NOT NULL REFERENCES family(id),child_id UUID NOT NULL REFERENCES child_profile(id),fund_id UUID NOT NULL REFERENCES virtual_fund(id),
 side VARCHAR(8) NOT NULL,input_amount NUMERIC(19,8) NOT NULL,gross_money NUMERIC(19,2) NOT NULL,fee_amount NUMERIC(19,2) NOT NULL,
 net_money NUMERIC(19,2) NOT NULL,shares NUMERIC(19,8) NOT NULL,nav_id UUID NOT NULL REFERENCES fund_nav(id),nav NUMERIC(19,6) NOT NULL,
 fee_rule_id UUID NOT NULL REFERENCES fund_fee_rule(id),fee_rule_version BIGINT NOT NULL,status VARCHAR(16) NOT NULL,expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
 order_id UUID,notice VARCHAR(200) NOT NULL,created_at TIMESTAMP WITH TIME ZONE NOT NULL,
 CONSTRAINT ck_fund_preview_side CHECK(side IN('BUY','SELL')),CONSTRAINT ck_fund_preview_status CHECK(status IN('OPEN','CONFIRMED'))
);
CREATE TABLE fund_position(
 family_id UUID NOT NULL REFERENCES family(id),child_id UUID NOT NULL REFERENCES child_profile(id),fund_id UUID NOT NULL REFERENCES virtual_fund(id),
 shares NUMERIC(19,8) NOT NULL,total_cost NUMERIC(19,2) NOT NULL,realized_pnl NUMERIC(19,2) NOT NULL,version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP WITH TIME ZONE NOT NULL,updated_at TIMESTAMP WITH TIME ZONE NOT NULL,PRIMARY KEY(child_id,fund_id),
 CONSTRAINT ck_fund_position CHECK(shares>=0 AND total_cost>=0)
);
CREATE TABLE fund_trade_order(
 id UUID PRIMARY KEY,preview_id UUID NOT NULL UNIQUE REFERENCES fund_trade_preview(id),family_id UUID NOT NULL REFERENCES family(id),
 child_id UUID NOT NULL REFERENCES child_profile(id),fund_id UUID NOT NULL REFERENCES virtual_fund(id),side VARCHAR(8) NOT NULL,
 gross_money NUMERIC(19,2) NOT NULL,fee_amount NUMERIC(19,2) NOT NULL,net_money NUMERIC(19,2) NOT NULL,shares NUMERIC(19,8) NOT NULL,
 nav NUMERIC(19,6) NOT NULL,realized_pnl NUMERIC(19,2) NOT NULL,ledger_group_id UUID NOT NULL,idempotency_key VARCHAR(100) NOT NULL,
 actor_id UUID NOT NULL,created_at TIMESTAMP WITH TIME ZONE NOT NULL,CONSTRAINT uq_fund_order_key UNIQUE(family_id,idempotency_key)
);
CREATE INDEX idx_fund_order_child ON fund_trade_order(family_id,child_id,created_at);
