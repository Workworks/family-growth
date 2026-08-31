CREATE TABLE child_data_request (
 id UUID PRIMARY KEY,family_id UUID NOT NULL REFERENCES family(id),child_id UUID NOT NULL REFERENCES child_profile(id),
 actor_id UUID NOT NULL REFERENCES parent_profile(id),request_type VARCHAR(16) NOT NULL,status VARCHAR(16) NOT NULL,
 confirmation_token_hash VARCHAR(64),confirmation_expires_at TIMESTAMP WITH TIME ZONE,idempotency_key VARCHAR(120) NOT NULL,
 payload_hash VARCHAR(64) NOT NULL,created_at TIMESTAMP WITH TIME ZONE NOT NULL,completed_at TIMESTAMP WITH TIME ZONE,
 CONSTRAINT uq_child_data_request_key UNIQUE(family_id,idempotency_key),
 CONSTRAINT ck_child_data_request_type CHECK(request_type IN('EXPORT','ERASURE')),
 CONSTRAINT ck_child_data_request_status CHECK(status IN('PREVIEWED','COMPLETED'))
);
CREATE INDEX idx_child_data_request_child ON child_data_request(family_id,child_id,created_at);

CREATE TABLE child_data_action (
 id UUID PRIMARY KEY,family_id UUID NOT NULL REFERENCES family(id),child_id UUID NOT NULL REFERENCES child_profile(id),
 actor_id UUID NOT NULL REFERENCES parent_profile(id),action_type VARCHAR(24) NOT NULL,request_id UUID NOT NULL REFERENCES child_data_request(id),
 detail VARCHAR(1000) NOT NULL,created_at TIMESTAMP WITH TIME ZONE NOT NULL,
 CONSTRAINT ck_child_data_action_type CHECK(action_type IN('EXPORT_GENERATED','ERASURE_CONFIRMED')),
 CONSTRAINT uq_child_data_action_request UNIQUE(request_id,action_type)
);
CREATE INDEX idx_child_data_action_child ON child_data_action(family_id,child_id,created_at);
