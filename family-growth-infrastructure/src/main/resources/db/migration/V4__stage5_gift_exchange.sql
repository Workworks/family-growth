CREATE TABLE gift_money (
    id UUID PRIMARY KEY,
    family_id UUID NOT NULL REFERENCES family(id),
    child_id UUID NOT NULL REFERENCES child_profile(id),
    amount NUMERIC(19,2) NOT NULL,
    note VARCHAR(500) NOT NULL DEFAULT '',
    ledger_group_id UUID NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    actor_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_gift_key UNIQUE (family_id, idempotency_key),
    CONSTRAINT ck_gift_positive CHECK (amount > 0)
);

CREATE TABLE exchange_rule (
    id UUID PRIMARY KEY,
    family_id UUID NOT NULL REFERENCES family(id),
    rule_version BIGINT NOT NULL,
    money_to_coin_rate NUMERIC(19,6) NOT NULL,
    coin_to_money_rate NUMERIC(19,6) NOT NULL,
    money_to_coin_fee_rate NUMERIC(8,6) NOT NULL,
    coin_to_money_fee_rate NUMERIC(8,6) NOT NULL,
    max_source_amount NUMERIC(19,2) NOT NULL,
    active BOOLEAN NOT NULL,
    actor_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_exchange_rule_version UNIQUE (family_id, rule_version),
    CONSTRAINT ck_exchange_rule_rates CHECK (money_to_coin_rate > 0 AND coin_to_money_rate > 0),
    CONSTRAINT ck_exchange_rule_fees CHECK (money_to_coin_fee_rate >= 0 AND money_to_coin_fee_rate < 1 AND coin_to_money_fee_rate >= 0 AND coin_to_money_fee_rate < 1),
    CONSTRAINT ck_exchange_rule_budget CHECK (max_source_amount > 0)
);
CREATE INDEX idx_exchange_rule_active ON exchange_rule(family_id, active);

CREATE TABLE exchange_preview (
    id UUID PRIMARY KEY,
    family_id UUID NOT NULL REFERENCES family(id),
    child_id UUID NOT NULL REFERENCES child_profile(id),
    direction VARCHAR(24) NOT NULL,
    source_amount NUMERIC(19,2) NOT NULL,
    source_fee NUMERIC(19,2) NOT NULL,
    net_source NUMERIC(19,2) NOT NULL,
    target_amount NUMERIC(19,2) NOT NULL,
    applied_rate NUMERIC(19,6) NOT NULL,
    applied_fee_rate NUMERIC(8,6) NOT NULL,
    education_notice VARCHAR(200) NOT NULL,
    rule_id UUID NOT NULL REFERENCES exchange_rule(id),
    rule_version BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    confirmed_order_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_exchange_preview_direction CHECK (direction IN ('MONEY_TO_COIN','COIN_TO_MONEY')),
    CONSTRAINT ck_exchange_preview_status CHECK (status IN ('OPEN','CONFIRMED')),
    CONSTRAINT ck_exchange_preview_amounts CHECK (source_amount > 0 AND source_fee >= 0 AND net_source > 0 AND target_amount > 0)
);

CREATE TABLE exchange_order (
    id UUID PRIMARY KEY,
    preview_id UUID NOT NULL UNIQUE REFERENCES exchange_preview(id),
    family_id UUID NOT NULL REFERENCES family(id),
    child_id UUID NOT NULL REFERENCES child_profile(id),
    direction VARCHAR(24) NOT NULL,
    source_amount NUMERIC(19,2) NOT NULL,
    source_fee NUMERIC(19,2) NOT NULL,
    target_amount NUMERIC(19,2) NOT NULL,
    rule_id UUID NOT NULL REFERENCES exchange_rule(id),
    rule_version BIGINT NOT NULL,
    ledger_group_id UUID NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    actor_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_exchange_order_key UNIQUE (family_id, idempotency_key)
);
CREATE INDEX idx_exchange_order_child ON exchange_order(family_id, child_id, created_at);
