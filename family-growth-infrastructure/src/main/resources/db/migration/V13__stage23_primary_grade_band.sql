ALTER TABLE child_experience_profile
    ADD COLUMN primary_band_override VARCHAR(32);

ALTER TABLE child_experience_profile
    ADD CONSTRAINT ck_experience_primary_band
    CHECK (primary_band_override IS NULL OR primary_band_override IN ('LOWER_PRIMARY','UPPER_PRIMARY'));

ALTER TABLE child_experience_audit
    ADD COLUMN old_primary_band_override VARCHAR(32);

ALTER TABLE child_experience_audit
    ADD COLUMN new_primary_band_override VARCHAR(32);

ALTER TABLE child_experience_audit
    ADD CONSTRAINT ck_experience_audit_old_primary_band
    CHECK (old_primary_band_override IS NULL OR old_primary_band_override IN ('LOWER_PRIMARY','UPPER_PRIMARY'));

ALTER TABLE child_experience_audit
    ADD CONSTRAINT ck_experience_audit_new_primary_band
    CHECK (new_primary_band_override IS NULL OR new_primary_band_override IN ('LOWER_PRIMARY','UPPER_PRIMARY'));
