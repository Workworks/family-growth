CREATE TABLE reward_budget_rule (
 id UUID PRIMARY KEY,family_id UUID NOT NULL REFERENCES family(id),child_id UUID NOT NULL REFERENCES child_profile(id),rule_version BIGINT NOT NULL,
 zone_id VARCHAR(64) NOT NULL,daily_money_limit NUMERIC(19,2) NOT NULL,weekly_money_limit NUMERIC(19,2) NOT NULL,monthly_money_limit NUMERIC(19,2) NOT NULL,
 overflow_policy VARCHAR(32) NOT NULL,excess_coin_per_money NUMERIC(19,6) NOT NULL,excess_xp_per_money NUMERIC(19,6) NOT NULL,
 active BOOLEAN NOT NULL,actor_id UUID NOT NULL,created_at TIMESTAMP WITH TIME ZONE NOT NULL,
 CONSTRAINT uq_reward_budget_rule_version UNIQUE(family_id,child_id,rule_version),
 CONSTRAINT ck_reward_budget_limits CHECK(daily_money_limit>=0 AND weekly_money_limit>=daily_money_limit AND monthly_money_limit>=weekly_money_limit),
 CONSTRAINT ck_reward_budget_policy CHECK(overflow_policy IN('HOLD_FOR_PARENT','CONVERT_TO_COIN','CONVERT_TO_XP')),
 CONSTRAINT ck_reward_budget_rates CHECK(excess_coin_per_money>=0 AND excess_xp_per_money>=0)
);
CREATE INDEX idx_reward_budget_active ON reward_budget_rule(family_id,child_id,active);

CREATE TABLE reward_budget_rule_action (
 id UUID PRIMARY KEY,family_id UUID NOT NULL REFERENCES family(id),child_id UUID NOT NULL REFERENCES child_profile(id),rule_id UUID NOT NULL REFERENCES reward_budget_rule(id),
 actor_id UUID NOT NULL,action_type VARCHAR(24) NOT NULL,idempotency_key VARCHAR(100) NOT NULL,payload_hash VARCHAR(64) NOT NULL,created_at TIMESTAMP WITH TIME ZONE NOT NULL,
 CONSTRAINT uq_reward_budget_rule_action_key UNIQUE(family_id,idempotency_key)
);

CREATE TABLE reward_budget_override (
 id UUID PRIMARY KEY,family_id UUID NOT NULL REFERENCES family(id),child_id UUID NOT NULL REFERENCES child_profile(id),completion_id UUID NOT NULL REFERENCES task_completion(id),
 proposed_money NUMERIC(19,2) NOT NULL,proposed_coin BIGINT NOT NULL,proposed_xp BIGINT NOT NULL,reward_hash VARCHAR(64) NOT NULL,reason VARCHAR(500) NOT NULL,
 actor_id UUID NOT NULL,idempotency_key VARCHAR(100) NOT NULL,created_at TIMESTAMP WITH TIME ZONE NOT NULL,consumed_at TIMESTAMP WITH TIME ZONE,decision_id UUID,
 CONSTRAINT uq_reward_budget_override_key UNIQUE(family_id,idempotency_key),
 CONSTRAINT ck_reward_budget_override_values CHECK(proposed_money>=0 AND proposed_coin>=0 AND proposed_xp>=0)
);
CREATE INDEX idx_reward_budget_override_completion ON reward_budget_override(family_id,child_id,completion_id,consumed_at);

CREATE TABLE reward_budget_decision (
 id UUID PRIMARY KEY,family_id UUID NOT NULL REFERENCES family(id),child_id UUID NOT NULL REFERENCES child_profile(id),completion_id UUID NOT NULL REFERENCES task_completion(id),
 rule_id UUID REFERENCES reward_budget_rule(id),rule_version BIGINT,overflow_policy VARCHAR(32) NOT NULL,
 proposed_money NUMERIC(19,2) NOT NULL,proposed_coin BIGINT NOT NULL,proposed_xp BIGINT NOT NULL,
 actual_money NUMERIC(19,2) NOT NULL,actual_coin BIGINT NOT NULL,actual_xp BIGINT NOT NULL,overflow_money NUMERIC(19,2) NOT NULL,
 override_id UUID REFERENCES reward_budget_override(id),actor_id UUID NOT NULL,review_key VARCHAR(100) NOT NULL,created_at TIMESTAMP WITH TIME ZONE NOT NULL,
 CONSTRAINT uq_reward_budget_decision_review UNIQUE(family_id,review_key),
 CONSTRAINT ck_reward_budget_decision_values CHECK(proposed_money>=0 AND proposed_coin>=0 AND proposed_xp>=0 AND actual_money>=0 AND actual_coin>=0 AND actual_xp>=0 AND overflow_money>=0)
);

