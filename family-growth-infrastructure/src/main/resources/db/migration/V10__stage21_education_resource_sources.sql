CREATE TABLE education_resource_source (
    id UUID PRIMARY KEY,
    family_id UUID NOT NULL REFERENCES family(id),
    title VARCHAR(160) NOT NULL,
    source_url VARCHAR(1000) NOT NULL,
    usage_note VARCHAR(500) NOT NULL,
    status VARCHAR(16) NOT NULL,
    refresh_status VARCHAR(16) NOT NULL,
    refresh_error VARCHAR(240) NOT NULL DEFAULT '',
    last_refreshed_at TIMESTAMP WITH TIME ZONE,
    idempotency_key VARCHAR(120) NOT NULL,
    payload_hash VARCHAR(64) NOT NULL,
    created_by UUID NOT NULL REFERENCES parent_profile(id),
    updated_by UUID NOT NULL REFERENCES parent_profile(id),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_resource_source_status CHECK (status IN ('DRAFT','APPROVED','WITHDRAWN')),
    CONSTRAINT ck_resource_refresh_status CHECK (refresh_status IN ('NEVER','READY','FAILED')),
    CONSTRAINT uq_resource_source_key UNIQUE (family_id, idempotency_key),
    CONSTRAINT uq_resource_source_url UNIQUE (family_id, source_url)
);
CREATE INDEX idx_resource_source_family ON education_resource_source(family_id, status, updated_at);

CREATE TABLE education_resource_source_stage (
    source_id UUID NOT NULL REFERENCES education_resource_source(id),
    school_stage VARCHAR(32) NOT NULL,
    PRIMARY KEY (source_id, school_stage),
    CONSTRAINT ck_resource_source_stage CHECK (school_stage IN ('KINDERGARTEN','PRIMARY','JUNIOR_MIDDLE','SENIOR_HIGH'))
);

CREATE TABLE education_resource_category (
    id UUID PRIMARY KEY,
    source_id UUID NOT NULL REFERENCES education_resource_source(id),
    title VARCHAR(120) NOT NULL,
    category_url VARCHAR(1000) NOT NULL,
    display_order INTEGER NOT NULL,
    discovered_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_resource_category_order CHECK (display_order BETWEEN 0 AND 29),
    CONSTRAINT uq_resource_category_url UNIQUE (source_id, category_url),
    CONSTRAINT uq_resource_category_order UNIQUE (source_id, display_order)
);
CREATE INDEX idx_resource_category_source ON education_resource_category(source_id, display_order);

CREATE TABLE education_resource_action (
    id UUID PRIMARY KEY,
    family_id UUID NOT NULL REFERENCES family(id),
    source_id UUID NOT NULL REFERENCES education_resource_source(id),
    actor_id UUID NOT NULL REFERENCES parent_profile(id),
    action_type VARCHAR(16) NOT NULL,
    idempotency_key VARCHAR(120) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_resource_action_type CHECK (action_type IN ('REFRESH','APPROVE','WITHDRAW')),
    CONSTRAINT uq_resource_action_key UNIQUE (family_id, idempotency_key)
);
CREATE INDEX idx_resource_action_source ON education_resource_action(family_id, source_id, created_at);
