package com.familygrowth.infrastructure;

import com.familygrowth.application.EducationResourceDiscovery.DiscoveredCategory;
import com.familygrowth.application.FamilyGrowthService;
import com.familygrowth.application.Stage21ResourceStore;
import com.familygrowth.application.Stage3Service;
import com.familygrowth.domain.Stage20Models.SchoolStage;
import com.familygrowth.domain.Stage21ResourceModels.EducationResourceSource;
import com.familygrowth.domain.Stage21ResourceModels.ResourceCategory;
import com.familygrowth.domain.Stage21ResourceModels.ResourceRefreshStatus;
import com.familygrowth.domain.Stage21ResourceModels.ResourceSourceStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class JdbcStage21ResourceStore implements Stage21ResourceStore {
    private final JdbcTemplate jdbc;

    JdbcStage21ResourceStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<EducationResourceSource> byCreateKey(UUID familyId, String key) {
        return querySources("SELECT * FROM education_resource_source WHERE family_id=? AND idempotency_key=?",
            familyId, key).stream().findFirst();
    }

    @Override
    @Transactional
    public EducationResourceSource create(EducationResourceSource source, String key, String payloadHash) {
        jdbc.update("""
            INSERT INTO education_resource_source
            (id,family_id,title,source_url,usage_note,status,refresh_status,refresh_error,last_refreshed_at,
             idempotency_key,payload_hash,created_by,updated_by,version,created_at,updated_at)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """, source.id(), source.familyId(), source.title(), source.sourceUrl(), source.usageNote(),
            source.status().name(), source.refreshStatus().name(), source.refreshError(), null, key, payloadHash,
            source.createdBy(), source.updatedBy(), 0L, Timestamp.from(source.createdAt()), Timestamp.from(source.updatedAt()));
        for (SchoolStage stage : source.schoolStages()) {
            jdbc.update("INSERT INTO education_resource_source_stage(source_id,school_stage) VALUES (?,?)",
                source.id(), stage.name());
        }
        return source;
    }

    @Override
    public List<EducationResourceSource> sources(UUID familyId) {
        return querySources("SELECT * FROM education_resource_source WHERE family_id=? ORDER BY created_at,id", familyId);
    }

    @Override
    public Optional<EducationResourceSource> source(UUID familyId, UUID sourceId) {
        return querySources("SELECT * FROM education_resource_source WHERE family_id=? AND id=?", familyId, sourceId)
            .stream().findFirst();
    }

    @Override
    public Optional<ActionReplay> action(UUID familyId, String key) {
        return jdbc.query("""
            SELECT source_id,action_type FROM education_resource_action WHERE family_id=? AND idempotency_key=?
            """, (rs, row) -> new ActionReplay(rs.getObject("source_id", UUID.class), rs.getString("action_type")),
            familyId, key).stream().findFirst();
    }

    @Override
    @Transactional
    public EducationResourceSource refreshSuccess(UUID familyId, UUID sourceId,
                                                   List<DiscoveredCategory> discovered, UUID actorId,
                                                   String key, Instant now) {
        EducationResourceSource current = locked(familyId, sourceId);
        ensureRefreshable(current);
        insertAction(familyId, sourceId, actorId, "REFRESH", key, now);
        jdbc.update("DELETE FROM education_resource_category WHERE source_id=?", sourceId);
        int order = 0;
        for (DiscoveredCategory category : discovered.stream().limit(30).toList()) {
            jdbc.update("""
                INSERT INTO education_resource_category(id,source_id,title,category_url,display_order,discovered_at)
                VALUES (?,?,?,?,?,?)
                """, UUID.randomUUID(), sourceId, category.title(), category.url(), order++, Timestamp.from(now));
        }
        jdbc.update("""
            UPDATE education_resource_source SET status='DRAFT',refresh_status='READY',refresh_error='',last_refreshed_at=?,
            updated_by=?,version=version+1,updated_at=? WHERE family_id=? AND id=?
            """, Timestamp.from(now), actorId, Timestamp.from(now), familyId, sourceId);
        return source(familyId, sourceId).orElseThrow();
    }

    @Override
    @Transactional
    public EducationResourceSource refreshFailure(UUID familyId, UUID sourceId, String safeError,
                                                   UUID actorId, String key, Instant now) {
        EducationResourceSource current = locked(familyId, sourceId);
        ensureRefreshable(current);
        insertAction(familyId, sourceId, actorId, "REFRESH", key, now);
        jdbc.update("""
            UPDATE education_resource_source SET refresh_status='FAILED',refresh_error=?,updated_by=?,
            version=version+1,updated_at=? WHERE family_id=? AND id=?
            """, truncate(safeError, 240), actorId, Timestamp.from(now), familyId, sourceId);
        return source(familyId, sourceId).orElseThrow();
    }

    @Override
    @Transactional
    public EducationResourceSource transition(UUID familyId, UUID sourceId, ResourceSourceStatus target,
                                               UUID actorId, String key, Instant now) {
        EducationResourceSource current = locked(familyId, sourceId);
        boolean valid = (current.status() == ResourceSourceStatus.DRAFT && target == ResourceSourceStatus.APPROVED)
            || (current.status() != ResourceSourceStatus.WITHDRAWN && target == ResourceSourceStatus.WITHDRAWN);
        if (!valid) throw new Stage3Service.ConflictException("Invalid resource source status transition");
        String action = target == ResourceSourceStatus.APPROVED ? "APPROVE" : "WITHDRAW";
        insertAction(familyId, sourceId, actorId, action, key, now);
        jdbc.update("""
            UPDATE education_resource_source SET status=?,updated_by=?,version=version+1,updated_at=?
            WHERE family_id=? AND id=?
            """, target.name(), actorId, Timestamp.from(now), familyId, sourceId);
        return source(familyId, sourceId).orElseThrow();
    }

    private EducationResourceSource locked(UUID familyId, UUID sourceId) {
        return jdbc.query("SELECT * FROM education_resource_source WHERE family_id=? AND id=? FOR UPDATE",
            (rs, row) -> mapSource(rs), familyId, sourceId).stream().findFirst()
            .map(source -> hydrate(source, familyId)).orElseThrow(FamilyGrowthService.NotFoundException::new);
    }

    private void ensureRefreshable(EducationResourceSource source) {
        if (source.status() == ResourceSourceStatus.WITHDRAWN) {
            throw new Stage3Service.ConflictException("Withdrawn source cannot be refreshed");
        }
    }

    private void insertAction(UUID familyId, UUID sourceId, UUID actorId, String action, String key, Instant now) {
        jdbc.update("""
            INSERT INTO education_resource_action(id,family_id,source_id,actor_id,action_type,idempotency_key,created_at)
            VALUES (?,?,?,?,?,?,?)
            """, UUID.randomUUID(), familyId, sourceId, actorId, action, key, Timestamp.from(now));
    }

    private List<EducationResourceSource> querySources(String sql, Object... args) {
        return jdbc.query(sql, (rs, row) -> mapSource(rs), args).stream()
            .map(source -> hydrate(source, source.familyId())).toList();
    }

    private EducationResourceSource hydrate(EducationResourceSource bare, UUID familyId) {
        List<SchoolStage> stages = jdbc.query("""
            SELECT school_stage FROM education_resource_source_stage WHERE source_id=? ORDER BY school_stage
            """, (rs, row) -> SchoolStage.valueOf(rs.getString(1)), bare.id());
        List<ResourceCategory> categories = jdbc.query("""
            SELECT id,title,category_url,display_order FROM education_resource_category
            WHERE source_id=? ORDER BY display_order,id
            """, (rs, row) -> new ResourceCategory(rs.getObject("id", UUID.class), rs.getString("title"),
            rs.getString("category_url"), rs.getInt("display_order")), bare.id());
        return new EducationResourceSource(bare.id(), familyId, bare.title(), bare.sourceUrl(), stages,
            bare.usageNote(), bare.status(), bare.refreshStatus(), bare.refreshError(), bare.lastRefreshedAt(),
            categories, bare.createdBy(), bare.updatedBy(), bare.version(), bare.createdAt(), bare.updatedAt());
    }

    private static EducationResourceSource mapSource(ResultSet rs) throws SQLException {
        Timestamp refreshed = rs.getTimestamp("last_refreshed_at");
        return new EducationResourceSource(rs.getObject("id", UUID.class), rs.getObject("family_id", UUID.class),
            rs.getString("title"), rs.getString("source_url"), List.of(SchoolStage.PRIMARY),
            rs.getString("usage_note"), ResourceSourceStatus.valueOf(rs.getString("status")),
            ResourceRefreshStatus.valueOf(rs.getString("refresh_status")), rs.getString("refresh_error"),
            refreshed == null ? null : refreshed.toInstant(), List.of(), rs.getObject("created_by", UUID.class),
            rs.getObject("updated_by", UUID.class), rs.getLong("version"), rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("updated_at").toInstant());
    }

    private static String truncate(String value, int max) {
        String text = value == null ? "来源暂时无法读取" : value.trim();
        return text.length() <= max ? text : text.substring(0, max);
    }
}
