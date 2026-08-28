CREATE TABLE teaching_course (
    id UUID PRIMARY KEY,
    family_id UUID NOT NULL REFERENCES family(id),
    school_stage VARCHAR(32) NOT NULL,
    subject_code VARCHAR(40) NOT NULL,
    title VARCHAR(160) NOT NULL,
    created_by UUID NOT NULL REFERENCES parent_profile(id),
    idempotency_key VARCHAR(120) NOT NULL,
    payload_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_teaching_course_stage CHECK (school_stage IN ('KINDERGARTEN','PRIMARY','JUNIOR_MIDDLE','SENIOR_HIGH')),
    CONSTRAINT uq_teaching_course_key UNIQUE (family_id,idempotency_key)
);
CREATE INDEX idx_teaching_course_family ON teaching_course(family_id,school_stage,created_at);

CREATE TABLE teaching_course_version (
    id UUID PRIMARY KEY,
    course_id UUID NOT NULL REFERENCES teaching_course(id),
    version_number INTEGER NOT NULL,
    summary VARCHAR(500) NOT NULL,
    rights_basis VARCHAR(500) NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_by UUID NOT NULL REFERENCES parent_profile(id),
    published_by UUID REFERENCES parent_profile(id),
    published_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_teaching_version_status CHECK (status IN ('DRAFT','PUBLISHED')),
    CONSTRAINT uq_teaching_course_version UNIQUE (course_id,version_number)
);
CREATE INDEX idx_teaching_version_course ON teaching_course_version(course_id,status,version_number);

CREATE TABLE teaching_unit (
    id UUID PRIMARY KEY,
    course_version_id UUID NOT NULL REFERENCES teaching_course_version(id),
    title VARCHAR(160) NOT NULL,
    display_order INTEGER NOT NULL,
    CONSTRAINT ck_teaching_unit_order CHECK (display_order BETWEEN 0 AND 11),
    CONSTRAINT uq_teaching_unit_order UNIQUE (course_version_id,display_order)
);

CREATE TABLE teaching_lesson (
    id UUID PRIMARY KEY,
    unit_id UUID NOT NULL REFERENCES teaching_unit(id),
    title VARCHAR(160) NOT NULL,
    summary VARCHAR(500) NOT NULL,
    display_order INTEGER NOT NULL,
    CONSTRAINT ck_teaching_lesson_order CHECK (display_order BETWEEN 0 AND 29),
    CONSTRAINT uq_teaching_lesson_order UNIQUE (unit_id,display_order)
);

CREATE TABLE learning_activity (
    id UUID PRIMARY KEY,
    lesson_id UUID NOT NULL REFERENCES teaching_lesson(id),
    activity_type VARCHAR(32) NOT NULL,
    title VARCHAR(160) NOT NULL,
    instruction VARCHAR(500) NOT NULL,
    content_ref VARCHAR(160) NOT NULL DEFAULT '',
    expected_minutes INTEGER NOT NULL,
    display_order INTEGER NOT NULL,
    CONSTRAINT ck_learning_activity_type CHECK (activity_type IN ('SHORT_VIDEO','PARENT_CHILD_READING','LISTEN_CHOOSE','SINGLE_CHOICE','MATCHING','SORTING','ORAL_RESPONSE','OFFLINE_PRACTICE','PARENT_CONFIRMATION')),
    CONSTRAINT ck_learning_activity_minutes CHECK (expected_minutes BETWEEN 1 AND 60),
    CONSTRAINT ck_learning_activity_order CHECK (display_order BETWEEN 0 AND 19),
    CONSTRAINT uq_learning_activity_order UNIQUE (lesson_id,display_order)
);

CREATE TABLE learning_question (
    id UUID PRIMARY KEY,
    activity_id UUID NOT NULL UNIQUE REFERENCES learning_activity(id),
    prompt VARCHAR(500) NOT NULL,
    hint VARCHAR(300) NOT NULL DEFAULT '',
    answer_key VARCHAR(1000) NOT NULL,
    content_version INTEGER NOT NULL DEFAULT 1,
    CONSTRAINT ck_learning_question_version CHECK (content_version > 0)
);

