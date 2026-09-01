package com.familygrowth.web;

import com.familygrowth.application.Stage29CollaborationService;
import com.familygrowth.domain.Stage29CollaborationModels.*;
import com.familygrowth.domain.Stage3Models;
import com.familygrowth.domain.Stage3Models.Actor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/families/{familyId}")
class Stage29CollaborationController {
    private final Stage29CollaborationService service; Stage29CollaborationController(Stage29CollaborationService service){this.service=service;}
    @PostMapping("/parent-invitations")@ResponseStatus(HttpStatus.CREATED) ApiResponse<InvitationCreated> invite(@RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE)Actor actor,@PathVariable UUID familyId,@RequestHeader("Idempotency-Key")String key,@Valid@RequestBody InvitationRequest r){return ApiResponse.ok(service.invite(actor,familyId,r.invitedName(),key));}
    @PostMapping("/parent-invitations/{invitationId}/revoke") ApiResponse<Invitation> revokeInvite(@RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE)Actor actor,@PathVariable UUID familyId,@PathVariable UUID invitationId){return ApiResponse.ok(service.revokeInvitation(actor,familyId,invitationId));}
    @GetMapping("/members") ApiResponse<List<Member>> members(@RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE)Actor actor,@PathVariable UUID familyId){return ApiResponse.ok(service.members(actor,familyId));}
    @PostMapping("/members/{parentId}/reset-pin") ApiResponse<Void> resetPin(@RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE)Actor actor,@PathVariable UUID familyId,@PathVariable UUID parentId,@Valid@RequestBody ResetPinRequest r){service.resetPin(actor,familyId,parentId,r.pin());return ApiResponse.ok(null);}
    @PostMapping("/members/{parentId}/revoke") ApiResponse<Void> revokeMember(@RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE)Actor actor,@PathVariable UUID familyId,@PathVariable UUID parentId){service.revokeMember(actor,familyId,parentId);return ApiResponse.ok(null);}
    @GetMapping("/children") ApiResponse<List<ChildChoice>> children(@RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE)Actor actor,@PathVariable UUID familyId){return ApiResponse.ok(service.children(actor,familyId));}
    @PostMapping("/device-pairings")@ResponseStatus(HttpStatus.CREATED) ApiResponse<PairingCreated> pairing(@RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE)Actor actor,@PathVariable UUID familyId,@RequestHeader("Idempotency-Key")String key,@Valid@RequestBody PairingRequest r){return ApiResponse.ok(service.createPairing(actor,familyId,r.role(),r.childId(),key));}
    @GetMapping("/devices") ApiResponse<List<Device>> devices(@RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE)Actor actor,@PathVariable UUID familyId){return ApiResponse.ok(service.devices(actor,familyId));}
    @PostMapping("/devices/{deviceId}/revoke") ApiResponse<Device> revokeDevice(@RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE)Actor actor,@PathVariable UUID familyId,@PathVariable UUID deviceId){return ApiResponse.ok(service.revokeDevice(actor,familyId,deviceId));}
    @GetMapping("/notifications") ApiResponse<List<Notification>> notifications(@RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE)Actor actor,@PathVariable UUID familyId){return ApiResponse.ok(service.notifications(actor,familyId));}
    @PostMapping("/notifications/{notificationId}/read") ApiResponse<Notification> markRead(@RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE)Actor actor,@PathVariable UUID familyId,@PathVariable UUID notificationId){return ApiResponse.ok(service.markRead(actor,familyId,notificationId));}
    record InvitationRequest(@NotBlank@Size(max=80)String invitedName){} record ResetPinRequest(@NotBlank@Pattern(regexp="\\d{6}")String pin){} record PairingRequest(@NotNull PairingRole role,UUID childId){}
}

@RestController
@RequestMapping("/api/v1/auth")
class Stage29PairingController {
    private final Stage29CollaborationService service; Stage29PairingController(Stage29CollaborationService service){this.service=service;}
    @PostMapping("/parent-invitations/accept") ApiResponse<com.familygrowth.domain.Stage3Models.AuthSession> acceptInvitation(@Valid@RequestBody AcceptInvitationRequest r){return ApiResponse.ok(service.acceptInvitation(r.code(),r.pin()));}
    @PostMapping("/device-pairings/accept") ApiResponse<PairingResult> acceptPairing(@Valid@RequestBody AcceptPairingRequest r){return ApiResponse.ok(service.acceptPairing(r.code(),r.deviceName()));}
    record AcceptInvitationRequest(@NotBlank@Size(min=10,max=40)String code,@NotBlank@Pattern(regexp="\\d{6}")String pin){} record AcceptPairingRequest(@NotBlank@Size(min=10,max=40)String code,@NotBlank@Size(max=100)String deviceName){}
}
