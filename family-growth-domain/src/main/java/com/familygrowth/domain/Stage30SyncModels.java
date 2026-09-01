package com.familygrowth.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class Stage30SyncModels {
    private Stage30SyncModels() {}
    public record SyncFact(String key, String digest, Map<String,Object> payload) {}
    public record DeltaSyncRequest(String clientId, long afterCursor, Map<String,String> knownDigests) {}
    public record DeltaSyncResult(long cursor, List<SyncFact> changed, List<String> tombstones,
                                  String projectionDigest, Instant serverTime) {}
    public record SyncCheckpoint(long cursor, String requestHash, String projectionHash) {}
}
