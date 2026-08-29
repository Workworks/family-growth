ALTER TABLE teaching_course_version ADD COLUMN kindergarten_age_band VARCHAR(24);
ALTER TABLE teaching_course_version ADD COLUMN kindergarten_domains VARCHAR(120) NOT NULL DEFAULT '';

ALTER TABLE teaching_course_version ADD CONSTRAINT ck_teaching_version_kindergarten_age_band
    CHECK (kindergarten_age_band IS NULL OR kindergarten_age_band IN ('SHARED_3_4','TRANSITION_5_6'));
