package com.familygrowth.application;

import com.familygrowth.application.EducationResourceDiscovery.DiscoveredCategory;
import com.familygrowth.domain.Stage20Models.SchoolStage;
import com.familygrowth.domain.Stage21ResourceModels.ChildResourceCategory;
import com.familygrowth.domain.Stage21ResourceModels.ChildResourceListing;
import com.familygrowth.domain.Stage21ResourceModels.EducationResourceSource;
import com.familygrowth.domain.Stage21ResourceModels.ParentResourceListing;
import com.familygrowth.domain.Stage21ResourceModels.ResourceRefreshStatus;
import com.familygrowth.domain.Stage21ResourceModels.ResourceSourceStatus;
import com.familygrowth.domain.Stage3Models.Actor;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class Stage21ResourceService {
    private final Stage3Service auth;
    private final Stage20Service experience;
    private final Stage21ResourceStore store;
    private final EducationResourceDiscovery discovery;
    private final Clock clock;

    public Stage21ResourceService(Stage3Service auth, Stage20Service experience, Stage21ResourceStore store,
                                  EducationResourceDiscovery discovery, Clock clock) {
        this.auth = auth;
        this.experience = experience;
        this.store = store;
        this.discovery = discovery;
        this.clock = clock;
    }

    @Transactional
    public ParentResourceListing create(Actor actor, UUID familyId, String title, String sourceUrl,
                                        List<SchoolStage> stages, String usageNote, String rawKey) {
        auth.requireParent(actor, familyId);
        String key = requireKey(rawKey);
        var source = new EducationResourceSource(UUID.randomUUID(), familyId, title, sourceUrl, stages, usageNote,
            ResourceSourceStatus.DRAFT, ResourceRefreshStatus.NEVER, "", null, List.of(), actor.actorId(),
            actor.actorId(), 0, clock.instant(), clock.instant());
        String payloadHash = hash(source.title() + "|" + source.sourceUrl() + "|" + source.schoolStages()
            + "|" + source.usageNote());
        EducationResourceSource result = store.byCreateKey(familyId, key).map(existing -> {
            String existingHash = hash(existing.title() + "|" + existing.sourceUrl() + "|"
                + existing.schoolStages() + "|" + existing.usageNote());
            if (!existingHash.equals(payloadHash)) throw new Stage3Service.ConflictException("Idempotency key payload mismatch");
            return existing;
        }).orElseGet(() -> store.create(source, key, payloadHash));
        return parentListing(result);
    }

    @Transactional(readOnly = true)
    public List<ParentResourceListing> parentSources(Actor actor, UUID familyId) {
        auth.requireParent(actor, familyId);
        return store.sources(familyId).stream().map(this::parentListing).toList();
    }

    public ParentResourceListing refresh(Actor actor, UUID familyId, UUID sourceId, String rawKey) {
        auth.requireParent(actor, familyId);
        String key = requireKey(rawKey);
        EducationResourceSource source = source(familyId, sourceId);
        if (source.status() == ResourceSourceStatus.WITHDRAWN) {
            throw new Stage3Service.ConflictException("Withdrawn source cannot be refreshed");
        }
        var replay = store.action(familyId, key);
        if (replay.isPresent()) {
            verifyReplay(replay.get(), sourceId, "REFRESH");
            return parentListing(source(familyId, sourceId));
        }
        List<DiscoveredCategory> categories;
        try {
            categories = discovery.discover(URI.create(source.sourceUrl()));
            if (categories.isEmpty()) throw new IllegalArgumentException("未发现可用栏目");
        } catch (RuntimeException ex) {
            String safe = ex instanceof IllegalArgumentException && ex.getMessage() != null
                ? ex.getMessage() : "来源暂时无法读取";
            return parentListing(store.refreshFailure(familyId, sourceId, safe, actor.actorId(), key, clock.instant()));
        }
        return parentListing(store.refreshSuccess(familyId, sourceId, categories, actor.actorId(), key, clock.instant()));
    }

    @Transactional
    public ParentResourceListing approve(Actor actor, UUID familyId, UUID sourceId, String key) {
        auth.requireParent(actor, familyId);
        String normalizedKey = requireKey(key);
        var replay = store.action(familyId, normalizedKey);
        if (replay.isPresent()) {
            verifyReplay(replay.get(), sourceId, "APPROVE");
            return parentListing(source(familyId, sourceId));
        }
        EducationResourceSource source = source(familyId, sourceId);
        if (source.refreshStatus() != ResourceRefreshStatus.READY || source.categories().isEmpty()) {
            throw new Stage3Service.ConflictException("Source requires a successful refresh before approval");
        }
        return parentListing(transition(familyId, sourceId, ResourceSourceStatus.APPROVED,
            actor.actorId(), normalizedKey, "APPROVE"));
    }

    @Transactional
    public ParentResourceListing withdraw(Actor actor, UUID familyId, UUID sourceId, String key) {
        auth.requireParent(actor, familyId);
        return parentListing(transition(familyId, sourceId, ResourceSourceStatus.WITHDRAWN,
            actor.actorId(), requireKey(key), "WITHDRAW"));
    }

    @Transactional(readOnly = true)
    public List<ChildResourceListing> childCatalog(Actor actor, UUID familyId, UUID childId) {
        auth.requireChildOrParent(actor, familyId, childId);
        SchoolStage stage = experience.experience(actor, familyId, childId).effectiveStage();
        if (stage == SchoolStage.KINDERGARTEN || stage == SchoolStage.PARENT_ONLY) return List.of();
        return store.sources(familyId).stream()
            .filter(source -> source.status() == ResourceSourceStatus.APPROVED)
            .filter(source -> source.schoolStages().contains(stage))
            .filter(source -> !source.categories().isEmpty())
            .map(source -> new ChildResourceListing(source.id(), source.title(), source.categories().stream()
                .map(category -> new ChildResourceCategory(category.id(), category.title(), category.displayOrder()))
                .toList(), source.lastRefreshedAt(), true))
            .toList();
    }

    private EducationResourceSource transition(UUID familyId, UUID sourceId, ResourceSourceStatus target,
                                               UUID actorId, String key, String action) {
        var replay = store.action(familyId, key);
        if (replay.isPresent()) {
            verifyReplay(replay.get(), sourceId, action);
            return source(familyId, sourceId);
        }
        return store.transition(familyId, sourceId, target, actorId, key, clock.instant());
    }

    private EducationResourceSource source(UUID familyId, UUID sourceId) {
        return store.source(familyId, sourceId).orElseThrow(FamilyGrowthService.NotFoundException::new);
    }

    private ParentResourceListing parentListing(EducationResourceSource source) {
        return new ParentResourceListing(source.id(), source.title(), source.sourceUrl(), source.schoolStages(),
            source.usageNote(), source.status(), source.refreshStatus(), source.refreshError(),
            source.lastRefreshedAt(), source.categories(), source.version());
    }

    private static void verifyReplay(Stage21ResourceStore.ActionReplay replay, UUID sourceId, String action) {
        if (!replay.sourceId().equals(sourceId) || !replay.actionType().equals(action)) {
            throw new Stage3Service.ConflictException("Idempotency key payload mismatch");
        }
    }

    private static String requireKey(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Idempotency-Key is required");
        String key = value.trim();
        if (key.length() > 120) throw new IllegalArgumentException("Idempotency-Key is too long");
        return key;
    }

    private static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
