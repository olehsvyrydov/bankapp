CREATE SCHEMA IF NOT EXISTS cash;

CREATE TABLE cash.transactions (
   id BIGSERIAL PRIMARY KEY,
   bank_account_id BIGINT NOT NULL,
   type VARCHAR(20) NOT NULL,
   amount DOUBLE PRECISION NOT NULL,
   currency VARCHAR(10) NOT NULL,
   status VARCHAR(20) NOT NULL,
   description TEXT,
   created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_transactions_bank_account ON cash.transactions(bank_account_id);
CREATE INDEX idx_transactions_created_at ON cash.transactions(created_at);
