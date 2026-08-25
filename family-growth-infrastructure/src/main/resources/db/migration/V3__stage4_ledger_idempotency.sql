ALTER TABLE ledger_entry
    ADD CONSTRAINT uq_ledger_entry_idempotency
    UNIQUE (family_id, entry_type, idempotency_key, asset_type);

CREATE INDEX idx_ledger_reconciliation
    ON ledger_entry(family_id, child_id, asset_type, created_at);
