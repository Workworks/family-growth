package com.familygrowth;

import static org.assertj.core.api.Assertions.assertThat;

import com.familygrowth.application.Stage21TeachingService;
import com.familygrowth.application.Stage3Service;
import com.familygrowth.domain.AgeStage;
import com.familygrowth.domain.Stage20Models.SchoolStage;
import com.familygrowth.domain.Stage21TeachingModels.ActivityDraft;
import com.familygrowth.domain.Stage21TeachingModels.ActivityType;
import com.familygrowth.domain.Stage21TeachingModels.AssignmentStatus;
import com.familygrowth.domain.Stage21TeachingModels.LessonDraft;
import com.familygrowth.domain.Stage21TeachingModels.ReviewDecision;
import com.familygrowth.domain.Stage21TeachingModels.UnitDraft;
import com.familygrowth.domain.Stage21TeachingModels.VersionDraft;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@EnabledIfEnvironmentVariable(named = "FAMILY_GROWTH_TEST_POSTGRES_URL", matches = ".+")
@SpringBootTest(properties = {
    "spring.datasource.url=${FAMILY_GROWTH_TEST_POSTGRES_URL}",
    "spring.datasource.username=${FAMILY_GROWTH_TEST_POSTGRES_USER:family_growth}",
    "spring.datasource.password=${FAMILY_GROWTH_TEST_POSTGRES_PASSWORD:}",
    "spring.jpa.hibernate.ddl-auto=validate"
})
class Stage21TeachingConcurrencyTest {
    @Autowired Stage3Service stage3;
    @Autowired Stage21TeachingService teaching;
    @Autowired JdbcTemplate jdbc;

    @Test
    void concurrentApproveAndReworkAcceptExactlyOneExpectedVersion() throws Exception {
        var bootstrap = stage3.bootstrap("Stage21-review-race", "家长", "728146");
        var parent = bootstrap.session().actor();
        var child = stage3.addChild(parent, bootstrap.familyId(), "孩子", LocalDate.of(2017, 1, 1), AgeStage.CHILD_6_9);
        var childActor = stage3.createChildSession(parent, child.id()).actor();
        var activity = new ActivityDraft(ActivityType.OFFLINE_PRACTICE, "找一片叶子", "离开屏幕找一找", "", 5,
            "", "", List.of(), "");
        var draft = new VersionDraft("一次一件事", "家庭原创活动",
            List.of(new UnitDraft("自然", List.of(new LessonDraft("叶子", "观察一片叶子", List.of(activity))))));
        var version = teaching.createCourse(parent, bootstrap.familyId(), SchoolStage.PRIMARY, "SCIENCE",
            "自然观察", draft, "race-course");
        version = teaching.publish(parent, bootstrap.familyId(), version.versionId(), "race-publish");
        var lesson = version.units().get(0).lessons().get(0);
        var assignment = teaching.assign(parent, bootstrap.familyId(), child.id(), version.versionId(), lesson.id(), "race-assign");
        assignment = teaching.attempt(childActor, bootstrap.familyId(), child.id(), assignment.id(),
            assignment.activities().get(0).id(), "完成了", null, null, "race-attempt");
        assignment = teaching.submit(childActor, bootstrap.familyId(), child.id(), assignment.id(), assignment.version(), "race-submit");

        long expectedVersion = assignment.version();
        var assignmentId = assignment.id();
        CountDownLatch start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        var approve = pool.submit(() -> attempt(start, () -> teaching.review(parent, bootstrap.familyId(), child.id(),
            assignmentId, ReviewDecision.APPROVE, "认真完成", expectedVersion, "race-approve")));
        var rework = pool.submit(() -> attempt(start, () -> teaching.review(parent, bootstrap.familyId(), child.id(),
            assignmentId, ReviewDecision.REWORK, "再观察一次", expectedVersion, "race-rework")));
        start.countDown();

        assertThat(List.of(approve.get(), rework.get()).stream().filter(Boolean::booleanValue).count()).isEqualTo(1);
        pool.shutdownNow();
        var result = teaching.catalog(parent, bootstrap.familyId(), child.id()).get(0);
        assertThat(result.status()).isIn(AssignmentStatus.COMPLETED, AssignmentStatus.REWORK_REQUIRED);
        int mastered = jdbc.queryForObject("SELECT COUNT(*) FROM mastery_evidence WHERE assignment_id=? AND evidence_type='MASTERED'",
            Integer.class, result.id());
        assertThat(mastered).isEqualTo(result.status() == AssignmentStatus.COMPLETED ? 1 : 0);
    }

    private static boolean attempt(CountDownLatch start, Runnable operation) {
        try { start.await(); operation.run(); return true; }
        catch (RuntimeException rejected) { return false; }
        catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); return false; }
    }
}
