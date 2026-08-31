ALTER TABLE usage_policy ADD COLUMN rest_minutes INTEGER NOT NULL DEFAULT 10;
ALTER TABLE usage_policy ADD CONSTRAINT ck_usage_policy_rest CHECK(rest_minutes BETWEEN 5 AND 60);
ALTER TABLE usage_policy_action ADD COLUMN rest_minutes INTEGER NOT NULL DEFAULT 10;

CREATE TABLE usage_session_state (
 child_id UUID PRIMARY KEY REFERENCES child_profile(id),family_id UUID NOT NULL REFERENCES family(id),
 session_minutes INTEGER NOT NULL DEFAULT 0,last_activity_at TIMESTAMP WITH TIME ZONE,
 rest_until TIMESTAMP WITH TIME ZONE,version BIGINT NOT NULL DEFAULT 0,updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
 CONSTRAINT ck_usage_session_minutes CHECK(session_minutes>=0)
);
CREATE INDEX idx_usage_session_family ON usage_session_state(family_id,child_id);

CREATE TABLE child_data_retention_policy (
 child_id UUID PRIMARY KEY REFERENCES child_profile(id),family_id UUID NOT NULL REFERENCES family(id),
 usage_detail_days INTEGER NOT NULL DEFAULT 90,version BIGINT NOT NULL DEFAULT 0,
 updated_by UUID NOT NULL REFERENCES parent_profile(id),created_at TIMESTAMP WITH TIME ZONE NOT NULL,
 updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
 CONSTRAINT ck_retention_usage_days CHECK(usage_detail_days BETWEEN 30 AND 365)
);
CREATE TABLE child_data_retention_run (
 id UUID PRIMARY KEY,family_id UUID NOT NULL REFERENCES family(id),child_id UUID NOT NULL REFERENCES child_profile(id),
 trigger_type VARCHAR(16) NOT NULL,actor_id UUID,usage_events_deleted INTEGER NOT NULL,
 allowances_redacted INTEGER NOT NULL,expired_tokens_cleared INTEGER NOT NULL,cutoff_at TIMESTAMP WITH TIME ZONE NOT NULL,
 created_at TIMESTAMP WITH TIME ZONE NOT NULL,
 CONSTRAINT ck_retention_trigger CHECK(trigger_type IN('SCHEDULED','PARENT')),
 CONSTRAINT ck_retention_counts CHECK(usage_events_deleted>=0 AND allowances_redacted>=0 AND expired_tokens_cleared>=0)
);
CREATE INDEX idx_retention_run_child ON child_data_retention_run(family_id,child_id,created_at);

CREATE TABLE child_data_retention_policy_action (
 id UUID PRIMARY KEY,family_id UUID NOT NULL REFERENCES family(id),child_id UUID NOT NULL REFERENCES child_profile(id),
 actor_id UUID NOT NULL REFERENCES parent_profile(id),old_usage_detail_days INTEGER NOT NULL,
 new_usage_detail_days INTEGER NOT NULL,reason VARCHAR(240) NOT NULL,created_at TIMESTAMP WITH TIME ZONE NOT NULL,
 CONSTRAINT ck_retention_action_days CHECK(old_usage_detail_days BETWEEN 30 AND 365 AND new_usage_detail_days BETWEEN 30 AND 365),
 CONSTRAINT ck_retention_action_reason CHECK(CHAR_LENGTH(TRIM(reason))>0)
);
CREATE INDEX idx_retention_policy_action_child ON child_data_retention_policy_action(family_id,child_id,created_at);