CREATE TABLE exchange_control_rule (
 id UUID PRIMARY KEY,family_id UUID NOT NULL REFERENCES family(id),rule_version BIGINT NOT NULL,
 money_to_coin_enabled BOOLEAN NOT NULL,coin_to_money_enabled BOOLEAN NOT NULL,
 money_to_coin_daily_limit NUMERIC(19,2) NOT NULL,money_to_coin_monthly_limit NUMERIC(19,2) NOT NULL,
 coin_to_money_daily_limit NUMERIC(19,2) NOT NULL,coin_to_money_monthly_limit NUMERIC(19,2) NOT NULL,
 child_requires_parent_approval BOOLEAN NOT NULL,zone_id VARCHAR(64) NOT NULL,active BOOLEAN NOT NULL,actor_id UUID NOT NULL,created_at TIMESTAMP WITH TIME ZONE NOT NULL,
 CONSTRAINT uq_exchange_control_rule_version UNIQUE(family_id,rule_version),
 CONSTRAINT ck_exchange_control_limits CHECK(money_to_coin_daily_limit>=0 AND money_to_coin_monthly_limit>=money_to_coin_daily_limit AND coin_to_money_daily_limit>=0 AND coin_to_money_monthly_limit>=coin_to_money_daily_limit)
);
CREATE INDEX idx_exchange_control_active ON exchange_control_rule(family_id,active);

CREATE TABLE exchange_quota_usage (
 family_id UUID NOT NULL REFERENCES family(id),child_id UUID NOT NULL REFERENCES child_profile(id),direction VARCHAR(24) NOT NULL,
 period_type VARCHAR(8) NOT NULL,period_start DATE NOT NULL,used_source NUMERIC(19,2) NOT NULL,updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
 PRIMARY KEY(family_id,child_id,direction,period_type,period_start),
 CONSTRAINT ck_exchange_quota_direction CHECK(direction IN('MONEY_TO_COIN','COIN_TO_MONEY')),
 CONSTRAINT ck_exchange_quota_period CHECK(period_type IN('DAY','MONTH')),
 CONSTRAINT ck_exchange_quota_used CHECK(used_source>=0)
);

CREATE TABLE exchange_control_action (
 id UUID PRIMARY KEY,family_id UUID NOT NULL REFERENCES family(id),rule_id UUID NOT NULL REFERENCES exchange_control_rule(id),actor_id UUID NOT NULL,
 action_type VARCHAR(24) NOT NULL,idempotency_key VARCHAR(100) NOT NULL,payload_hash VARCHAR(64) NOT NULL,created_at TIMESTAMP WITH TIME ZONE NOT NULL,
 CONSTRAINT uq_exchange_control_action_key UNIQUE(family_id,idempotency_key)
);

ALTER TABLE exchange_preview ADD COLUMN control_rule_id UUID REFERENCES exchange_control_rule(id);
ALTER TABLE exchange_preview ADD COLUMN control_rule_version BIGINT;

CREATE TABLE exchange_approval_request (
 id UUID PRIMARY KEY,family_id UUID NOT NULL REFERENCES family(id),child_id UUID NOT NULL REFERENCES child_profile(id),preview_id UUID NOT NULL UNIQUE REFERENCES exchange_preview(id),
 status VARCHAR(16) NOT NULL,submit_key VARCHAR(100) NOT NULL,submitted_by UUID NOT NULL,review_key VARCHAR(100),reviewed_by UUID,review_note VARCHAR(500) NOT NULL DEFAULT '',
 order_id UUID REFERENCES exchange_order(id),created_at TIMESTAMP WITH TIME ZONE NOT NULL,reviewed_at TIMESTAMP WITH TIME ZONE,
 CONSTRAINT uq_exchange_approval_submit UNIQUE(family_id,submit_key),CONSTRAINT uq_exchange_approval_review UNIQUE(family_id,review_key),
 CONSTRAINT ck_exchange_approval_status CHECK(status IN('PENDING','APPROVED','REJECTED'))
);
CREATE INDEX idx_exchange_approval_child ON exchange_approval_request(family_id,child_id,status,created_at);

ALTER TABLE reward_order DROP CONSTRAINT ck_reward_order_status;
ALTER TABLE reward_order ADD CONSTRAINT ck_reward_order_status CHECK(status IN('CREATED','APPROVED','REJECTED','CANCELED','FULFILLED'));
ALTER TABLE reward_order ADD COLUMN fulfillment_note VARCHAR(500) NOT NULL DEFAULT '';
ALTER TABLE reward_order ADD COLUMN fulfill_key VARCHAR(100);
ALTER TABLE reward_order ADD COLUMN fulfilled_by UUID;
ALTER TABLE reward_order ADD COLUMN fulfilled_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE reward_order ADD CONSTRAINT uq_reward_order_fulfill UNIQUE(family_id,fulfill_key);

CREATE TABLE reward_order_action (
 id UUID PRIMARY KEY,family_id UUID NOT NULL REFERENCES family(id),child_id UUID NOT NULL REFERENCES child_profile(id),order_id UUID NOT NULL REFERENCES reward_order(id),actor_id UUID NOT NULL,
 action_type VARCHAR(24) NOT NULL,old_status VARCHAR(16) NOT NULL,new_status VARCHAR(16) NOT NULL,note VARCHAR(500) NOT NULL,idempotency_key VARCHAR(100) NOT NULL,created_at TIMESTAMP WITH TIME ZONE NOT NULL,
 CONSTRAINT uq_reward_order_action_key UNIQUE(family_id,idempotency_key)
);
