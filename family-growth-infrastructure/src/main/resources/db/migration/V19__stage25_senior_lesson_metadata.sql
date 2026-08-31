CREATE TABLE senior_lesson_metadata (
    lesson_id UUID PRIMARY KEY REFERENCES teaching_lesson(id),
    module_type VARCHAR(24) NOT NULL,
    topic_title VARCHAR(160) NOT NULL,
    inquiry_question VARCHAR(500) NOT NULL,
    expected_evidence VARCHAR(500) NOT NULL,
    safety_note VARCHAR(500) NOT NULL,
    CONSTRAINT ck_senior_lesson_module_type CHECK (module_type IN ('REQUIRED','SELECTIVE_REQUIRED','ELECTIVE'))
);
