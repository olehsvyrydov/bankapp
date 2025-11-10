CREATE SCHEMA IF NOT EXISTS transfer;

CREATE TABLE transfer.transfers (
    id BIGSERIAL PRIMARY KEY,
    from_bank_account_id BIGINT NOT NULL,
    to_bank_account_id BIGINT NOT NULL,
    amount NUMERIC(38,2) NOT NULL DEFAULT 0,
    from_currency VARCHAR(10) NOT NULL,
    to_currency VARCHAR(10) NOT NULL,
    converted_amount NUMERIC(38,2),
    status VARCHAR(20) NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_transfers_from_account ON transfer.transfers(from_bank_account_id);
CREATE INDEX idx_transfers_to_account ON transfer.transfers(to_bank_account_id);
