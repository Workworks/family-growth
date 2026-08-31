package com.familygrowth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import com.familygrowth.domain.Stage9Models.*;
import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class Stage9ModelsTest {
    private final UsagePolicy policy=new UsagePolicy(UUID.randomUUID(),UUID.randomUUID(),"Asia/Shanghai",60,10,5,LocalTime.of(21,30),LocalTime.of(6,30),0,Instant.EPOCH);

    @Test void contiguousSessionSurvivesMidnightAndStartsMandatoryRest(){
        Instant beforeMidnight=Instant.parse("2026-08-30T15:59:00Z"),afterMidnight=Instant.parse("2026-08-30T16:01:00Z");
        UsageSessionState old=new UsageSessionState(5,beforeMidnight,null);
        UsageSessionState next=Stage9Models.nextSession(old,policy,5,afterMidnight,afterMidnight);
        assertThat(next.sessionMinutes()).isEqualTo(10);
        assertThat(next.restUntil()).isEqualTo(afterMidnight.plus(Duration.ofMinutes(5)));
    }

    @Test void replayDuringRestAndDelayedEventCannotExtendOrResetState(){
        Instant now=Instant.parse("2026-08-31T08:00:00Z");
        UsageSessionState resting=new UsageSessionState(10,now,now.plus(Duration.ofMinutes(5)));
        assertThat(Stage9Models.nextSession(resting,policy,5,now.plusSeconds(30),now.plusSeconds(30))).isEqualTo(resting);
        UsageSessionState available=new UsageSessionState(4,now.minusSeconds(60),null);
        assertThat(Stage9Models.nextSession(available,policy,5,now.minusSeconds(600),now)).isEqualTo(available);
    }

    @Test void fullRestGapStartsANewSession(){
        Instant now=Instant.parse("2026-08-31T08:10:00Z");
        UsageSessionState old=new UsageSessionState(8,now.minus(Duration.ofMinutes(6)),null);
        assertThat(Stage9Models.nextSession(old,policy,2,now,now).sessionMinutes()).isEqualTo(2);
    }
}
