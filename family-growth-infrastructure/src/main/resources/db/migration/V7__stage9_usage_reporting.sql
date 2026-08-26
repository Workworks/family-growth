CREATE TABLE usage_policy(
 child_id UUID PRIMARY KEY REFERENCES child_profile(id),family_id UUID NOT NULL REFERENCES family(id),zone_id VARCHAR(60) NOT NULL,
 daily_limit_minutes INTEGER NOT NULL,session_limit_minutes INTEGER NOT NULL,version BIGINT NOT NULL DEFAULT 0,
 actor_id UUID NOT NULL,created_at TIMESTAMP WITH TIME ZONE NOT NULL,updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
 CONSTRAINT ck_usage_policy CHECK(daily_limit_minutes BETWEEN 10 AND 480 AND session_limit_minutes BETWEEN 5 AND 240 AND session_limit_minutes<=daily_limit_minutes)
);
CREATE TABLE usage_event(
 id UUID PRIMARY KEY,family_id UUID NOT NULL REFERENCES family(id),child_id UUID NOT NULL REFERENCES child_profile(id),event_type VARCHAR(20) NOT NULL,
 minutes INTEGER NOT NULL,occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,idempotency_key VARCHAR(100) NOT NULL,actor_id UUID NOT NULL,created_at TIMESTAMP WITH TIME ZONE NOT NULL,
 CONSTRAINT uq_usage_event_key UNIQUE(family_id,idempotency_key),CONSTRAINT ck_usage_event_type CHECK(event_type IN('APP_ACTIVE','LEARNING')),CONSTRAINT ck_usage_event_minutes CHECK(minutes BETWEEN 1 AND 60)
);
CREATE INDEX idx_usage_event_child_time ON usage_event(family_id,child_id,occurred_at);
