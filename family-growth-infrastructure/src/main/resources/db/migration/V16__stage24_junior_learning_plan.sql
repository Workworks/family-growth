CREATE TABLE junior_learning_plan (
    child_id UUID PRIMARY KEY REFERENCES child_profile(id),
    family_id UUID NOT NULL REFERENCES family(id),
    revision BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_junior_plan_revision CHECK (revision >= 0)
);

CREATE TABLE junior_learning_plan_item (
    assignment_id UUID PRIMARY KEY REFERENCES lesson_assignment(id),
    family_id UUID NOT NULL REFERENCES family(id),
    child_id UUID NOT NULL REFERENCES child_profile(id),
    position INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_junior_plan_position CHECK (position >= 0),
    CONSTRAINT uq_junior_plan_position UNIQUE (child_id,position)
);
CREATE INDEX idx_junior_plan_item_child ON junior_learning_plan_item(family_id,child_id,position);

CREATE TABLE junior_learning_plan_action (
    id UUID PRIMARY KEY,
    family_id UUID NOT NULL REFERENCES family(id),
    child_id UUID NOT NULL REFERENCES child_profile(id),
    assignment_id UUID NOT NULL REFERENCES lesson_assignment(id),
    actor_id UUID NOT NULL,
    direction VARCHAR(8) NOT NULL,
    old_position INTEGER NOT NULL,
    new_position INTEGER NOT NULL,
    old_revision BIGINT NOT NULL,
    new_revision BIGINT NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    payload_hash CHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_junior_plan_direction CHECK (direction IN ('UP','DOWN')),
    CONSTRAINT uq_junior_plan_action_key UNIQUE (family_id,idempotency_key)
);
CREATE INDEX idx_junior_plan_action_child ON junior_learning_plan_action(family_id,child_id,created_at);
