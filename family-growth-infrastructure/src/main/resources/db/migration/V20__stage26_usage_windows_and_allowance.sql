ALTER TABLE usage_policy ADD COLUMN quiet_start TIME NOT NULL DEFAULT '21:30:00';
ALTER TABLE usage_policy ADD COLUMN quiet_end TIME NOT NULL DEFAULT '06:30:00';

CREATE TABLE usage_policy_action (
 id UUID PRIMARY KEY,family_id UUID NOT NULL REFERENCES family(id),child_id UUID NOT NULL REFERENCES child_profile(id),
 actor_id UUID NOT NULL REFERENCES parent_profile(id),daily_limit_minutes INTEGER NOT NULL,session_limit_minutes INTEGER NOT NULL,
 zone_id VARCHAR(60) NOT NULL,quiet_start TIME NOT NULL,quiet_end TIME NOT NULL,created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_usage_policy_action_child ON usage_policy_action(family_id,child_id,created_at);

CREATE TABLE usage_temporary_allowance (
 id UUID PRIMARY KEY,family_id UUID NOT NULL REFERENCES family(id),child_id UUID NOT NULL REFERENCES child_profile(id),
 actor_id UUID NOT NULL REFERENCES parent_profile(id),reason VARCHAR(240) NOT NULL,starts_at TIMESTAMP WITH TIME ZONE NOT NULL,
 expires_at TIMESTAMP WITH TIME ZONE NOT NULL,idempotency_key VARCHAR(120) NOT NULL,payload_hash VARCHAR(64) NOT NULL,
 created_at TIMESTAMP WITH TIME ZONE NOT NULL,
 CONSTRAINT uq_usage_allowance_key UNIQUE(family_id,idempotency_key),
 CONSTRAINT ck_usage_allowance_reason CHECK(CHAR_LENGTH(TRIM(reason))>0),
 CONSTRAINT ck_usage_allowance_time CHECK(expires_at>starts_at)
);
CREATE INDEX idx_usage_allowance_child_time ON usage_temporary_allowance(family_id,child_id,starts_at,expires_at);
