package com.familygrowth.application;

import java.net.URI;
import java.util.List;

public interface EducationResourceDiscovery {
    record DiscoveredCategory(String title, String url) {
    }

    List<DiscoveredCategory> discover(URI sourceUrl);
}
