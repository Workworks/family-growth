package com.familygrowth;

import static org.assertj.core.api.Assertions.assertThat;

import com.familygrowth.application.Stage17Service;
import com.familygrowth.application.Stage3Service;
import com.familygrowth.application.Stage4Service;
import com.familygrowth.domain.AgeStage;
import com.familygrowth.domain.Stage3Models.AssetType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
class Stage17ConcurrencyTest {
    @Autowired Stage3Service stage3;
    @Autowired Stage4Service stage4;
    @Autowired Stage17Service stage17;
    @Autowired JdbcTemplate jdbc;

    @Test
    void approvalAndDebitRaceCannotConsumeTheSameAvailableMoney() throws Exception {
        var fixture = fixture("approval-debit", "70.00");
        CountDownLatch start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        var approve = pool.submit(() -> attempt(start, () -> stage17.approve(
            fixture.actor, fixture.family, fixture.request, "race-approve")));
        var debit = pool.submit(() -> attempt(start, () -> stage4.adjust(
            fixture.actor, fixture.family, fixture.child, AssetType.MONEY,
            new BigDecimal("-40.00"), "race debit", "race-debit")));
        start.countDown();

        assertThat(List.of(approve.get(), debit.get()).stream().filter(Boolean::booleanValue).count())
            .isEqualTo(1);
        pool.shutdownNow();
        var wallet = stage3.wallet(fixture.actor, fixture.family, fixture.child);
        assertThat(wallet.reservedMoney()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat(wallet.availableMoney()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat(wallet.reservedMoney()).isLessThanOrEqualTo(wallet.moneyBalance());
    }

    @Test
    void concurrentPaidReplayCreatesOneLedgerDebit() throws Exception {
        var fixture = fixture("paid-replay", "60.00");
        stage17.approve(fixture.actor, fixture.family, fixture.request, "paid-approve");
        CountDownLatch start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        var first = pool.submit(() -> attempt(start, () -> stage17.markPaid(
            fixture.actor, fixture.family, fixture.request, "paid-replay-key")));
        var second = pool.submit(() -> attempt(start, () -> stage17.markPaid(
            fixture.actor, fixture.family, fixture.request, "paid-replay-key")));
        start.countDown();

        assertThat(first.get()).isTrue();
        assertThat(second.get()).isTrue();
        pool.shutdownNow();
        var wallet = stage3.wallet(fixture.actor, fixture.family, fixture.child);
        assertThat(wallet.moneyBalance()).isEqualByComparingTo("40.00");
        assertThat(wallet.reservedMoney()).isEqualByComparingTo("0.00");
        assertThat(jdbc.queryForObject("""
            SELECT COUNT(*) FROM ledger_entry WHERE family_id=? AND business_type='WITHDRAWAL'
            AND business_id=?
            """, Integer.class, fixture.family, fixture.request)).isEqualTo(1);
    }

    @Test
    void concurrentQuoteIdempotencyRejectsDifferentPayload() throws Exception {
        var bootstrap = stage3.bootstrap("Stage17-quote-race", "家长", "667788");
        var actor = bootstrap.session().actor();
        var child = stage3.addChild(actor, bootstrap.familyId(), "孩子",
            LocalDate.of(2017, 1, 1), AgeStage.CHILD_6_9);
        CountDownLatch start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        Future<Boolean> first = pool.submit(() -> attempt(start, () -> stage17.quote(
            actor, bootstrap.familyId(), child.id(), new BigDecimal("10.00"), "quote-race-key")));
        Future<Boolean> second = pool.submit(() -> attempt(start, () -> stage17.quote(
            actor, bootstrap.familyId(), child.id(), new BigDecimal("11.00"), "quote-race-key")));
        start.countDown();

        assertThat(List.of(first.get(), second.get()).stream().filter(Boolean::booleanValue).count())
            .isEqualTo(1);
        pool.shutdownNow();
        assertThat(jdbc.queryForObject("""
            SELECT COUNT(*) FROM withdrawal_quote WHERE family_id=? AND idempotency_key=?
            """, Integer.class, bootstrap.familyId(), "quote-race-key")).isEqualTo(1);
    }

    private Fixture fixture(String suffix, String amount) {
        var bootstrap = stage3.bootstrap("Stage17-" + suffix, "家长", "556677");
        var actor = bootstrap.session().actor();
        var child = stage3.addChild(actor, bootstrap.familyId(), "孩子",
            LocalDate.of(2017, 1, 1), AgeStage.CHILD_6_9);
        stage4.adjust(actor, bootstrap.familyId(), child.id(), AssetType.MONEY,
            new BigDecimal("100.00"), "fixture", suffix + "-credit");
        var quote = stage17.quote(actor, bootstrap.familyId(), child.id(),
            new BigDecimal(amount), suffix + "-quote");
        var request = stage17.request(actor, bootstrap.familyId(), child.id(), quote.id(),
            suffix + "-request");
        return new Fixture(actor, bootstrap.familyId(), child.id(), request.id());
    }

    private static boolean attempt(CountDownLatch start, Runnable operation) {
        try {
            start.await();
            operation.run();
            return true;
        } catch (RuntimeException rejected) {
            return false;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private record Fixture(
        com.familygrowth.domain.Stage3Models.Actor actor,
        UUID family,
        UUID child,
        UUID request
    ) {
    }
}
