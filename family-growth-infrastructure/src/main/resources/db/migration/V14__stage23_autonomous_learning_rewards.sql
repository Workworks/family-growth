CREATE TABLE autonomous_learning_reward_policy (
    child_id UUID PRIMARY KEY REFERENCES child_profile(id),
    family_id UUID NOT NULL REFERENCES family(id),
    money_reward NUMERIC(19,2) NOT NULL DEFAULT 0.00,
    coin_reward BIGINT NOT NULL DEFAULT 0,
    xp_reward BIGINT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    updated_by UUID NOT NULL REFERENCES parent_profile(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_autonomous_reward_nonnegative CHECK (money_reward >= 0 AND coin_reward >= 0 AND xp_reward >= 0),
    CONSTRAINT ck_autonomous_reward_limits CHECK (money_reward <= 10000.00 AND coin_reward <= 1000000 AND xp_reward <= 1000000),
    CONSTRAINT uq_autonomous_reward_family_child UNIQUE (family_id,child_id)
);

CREATE TABLE autonomous_learning_reward_audit (
    id UUID PRIMARY KEY,
    family_id UUID NOT NULL REFERENCES family(id),
    child_id UUID NOT NULL REFERENCES child_profile(id),
    old_money_reward NUMERIC(19,2) NOT NULL,
    new_money_reward NUMERIC(19,2) NOT NULL,
    old_coin_reward BIGINT NOT NULL,
    new_coin_reward BIGINT NOT NULL,
    old_xp_reward BIGINT NOT NULL,
    new_xp_reward BIGINT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    actor_id UUID NOT NULL REFERENCES parent_profile(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_autonomous_reward_audit_reason CHECK (CHAR_LENGTH(TRIM(reason)) > 0)
);
CREATE INDEX idx_autonomous_reward_audit_child ON autonomous_learning_reward_audit(family_id,child_id,created_at);

ALTER TABLE lesson_assignment ADD COLUMN assignment_source VARCHAR(16) NOT NULL DEFAULT 'PARENT';
ALTER TABLE lesson_assignment ADD COLUMN money_reward_snapshot NUMERIC(19,2) NOT NULL DEFAULT 0.00;
ALTER TABLE lesson_assignment ADD COLUMN coin_reward_snapshot BIGINT NOT NULL DEFAULT 0;
ALTER TABLE lesson_assignment ADD COLUMN xp_reward_snapshot BIGINT NOT NULL DEFAULT 0;
ALTER TABLE lesson_assignment ADD COLUMN reward_settled_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE lesson_assignment ADD COLUMN reward_group_id UUID;
ALTER TABLE lesson_assignment ADD CONSTRAINT ck_lesson_assignment_source CHECK (assignment_source IN ('PARENT','AUTONOMOUS'));
ALTER TABLE lesson_assignment ADD CONSTRAINT ck_lesson_assignment_rewards CHECK (money_reward_snapshot >= 0 AND coin_reward_snapshot >= 0 AND xp_reward_snapshot >= 0);

CREATE TABLE autonomous_enrollment_action (
    id UUID PRIMARY KEY,
    family_id UUID NOT NULL REFERENCES family(id),
    child_id UUID NOT NULL REFERENCES child_profile(id),
    school_stage VARCHAR(32) NOT NULL,
    actor_id UUID NOT NULL,
    payload_hash VARCHAR(64) NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    created_count INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_autonomous_enrollment_key UNIQUE (family_id,idempotency_key),
    CONSTRAINT ck_autonomous_enrollment_count CHECK (created_count >= 0)
);
