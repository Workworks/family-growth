package com.familygrowth.application;

import com.familygrowth.domain.Stage3Models.Actor;
import com.familygrowth.domain.Stage3Models.LedgerEntry;
import com.familygrowth.domain.Stage3Models.RewardGrant;
import com.familygrowth.domain.Stage3Models.TaskCompletion;
import com.familygrowth.domain.Stage3Models.Wallet;
import com.familygrowth.domain.Stage3Models.WalletReconciliation;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface Stage3Store {
    record PinCredential(UUID familyId, UUID parentId, String pinHash, int failedAttempts, Instant lockedUntil) {}
    record StoredSession(String tokenHash, Actor actor, Instant expiresAt, Instant revokedAt) {}

    void createPinCredential(UUID familyId, UUID parentId, String pinHash, Instant now);
    Optional<PinCredential> findPinCredential(UUID familyId, UUID parentId);
    void recordFailedPin(UUID parentId, int failedAttempts, Instant lockedUntil, Instant now);
    void clearFailedPin(UUID parentId, Instant now);
    void saveSession(String tokenHash, Actor actor, Instant expiresAt, Instant now);
    Optional<StoredSession> findSession(String tokenHash, Instant now);

    void ensureChildAccounts(UUID familyId, UUID childId, Instant now);
    boolean taskBelongsToChild(UUID familyId, UUID childId, UUID taskId);
    Optional<TaskCompletion> findCompletionByIdempotency(UUID familyId, String idempotencyKey);
    TaskCompletion submitCompletion(
        UUID familyId, UUID childId, UUID taskId, UUID submittedBy,
        String evidenceNote, String idempotencyKey, Instant now);
    Optional<TaskCompletion> findCompletion(UUID familyId, UUID completionId);
    TaskCompletion reviewCompletion(
        UUID familyId, UUID completionId, UUID reviewerId, boolean approve,
        RewardGrant rewards, String reviewNote, String idempotencyKey, Instant now);
    Wallet getWallet(UUID familyId, UUID childId);
    List<LedgerEntry> getLedger(UUID familyId, UUID childId, int limit);
    Optional<LedgerEntry> findAdjustment(UUID familyId, String idempotencyKey);
    LedgerEntry adjustWallet(
        UUID familyId, UUID childId, UUID actorId,
        com.familygrowth.domain.Stage3Models.AssetType assetType,
        java.math.BigDecimal delta, String reason, String idempotencyKey, Instant now);
    WalletReconciliation reconcile(UUID familyId, UUID childId);
}
