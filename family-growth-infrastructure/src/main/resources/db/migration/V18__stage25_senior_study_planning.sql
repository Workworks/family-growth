CREATE TABLE senior_module_config (
    child_id UUID PRIMARY KEY REFERENCES child_profile(id),
    family_id UUID NOT NULL REFERENCES family(id),
    revision BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_senior_module_revision CHECK (revision >= 0)
);

CREATE TABLE senior_module_selection (
    id UUID PRIMARY KEY,
    family_id UUID NOT NULL REFERENCES family(id),
    child_id UUID NOT NULL REFERENCES child_profile(id),
    subject_code VARCHAR(40) NOT NULL,
    module_type VARCHAR(24) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_senior_module_type CHECK (module_type IN ('REQUIRED','SELECTIVE_REQUIRED','ELECTIVE')),
    CONSTRAINT uq_senior_module_selection UNIQUE (child_id,subject_code,module_type)
);
CREATE INDEX idx_senior_module_child ON senior_module_selection(family_id,child_id,subject_code);

CREATE TABLE senior_module_action (
    id UUID PRIMARY KEY,
    family_id UUID NOT NULL REFERENCES family(id),
    child_id UUID NOT NULL REFERENCES child_profile(id),
    actor_id UUID NOT NULL,
    old_revision BIGINT NOT NULL,
    new_revision BIGINT NOT NULL,
    old_snapshot VARCHAR(4000) NOT NULL,
    new_snapshot VARCHAR(4000) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    payload_hash CHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_senior_module_action_key UNIQUE (family_id,idempotency_key)
);

CREATE TABLE senior_weekly_goal (
    id UUID PRIMARY KEY,
    family_id UUID NOT NULL REFERENCES family(id),
    child_id UUID NOT NULL REFERENCES child_profile(id),
    assignment_id UUID REFERENCES lesson_assignment(id),
    subject_code VARCHAR(40) NOT NULL,
    module_type VARCHAR(24) NOT NULL,
    week_start DATE NOT NULL,
    title VARCHAR(160) NOT NULL,
    evidence_target VARCHAR(500) NOT NULL,
    next_action VARCHAR(500) NOT NULL,
    status VARCHAR(16) NOT NULL,
    revision BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_senior_goal_module_type CHECK (module_type IN ('REQUIRED','SELECTIVE_REQUIRED','ELECTIVE')),
    CONSTRAINT ck_senior_goal_status CHECK (status IN ('ACTIVE','ARCHIVED')),
    CONSTRAINT ck_senior_goal_revision CHECK (revision >= 0)
);
CREATE INDEX idx_senior_goal_child_week ON senior_weekly_goal(family_id,child_id,week_start,status);

CREATE TABLE senior_goal_action (
    id UUID PRIMARY KEY,
    family_id UUID NOT NULL REFERENCES family(id),
    child_id UUID NOT NULL REFERENCES child_profile(id),
    goal_id UUID NOT NULL REFERENCES senior_weekly_goal(id),
    actor_id UUID NOT NULL,
    action_type VARCHAR(16) NOT NULL,
    old_revision BIGINT NOT NULL,
    new_revision BIGINT NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    payload_hash CHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_senior_goal_action CHECK (action_type IN ('CREATE','UPDATE','ARCHIVE')),
    CONSTRAINT uq_senior_goal_action_key UNIQUE (family_id,idempotency_key)
);

CREATE TABLE senior_reflection (
    id UUID PRIMARY KEY,
    family_id UUID NOT NULL REFERENCES family(id),
    child_id UUID NOT NULL REFERENCES child_profile(id),
    goal_id UUID REFERENCES senior_weekly_goal(id),
    assignment_id UUID REFERENCES lesson_assignment(id),
    evidence_summary VARCHAR(1000) NOT NULL,
    strategy VARCHAR(32) NOT NULL,
    next_action VARCHAR(500) NOT NULL,
    support_requested BOOLEAN NOT NULL DEFAULT FALSE,
    actor_id UUID NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    payload_hash CHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_senior_reflection_strategy CHECK (strategy IN ('CONTINUE','REVIEW_FOUNDATION','TRY_ANOTHER_METHOD','ASK_FOR_SUPPORT','PAUSE_AND_REPLAN')),
    CONSTRAINT uq_senior_reflection_key UNIQUE (family_id,idempotency_key)
);
CREATE INDEX idx_senior_reflection_child ON senior_reflection(family_id,child_id,created_at);
