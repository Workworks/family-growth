package com.familygrowth.infrastructure;

import com.familygrowth.application.EducationResourceDiscovery;
import com.familygrowth.domain.Stage21ResourceModels;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

@Component
class SafeHtmlEducationResourceDiscovery implements EducationResourceDiscovery {
    private static final int MAX_BODY_BYTES = 512 * 1024;
    private static final int MAX_REDIRECTS = 3;
    private static final Set<String> EXCLUDED = Set.of(
        "首页", "主页", "登录", "注册", "关于", "联系我们", "隐私", "条款", "下载", "客户端", "app",
        "home", "login", "register", "about", "contact", "privacy", "terms", "download"
    );

    @Override
    public List<DiscoveredCategory> discover(URI sourceUrl) {
        URI current = URI.create(Stage21ResourceModels.normalizePublicHttpsUrl(sourceUrl.toString()));
        URI origin = current;
        for (int redirects = 0; redirects <= MAX_REDIRECTS; redirects++) {
            verifyPublicDns(current.getHost());
            try {
                Connection.Response response = Jsoup.connect(current.toString())
                    .userAgent("FamilyGrowth-EducationCatalog/1.0")
                    .referrer("")
                    .timeout(8_000)
                    .maxBodySize(MAX_BODY_BYTES)
                    .followRedirects(false)
                    .ignoreHttpErrors(true)
                    .ignoreContentType(false)
                    .execute();
                int status = response.statusCode();
                if (status >= 300 && status < 400) {
                    String location = response.header("Location");
                    if (location == null || redirects == MAX_REDIRECTS) throw new IllegalArgumentException("来源重定向无效或过多");
                    URI next = current.resolve(location);
                    next = URI.create(Stage21ResourceModels.normalizePublicHttpsUrl(next.toString()));
                    if (!Stage21ResourceModels.sameOrigin(origin, next)) throw new IllegalArgumentException("来源重定向离开原网站");
                    current = next;
                    continue;
                }
                if (status < 200 || status >= 300) throw new IllegalArgumentException("来源返回 HTTP " + status);
                String type = response.contentType();
                if (type == null || !type.toLowerCase(Locale.ROOT).contains("text/html")) {
                    throw new IllegalArgumentException("来源不是 HTML 页面");
                }
                return extractCategories(response.parse(), origin);
            } catch (IllegalArgumentException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new IllegalArgumentException("来源暂时无法读取");
            }
        }
        throw new IllegalArgumentException("来源重定向过多");
    }

    static List<DiscoveredCategory> extractCategories(Document document, URI origin) {
        var unique = new LinkedHashMap<String, DiscoveredCategory>();
        List<Element> candidates = new ArrayList<>(document.select("nav a[href], [role=navigation] a[href], header a[href]"));
        if (candidates.isEmpty()) candidates.addAll(document.select("a[href]"));
        for (Element anchor : candidates) {
            String title = anchor.text().replaceAll("\\s+", " ").trim();
            if (title.length() < 2 || title.length() > 120 || EXCLUDED.contains(title.toLowerCase(Locale.ROOT))) continue;
            String absolute = anchor.absUrl("href");
            if (absolute.isBlank()) absolute = origin.resolve(anchor.attr("href")).toString();
            URI uri;
            try {
                uri = URI.create(Stage21ResourceModels.normalizePublicHttpsUrl(absolute));
            } catch (IllegalArgumentException ex) {
                continue;
            }
            if (!Stage21ResourceModels.sameOrigin(origin, uri)) continue;
            String path = uri.getPath() == null ? "/" : uri.getPath();
            if (path.equals("/") && uri.getQuery() == null) continue;
            String canonical = uri.toString();
            unique.putIfAbsent(canonical, new DiscoveredCategory(title, canonical));
            if (unique.size() == 30) break;
        }
        return List.copyOf(unique.values());
    }

    private static void verifyPublicDns(String host) {
        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            if (addresses.length == 0) throw new IllegalArgumentException("来源域名无法解析");
            for (InetAddress address : addresses) {
                if (!isPublic(address)) throw new IllegalArgumentException("来源域名指向非公网地址");
            }
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("来源域名无法解析");
        }
    }

    static boolean isPublic(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
            || address.isSiteLocalAddress() || address.isMulticastAddress()) return false;
        if (address instanceof Inet4Address) {
            byte[] b = address.getAddress();
            int a = b[0] & 255, c = b[1] & 255;
            if (a == 0 || a == 10 || a == 127 || a >= 224) return false;
            if (a == 100 && c >= 64 && c <= 127) return false;
            if (a == 169 && c == 254) return false;
            if (a == 172 && c >= 16 && c <= 31) return false;
            if (a == 192 && (c == 0 || c == 168)) return false;
            if (a == 198 && (c == 18 || c == 19 || c == 51)) return false;
            if (a == 203 && c == 0) return false;
        } else {
            String value = address.getHostAddress().toLowerCase(Locale.ROOT);
            if (value.startsWith("fc") || value.startsWith("fd") || value.startsWith("fe80")
                || value.startsWith("2001:db8")) return false;
        }
        return true;
    }
}
