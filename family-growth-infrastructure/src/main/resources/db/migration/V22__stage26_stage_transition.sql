ALTER TABLE lesson_assignment ADD COLUMN stage_archived_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE lesson_assignment ADD COLUMN stage_archive_reason VARCHAR(240) NOT NULL DEFAULT '';
CREATE INDEX idx_assignment_stage_archive ON lesson_assignment(family_id,child_id,stage_archived_at,status);

CREATE TABLE child_stage_transition_action(
 id UUID PRIMARY KEY,family_id UUID NOT NULL REFERENCES family(id),child_id UUID NOT NULL REFERENCES child_profile(id),
 actor_id UUID NOT NULL REFERENCES parent_profile(id),old_stage VARCHAR(32) NOT NULL,new_stage VARCHAR(32) NOT NULL,
 archived_assignments INTEGER NOT NULL,restored_assignments INTEGER NOT NULL,reason VARCHAR(240) NOT NULL,
 created_at TIMESTAMP WITH TIME ZONE NOT NULL,
 CONSTRAINT ck_stage_transition_old CHECK(old_stage IN('PARENT_ONLY','KINDERGARTEN','PRIMARY','JUNIOR_MIDDLE','SENIOR_HIGH')),
 CONSTRAINT ck_stage_transition_new CHECK(new_stage IN('PARENT_ONLY','KINDERGARTEN','PRIMARY','JUNIOR_MIDDLE','SENIOR_HIGH')),
 CONSTRAINT ck_stage_transition_counts CHECK(archived_assignments>=0 AND restored_assignments>=0)
);
CREATE INDEX idx_stage_transition_child ON child_stage_transition_action(family_id,child_id,created_at);
