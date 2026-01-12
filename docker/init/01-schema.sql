-- accounts
CREATE TABLE accounts
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_name  VARCHAR(50) NOT NULL,
    balance_won BIGINT      NOT NULL DEFAULT 0,
    status      VARCHAR(20) NOT NULL,
    created_at  DATETIME(6) NOT NULL,
    updated_at  DATETIME(6) NOT NULL
);

-- daily_usages (composite key)
CREATE TABLE daily_usages
(
    account_id           BIGINT      NOT NULL,
    usage_date           DATE        NOT NULL,
    withdrawal_total_won BIGINT      NOT NULL DEFAULT 0,
    transfer_total_won   BIGINT      NOT NULL DEFAULT 0,
    created_at           DATETIME(6) NOT NULL,
    updated_at           DATETIME(6) NOT NULL,
    PRIMARY KEY (account_id, usage_date)
);

-- account_transaction_entries
CREATE TABLE account_transaction_entries
(
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id              BIGINT      NOT NULL,
    type                    VARCHAR(20) NOT NULL,
    amount_won              BIGINT      NOT NULL,
    counterparty_account_id BIGINT,
    related_transfer_id     BINARY(16),
    occurred_at             DATETIME(6) NOT NULL,
    created_at              DATETIME(6) NOT NULL,
    updated_at              DATETIME(6) NOT NULL,
    INDEX idx_account_id (account_id),
    INDEX idx_occurred_at (occurred_at)
);

-- idempotency_records
CREATE TABLE idempotency_records
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    idempotency_key VARCHAR(100) NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    transfer_id     BINARY(16),
    amount_won      BIGINT,
    fee_won         BIGINT,
    occurred_at     DATETIME(6),
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    UNIQUE KEY uk_idempotency_key (idempotency_key)
);