CREATE TABLE learning_question_option (
    id UUID PRIMARY KEY,
    question_id UUID NOT NULL REFERENCES learning_question(id),
    option_value VARCHAR(160) NOT NULL,
    option_label VARCHAR(240) NOT NULL,
    display_order INTEGER NOT NULL,
    CONSTRAINT ck_learning_option_order CHECK (display_order BETWEEN 0 AND 19),
    CONSTRAINT uq_learning_option_value UNIQUE (question_id,option_value),
    CONSTRAINT uq_learning_option_order UNIQUE (question_id,display_order)
);

CREATE TABLE lesson_assignment (
    id UUID PRIMARY KEY,
    family_id UUID NOT NULL REFERENCES family(id),
    child_id UUID NOT NULL REFERENCES child_profile(id),
    course_version_id UUID NOT NULL REFERENCES teaching_course_version(id),
    lesson_id UUID NOT NULL REFERENCES teaching_lesson(id),
    status VARCHAR(24) NOT NULL,
    assigned_by UUID NOT NULL REFERENCES parent_profile(id),
    idempotency_key VARCHAR(120) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_lesson_assignment_status CHECK (status IN ('ASSIGNED','IN_PROGRESS','SUBMITTED','COMPLETED','REWORK_REQUIRED')),
    CONSTRAINT uq_lesson_assignment_key UNIQUE (family_id,idempotency_key),
    CONSTRAINT uq_lesson_assignment_lesson UNIQUE (child_id,lesson_id)
);
CREATE INDEX idx_lesson_assignment_child ON lesson_assignment(family_id,child_id,status,updated_at);

CREATE TABLE activity_attempt (
    id UUID PRIMARY KEY,
    family_id UUID NOT NULL REFERENCES family(id),
    assignment_id UUID NOT NULL REFERENCES lesson_assignment(id),
    activity_id UUID NOT NULL REFERENCES learning_activity(id),
    actor_id UUID NOT NULL,
    response_text VARCHAR(1000) NOT NULL,
    evidence_type VARCHAR(24) NOT NULL,
    checked_correct BOOLEAN,
    idempotency_key VARCHAR(120) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_activity_attempt_evidence CHECK (evidence_type IN ('VIEWED','ATTEMPTED','CHECKED')),
    CONSTRAINT uq_activity_attempt_key UNIQUE (family_id,idempotency_key)
);
CREATE INDEX idx_activity_attempt_assignment ON activity_attempt(assignment_id,activity_id,created_at);

CREATE TABLE learning_completion (
    assignment_id UUID PRIMARY KEY REFERENCES lesson_assignment(id),
    status VARCHAR(24) NOT NULL,
    submitted_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    rework_requested_at TIMESTAMP WITH TIME ZONE,
    review_note VARCHAR(500) NOT NULL DEFAULT '',
    reviewed_by UUID REFERENCES parent_profile(id),
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_learning_completion_status CHECK (status IN ('ASSIGNED','IN_PROGRESS','SUBMITTED','COMPLETED','REWORK_REQUIRED'))
);

CREATE TABLE mastery_evidence (
    id UUID PRIMARY KEY,
    assignment_id UUID NOT NULL REFERENCES lesson_assignment(id),
    activity_id UUID REFERENCES learning_activity(id),
    evidence_type VARCHAR(24) NOT NULL,
    note VARCHAR(500) NOT NULL DEFAULT '',
    actor_id UUID NOT NULL,
    source_attempt_id UUID REFERENCES activity_attempt(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_mastery_evidence_type CHECK (evidence_type IN ('VIEWED','ATTEMPTED','CHECKED','PARENT_CONFIRMED','MASTERED')),
    CONSTRAINT uq_mastery_attempt UNIQUE (source_attempt_id,evidence_type)
);
CREATE INDEX idx_mastery_evidence_assignment ON mastery_evidence(assignment_id,activity_id,evidence_type,created_at);

CREATE TABLE teaching_action (
    id UUID PRIMARY KEY,
    family_id UUID NOT NULL REFERENCES family(id),
    target_id UUID NOT NULL,
    result_id UUID NOT NULL,
    actor_id UUID NOT NULL,
    action_type VARCHAR(24) NOT NULL,
    payload_hash VARCHAR(64) NOT NULL,
    idempotency_key VARCHAR(120) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_teaching_action_type CHECK (action_type IN ('CREATE_VERSION','PUBLISH','ASSIGN','ATTEMPT','SUBMIT','APPROVE','REWORK')),
    CONSTRAINT uq_teaching_action_key UNIQUE (family_id,idempotency_key)
);
CREATE INDEX idx_teaching_action_target ON teaching_action(family_id,target_id,created_at);
