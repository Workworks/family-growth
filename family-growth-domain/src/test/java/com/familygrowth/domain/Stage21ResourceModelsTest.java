package com.familygrowth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import org.junit.jupiter.api.Test;

class Stage21ResourceModelsTest {
    @Test
    void acceptsOnlyCredentialFreePublicHttpsHostnames() {
        assertThat(Stage21ResourceModels.normalizePublicHttpsUrl("https://Learn.Example.org/courses"))
            .isEqualTo("https://learn.example.org/courses");
        assertThatThrownBy(() -> Stage21ResourceModels.normalizePublicHttpsUrl("http://learn.example.org"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Stage21ResourceModels.normalizePublicHttpsUrl("https://127.0.0.1/private"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Stage21ResourceModels.normalizePublicHttpsUrl("https://user:pass@learn.example.org"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Stage21ResourceModels.normalizePublicHttpsUrl("https://learn.example.org:8443"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Stage21ResourceModels.normalizePublicHttpsUrl("https://learn.example.org/?token=secret"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sameOriginRejectsCrossSiteCategory() {
        assertThat(Stage21ResourceModels.sameOrigin(
            URI.create("https://learn.example.org/"), URI.create("https://learn.example.org/math"))).isTrue();
        assertThat(Stage21ResourceModels.sameOrigin(
            URI.create("https://learn.example.org/"), URI.create("https://video.example.org/math"))).isFalse();
    }
}
