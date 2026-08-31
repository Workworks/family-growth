package com.familygrowth.application;

import com.familygrowth.domain.ChildProfile;
import com.familygrowth.domain.Stage3Models.Actor;
import com.familygrowth.domain.Stage3Models.ActorRole;
import com.familygrowth.domain.Stage3Models.AuthSession;
import com.familygrowth.domain.Stage3Models.LedgerEntry;
import com.familygrowth.domain.Stage3Models.RewardGrant;
import com.familygrowth.domain.Stage3Models.TaskCompletion;
import com.familygrowth.domain.Stage3Models.Wallet;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class Stage3Service {
    private static final Duration SESSION_LIFETIME = Duration.ofHours(12);
    private static final Duration PIN_LOCK_TIME = Duration.ofMinutes(15);
    private static final int MAX_PIN_FAILURES = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final FamilyGrowthService familyService;
    private final FamilyGrowthStore familyStore;
    private final Stage3Store store;
    private final Stage28RewardStore rewardGovernance;
    private final Clock clock;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    public Stage3Service(
        FamilyGrowthService familyService,
        FamilyGrowthStore familyStore,
        Stage3Store store,
        Stage28RewardStore rewardGovernance,
        Clock clock
    ) {
        this.familyService = familyService;
        this.familyStore = familyStore;
        this.store = store;
        this.rewardGovernance = rewardGovernance;
        this.clock = clock;
    }

    public BootstrapResult bootstrap(String familyName, String parentName, String pin) {
        validatePin(pin);
        var family = familyService.createFamily(familyName);
        var parent = familyService.addParent(family.id(), parentName);
        var now = clock.instant();
        store.createPinCredential(family.id(), parent.id(), encoder.encode(pin), now);
        var session = issueSession(new Actor(family.id(), parent.id(), ActorRole.PARENT, null), now);
        return new BootstrapResult(family.id(), parent.id(), session);
    }

    @Transactional(noRollbackFor = {AuthenticationException.class, PinLockedException.class})
    public AuthSession login(UUID familyId, UUID parentId, String pin) {
        var now = clock.instant();
        var credential = store.findPinCredential(familyId, parentId).orElseThrow(AuthenticationException::new);
        if (credential.lockedUntil() != null && credential.lockedUntil().isAfter(now)) {
            throw new PinLockedException(credential.lockedUntil());
        }
        if (!encoder.matches(pin, credential.pinHash())) {
            int failures = credential.failedAttempts() + 1;
            Instant lockedUntil = failures >= MAX_PIN_FAILURES ? now.plus(PIN_LOCK_TIME) : null;
            store.recordFailedPin(parentId, failures, lockedUntil, now);
            if (lockedUntil != null) {
                throw new PinLockedException(lockedUntil);
            }
            throw new AuthenticationException();
        }
        store.clearFailedPin(parentId, now);
        return issueSession(new Actor(familyId, parentId, ActorRole.PARENT, null), now);
    }

    @Transactional(readOnly = true)
    public Optional<Actor> authenticate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }
        return store.findSession(hash(rawToken), clock.instant()).map(Stage3Store.StoredSession::actor);
    }

    public AuthSession createChildSession(Actor actor, UUID childId) {
        requireParent(actor, actor.familyId());
        var child = child(actor.familyId(), childId);
        return issueSession(new Actor(actor.familyId(), child.id(), ActorRole.CHILD, child.id()), clock.instant());
    }

    @Transactional(noRollbackFor = {AuthenticationException.class, PinLockedException.class})
    public void verifyParentPin(Actor actor,String pin){requireParent(actor,actor.familyId());var now=clock.instant();var credential=store.findPinCredential(actor.familyId(),actor.actorId()).orElseThrow(AuthenticationException::new);if(credential.lockedUntil()!=null&&credential.lockedUntil().isAfter(now))throw new PinLockedException(credential.lockedUntil());if(!encoder.matches(pin,credential.pinHash())){int failures=credential.failedAttempts()+1;Instant lockedUntil=failures>=MAX_PIN_FAILURES?now.plus(PIN_LOCK_TIME):null;store.recordFailedPin(actor.actorId(),failures,lockedUntil,now);if(lockedUntil!=null)throw new PinLockedException(lockedUntil);throw new AuthenticationException();}store.clearFailedPin(actor.actorId(),now);}

    public ChildProfile addChild(
        Actor actor, UUID familyId, String name, LocalDate birthDate, com.familygrowth.domain.AgeStage stage
    ) {
        requireParent(actor, familyId);
        var child = familyService.addChild(familyId, name, birthDate, stage);
        store.ensureChildAccounts(familyId, child.id(), clock.instant());
        return child;
    }

    public void requireParent(Actor actor, UUID familyId) {
        if (actor == null) {
            throw new AuthenticationException();
        }
        if (!actor.familyId().equals(familyId)) {
            throw new FamilyGrowthService.NotFoundException();
        }
        if (actor.role() != ActorRole.PARENT) {
            throw new ForbiddenException();
        }
    }

    public void requireChildOrParent(Actor actor, UUID familyId, UUID childId) {
        if (actor == null) {
            throw new AuthenticationException();
        }
        if (!actor.familyId().equals(familyId)) {
            throw new FamilyGrowthService.NotFoundException();
        }
        if (actor.role() == ActorRole.CHILD && !childId.equals(actor.childId())) {
            throw new FamilyGrowthService.NotFoundException();
        }
        child(familyId, childId);
    }

    public TaskCompletion submit(
        Actor actor, UUID familyId, UUID childId, UUID taskId, String evidenceNote, String idempotencyKey
    ) {
        requireChild(actor, familyId, childId);
        requireKey(idempotencyKey);
        if (!store.taskBelongsToChild(familyId, childId, taskId)) {
            throw new FamilyGrowthService.NotFoundException();
        }
        var existing = store.findCompletionByIdempotency(familyId, idempotencyKey);
        if (existing.isPresent()) {
            if (!existing.get().childId().equals(childId) || !existing.get().taskId().equals(taskId)) {
                throw new ConflictException("Idempotency-Key was used for another submission");
            }
            return existing.get();
        }
        return store.submitCompletion(
            familyId, childId, taskId, actor.actorId(), normalize(evidenceNote), idempotencyKey, clock.instant());
    }

    public TaskCompletion review(
        Actor actor, UUID familyId, UUID completionId, boolean approve,
        RewardGrant rewards, String reviewNote, String idempotencyKey
    ) {
        requireParent(actor, familyId);
        requireKey(idempotencyKey);
        var completion = store.findCompletion(familyId, completionId)
            .orElseThrow(FamilyGrowthService.NotFoundException::new);
        if (completion.status() != com.familygrowth.domain.Stage3Models.CompletionStatus.SUBMITTED) {
            throw new ConflictException("Completion is already reviewed");
        }
        if (approve && rewards.xp() == 0 && rewards.coin() == 0 && rewards.money().signum() == 0) {
            throw new IllegalArgumentException("Approved completion requires a reward");
        }
        if (!approve && (rewards.xp() != 0 || rewards.coin() != 0 || rewards.money().signum() != 0)) {
            throw new IllegalArgumentException("Rejected completion cannot grant rewards");
        }
        RewardGrant actualRewards = approve
            ? rewardGovernance.governReward(familyId, completion.childId(), completionId, rewards,
                actor.actorId(), idempotencyKey, clock.instant())
            : rewards;
        boolean proposedSomething = rewards.xp() != 0 || rewards.coin() != 0 || rewards.money().signum() != 0;
        if (approve && proposedSomething && actualRewards.xp() == 0 && actualRewards.coin() == 0
            && actualRewards.money().signum() == 0) {
            throw new ConflictException("Reward budget would produce an empty reward");
        }
        return store.reviewCompletion(
            familyId, completionId, actor.actorId(), approve, actualRewards,
            normalize(reviewNote), idempotencyKey, clock.instant());
    }

    @Transactional(readOnly = true)
    public Wallet wallet(Actor actor, UUID familyId, UUID childId) {
        requireChildOrParent(actor, familyId, childId);
        return store.getWallet(familyId, childId);
    }

    @Transactional(readOnly = true)
    public List<LedgerEntry> ledger(Actor actor, UUID familyId, UUID childId, int limit) {
        requireChildOrParent(actor, familyId, childId);
        return store.getLedger(familyId, childId, Math.max(1, Math.min(limit, 200)));
    }

    private void requireChild(Actor actor, UUID familyId, UUID childId) {
        requireChildOrParent(actor, familyId, childId);
        if (actor.role() != ActorRole.CHILD) {
            throw new ForbiddenException();
        }
    }

    private ChildProfile child(UUID familyId, UUID childId) {
        return familyStore.findChild(childId).filter(c -> c.familyId().equals(familyId))
            .orElseThrow(FamilyGrowthService.NotFoundException::new);
    }

    private AuthSession issueSession(Actor actor, Instant now) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expiresAt = now.plus(SESSION_LIFETIME);
        store.saveSession(hash(token), actor, expiresAt, now);
        return new AuthSession(token, actor, expiresAt);
    }

    private static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static void validatePin(String pin) {
        if (pin == null || !pin.matches("\\d{6}")) {
            throw new IllegalArgumentException("PIN must contain exactly six digits");
        }
    }

    private static void requireKey(String key) {
        if (key == null || key.isBlank() || key.length() > 100) {
            throw new IllegalArgumentException("Valid Idempotency-Key is required");
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    public record BootstrapResult(UUID familyId, UUID parentId, AuthSession session) {}
    public static final class AuthenticationException extends RuntimeException {}
    public static final class ForbiddenException extends RuntimeException {}
    public static final class ConflictException extends RuntimeException {
        public ConflictException(String message) { super(message); }
    }
    public static final class PinLockedException extends RuntimeException {
        private final Instant lockedUntil;
        public PinLockedException(Instant lockedUntil) { this.lockedUntil = lockedUntil; }
        public Instant lockedUntil() { return lockedUntil; }
    }
}
