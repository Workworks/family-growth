package com.familygrowth.application;

import static com.familygrowth.domain.Stage30SyncModels.*;
import com.familygrowth.domain.Stage3Models.Actor;
import com.familygrowth.domain.Stage3Models.ActorRole;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional
public class Stage30SyncService {
    private final Stage3Service auth; private final Stage30SyncStore store; private final Clock clock;
    public Stage30SyncService(Stage3Service auth, Stage30SyncStore store, Clock clock){this.auth=auth;this.store=store;this.clock=clock;}
    public DeltaSyncResult delta(Actor actor, UUID family, UUID child, DeltaSyncRequest request, String key){
        auth.requireChildOrParent(actor,family,child);
        if(request==null||request.clientId()==null||!request.clientId().matches("[A-Za-z0-9._-]{8,100}")||request.afterCursor()<0)throw new IllegalArgumentException("Invalid sync request");
        Map<String,String> known=request.knownDigests()==null?Map.of():new TreeMap<>(request.knownDigests());
        if(known.size()>2000||known.entrySet().stream().anyMatch(e->e.getKey()==null||e.getKey().length()>160||e.getValue()==null||!e.getValue().matches("[0-9a-f]{64}")))throw new IllegalArgumentException("Invalid known digests");
        String idempotency=required(key), requestHash=hash(request.clientId()+"|"+request.afterCursor()+"|"+known);
        List<SyncFact> all=store.facts(family,child,actor.actorId(),actor.role()==ActorRole.PARENT);
        if(all.size()>2000)throw new IllegalStateException("Sync projection exceeds the safe page limit");
        String projectionHash=hash(all.stream().sorted(Comparator.comparing(SyncFact::key)).map(f->f.key()+":"+f.digest()).collect(Collectors.joining("|")));
        Optional<SyncCheckpoint> replay=store.replay(family,actor.actorId(),request.clientId(),idempotency);
        long cursor;
        if(replay.isPresent()){
            if(!replay.get().requestHash().equals(requestHash)||!replay.get().projectionHash().equals(projectionHash))throw new Stage3Service.ConflictException("Idempotency-Key payload or projection mismatch");
            cursor=replay.get().cursor();
        } else cursor=store.checkpoint(family,child,actor.actorId(),request.clientId(),idempotency,requestHash,projectionHash,clock.instant());
        Map<String,SyncFact> current=all.stream().collect(Collectors.toMap(SyncFact::key,f->f));
        List<SyncFact> changed=all.stream().filter(f->!f.digest().equals(known.get(f.key()))).sorted(Comparator.comparing(SyncFact::key)).toList();
        List<String> tombstones=known.keySet().stream().filter(k->!current.containsKey(k)).sorted().toList();
        return new DeltaSyncResult(cursor,changed,tombstones,projectionHash,clock.instant());
    }
    public static String digest(String canonical){return hash(canonical);}
    private static String required(String v){if(v==null||v.isBlank()||v.length()>100)throw new IllegalArgumentException("Idempotency-Key is required");return v.trim();}
    private static String hash(String v){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(v.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
}
