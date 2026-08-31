CREATE TABLE junior_lesson_metadata (
    lesson_id UUID PRIMARY KEY REFERENCES teaching_lesson(id),
    chapter_title VARCHAR(160) NOT NULL,
    knowledge_points VARCHAR(1000) NOT NULL,
    learning_goal VARCHAR(500) NOT NULL,
    safety_note VARCHAR(500) NOT NULL
);
