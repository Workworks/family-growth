package com.familygrowth;

import static org.assertj.core.api.Assertions.assertThat;

import com.familygrowth.application.Stage3Service;
import com.familygrowth.application.Stage4Service;
import com.familygrowth.domain.AgeStage;
import com.familygrowth.domain.Stage3Models.AssetType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@EnabledIfEnvironmentVariable(named = "FAMILY_GROWTH_TEST_POSTGRES_URL", matches = ".+")
@SpringBootTest(properties = {
    "spring.datasource.url=${FAMILY_GROWTH_TEST_POSTGRES_URL}",
    "spring.datasource.username=${FAMILY_GROWTH_TEST_POSTGRES_USER:family_growth}",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=validate"
})
class Stage4ConcurrencyTest {
    @Autowired Stage3Service stage3;
    @Autowired Stage4Service stage4;

    @Test
    void concurrentDebitsCannotOverdrawAndLedgerStillReconciles() throws Exception {
        var bootstrap = stage3.bootstrap("并发家庭", "并发家长", "556677");
        var actor = bootstrap.session().actor();
        var child = stage3.addChild(
            actor, bootstrap.familyId(), "并发孩子", LocalDate.of(2017, 1, 1), AgeStage.CHILD_6_9);
        stage4.adjust(actor, bootstrap.familyId(), child.id(), AssetType.MONEY,
            new BigDecimal("10.00"), "并发测试准备金", "concurrency-credit");

        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        var futures = new ArrayList<java.util.concurrent.Future<Boolean>>();
        for (int index = 0; index < 2; index++) {
            String key = "concurrency-debit-" + index;
            futures.add(pool.submit(() -> {
                start.await();
                try {
                    stage4.adjust(actor, bootstrap.familyId(), child.id(), AssetType.MONEY,
                        new BigDecimal("-7.00"), "并发扣款", key);
                    return true;
                } catch (RuntimeException rejected) {
                    return false;
                }
            }));
        }
        start.countDown();
        int successes = 0;
        for (var future : futures) {
            if (future.get()) successes++;
        }
        pool.shutdownNow();

        assertThat(successes).isEqualTo(1);
        var wallet = stage3.wallet(actor, bootstrap.familyId(), child.id());
        assertThat(wallet.moneyBalance()).isEqualByComparingTo("3.00");
        assertThat(stage4.reconcile(actor, bootstrap.familyId(), child.id()).balanced()).isTrue();
    }
}
