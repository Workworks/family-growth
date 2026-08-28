CREATE TABLE child_experience_profile (
    child_id UUID PRIMARY KEY REFERENCES child_profile(id),
    family_id UUID NOT NULL REFERENCES family(id),
    stage_override VARCHAR(32),
    override_reason VARCHAR(240) NOT NULL DEFAULT '',
    haptics_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    updated_by UUID NOT NULL REFERENCES parent_profile(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_experience_stage_override CHECK (stage_override IS NULL OR stage_override IN ('KINDERGARTEN','PRIMARY','JUNIOR_MIDDLE','SENIOR_HIGH')),
    CONSTRAINT ck_experience_override_reason CHECK (stage_override IS NULL OR CHAR_LENGTH(TRIM(override_reason)) > 0),
    CONSTRAINT uq_experience_family_child UNIQUE (family_id, child_id)
);
CREATE INDEX idx_experience_family ON child_experience_profile(family_id, child_id);

CREATE TABLE child_experience_audit (
    id UUID PRIMARY KEY,
    family_id UUID NOT NULL REFERENCES family(id),
    child_id UUID NOT NULL REFERENCES child_profile(id),
    actor_id UUID NOT NULL REFERENCES parent_profile(id),
    old_birth_date DATE NOT NULL,
    new_birth_date DATE NOT NULL,
    old_stage_override VARCHAR(32),
    new_stage_override VARCHAR(32),
    old_haptics_enabled BOOLEAN NOT NULL,
    new_haptics_enabled BOOLEAN NOT NULL,
    reason VARCHAR(240) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_experience_audit_old_stage CHECK (old_stage_override IS NULL OR old_stage_override IN ('KINDERGARTEN','PRIMARY','JUNIOR_MIDDLE','SENIOR_HIGH')),
    CONSTRAINT ck_experience_audit_new_stage CHECK (new_stage_override IS NULL OR new_stage_override IN ('KINDERGARTEN','PRIMARY','JUNIOR_MIDDLE','SENIOR_HIGH')),
    CONSTRAINT ck_experience_audit_reason CHECK (CHAR_LENGTH(TRIM(reason)) > 0)
);
CREATE INDEX idx_experience_audit_child ON child_experience_audit(family_id, child_id, created_at);

CREATE TABLE documentary_source (
    id UUID PRIMARY KEY,
    family_id UUID NOT NULL REFERENCES family(id),
    school_stage VARCHAR(32) NOT NULL,
    title VARCHAR(160) NOT NULL,
    description VARCHAR(1000) NOT NULL DEFAULT '',
    language_tag VARCHAR(35) NOT NULL,
    duration_seconds INTEGER,
    access_mode VARCHAR(32) NOT NULL,
    source_reference VARCHAR(1000) NOT NULL,
    rights_holder VARCHAR(240) NOT NULL,
    rights_reference VARCHAR(1000) NOT NULL,
    license_expires_on DATE,
    status VARCHAR(16) NOT NULL,
    idempotency_key VARCHAR(120) NOT NULL,
    payload_hash VARCHAR(64) NOT NULL,
    created_by UUID NOT NULL REFERENCES parent_profile(id),
    updated_by UUID NOT NULL REFERENCES parent_profile(id),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_documentary_school_stage CHECK (school_stage IN ('KINDERGARTEN','PRIMARY','JUNIOR_MIDDLE','SENIOR_HIGH')),
    CONSTRAINT ck_documentary_access_mode CHECK (access_mode IN ('ORIGINAL_OFFLINE','LICENSED_OFFLINE','OFFICIAL_LINK')),
    CONSTRAINT ck_documentary_status CHECK (status IN ('DRAFT','APPROVED','WITHDRAWN')),
    CONSTRAINT ck_documentary_duration CHECK (duration_seconds IS NULL OR duration_seconds BETWEEN 10 AND 14400),
    CONSTRAINT uq_documentary_key UNIQUE (family_id, idempotency_key)
);
CREATE INDEX idx_documentary_family_stage ON documentary_source(family_id, school_stage, status, created_at);

CREATE TABLE documentary_source_action (
    id UUID PRIMARY KEY,
    family_id UUID NOT NULL REFERENCES family(id),
    source_id UUID NOT NULL REFERENCES documentary_source(id),
    actor_id UUID NOT NULL REFERENCES parent_profile(id),
    old_status VARCHAR(16) NOT NULL,
    new_status VARCHAR(16) NOT NULL,
    idempotency_key VARCHAR(120) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_documentary_action_old CHECK (old_status IN ('DRAFT','APPROVED','WITHDRAWN')),
    CONSTRAINT ck_documentary_action_new CHECK (new_status IN ('APPROVED','WITHDRAWN')),
    CONSTRAINT uq_documentary_action_key UNIQUE (family_id, idempotency_key)
);
CREATE INDEX idx_documentary_action_source ON documentary_source_action(family_id, source_id, created_at);
