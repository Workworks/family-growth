CREATE TABLE parent_pin_credential (
    parent_id UUID PRIMARY KEY REFERENCES parent_profile(id),
    family_id UUID NOT NULL REFERENCES family(id),
    pin_hash VARCHAR(100) NOT NULL,
    failed_attempts INTEGER NOT NULL DEFAULT 0,
    locked_until TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_pin_failures CHECK (failed_attempts >= 0)
);
CREATE INDEX idx_pin_family ON parent_pin_credential(family_id, parent_id);

CREATE TABLE auth_session (
    id UUID PRIMARY KEY,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    family_id UUID NOT NULL REFERENCES family(id),
    actor_id UUID NOT NULL,
    actor_role VARCHAR(16) NOT NULL,
    child_id UUID REFERENCES child_profile(id),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_session_role CHECK (actor_role IN ('PARENT', 'CHILD')),
    CONSTRAINT ck_session_child_scope CHECK (
        (actor_role = 'PARENT' AND child_id IS NULL) OR
        (actor_role = 'CHILD' AND child_id IS NOT NULL)
    )
);
CREATE INDEX idx_session_active ON auth_session(token_hash, expires_at, revoked_at);

CREATE TABLE child_progress (
    child_id UUID PRIMARY KEY REFERENCES child_profile(id),
    family_id UUID NOT NULL REFERENCES family(id),
    xp_balance BIGINT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_progress_xp CHECK (xp_balance >= 0)
);

CREATE TABLE wallet (
    child_id UUID PRIMARY KEY REFERENCES child_profile(id),
    family_id UUID NOT NULL REFERENCES family(id),
    money_balance NUMERIC(19,2) NOT NULL DEFAULT 0.00,
    coin_balance BIGINT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_wallet_money_nonnegative CHECK (money_balance >= 0),
    CONSTRAINT ck_wallet_coin_nonnegative CHECK (coin_balance >= 0)
);
CREATE INDEX idx_wallet_family_child ON wallet(family_id, child_id);

CREATE TABLE task_completion (
    id UUID PRIMARY KEY,
    family_id UUID NOT NULL REFERENCES family(id),
    child_id UUID NOT NULL REFERENCES child_profile(id),
    task_id UUID NOT NULL REFERENCES growth_task(id),
    status VARCHAR(16) NOT NULL,
    evidence_note VARCHAR(1000) NOT NULL DEFAULT '',
    submitted_by UUID NOT NULL,
    reviewed_by UUID,
    review_note VARCHAR(1000) NOT NULL DEFAULT '',
    xp_reward BIGINT NOT NULL DEFAULT 0,
    coin_reward BIGINT NOT NULL DEFAULT 0,
    money_reward NUMERIC(19,2) NOT NULL DEFAULT 0.00,
    ledger_group_id UUID,
    submit_idempotency_key VARCHAR(100) NOT NULL,
    review_idempotency_key VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    submitted_at TIMESTAMP WITH TIME ZONE NOT NULL,
    reviewed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_completion_submit_key UNIQUE (family_id, submit_idempotency_key),
    CONSTRAINT uq_completion_review_key UNIQUE (family_id, review_idempotency_key),
    CONSTRAINT ck_completion_status CHECK (status IN ('SUBMITTED', 'APPROVED', 'REJECTED')),
    CONSTRAINT ck_completion_rewards CHECK (xp_reward >= 0 AND coin_reward >= 0 AND money_reward >= 0)
);
CREATE INDEX idx_completion_child_status ON task_completion(family_id, child_id, status, submitted_at);

CREATE TABLE ledger_entry (
    id UUID PRIMARY KEY,
    family_id UUID NOT NULL REFERENCES family(id),
    child_id UUID NOT NULL REFERENCES child_profile(id),
    asset_type VARCHAR(16) NOT NULL,
    delta NUMERIC(19,2) NOT NULL,
    before_balance NUMERIC(19,2) NOT NULL,
    after_balance NUMERIC(19,2) NOT NULL,
    entry_type VARCHAR(40) NOT NULL,
    business_type VARCHAR(40) NOT NULL,
    business_id UUID NOT NULL,
    group_id UUID NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    actor_id UUID NOT NULL,
    reason VARCHAR(500) NOT NULL DEFAULT '',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_ledger_business_asset UNIQUE (family_id, business_type, business_id, asset_type),
    CONSTRAINT ck_ledger_asset CHECK (asset_type IN ('MONEY', 'COIN')),
    CONSTRAINT ck_ledger_arithmetic CHECK (after_balance = before_balance + delta),
    CONSTRAINT ck_ledger_nonnegative CHECK (after_balance >= 0),
    CONSTRAINT ck_ledger_coin_integer CHECK (asset_type <> 'COIN' OR delta = TRUNC(delta))
);
CREATE INDEX idx_ledger_child_time ON ledger_entry(family_id, child_id, created_at);

CREATE TABLE idempotency_operation (
    id UUID PRIMARY KEY,
    family_id UUID NOT NULL REFERENCES family(id),
    operation_type VARCHAR(50) NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    result_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_idempotency_operation UNIQUE (family_id, operation_type, idempotency_key)
);
