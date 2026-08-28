package com.familygrowth.web;

import com.familygrowth.application.Stage21TeachingService;
import com.familygrowth.domain.Stage20Models.SchoolStage;
import com.familygrowth.domain.Stage21TeachingModels.ActivityDraft;
import com.familygrowth.domain.Stage21TeachingModels.ActivityType;
import com.familygrowth.domain.Stage21TeachingModels.CourseVersion;
import com.familygrowth.domain.Stage21TeachingModels.LearningAssignment;
import com.familygrowth.domain.Stage21TeachingModels.LessonDraft;
import com.familygrowth.domain.Stage21TeachingModels.ParentCourseSummary;
import com.familygrowth.domain.Stage21TeachingModels.QuestionOption;
import com.familygrowth.domain.Stage21TeachingModels.ReviewDecision;
import com.familygrowth.domain.Stage21TeachingModels.UnitDraft;
import com.familygrowth.domain.Stage21TeachingModels.VersionDraft;
import com.familygrowth.domain.Stage3Models;
import com.familygrowth.domain.Stage3Models.Actor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/families/{familyId}")
class Stage21TeachingController {
    private final Stage21TeachingService service;
    Stage21TeachingController(Stage21TeachingService service) { this.service = service; }

    @PostMapping("/teaching/courses")
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<CourseVersion> createCourse(
        @RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,
        @PathVariable UUID familyId,
        @RequestHeader("Idempotency-Key") String key,
        @Valid @RequestBody CourseRequest request
    ) {
        return ApiResponse.ok(service.createCourse(actor, familyId, request.schoolStage(), request.subjectCode(),
            request.title(), request.version().toDomain(), key));
    }

    @PostMapping("/teaching/courses/{courseId}/versions")
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<CourseVersion> createVersion(
        @RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,
        @PathVariable UUID familyId, @PathVariable UUID courseId,
        @RequestHeader("Idempotency-Key") String key,
        @Valid @RequestBody VersionRequest request
    ) {
        return ApiResponse.ok(service.createVersion(actor, familyId, courseId, request.toDomain(), key));
    }

    @PostMapping("/teaching/course-versions/{versionId}/publish")
    ApiResponse<CourseVersion> publish(
        @RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,
        @PathVariable UUID familyId, @PathVariable UUID versionId,
        @RequestHeader("Idempotency-Key") String key
    ) { return ApiResponse.ok(service.publish(actor, familyId, versionId, key)); }

    @GetMapping("/teaching/courses")
    ApiResponse<List<ParentCourseSummary>> courses(
        @RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor, @PathVariable UUID familyId
    ) { return ApiResponse.ok(service.courses(actor, familyId)); }

    @GetMapping("/teaching/course-versions/{versionId}")
    ApiResponse<CourseVersion> courseVersion(
        @RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,
        @PathVariable UUID familyId, @PathVariable UUID versionId
    ) { return ApiResponse.ok(service.courseVersion(actor, familyId, versionId)); }

    @PostMapping("/children/{childId}/learning/assignments")
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<LearningAssignment> assign(
        @RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,
        @PathVariable UUID familyId, @PathVariable UUID childId,
        @RequestHeader("Idempotency-Key") String key,
        @Valid @RequestBody AssignmentRequest request
    ) { return ApiResponse.ok(service.assign(actor, familyId, childId, request.courseVersionId(), request.lessonId(), key)); }

    @GetMapping("/children/{childId}/learning/assignments")
    ApiResponse<List<LearningAssignment>> catalog(
        @RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,
        @PathVariable UUID familyId, @PathVariable UUID childId
    ) { return ApiResponse.ok(service.catalog(actor, familyId, childId)); }

