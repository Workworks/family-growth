package com.familygrowth.application;

import com.familygrowth.domain.Stage29CollaborationModels.*;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface Stage29CollaborationStore {
    boolean isOwner(UUID family,UUID parent); List<Member> members(UUID family); Optional<Member> member(UUID family,UUID parent);
    Optional<Invitation> invitationReplay(UUID family,String key); Invitation createInvitation(UUID family,String name,String codeHash,UUID actor,String key,String payloadHash,Instant expires,Instant now); Optional<Invitation> invitationByCode(String codeHash,boolean lock); Member acceptInvitation(UUID invitation,String pinHash,Instant now); Invitation revokeInvitation(UUID family,UUID invitation,UUID actor,Instant now);
    void resetParentPin(UUID family,UUID parent,String pinHash,UUID actor,Instant now); void revokeMember(UUID family,UUID parent,UUID actor,Instant now);
    List<ChildChoice> children(UUID family);
    Optional<PairingCode> pairingReplay(UUID family,String key); PairingCode createPairing(UUID family,PairingRole role,UUID actorId,UUID childId,String codeHash,UUID createdBy,String key,String payloadHash,Instant expires,Instant now); Optional<PairingCode> pairingByCode(String codeHash,boolean lock); Device consumePairing(UUID pairing,String deviceName,Instant now); List<Device> devices(UUID family); Device revokeDevice(UUID family,UUID device,UUID actor,Instant now);
    void revokeDeviceSessions(UUID family,UUID device,Instant now); void revokeActorSessions(UUID family,UUID actor,Instant now); void resetPinCredential(UUID family,UUID parent,String pinHash,Instant now);
    void emitToParents(UUID family,UUID child,NotificationType type,String title,String body,String sourceType,UUID source,Instant now); List<Notification> notifications(UUID family,UUID actor); Notification markRead(UUID family,UUID actor,UUID notification,Instant now);
    void redactChildFreeText(UUID family,UUID child);
}
