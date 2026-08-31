package com.familygrowth.web;

import com.familygrowth.application.Stage25SeniorService;
import com.familygrowth.domain.Stage25SeniorModels.*;
import com.familygrowth.domain.Stage3Models;
import com.familygrowth.domain.Stage3Models.Actor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/families/{familyId}/children/{childId}/senior-learning")
class Stage25SeniorController {
    private final Stage25SeniorService service;Stage25SeniorController(Stage25SeniorService service){this.service=service;}
    @GetMapping("/modules") ApiResponse<ModuleConfiguration> modules(@RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,@PathVariable UUID familyId,@PathVariable UUID childId){return ApiResponse.ok(service.modules(actor,familyId,childId));}
    @PutMapping("/modules") ApiResponse<ModuleConfiguration> updateModules(@RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,@PathVariable UUID familyId,@PathVariable UUID childId,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody ModuleRequest request){return ApiResponse.ok(service.updateModules(actor,familyId,childId,request.selections(),request.expectedRevision(),request.reason(),key));}
    @GetMapping("/goals") ApiResponse<List<WeeklyGoal>> goals(@RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,@PathVariable UUID familyId,@PathVariable UUID childId){return ApiResponse.ok(service.goals(actor,familyId,childId));}
    @PostMapping("/goals") ApiResponse<WeeklyGoal> createGoal(@RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,@PathVariable UUID familyId,@PathVariable UUID childId,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody GoalCreateRequest request){return ApiResponse.ok(service.createGoal(actor,familyId,childId,request.assignmentId(),request.module(),request.weekStart(),request.title(),request.evidenceTarget(),request.nextAction(),key));}
    @PutMapping("/goals/{goalId}") ApiResponse<WeeklyGoal> updateGoal(@RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,@PathVariable UUID familyId,@PathVariable UUID childId,@PathVariable UUID goalId,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody GoalUpdateRequest request){return ApiResponse.ok(service.updateGoal(actor,familyId,childId,goalId,request.title(),request.evidenceTarget(),request.nextAction(),request.expectedRevision(),key));}
    @PostMapping("/goals/{goalId}/archive") ApiResponse<WeeklyGoal> archiveGoal(@RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,@PathVariable UUID familyId,@PathVariable UUID childId,@PathVariable UUID goalId,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody RevisionRequest request){return ApiResponse.ok(service.archiveGoal(actor,familyId,childId,goalId,request.expectedRevision(),key));}
    @GetMapping("/reflections") ApiResponse<List<Reflection>> reflections(@RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,@PathVariable UUID familyId,@PathVariable UUID childId){return ApiResponse.ok(service.reflections(actor,familyId,childId));}
    @PostMapping("/reflections") ApiResponse<Reflection> reflect(@RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,@PathVariable UUID familyId,@PathVariable UUID childId,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody ReflectionRequest request){return ApiResponse.ok(service.reflect(actor,familyId,childId,request.goalId(),request.assignmentId(),request.evidenceSummary(),request.strategy(),request.nextAction(),request.supportRequested(),key));}
    @GetMapping("/report") ApiResponse<SeniorLearningReport> report(@RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,@PathVariable UUID familyId,@PathVariable UUID childId){return ApiResponse.ok(service.report(actor,familyId,childId));}
    record ModuleRequest(@NotEmpty @Size(max=12) List<@NotNull ModuleSelection> selections,@Min(0) long expectedRevision,@NotBlank @Size(max=500) String reason){}
    record GoalCreateRequest(UUID assignmentId,@NotNull ModuleSelection module,@NotNull LocalDate weekStart,@NotBlank @Size(max=160) String title,@NotBlank @Size(max=500) String evidenceTarget,@NotBlank @Size(max=500) String nextAction){}
    record GoalUpdateRequest(@NotBlank @Size(max=160) String title,@NotBlank @Size(max=500) String evidenceTarget,@NotBlank @Size(max=500) String nextAction,@Min(0) long expectedRevision){}
    record RevisionRequest(@Min(0) long expectedRevision){}
    record ReflectionRequest(UUID goalId,UUID assignmentId,@NotBlank @Size(max=1000) String evidenceSummary,@NotNull ReflectionStrategy strategy,@NotBlank @Size(max=500) String nextAction,boolean supportRequested){}
}
