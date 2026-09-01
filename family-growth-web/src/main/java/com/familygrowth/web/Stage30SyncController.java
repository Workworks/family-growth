package com.familygrowth.web;

import com.familygrowth.application.Stage30SyncService;
import com.familygrowth.domain.Stage30SyncModels.*;
import com.familygrowth.domain.Stage3Models;
import com.familygrowth.domain.Stage3Models.Actor;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/families/{familyId}/children/{childId}/sync")
class Stage30SyncController {
 private final Stage30SyncService service; Stage30SyncController(Stage30SyncService service){this.service=service;}
 @PostMapping("/delta") ApiResponse<DeltaSyncResult> delta(@RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE)Actor actor,@PathVariable UUID familyId,@PathVariable UUID childId,@RequestHeader("Idempotency-Key")String key,@RequestBody DeltaSyncRequest request){return ApiResponse.ok(service.delta(actor,familyId,childId,request,key));}
}
