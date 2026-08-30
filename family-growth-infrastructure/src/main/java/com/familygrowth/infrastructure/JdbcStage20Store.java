package com.familygrowth.infrastructure;

import com.familygrowth.application.FamilyGrowthService;
import com.familygrowth.application.Stage20Store;
import com.familygrowth.application.Stage3Service;
import com.familygrowth.domain.Stage20Models.DocumentaryAccessMode;
import com.familygrowth.domain.Stage20Models.DocumentarySource;
import com.familygrowth.domain.Stage20Models.DocumentaryStatus;
import com.familygrowth.domain.Stage20Models.ExperienceAudit;
import com.familygrowth.domain.Stage20Models.SchoolStage;
import com.familygrowth.domain.Stage20Models.PrimaryGradeBand;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcStage20Store implements Stage20Store {
    private final JdbcTemplate jdbc;

    JdbcStage20Store(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public StoredExperience experience(UUID familyId, UUID childId, Instant now) {
        return jdbc.query("""
            SELECT c.birth_date, p.stage_override, p.primary_band_override,
                   COALESCE(p.override_reason, '') AS override_reason,
                   COALESCE(p.haptics_enabled, TRUE) AS haptics_enabled,
                   COALESCE(p.version, 0) AS profile_version,
                   COALESCE(p.updated_at, c.created_at) AS updated_at
            FROM child_profile c
            LEFT JOIN child_experience_profile p ON p.child_id=c.id AND p.family_id=c.family_id
            WHERE c.family_id=? AND c.id=?
            """, (rs, row) -> storedExperience(rs), familyId, childId).stream().findFirst()
            .orElseThrow(FamilyGrowthService.NotFoundException::new);
    }

    @Override
    public StoredExperience updateExperience(
        UUID familyId, UUID childId, UUID actorId, ExperienceUpdate update, Instant now
    ) {
        LocalDate oldBirthDate = jdbc.query("""
            SELECT birth_date FROM child_profile WHERE family_id=? AND id=? FOR UPDATE
            """, (rs, row) -> rs.getObject(1, LocalDate.class), familyId, childId).stream().findFirst()
            .orElseThrow(FamilyGrowthService.NotFoundException::new);

        Optional<StoredExperience> existing = jdbc.query("""
            SELECT c.birth_date, p.stage_override, p.primary_band_override, p.override_reason, p.haptics_enabled,
                   p.version AS profile_version, p.updated_at
            FROM child_experience_profile p
            JOIN child_profile c ON c.id=p.child_id AND c.family_id=p.family_id
            WHERE p.family_id=? AND p.child_id=? FOR UPDATE
            """, (rs, row) -> storedExperience(rs), familyId, childId).stream().findFirst();
        StoredExperience old = existing.orElse(new StoredExperience(oldBirthDate, null, null, "", true, 0, now));
        if (old.version() != update.expectedVersion()) {
            throw new Stage3Service.ConflictException("Experience profile version changed");
        }

        jdbc.update("UPDATE child_profile SET birth_date=?, version=version+1 WHERE family_id=? AND id=?",
            update.birthDate(), familyId, childId);
        long nextVersion = old.version() + 1;
        if (existing.isPresent()) {
            jdbc.update("""
                UPDATE child_experience_profile
                SET stage_override=?, primary_band_override=?, override_reason=?, haptics_enabled=?, version=?, updated_by=?, updated_at=?
                WHERE family_id=? AND child_id=?
                """, enumName(update.stageOverride()), enumName(update.primaryBandOverride()), update.overrideReason(), update.hapticsEnabled(), nextVersion,
                actorId, Timestamp.from(now), familyId, childId);
        } else {
            jdbc.update("""
                INSERT INTO child_experience_profile
                (child_id,family_id,stage_override,primary_band_override,override_reason,haptics_enabled,version,updated_by,created_at,updated_at)
                VALUES (?,?,?,?,?,?,?,?,?,?)
                """, childId, familyId, enumName(update.stageOverride()), enumName(update.primaryBandOverride()), update.overrideReason(),
                update.hapticsEnabled(), nextVersion, actorId, Timestamp.from(now), Timestamp.from(now));
        }
        jdbc.update("""
            INSERT INTO child_experience_audit
            (id,family_id,child_id,actor_id,old_birth_date,new_birth_date,old_stage_override,new_stage_override,
             old_primary_band_override,new_primary_band_override,old_haptics_enabled,new_haptics_enabled,reason,created_at)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """, UUID.randomUUID(), familyId, childId, actorId, oldBirthDate, update.birthDate(),
            enumName(old.stageOverride()), enumName(update.stageOverride()), enumName(old.primaryBandOverride()),
            enumName(update.primaryBandOverride()), old.hapticsEnabled(),
            update.hapticsEnabled(), update.auditReason(), Timestamp.from(now));
        return new StoredExperience(update.birthDate(), update.stageOverride(), update.primaryBandOverride(), update.overrideReason(),
            update.hapticsEnabled(), nextVersion, now);
    }

    @Override
    public List<ExperienceAudit> experienceAudit(UUID familyId, UUID childId, int limit) {
        return jdbc.query("""
            SELECT * FROM child_experience_audit
            WHERE family_id=? AND child_id=? ORDER BY created_at DESC, id DESC LIMIT ?
            """, (rs, row) -> new ExperienceAudit(
            rs.getObject("id", UUID.class), rs.getObject("family_id", UUID.class),
            rs.getObject("child_id", UUID.class), rs.getObject("actor_id", UUID.class),
            rs.getObject("old_birth_date", LocalDate.class), rs.getObject("new_birth_date", LocalDate.class),
            schoolStage(rs.getString("old_stage_override")), schoolStage(rs.getString("new_stage_override")),
            primaryGradeBand(rs.getString("old_primary_band_override")), primaryGradeBand(rs.getString("new_primary_band_override")),
            rs.getBoolean("old_haptics_enabled"), rs.getBoolean("new_haptics_enabled"),
            rs.getString("reason"), rs.getTimestamp("created_at").toInstant()), familyId, childId, limit);
    }

    @Override
    public Optional<DocumentarySource> documentaryByKey(UUID familyId, String key) {
        return documentaries("SELECT * FROM documentary_source WHERE family_id=? AND idempotency_key=?", familyId, key)
            .stream().findFirst();
    }

    @Override
    public DocumentarySource createDocumentary(
        UUID familyId, DocumentarySource source, String key, String payloadHash
    ) {
        jdbc.update("""
            INSERT INTO documentary_source
            (id,family_id,school_stage,title,description,language_tag,duration_seconds,access_mode,source_reference,
             rights_holder,rights_reference,license_expires_on,status,idempotency_key,payload_hash,created_by,updated_by,
             version,created_at,updated_at)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """, source.id(), familyId, source.schoolStage().name(), source.title(), source.description(),
            source.languageTag(), source.durationSeconds(), source.accessMode().name(), source.sourceReference(),
            source.rightsHolder(), source.rightsReference(), source.licenseExpiresOn(), source.status().name(), key,
            payloadHash, source.createdBy(), source.updatedBy(), 0L, Timestamp.from(source.createdAt()),
            Timestamp.from(source.updatedAt()));
        return source;
    }

    @Override
    public List<DocumentarySource> documentaries(UUID familyId) {
        return documentaries("SELECT * FROM documentary_source WHERE family_id=? ORDER BY created_at,id", familyId);
    }

    @Override
    public Optional<DocumentarySource> documentary(UUID familyId, UUID sourceId) {
        return documentaries("SELECT * FROM documentary_source WHERE family_id=? AND id=?", familyId, sourceId)
            .stream().findFirst();
    }

    @Override
    public Optional<DocumentarySource> documentaryActionByKey(UUID familyId, String key) {
        return jdbc.query("""
            SELECT s.*, a.new_status AS replay_status
            FROM documentary_source_action a JOIN documentary_source s ON s.id=a.source_id AND s.family_id=a.family_id
            WHERE a.family_id=? AND a.idempotency_key=?
            """, (rs, row) -> documentary(rs, rs.getString("replay_status")), familyId, key).stream().findFirst();
    }

    @Override
    public DocumentarySource transitionDocumentary(
        UUID familyId, UUID sourceId, DocumentaryStatus target, UUID actorId, String key, Instant now
    ) {
        DocumentarySource current = jdbc.query("""
            SELECT * FROM documentary_source WHERE family_id=? AND id=? FOR UPDATE
            """, (rs, row) -> documentary(rs, null), familyId, sourceId).stream().findFirst()
            .orElseThrow(FamilyGrowthService.NotFoundException::new);
        boolean valid = (current.status() == DocumentaryStatus.DRAFT && target == DocumentaryStatus.APPROVED)
            || (current.status() != DocumentaryStatus.WITHDRAWN && target == DocumentaryStatus.WITHDRAWN);
        if (!valid) throw new Stage3Service.ConflictException("Invalid documentary status transition");
        jdbc.update("""
            INSERT INTO documentary_source_action
            (id,family_id,source_id,actor_id,old_status,new_status,idempotency_key,created_at)
            VALUES (?,?,?,?,?,?,?,?)
            """, UUID.randomUUID(), familyId, sourceId, actorId, current.status().name(), target.name(), key,
            Timestamp.from(now));
        jdbc.update("""
            UPDATE documentary_source SET status=?, updated_by=?, version=version+1, updated_at=?
            WHERE family_id=? AND id=?
            """, target.name(), actorId, Timestamp.from(now), familyId, sourceId);
        return new DocumentarySource(current.id(), current.familyId(), current.schoolStage(), current.title(),
            current.description(), current.languageTag(), current.durationSeconds(), current.accessMode(),
            current.sourceReference(), current.rightsHolder(), current.rightsReference(), current.licenseExpiresOn(),
            target, current.createdBy(), actorId, current.version() + 1, current.createdAt(), now);
    }

    private List<DocumentarySource> documentaries(String sql, Object... args) {
        return jdbc.query(sql, (rs, row) -> documentary(rs, null), args);
    }

    private static StoredExperience storedExperience(ResultSet rs) throws SQLException {
        String override = rs.getString("stage_override");
        return new StoredExperience(rs.getObject("birth_date", LocalDate.class), schoolStage(override),
            primaryGradeBand(rs.getString("primary_band_override")),
            rs.getString("override_reason"), rs.getBoolean("haptics_enabled"), rs.getLong("profile_version"),
            rs.getTimestamp("updated_at").toInstant());
    }

    private static DocumentarySource documentary(ResultSet rs, String statusOverride) throws SQLException {
        return new DocumentarySource(rs.getObject("id", UUID.class), rs.getObject("family_id", UUID.class),
            SchoolStage.valueOf(rs.getString("school_stage")), rs.getString("title"), rs.getString("description"),
            rs.getString("language_tag"), (Integer) rs.getObject("duration_seconds"),
            DocumentaryAccessMode.valueOf(rs.getString("access_mode")), rs.getString("source_reference"),
            rs.getString("rights_holder"), rs.getString("rights_reference"),
            rs.getObject("license_expires_on", LocalDate.class),
            DocumentaryStatus.valueOf(statusOverride == null ? rs.getString("status") : statusOverride),
            rs.getObject("created_by", UUID.class), rs.getObject("updated_by", UUID.class), rs.getLong("version"),
            rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant());
    }

    private static SchoolStage schoolStage(String value) {
        return value == null ? null : SchoolStage.valueOf(value);
    }

    private static PrimaryGradeBand primaryGradeBand(String value) {
        return value == null ? null : PrimaryGradeBand.valueOf(value);
    }

    private static String enumName(Enum<?> value) {
        return value == null ? null : value.name();
    }
}
