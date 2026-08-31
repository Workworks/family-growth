package com.familygrowth.web;

import com.familygrowth.application.Stage24JuniorService;
import com.familygrowth.domain.Stage24JuniorModels.JuniorLearningPlan;
import com.familygrowth.domain.Stage24JuniorModels.MoveDirection;
import com.familygrowth.domain.Stage24JuniorModels.JuniorLearningReport;
import com.familygrowth.domain.Stage3Models;
import com.familygrowth.domain.Stage3Models.Actor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/families/{familyId}/children/{childId}/junior-learning")
class Stage24JuniorController {
    private final Stage24JuniorService service;
    Stage24JuniorController(Stage24JuniorService service){this.service=service;}
    @GetMapping("/plan") ApiResponse<JuniorLearningPlan> plan(@RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,
        @PathVariable UUID familyId,@PathVariable UUID childId){return ApiResponse.ok(service.plan(actor,familyId,childId));}
    @PostMapping("/plan/move") ApiResponse<JuniorLearningPlan> move(@RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,
        @PathVariable UUID familyId,@PathVariable UUID childId,@RequestHeader("Idempotency-Key") String key,
        @Valid @RequestBody MoveRequest request){return ApiResponse.ok(service.move(actor,familyId,childId,request.assignmentId(),request.direction(),request.expectedRevision(),key));}
    @GetMapping("/report") ApiResponse<JuniorLearningReport> report(@RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,
        @PathVariable UUID familyId,@PathVariable UUID childId){return ApiResponse.ok(service.report(actor,familyId,childId));}
    record MoveRequest(@NotNull UUID assignmentId,@NotNull MoveDirection direction,@Min(0) long expectedRevision){}
}
