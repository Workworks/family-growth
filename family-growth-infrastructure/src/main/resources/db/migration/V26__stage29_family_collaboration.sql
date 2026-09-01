ALTER TABLE parent_profile ADD COLUMN member_role VARCHAR(16) NOT NULL DEFAULT 'OWNER';
ALTER TABLE parent_profile ADD COLUMN member_status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE parent_profile ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE parent_profile ADD CONSTRAINT ck_parent_member_role CHECK(member_role IN('OWNER','GUARDIAN'));
ALTER TABLE parent_profile ADD CONSTRAINT ck_parent_member_status CHECK(member_status IN('ACTIVE','REVOKED'));

CREATE TABLE family_parent_invitation (
 id UUID PRIMARY KEY,family_id UUID NOT NULL REFERENCES family(id),invited_name VARCHAR(80) NOT NULL,member_role VARCHAR(16) NOT NULL,
 code_hash VARCHAR(64) NOT NULL UNIQUE,status VARCHAR(16) NOT NULL,expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
 created_by UUID NOT NULL REFERENCES parent_profile(id),accepted_parent_id UUID REFERENCES parent_profile(id),revoked_by UUID REFERENCES parent_profile(id),
 idempotency_key VARCHAR(100) NOT NULL,payload_hash VARCHAR(64) NOT NULL,created_at TIMESTAMP WITH TIME ZONE NOT NULL,accepted_at TIMESTAMP WITH TIME ZONE,revoked_at TIMESTAMP WITH TIME ZONE,
 CONSTRAINT uq_parent_invitation_key UNIQUE(family_id,idempotency_key),
 CONSTRAINT ck_parent_invitation_role CHECK(member_role='GUARDIAN'),
 CONSTRAINT ck_parent_invitation_status CHECK(status IN('OPEN','ACCEPTED','REVOKED'))
);
CREATE INDEX idx_parent_invitation_family ON family_parent_invitation(family_id,status,created_at);

CREATE TABLE paired_device (
 id UUID PRIMARY KEY,family_id UUID NOT NULL REFERENCES family(id),actor_id UUID NOT NULL,actor_role VARCHAR(16) NOT NULL,
 child_id UUID REFERENCES child_profile(id),device_name VARCHAR(100) NOT NULL,status VARCHAR(16) NOT NULL,
 created_by UUID NOT NULL REFERENCES parent_profile(id),created_at TIMESTAMP WITH TIME ZONE NOT NULL,last_seen_at TIMESTAMP WITH TIME ZONE NOT NULL,revoked_at TIMESTAMP WITH TIME ZONE,
 CONSTRAINT ck_paired_device_role CHECK(actor_role IN('PARENT','CHILD')),
 CONSTRAINT ck_paired_device_scope CHECK((actor_role='PARENT' AND child_id IS NULL) OR (actor_role='CHILD' AND child_id IS NOT NULL)),
 CONSTRAINT ck_paired_device_status CHECK(status IN('ACTIVE','REVOKED'))
);
CREATE INDEX idx_paired_device_family ON paired_device(family_id,status,created_at);

ALTER TABLE auth_session ADD COLUMN device_id UUID REFERENCES paired_device(id);
CREATE INDEX idx_auth_session_device ON auth_session(family_id,device_id,revoked_at);

CREATE TABLE device_pairing_code (
 id UUID PRIMARY KEY,family_id UUID NOT NULL REFERENCES family(id),requested_role VARCHAR(16) NOT NULL,actor_id UUID NOT NULL,
 child_id UUID REFERENCES child_profile(id),code_hash VARCHAR(64) NOT NULL UNIQUE,status VARCHAR(16) NOT NULL,expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
 created_by UUID NOT NULL REFERENCES parent_profile(id),paired_device_id UUID REFERENCES paired_device(id),
 idempotency_key VARCHAR(100) NOT NULL,payload_hash VARCHAR(64) NOT NULL,created_at TIMESTAMP WITH TIME ZONE NOT NULL,consumed_at TIMESTAMP WITH TIME ZONE,
 CONSTRAINT uq_device_pairing_key UNIQUE(family_id,idempotency_key),
 CONSTRAINT ck_device_pairing_role CHECK(requested_role IN('PARENT','CHILD')),
 CONSTRAINT ck_device_pairing_scope CHECK((requested_role='PARENT' AND child_id IS NULL) OR (requested_role='CHILD' AND child_id IS NOT NULL)),
 CONSTRAINT ck_device_pairing_status CHECK(status IN('OPEN','CONSUMED','REVOKED'))
);
CREATE INDEX idx_device_pairing_family ON device_pairing_code(family_id,status,created_at);

CREATE TABLE family_notification (
 id UUID PRIMARY KEY,family_id UUID NOT NULL REFERENCES family(id),recipient_actor_id UUID NOT NULL,recipient_role VARCHAR(16) NOT NULL,
 child_id UUID REFERENCES child_profile(id),notification_type VARCHAR(32) NOT NULL,title VARCHAR(120) NOT NULL,body VARCHAR(300) NOT NULL,
 source_type VARCHAR(40) NOT NULL,source_id UUID NOT NULL,status VARCHAR(16) NOT NULL,created_at TIMESTAMP WITH TIME ZONE NOT NULL,read_at TIMESTAMP WITH TIME ZONE,
 CONSTRAINT uq_family_notification_source UNIQUE(family_id,recipient_actor_id,notification_type,source_type,source_id),
 CONSTRAINT ck_family_notification_role CHECK(recipient_role IN('PARENT','CHILD')),
 CONSTRAINT ck_family_notification_status CHECK(status IN('UNREAD','READ')),
 CONSTRAINT ck_family_notification_type CHECK(notification_type IN('TASK_REVIEW','EXCHANGE_REVIEW','REWARD_REVIEW','REWARD_FULFILL','CONTENT_UPDATED'))
);
CREATE INDEX idx_family_notification_inbox ON family_notification(family_id,recipient_actor_id,status,created_at);

CREATE TABLE family_collaboration_action (
 id UUID PRIMARY KEY,family_id UUID NOT NULL REFERENCES family(id),actor_id UUID NOT NULL,action_type VARCHAR(40) NOT NULL,
 target_type VARCHAR(24) NOT NULL,target_id UUID NOT NULL,detail VARCHAR(300) NOT NULL,idempotency_key VARCHAR(100),created_at TIMESTAMP WITH TIME ZONE NOT NULL,
 CONSTRAINT uq_family_collaboration_action_key UNIQUE(family_id,idempotency_key)
);
CREATE INDEX idx_family_collaboration_action ON family_collaboration_action(family_id,created_at);
