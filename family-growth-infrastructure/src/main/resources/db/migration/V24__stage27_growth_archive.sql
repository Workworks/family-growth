ALTER TABLE growth_plan ADD COLUMN category VARCHAR(32) NOT NULL DEFAULT 'OTHER';
ALTER TABLE growth_plan ADD COLUMN age_stage VARCHAR(32) NOT NULL DEFAULT 'ALL';
ALTER TABLE growth_plan ADD COLUMN target VARCHAR(500) NOT NULL DEFAULT '';
ALTER TABLE growth_plan ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE growth_plan ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE;
UPDATE growth_plan SET updated_at=created_at WHERE updated_at IS NULL;
ALTER TABLE growth_plan ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE growth_plan ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE growth_plan ADD CONSTRAINT ck_growth_plan_category CHECK(category IN ('LEARNING','HEALTH','HABITS','RELATIONSHIP','CREATIVITY','LIFE_SKILLS','OTHER'));
ALTER TABLE growth_plan ADD CONSTRAINT ck_growth_plan_status CHECK(status IN ('DRAFT','ACTIVE','PAUSED','COMPLETED','CANCELED'));

ALTER TABLE growth_goal ADD COLUMN target VARCHAR(500) NOT NULL DEFAULT '';
ALTER TABLE growth_goal ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE growth_goal ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE;
UPDATE growth_goal SET updated_at=created_at WHERE updated_at IS NULL;
ALTER TABLE growth_goal ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE growth_goal ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE growth_goal ADD CONSTRAINT ck_growth_goal_status CHECK(status IN ('ACTIVE','COMPLETED','CANCELED'));

CREATE TABLE growth_plan_action (
 id UUID PRIMARY KEY,family_id UUID NOT NULL REFERENCES family(id),child_id UUID NOT NULL REFERENCES child_profile(id),plan_id UUID NOT NULL REFERENCES growth_plan(id),actor_id UUID NOT NULL,
 action_type VARCHAR(24) NOT NULL,old_status VARCHAR(16),new_status VARCHAR(16) NOT NULL,old_revision BIGINT NOT NULL,new_revision BIGINT NOT NULL,
 reason VARCHAR(500) NOT NULL DEFAULT '',idempotency_key VARCHAR(100) NOT NULL,payload_hash VARCHAR(64) NOT NULL,created_at TIMESTAMP WITH TIME ZONE NOT NULL,
 CONSTRAINT uq_growth_plan_action_key UNIQUE(family_id,idempotency_key)
);
CREATE INDEX idx_growth_plan_action_child ON growth_plan_action(family_id,child_id,created_at);

CREATE TABLE growth_goal_action (
 id UUID PRIMARY KEY,family_id UUID NOT NULL REFERENCES family(id),child_id UUID NOT NULL REFERENCES child_profile(id),plan_id UUID NOT NULL REFERENCES growth_plan(id),goal_id UUID NOT NULL REFERENCES growth_goal(id),actor_id UUID NOT NULL,
 action_type VARCHAR(24) NOT NULL,old_status VARCHAR(16),new_status VARCHAR(16) NOT NULL,old_revision BIGINT NOT NULL,new_revision BIGINT NOT NULL,
 reason VARCHAR(500) NOT NULL DEFAULT '',idempotency_key VARCHAR(100) NOT NULL,payload_hash VARCHAR(64) NOT NULL,created_at TIMESTAMP WITH TIME ZONE NOT NULL,
 CONSTRAINT uq_growth_goal_action_key UNIQUE(family_id,idempotency_key)
);

CREATE TABLE growth_milestone (
 id UUID PRIMARY KEY,family_id UUID NOT NULL REFERENCES family(id),child_id UUID NOT NULL REFERENCES child_profile(id),plan_id UUID REFERENCES growth_plan(id),goal_id UUID REFERENCES growth_goal(id),
 occurred_on DATE NOT NULL,category VARCHAR(32) NOT NULL,title VARCHAR(160) NOT NULL,observation VARCHAR(1500) NOT NULL,actor_id UUID NOT NULL,
 revision BIGINT NOT NULL DEFAULT 0,created_at TIMESTAMP WITH TIME ZONE NOT NULL,updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
 CONSTRAINT ck_growth_milestone_category CHECK(category IN ('PARENT_CHILD_READING','MOVEMENT','LANGUAGE','SELF_CARE','EMOTIONAL_CONNECTION','LEARNING','CREATIVITY','OTHER'))
);
CREATE INDEX idx_growth_milestone_child ON growth_milestone(family_id,child_id,occurred_on DESC,created_at DESC);

CREATE TABLE growth_milestone_action (
 id UUID PRIMARY KEY,family_id UUID NOT NULL REFERENCES family(id),child_id UUID NOT NULL REFERENCES child_profile(id),milestone_id UUID NOT NULL REFERENCES growth_milestone(id),actor_id UUID NOT NULL,
 action_type VARCHAR(16) NOT NULL,old_revision BIGINT NOT NULL,new_revision BIGINT NOT NULL,idempotency_key VARCHAR(100) NOT NULL,payload_hash VARCHAR(64) NOT NULL,created_at TIMESTAMP WITH TIME ZONE NOT NULL,
 CONSTRAINT uq_growth_milestone_action_key UNIQUE(family_id,idempotency_key)
);

CREATE TABLE growth_artifact (
 id UUID PRIMARY KEY,family_id UUID NOT NULL REFERENCES family(id),child_id UUID NOT NULL REFERENCES child_profile(id),milestone_id UUID NOT NULL REFERENCES growth_milestone(id),actor_id UUID NOT NULL,
 content_type VARCHAR(32) NOT NULL,byte_size BIGINT NOT NULL,sha256 VARCHAR(64) NOT NULL,alt_text VARCHAR(300) NOT NULL,content BYTEA NOT NULL,
 idempotency_key VARCHAR(100) NOT NULL,payload_hash VARCHAR(64) NOT NULL,created_at TIMESTAMP WITH TIME ZONE NOT NULL,
 CONSTRAINT uq_growth_artifact_key UNIQUE(family_id,idempotency_key),CONSTRAINT ck_growth_artifact_size CHECK(byte_size BETWEEN 1 AND 5242880),
 CONSTRAINT ck_growth_artifact_type CHECK(content_type IN ('image/jpeg','image/png','image/webp'))
);
CREATE INDEX idx_growth_artifact_milestone ON growth_artifact(family_id,child_id,milestone_id,created_at);
