CREATE TABLE learning_support_event (
    id UUID PRIMARY KEY,
    family_id UUID NOT NULL REFERENCES family(id),
    child_id UUID NOT NULL REFERENCES child_profile(id),
    assignment_id UUID NOT NULL REFERENCES lesson_assignment(id),
    activity_id UUID REFERENCES learning_activity(id),
    event_type VARCHAR(40) NOT NULL,
    category VARCHAR(24),
    child_message VARCHAR(160) NOT NULL DEFAULT '',
    private_note VARCHAR(500) NOT NULL DEFAULT '',
    revisit_at TIMESTAMP WITH TIME ZONE,
    parent_event_id UUID REFERENCES learning_support_event(id),
    actor_id UUID NOT NULL,
    idempotency_key VARCHAR(120) NOT NULL,
    payload_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_support_event_type CHECK (event_type IN ('HELP_REQUESTED','INCORRECT_OBSERVED','MISCONCEPTION_CLASSIFIED','REVISIT_SCHEDULED','REVISIT_COMPLETED')),
    CONSTRAINT ck_support_category CHECK (category IS NULL OR category IN ('INSTRUCTION','CONCEPT','PROCEDURE','LANGUAGE','ATTENTION','OTHER')),
    CONSTRAINT uq_support_event_key UNIQUE (family_id,idempotency_key)
);
CREATE INDEX idx_support_event_timeline ON learning_support_event(family_id,child_id,assignment_id,created_at);
CREATE INDEX idx_support_event_revisit ON learning_support_event(assignment_id,activity_id,event_type,revisit_at);

CREATE TABLE teaching_course_withdrawal (
    course_version_id UUID PRIMARY KEY REFERENCES teaching_course_version(id),
    family_id UUID NOT NULL REFERENCES family(id),
    reason VARCHAR(500) NOT NULL,
    actor_id UUID NOT NULL REFERENCES parent_profile(id),
    idempotency_key VARCHAR(120) NOT NULL,
    payload_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_course_withdrawal_key UNIQUE (family_id,idempotency_key)
);
