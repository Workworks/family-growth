package com.familygrowth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.familygrowth.domain.Stage20Models.DocumentaryAccessMode;
import com.familygrowth.domain.Stage20Models.DocumentarySource;
import com.familygrowth.domain.Stage20Models.DocumentaryStatus;
import com.familygrowth.domain.Stage20Models.SchoolStage;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class Stage20ModelsTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 26);

    @Test
    void recommendsEveryAgeBoundaryWithoutFutureDates() {
        assertThat(SchoolStage.recommended(LocalDate.of(2024, 8, 26), TODAY)).isEqualTo(SchoolStage.PARENT_ONLY);
        assertThat(SchoolStage.recommended(LocalDate.of(2023, 8, 26), TODAY)).isEqualTo(SchoolStage.KINDERGARTEN);
        assertThat(SchoolStage.recommended(LocalDate.of(2020, 8, 26), TODAY)).isEqualTo(SchoolStage.PRIMARY);
        assertThat(SchoolStage.recommended(LocalDate.of(2014, 8, 26), TODAY)).isEqualTo(SchoolStage.JUNIOR_MIDDLE);
        assertThat(SchoolStage.recommended(LocalDate.of(2011, 8, 26), TODAY)).isEqualTo(SchoolStage.SENIOR_HIGH);
        assertThatThrownBy(() -> SchoolStage.recommended(TODAY.plusDays(1), TODAY))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void feedbackProfilesStayInsideChildSafetyLimits() {
        var kindergarten = Stage20Models.feedbackFor(SchoolStage.KINDERGARTEN, true);
        assertThat(kindergarten.maxAnimationMs()).isEqualTo(320);
        assertThat(kindergarten.hapticPulseCount()).isEqualTo(2);
        assertThat(kindergarten.primaryPressScale()).isEqualTo(1.10);
        assertThat(Stage20Models.feedbackFor(SchoolStage.KINDERGARTEN, false).hapticPulseCount()).isZero();
        assertThat(Stage20Models.feedbackFor(SchoolStage.SENIOR_HIGH, true).maxAnimationMs()).isLessThan(200);
        assertThat(Stage20Models.capabilitiesFor(SchoolStage.KINDERGARTEN)).containsExactly("TODAY", "DISCOVER", "MY", "PARENT_CO_USE");
        assertThat(Stage20Models.capabilitiesFor(SchoolStage.JUNIOR_MIDDLE)).contains("REWORK");
    }

    @Test
    void documentaryReferencesAreFailClosedByAccessMode() {
        assertThat(source(DocumentaryAccessMode.OFFICIAL_LINK, "https://science.nasa.gov/learn/").status())
            .isEqualTo(DocumentaryStatus.DRAFT);
        assertThat(source(DocumentaryAccessMode.ORIGINAL_OFFLINE, "asset://family-growth/original-1").accessMode())
            .isEqualTo(DocumentaryAccessMode.ORIGINAL_OFFLINE);
        assertThat(source(DocumentaryAccessMode.LICENSED_OFFLINE, "content-package://noaa/ocean-1").accessMode())
            .isEqualTo(DocumentaryAccessMode.LICENSED_OFFLINE);
        assertThatThrownBy(() -> source(DocumentaryAccessMode.OFFICIAL_LINK, "http://example.com/video"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> source(DocumentaryAccessMode.ORIGINAL_OFFLINE, "https://example.com/video"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private DocumentarySource source(DocumentaryAccessMode mode, String reference) {
        UUID actor = UUID.randomUUID();
        return new DocumentarySource(UUID.randomUUID(), UUID.randomUUID(), SchoolStage.PRIMARY,
            "海洋为什么有潮汐", "和家长一起看", "zh-CN", 300, mode, reference,
            "权利方", "rights-record-1", null, DocumentaryStatus.DRAFT, actor, actor, 0,
            Instant.parse("2026-08-26T00:00:00Z"), Instant.parse("2026-08-26T00:00:00Z"));
    }
}
