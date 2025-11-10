CREATE SCHEMA IF NOT EXISTS accounts;

CREATE TABLE accounts.accounts (
   id BIGSERIAL PRIMARY KEY,
   username VARCHAR(255) UNIQUE NOT NULL,
   first_name VARCHAR(255) NOT NULL,
   last_name VARCHAR(255) NOT NULL,
   email VARCHAR(255),
   birth_date DATE NOT NULL,
   created_at TIMESTAMP NOT NULL,
   updated_at TIMESTAMP NOT NULL
);

CREATE TABLE accounts.bank_accounts (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL REFERENCES accounts.accounts(id) ON DELETE CASCADE,
    currency VARCHAR(10) NOT NULL,
    balance NUMERIC(38,2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT unique_account_currency UNIQUE (account_id, currency)
);

CREATE INDEX idx_accounts_username ON accounts.accounts(username);
CREATE INDEX idx_bank_accounts_account_id ON accounts.bank_accounts(account_id);
