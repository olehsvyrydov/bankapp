CREATE SCHEMA IF NOT EXISTS exchange;

CREATE TABLE exchange.exchange_rates (
     id BIGSERIAL PRIMARY KEY,
     currency VARCHAR(10) UNIQUE NOT NULL,
     buy_rate DOUBLE PRECISION NOT NULL,
     sell_rate DOUBLE PRECISION NOT NULL,
     updated_at TIMESTAMP NOT NULL
);

-- Insert initial rates (RUB is base currency with rate 1.0)
INSERT INTO exchange.exchange_rates (currency, buy_rate, sell_rate, updated_at)
VALUES ('RUB', 1.0, 1.0, NOW()),
       ('USD', 75.0, 75.75, NOW()),
       ('CNY', 11.5, 11.62, NOW());
