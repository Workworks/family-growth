CREATE TABLE reward_product (
    id UUID PRIMARY KEY, family_id UUID NOT NULL REFERENCES family(id), title VARCHAR(120) NOT NULL,
    coin_cost BIGINT NOT NULL, stock_count INTEGER NOT NULL, active BOOLEAN NOT NULL,
    version BIGINT NOT NULL DEFAULT 0, actor_id UUID NOT NULL, created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_reward_product CHECK (coin_cost > 0 AND stock_count >= 0)
);
CREATE INDEX idx_reward_product_family ON reward_product(family_id,active,created_at);

CREATE TABLE reward_order (
    id UUID PRIMARY KEY, family_id UUID NOT NULL REFERENCES family(id), child_id UUID NOT NULL REFERENCES child_profile(id),
    product_id UUID NOT NULL REFERENCES reward_product(id), product_title VARCHAR(120) NOT NULL, coin_cost BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL, submit_key VARCHAR(100) NOT NULL, review_key VARCHAR(100), reviewed_by UUID,
    ledger_group_id UUID, created_at TIMESTAMP WITH TIME ZONE NOT NULL, reviewed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_reward_order_submit UNIQUE(family_id,submit_key),
    CONSTRAINT uq_reward_order_review UNIQUE(family_id,review_key),
    CONSTRAINT ck_reward_order_status CHECK(status IN('CREATED','APPROVED','REJECTED','CANCELED')),
    CONSTRAINT ck_reward_order_cost CHECK(coin_cost > 0)
);
CREATE INDEX idx_reward_order_child ON reward_order(family_id,child_id,status,created_at);

CREATE TABLE saving_account (
    child_id UUID PRIMARY KEY REFERENCES child_profile(id), family_id UUID NOT NULL REFERENCES family(id),
    balance NUMERIC(19,2) NOT NULL DEFAULT 0.00, version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL, updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_saving_balance CHECK(balance >= 0)
);
CREATE TABLE saving_transaction (
    id UUID PRIMARY KEY, family_id UUID NOT NULL REFERENCES family(id), child_id UUID NOT NULL REFERENCES child_profile(id),
    direction VARCHAR(16) NOT NULL, amount NUMERIC(19,2) NOT NULL,
    wallet_before NUMERIC(19,2) NOT NULL, wallet_after NUMERIC(19,2) NOT NULL,
    saving_before NUMERIC(19,2) NOT NULL, saving_after NUMERIC(19,2) NOT NULL,
    ledger_group_id UUID NOT NULL, idempotency_key VARCHAR(100) NOT NULL, actor_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_saving_tx_key UNIQUE(family_id,idempotency_key),
    CONSTRAINT ck_saving_direction CHECK(direction IN('DEPOSIT','WITHDRAW')),
    CONSTRAINT ck_saving_amount CHECK(amount > 0),
    CONSTRAINT ck_saving_conservation CHECK(wallet_before+saving_before=wallet_after+saving_after)
);

CREATE TABLE wish (
    id UUID PRIMARY KEY, family_id UUID NOT NULL REFERENCES family(id), child_id UUID NOT NULL REFERENCES child_profile(id),
    title VARCHAR(120) NOT NULL, target_amount NUMERIC(19,2) NOT NULL, allocated_amount NUMERIC(19,2) NOT NULL DEFAULT 0.00,
    version BIGINT NOT NULL DEFAULT 0, actor_id UUID NOT NULL, created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_wish_amounts CHECK(target_amount > 0 AND allocated_amount >= 0)
);
CREATE INDEX idx_wish_child ON wish(family_id,child_id,created_at);
CREATE TABLE wish_allocation (
    id UUID PRIMARY KEY, family_id UUID NOT NULL REFERENCES family(id), wish_id UUID NOT NULL REFERENCES wish(id),
    amount NUMERIC(19,2) NOT NULL, before_amount NUMERIC(19,2) NOT NULL, after_amount NUMERIC(19,2) NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL, actor_id UUID NOT NULL, created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_wish_allocation_key UNIQUE(family_id,idempotency_key),
    CONSTRAINT ck_wish_allocation CHECK(amount >= 0 AND after_amount >= 0)
);
