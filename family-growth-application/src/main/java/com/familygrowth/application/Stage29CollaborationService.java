package com.familygrowth.application;

import com.familygrowth.domain.Stage29CollaborationModels.*;
import com.familygrowth.domain.Stage3Models.Actor;
import com.familygrowth.domain.Stage3Models.ActorRole;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class Stage29CollaborationService {
    private static final Duration INVITE_LIFETIME=Duration.ofMinutes(10),PAIR_LIFETIME=Duration.ofMinutes(5);
    private static final char[] CODE_ALPHABET="23456789ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();
    private static final SecureRandom RANDOM=new SecureRandom();
    private final Stage3Service auth; private final Stage29CollaborationStore store; private final Clock clock;
    private final BCryptPasswordEncoder encoder=new BCryptPasswordEncoder(12);
    public Stage29CollaborationService(Stage3Service auth,Stage29CollaborationStore store,Clock clock){this.auth=auth;this.store=store;this.clock=clock;}

    public InvitationCreated invite(Actor actor,UUID family,String name,String key){owner(actor,family);String n=text(name,80),k=key(key),payload=hash(n+"|GUARDIAN");var replay=store.invitationReplay(family,k);if(replay.isPresent()){if(!replay.get().invitedName().equals(n))throw new Stage3Service.ConflictException("Idempotency-Key payload mismatch");return new InvitationCreated(replay.get(),null);}String code=code(20);Invitation invitation=store.createInvitation(family,n,hash(normalizeCode(code)),actor.actorId(),k,payload,clock.instant().plus(INVITE_LIFETIME),clock.instant());return new InvitationCreated(invitation,group(code));}
    public com.familygrowth.domain.Stage3Models.AuthSession acceptInvitation(String oneTimeCode,String pin){validPin(pin);Invitation invite=store.invitationByCode(hash(normalizeCode(oneTimeCode)),true).orElseThrow(Stage3Service.AuthenticationException::new);if(invite.status()!=InvitationStatus.OPEN||!invite.expiresAt().isAfter(clock.instant()))throw new Stage3Service.AuthenticationException();Member member=store.acceptInvitation(invite.id(),encoder.encode(pin),clock.instant());return auth.issuePairedSession(new Actor(member.familyId(),member.id(),ActorRole.PARENT,null),null);}
    public Invitation revokeInvitation(Actor actor,UUID family,UUID invitation){owner(actor,family);return store.revokeInvitation(family,invitation,actor.actorId(),clock.instant());}
    @Transactional(readOnly=true)public List<Member> members(Actor actor,UUID family){auth.requireParent(actor,family);return store.members(family);}
    public void resetPin(Actor actor,UUID family,UUID parent,String pin){owner(actor,family);if(actor.actorId().equals(parent))throw new Stage3Service.ConflictException("Use the normal PIN change flow for your own account");validPin(pin);Member target=store.member(family,parent).filter(m->m.status()==MemberStatus.ACTIVE).orElseThrow(FamilyGrowthService.NotFoundException::new);store.resetParentPin(family,target.id(),encoder.encode(pin),actor.actorId(),clock.instant());store.revokeActorSessions(family,target.id(),clock.instant());}
    public void revokeMember(Actor actor,UUID family,UUID parent){owner(actor,family);if(actor.actorId().equals(parent))throw new Stage3Service.ConflictException("An owner cannot revoke the current account");Member target=store.member(family,parent).filter(m->m.status()==MemberStatus.ACTIVE).orElseThrow(FamilyGrowthService.NotFoundException::new);if(target.role()==MemberRole.OWNER)throw new Stage3Service.ConflictException("The last owner cannot be revoked");store.revokeMember(family,parent,actor.actorId(),clock.instant());store.revokeActorSessions(family,parent,clock.instant());}
    @Transactional(readOnly=true)public List<ChildChoice> children(Actor actor,UUID family){auth.requireParent(actor,family);return store.children(family);}

    public PairingCreated createPairing(Actor actor,UUID family,PairingRole role,UUID child,String key){auth.requireParent(actor,family);UUID actorId;if(role==PairingRole.PARENT){if(child!=null)throw new IllegalArgumentException("Parent pairing cannot select a child");actorId=actor.actorId();}else{if(child==null||store.children(family).stream().noneMatch(c->c.id().equals(child)))throw new FamilyGrowthService.NotFoundException();actorId=child;}String k=key(key),payload=hash(role+"|"+actorId);var replay=store.pairingReplay(family,k);if(replay.isPresent()){PairingCode p=replay.get();if(p.role()!=role||!p.actorId().equals(actorId))throw new Stage3Service.ConflictException("Idempotency-Key payload mismatch");return new PairingCreated(p,null,true);}String raw=code(10);PairingCode pairing=store.createPairing(family,role,actorId,child,hash(raw),actor.actorId(),k,payload,clock.instant().plus(PAIR_LIFETIME),clock.instant());return new PairingCreated(pairing,group(raw),true);}
    public PairingResult acceptPairing(String oneTimeCode,String deviceName){PairingCode pairing=store.pairingByCode(hash(normalizeCode(oneTimeCode)),true).orElseThrow(Stage3Service.AuthenticationException::new);if(!pairing.expiresAt().isAfter(clock.instant()))throw new Stage3Service.AuthenticationException();Device device=store.consumePairing(pairing.id(),text(deviceName,100),clock.instant());Actor actor=new Actor(pairing.familyId(),pairing.actorId(),pairing.role()==PairingRole.PARENT?ActorRole.PARENT:ActorRole.CHILD,pairing.childId());var session=auth.issuePairedSession(actor,device.id());Member parent=pairing.role()==PairingRole.PARENT?store.member(pairing.familyId(),pairing.actorId()).orElseThrow(Stage3Service.AuthenticationException::new):null;return new PairingResult(session,device,parent,store.children(pairing.familyId()));}
    @Transactional(readOnly=true)public List<Device> devices(Actor actor,UUID family){owner(actor,family);return store.devices(family);}
    public Device revokeDevice(Actor actor,UUID family,UUID device){owner(actor,family);Device result=store.revokeDevice(family,device,actor.actorId(),clock.instant());store.revokeDeviceSessions(family,device,clock.instant());return result;}
    @Transactional(readOnly=true)public List<Notification> notifications(Actor actor,UUID family){if(actor.role()==ActorRole.PARENT)auth.requireParent(actor,family);else auth.requireChildOrParent(actor,family,actor.childId());return store.notifications(family,actor.actorId());}
    public Notification markRead(Actor actor,UUID family,UUID notification){if(actor.role()==ActorRole.PARENT)auth.requireParent(actor,family);else auth.requireChildOrParent(actor,family,actor.childId());return store.markRead(family,actor.actorId(),notification,clock.instant());}
    private void owner(Actor actor,UUID family){auth.requireParent(actor,family);if(!store.isOwner(family,actor.actorId()))throw new Stage3Service.ForbiddenException();}
    private static String code(int length){StringBuilder out=new StringBuilder(length);for(int i=0;i<length;i++)out.append(CODE_ALPHABET[RANDOM.nextInt(CODE_ALPHABET.length)]);return out.toString();}
    private static String group(String raw){return raw.length()>5?raw.substring(0,5)+"-"+raw.substring(5):raw;}
    private static String normalizeCode(String value){String v=Objects.requireNonNullElse(value,"").replace("-","").replace(" ","").toUpperCase(Locale.ROOT);if(v.length()<10||v.length()>32||!v.chars().allMatch(ch->new String(CODE_ALPHABET).indexOf(ch)>=0))throw new Stage3Service.AuthenticationException();return v;}
    private static void validPin(String pin){if(pin==null||!pin.matches("\\d{6}"))throw new IllegalArgumentException("PIN must contain exactly six digits");}
    private static String text(String value,int max){String v=Objects.requireNonNullElse(value,"").trim();if(v.isBlank()||v.length()>max)throw new IllegalArgumentException("Text is required");return v;}
    private static String key(String value){String v=text(value,100);return v;}
    private static String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception impossible){throw new IllegalStateException(impossible);}}
}