    @PostMapping("/children/{childId}/learning/assignments/{assignmentId}/activities/{activityId}/attempts")
    ApiResponse<LearningAssignment> attempt(
        @RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,
        @PathVariable UUID familyId, @PathVariable UUID childId, @PathVariable UUID assignmentId,
        @PathVariable UUID activityId, @RequestHeader("Idempotency-Key") String key,
        @Valid @RequestBody AttemptRequest request
    ) { return ApiResponse.ok(service.attempt(actor, familyId, childId, assignmentId, activityId,
        request.responseText(), request.playedSeconds(), request.durationSeconds(), key)); }

    @PostMapping("/children/{childId}/learning/assignments/{assignmentId}/submit")
    ApiResponse<LearningAssignment> submit(
        @RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,
        @PathVariable UUID familyId, @PathVariable UUID childId, @PathVariable UUID assignmentId,
        @RequestHeader("Idempotency-Key") String key, @Valid @RequestBody VersionCheck request
    ) { return ApiResponse.ok(service.submit(actor, familyId, childId, assignmentId, request.expectedVersion(), key)); }

    @PostMapping("/children/{childId}/learning/assignments/{assignmentId}/review")
    ApiResponse<LearningAssignment> review(
        @RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,
        @PathVariable UUID familyId, @PathVariable UUID childId, @PathVariable UUID assignmentId,
        @RequestHeader("Idempotency-Key") String key, @Valid @RequestBody ReviewRequest request
    ) { return ApiResponse.ok(service.review(actor, familyId, childId, assignmentId, request.decision(),
        request.note(), request.expectedVersion(), key)); }

    record CourseRequest(@NotNull SchoolStage schoolStage, @NotBlank @Size(max=40) String subjectCode,
                         @NotBlank @Size(max=160) String title, @NotNull @Valid VersionRequest version) { }
    record VersionRequest(@NotBlank @Size(max=500) String summary, @NotBlank @Size(max=500) String rightsBasis,
                          @NotEmpty @Size(max=12) List<@Valid UnitRequest> units) {
        VersionDraft toDomain() { return new VersionDraft(summary, rightsBasis, units.stream().map(UnitRequest::toDomain).toList()); }
    }
    record UnitRequest(@NotBlank @Size(max=160) String title,
                       @NotEmpty @Size(max=30) List<@Valid LessonRequest> lessons) {
        UnitDraft toDomain() { return new UnitDraft(title, lessons.stream().map(LessonRequest::toDomain).toList()); }
    }
    record LessonRequest(@NotBlank @Size(max=160) String title, @NotBlank @Size(max=500) String summary,
                         @NotEmpty @Size(max=20) List<@Valid ActivityRequest> activities) {
        LessonDraft toDomain() { return new LessonDraft(title, summary, activities.stream().map(ActivityRequest::toDomain).toList()); }
    }
    record ActivityRequest(@NotNull ActivityType type, @NotBlank @Size(max=160) String title,
                           @NotBlank @Size(max=500) String instruction, @Min(1) @Max(60) int expectedMinutes,
                           @Size(max=160) String contentRef,
                           @Size(max=500) String prompt, @Size(max=300) String hint,
                           @Size(max=20) List<@Valid OptionRequest> options, @Size(max=1000) String answerKey) {
        ActivityDraft toDomain() { return new ActivityDraft(type, title, instruction, contentRef, expectedMinutes, prompt, hint,
            options == null ? List.of() : options.stream().map(o -> new QuestionOption(o.value(), o.label())).toList(), answerKey); }
    }
    record OptionRequest(@NotBlank @Size(max=160) String value, @NotBlank @Size(max=240) String label) { }
    record AssignmentRequest(@NotNull UUID courseVersionId, @NotNull UUID lessonId) { }
    record AttemptRequest(@Size(max=1000) String responseText, @Min(0) Integer playedSeconds,
                          @Min(1) Integer durationSeconds) { }
    record VersionCheck(@Min(0) long expectedVersion) { }
    record ReviewRequest(@NotNull ReviewDecision decision, @Size(max=500) String note, @Min(0) long expectedVersion) { }
}
