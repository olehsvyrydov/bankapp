-- Insert test accounts
-- Password is 'password' encrypted with BCrypt: $2a$10$X5wFWMLs3HZZhQzK/6zwDe1qKvpZvZZLBvPXFZxhLbQPpJXZUJgXK

INSERT INTO accounts.accounts (username, first_name, last_name, email, birth_date, created_at, updated_at)
VALUES
    ('admin', 'Admin', 'User', 'admin@bank.com', '1990-01-01', NOW(), NOW()),
    ('tester', 'Test', 'User', 'tester@bank.com', '1995-05-15', NOW(), NOW())
ON CONFLICT (username) DO NOTHING;

-- Insert bank accounts for test users (3 currencies per user)
INSERT INTO accounts.bank_accounts (account_id, currency, balance, created_at, updated_at)
SELECT
    a.id,
    c.currency,
    1000.0,
    NOW(),
    NOW()
FROM accounts.accounts a
CROSS JOIN (VALUES ('RUB'), ('USD'), ('CNY')) AS c(currency)
WHERE a.username IN ('admin', 'tester')
ON CONFLICT (account_id, currency) DO NOTHING;

