CREATE TABLE transactions (
    id                      UUID                     NOT NULL PRIMARY KEY,
    idempotency_key         VARCHAR(100)             UNIQUE,
    account_id              VARCHAR(64)              NOT NULL,
    type                    VARCHAR(10)              NOT NULL,
    amount                  NUMERIC(19, 2)           NOT NULL,
    currency                VARCHAR(3)               NOT NULL,
    description             VARCHAR(255),
    status                  VARCHAR(20)              NOT NULL,
    provider_reference      VARCHAR(128),
    provider_transaction_id VARCHAR(128),
    balance_after           NUMERIC(19, 2),
    failure_code            VARCHAR(64),
    failure_message         VARCHAR(500),
    provider_references     VARCHAR(150),
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_transactions_account_created ON transactions (account_id, created_at DESC);
CREATE INDEX idx_transactions_status_created  ON transactions (status, created_at DESC);
CREATE INDEX idx_transactions_created         ON transactions (created_at DESC);
