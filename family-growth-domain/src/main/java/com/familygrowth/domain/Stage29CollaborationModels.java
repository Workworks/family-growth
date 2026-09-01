package com.familygrowth.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class Stage29CollaborationModels {
    private Stage29CollaborationModels() {}
    public enum MemberRole { OWNER, GUARDIAN }
    public enum MemberStatus { ACTIVE, REVOKED }
    public enum InvitationStatus { OPEN, ACCEPTED, REVOKED }
    public enum PairingRole { PARENT, CHILD }
    public enum DeviceStatus { ACTIVE, REVOKED }
    public enum NotificationStatus { UNREAD, READ }
    public enum NotificationType { TASK_REVIEW, EXCHANGE_REVIEW, REWARD_REVIEW, REWARD_FULFILL, CONTENT_UPDATED }

    public record Member(UUID id, UUID familyId, String displayName, MemberRole role, MemberStatus status,
                         Instant createdAt, Instant updatedAt) {
        public Member { Objects.requireNonNull(id); Objects.requireNonNull(familyId); Objects.requireNonNull(role); Objects.requireNonNull(status); displayName=text(displayName,80); }
    }
    public record Invitation(UUID id, UUID familyId, String invitedName, MemberRole role, InvitationStatus status,
                             Instant expiresAt, UUID acceptedParentId, Instant createdAt, Instant acceptedAt) {
        public Invitation { Objects.requireNonNull(id); Objects.requireNonNull(familyId); invitedName=text(invitedName,80); Objects.requireNonNull(role); Objects.requireNonNull(status); Objects.requireNonNull(expiresAt); }
    }
    public record InvitationCreated(Invitation invitation, String oneTimeCode) {
        public InvitationCreated { Objects.requireNonNull(invitation); if(oneTimeCode!=null)oneTimeCode=text(oneTimeCode,100); }
    }
    public record ChildChoice(UUID id, String displayName, LocalDate birthDate, String effectiveStage) {
        public ChildChoice { Objects.requireNonNull(id); displayName=text(displayName,80); Objects.requireNonNull(birthDate); effectiveStage=text(effectiveStage,32); }
    }
    public record PairingCode(UUID id, UUID familyId, PairingRole role, UUID actorId, UUID childId,
                              Instant expiresAt, Instant createdAt) {
        public PairingCode { Objects.requireNonNull(id); Objects.requireNonNull(familyId); Objects.requireNonNull(role); Objects.requireNonNull(actorId); Objects.requireNonNull(expiresAt); if(role==PairingRole.CHILD&&!Objects.equals(actorId,childId))throw new IllegalArgumentException("Child pairing scope mismatch"); }
    }
    public record PairingCreated(PairingCode pairing, String oneTimeCode, boolean trustedHttpsRequired) {
        public PairingCreated { Objects.requireNonNull(pairing); if(oneTimeCode!=null)oneTimeCode=text(oneTimeCode,32); if(!trustedHttpsRequired)throw new IllegalArgumentException("Trusted HTTPS is required"); }
    }
    public record Device(UUID id, UUID familyId, UUID actorId, PairingRole role, UUID childId, String deviceName,
                         DeviceStatus status, Instant createdAt, Instant lastSeenAt, Instant revokedAt) {
        public Device { Objects.requireNonNull(id); Objects.requireNonNull(familyId); Objects.requireNonNull(actorId); Objects.requireNonNull(role); deviceName=text(deviceName,100); Objects.requireNonNull(status); }
    }
    public record Notification(UUID id, UUID childId, NotificationType type, String title, String body,
                               String sourceType, UUID sourceId, NotificationStatus status, Instant createdAt, Instant readAt) {
        public Notification { Objects.requireNonNull(id); Objects.requireNonNull(type); title=text(title,120); body=text(body,300); sourceType=text(sourceType,40); Objects.requireNonNull(sourceId); Objects.requireNonNull(status); }
    }
    public record PairingResult(com.familygrowth.domain.Stage3Models.AuthSession session, Device device,
                                Member parent, List<ChildChoice> children) {
        public PairingResult { Objects.requireNonNull(session); Objects.requireNonNull(device); children=List.copyOf(children); }
    }
    private static String text(String value,int max){String v=value==null?"":value.trim();if(v.isBlank()||v.length()>max)throw new IllegalArgumentException("Invalid collaboration text");return v;}
}
