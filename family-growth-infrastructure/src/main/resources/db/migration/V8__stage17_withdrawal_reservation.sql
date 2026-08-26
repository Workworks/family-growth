ALTER TABLE wallet ADD COLUMN reserved_money NUMERIC(19,2) NOT NULL DEFAULT 0.00;
ALTER TABLE wallet ADD CONSTRAINT ck_wallet_reserved_nonnegative CHECK (reserved_money >= 0);
ALTER TABLE wallet ADD CONSTRAINT ck_wallet_reserved_within_balance CHECK (reserved_money <= money_balance);

CREATE TABLE withdrawal_rule (
    id UUID PRIMARY KEY,
    family_id UUID NOT NULL REFERENCES family(id),
    rule_version BIGINT NOT NULL,
    payout_rate NUMERIC(19,6) NOT NULL,
    fee_rate NUMERIC(8,6) NOT NULL,
    fixed_fee NUMERIC(19,2) NOT NULL,
    active BOOLEAN NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    actor_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_withdrawal_rule_version UNIQUE (family_id, rule_version),
    CONSTRAINT uq_withdrawal_rule_key UNIQUE (family_id, idempotency_key),
    CONSTRAINT ck_withdrawal_rule_rate CHECK (payout_rate > 0),
    CONSTRAINT ck_withdrawal_rule_fee CHECK (fee_rate >= 0 AND fee_rate < 1 AND fixed_fee >= 0)
);
CREATE INDEX idx_withdrawal_rule_active ON withdrawal_rule(family_id, active);

CREATE TABLE withdrawal_quote (
    id UUID PRIMARY KEY,
    family_id UUID NOT NULL REFERENCES family(id),
    child_id UUID NOT NULL REFERENCES child_profile(id),
    money_amount NUMERIC(19,2) NOT NULL,
    payout_rate NUMERIC(19,6) NOT NULL,
    gross_payout NUMERIC(19,2) NOT NULL,
    fee_rate NUMERIC(8,6) NOT NULL,
    fixed_fee NUMERIC(19,2) NOT NULL,
    fee_amount NUMERIC(19,2) NOT NULL,
    net_payout NUMERIC(19,2) NOT NULL,
    rule_id UUID NOT NULL REFERENCES withdrawal_rule(id),
    rule_version BIGINT NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    notice VARCHAR(200) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_withdrawal_quote_key UNIQUE (family_id, idempotency_key),
    CONSTRAINT ck_withdrawal_quote_amounts CHECK (
        money_amount > 0 AND gross_payout > 0 AND fee_amount >= 0
        AND net_payout > 0 AND gross_payout = fee_amount + net_payout
    )
);
CREATE INDEX idx_withdrawal_quote_child ON withdrawal_quote(family_id, child_id, created_at);

CREATE TABLE withdrawal_request (
    id UUID PRIMARY KEY,
    quote_id UUID NOT NULL UNIQUE REFERENCES withdrawal_quote(id),
    family_id UUID NOT NULL REFERENCES family(id),
    child_id UUID NOT NULL REFERENCES child_profile(id),
    money_amount NUMERIC(19,2) NOT NULL,
    payout_rate NUMERIC(19,6) NOT NULL,
    gross_payout NUMERIC(19,2) NOT NULL,
    fee_rate NUMERIC(8,6) NOT NULL,
    fixed_fee NUMERIC(19,2) NOT NULL,
    fee_amount NUMERIC(19,2) NOT NULL,
    net_payout NUMERIC(19,2) NOT NULL,
    rule_id UUID NOT NULL REFERENCES withdrawal_rule(id),
    rule_version BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL,
    ledger_group_id UUID,
    request_key VARCHAR(100) NOT NULL,
    requested_by UUID NOT NULL,
    decided_by UUID,
    paid_by UUID,
    cancelled_by UUID,
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL,
    decided_at TIMESTAMP WITH TIME ZONE,
    paid_at TIMESTAMP WITH TIME ZONE,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_withdrawal_request_key UNIQUE (family_id, request_key),
    CONSTRAINT ck_withdrawal_request_status CHECK (status IN ('REQUESTED','APPROVED','PAID','REJECTED','CANCELLED')),
    CONSTRAINT ck_withdrawal_request_amounts CHECK (
        money_amount > 0 AND gross_payout > 0 AND fee_amount >= 0
        AND net_payout > 0 AND gross_payout = fee_amount + net_payout
    )
);
CREATE INDEX idx_withdrawal_request_child_status
    ON withdrawal_request(family_id, child_id, status, requested_at);

CREATE TABLE withdrawal_action (
    id UUID PRIMARY KEY,
    family_id UUID NOT NULL REFERENCES family(id),
    request_id UUID NOT NULL REFERENCES withdrawal_request(id),
    action VARCHAR(16) NOT NULL,
    status_after VARCHAR(16) NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    actor_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_withdrawal_action_key UNIQUE (family_id, action, idempotency_key),
    CONSTRAINT ck_withdrawal_action_name CHECK (action IN ('APPROVE','REJECT','CANCEL','MARK_PAID')),
    CONSTRAINT ck_withdrawal_action_status CHECK (status_after IN ('APPROVED','PAID','REJECTED','CANCELLED'))
);
CREATE INDEX idx_withdrawal_action_request ON withdrawal_action(family_id, request_id, created_at);
