package com.familygrowth.web;
import com.familygrowth.application.FamilyGrowthService; import com.familygrowth.domain.*; import jakarta.validation.Valid; import jakarta.validation.constraints.*; import java.time.LocalDate; import java.util.UUID; import org.springframework.http.*; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1")
public class FamilyGrowthController {
 private final FamilyGrowthService service; public FamilyGrowthController(FamilyGrowthService service){this.service=service;}
 @PostMapping("/families") @ResponseStatus(HttpStatus.CREATED) ApiResponse<Family> family(@Valid @RequestBody CreateFamily r){return ApiResponse.ok(service.createFamily(r.name()));}
 @PostMapping("/families/{familyId}/parents") @ResponseStatus(HttpStatus.CREATED) ApiResponse<ParentProfile> parent(@PathVariable UUID familyId,@Valid @RequestBody Named r){return ApiResponse.ok(service.addParent(familyId,r.displayName()));}
 @PostMapping("/families/{familyId}/children") @ResponseStatus(HttpStatus.CREATED) ApiResponse<ChildProfile> child(@PathVariable UUID familyId,@Valid @RequestBody CreateChild r){return ApiResponse.ok(service.addChild(familyId,r.displayName(),r.birthDate(),r.ageStage()));}
 @PostMapping("/families/{familyId}/children/{childId}/plans") @ResponseStatus(HttpStatus.CREATED) ApiResponse<GrowthPlan> plan(@PathVariable UUID familyId,@PathVariable UUID childId,@Valid @RequestBody CreatePlan r){return ApiResponse.ok(service.createPlan(familyId,childId,r.title(),r.description(),r.startDate(),r.endDate()));}
 @PostMapping("/families/{familyId}/plans/{planId}/goals") @ResponseStatus(HttpStatus.CREATED) ApiResponse<GrowthGoal> goal(@PathVariable UUID familyId,@PathVariable UUID planId,@Valid @RequestBody CreateGoal r){return ApiResponse.ok(service.createGoal(familyId,planId,r.title(),r.description()));}
 @PostMapping("/families/{familyId}/goals/{goalId}/tasks") @ResponseStatus(HttpStatus.CREATED) ApiResponse<GrowthTask> task(@PathVariable UUID familyId,@PathVariable UUID goalId,@Valid @RequestBody CreateTask r){return ApiResponse.ok(service.createTask(familyId,goalId,r.title(),r.description(),r.category(),r.difficulty(),r.expectedMinutes()));}
 public record CreateFamily(@NotBlank @Size(max=100) String name){} public record Named(@NotBlank @Size(max=80) String displayName){}
 public record CreateChild(@NotBlank @Size(max=80) String displayName,@NotNull @PastOrPresent LocalDate birthDate,@NotNull AgeStage ageStage){}
 public record CreatePlan(@NotBlank @Size(max=120) String title,@Size(max=1000) String description,@NotNull LocalDate startDate,LocalDate endDate){}
 public record CreateGoal(@NotBlank @Size(max=120) String title,@Size(max=1000) String description){}
 public record CreateTask(@NotBlank @Size(max=120) String title,@Size(max=1000) String description,@NotNull TaskCategory category,@NotNull TaskDifficulty difficulty,@Min(1) @Max(480) int expectedMinutes){}
}
