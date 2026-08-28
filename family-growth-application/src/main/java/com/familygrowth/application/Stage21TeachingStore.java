package com.familygrowth.application;

import com.familygrowth.domain.Stage20Models.SchoolStage;
import com.familygrowth.domain.Stage21TeachingModels.AssignmentFacts;
import com.familygrowth.domain.Stage21TeachingModels.AssignmentStatus;
import com.familygrowth.domain.Stage21TeachingModels.CourseVersion;
import com.familygrowth.domain.Stage21TeachingModels.EvidenceType;
import com.familygrowth.domain.Stage21TeachingModels.LearningAssignment;
import com.familygrowth.domain.Stage21TeachingModels.ParentCourseSummary;
import com.familygrowth.domain.Stage21TeachingModels.VersionDraft;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface Stage21TeachingStore {
    record ActionReplay(String actionType, UUID targetId, UUID resultId, String payloadHash) { }
    record CourseReplay(CourseVersion version, String payloadHash) { }

    Optional<CourseReplay> courseByCreateKey(UUID familyId, String key);
    CourseVersion createCourse(UUID familyId, SchoolStage stage, String subjectCode, String title,
                               VersionDraft draft, UUID actorId, String key, String payloadHash, Instant now);
    CourseVersion createVersion(UUID familyId, UUID courseId, VersionDraft draft, UUID actorId,
                                String key, String payloadHash, Instant now);
    Optional<ActionReplay> action(UUID familyId, String key);
    Optional<CourseVersion> version(UUID familyId, UUID versionId);
    List<ParentCourseSummary> courses(UUID familyId);
    CourseVersion publish(UUID familyId, UUID versionId, UUID actorId, String key,
                          String payloadHash, Instant now);

    Optional<AssignmentFacts> assignment(UUID familyId, UUID childId, UUID assignmentId);
    List<LearningAssignment> assignments(UUID familyId, UUID childId, SchoolStage stage);
    LearningAssignment assign(UUID familyId, UUID childId, UUID versionId, UUID lessonId, UUID actorId,
                              String key, String payloadHash, Instant now);
    LearningAssignment attempt(UUID familyId, UUID childId, UUID assignmentId, UUID activityId, UUID actorId,
                               String responseText, EvidenceType evidenceType, Boolean correct,
                               String key, String payloadHash, Instant now);
    LearningAssignment transition(UUID familyId, UUID childId, UUID assignmentId, AssignmentStatus target,
                                  UUID actorId, String note, long expectedVersion, String key,
                                  String payloadHash, Instant now);
}
