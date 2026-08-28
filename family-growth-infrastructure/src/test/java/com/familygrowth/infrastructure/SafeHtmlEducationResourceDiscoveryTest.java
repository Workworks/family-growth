package com.familygrowth.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetAddress;
import java.net.URI;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

class SafeHtmlEducationResourceDiscoveryTest {
    @Test
    void extractsNavigationOnlyKeepsSameOriginAndDeduplicates() {
        var document = Jsoup.parse("""
            <header><a href='/'>首页</a><a href='/math'>数学</a><a href='/science'>科学探索</a>
            <a href='https://ads.example.net/tracker'>推荐广告</a><a href='/math'>数学课程</a></header>
            """, "https://learn.example.org/");
        var categories = SafeHtmlEducationResourceDiscovery.extractCategories(document,
            URI.create("https://learn.example.org/"));
        assertThat(categories).extracting("title").containsExactly("数学", "科学探索");
        assertThat(categories).extracting("url").containsExactly(
            "https://learn.example.org/math", "https://learn.example.org/science");
    }

    @Test
    void rejectsPrivateAndDocumentationAddresses() throws Exception {
        assertThat(SafeHtmlEducationResourceDiscovery.isPublic(InetAddress.getByName("10.0.0.1"))).isFalse();
        assertThat(SafeHtmlEducationResourceDiscovery.isPublic(InetAddress.getByName("127.0.0.1"))).isFalse();
        assertThat(SafeHtmlEducationResourceDiscovery.isPublic(InetAddress.getByName("203.0.113.1"))).isFalse();
        assertThat(SafeHtmlEducationResourceDiscovery.isPublic(InetAddress.getByName("8.8.8.8"))).isTrue();
    }
}
