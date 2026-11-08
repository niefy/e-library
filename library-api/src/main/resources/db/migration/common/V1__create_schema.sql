CREATE TABLE book (
    id           VARCHAR(36)  NOT NULL,
    isbn         VARCHAR(20)  NOT NULL,
    title        VARCHAR(255) NOT NULL,
    author       VARCHAR(255) NOT NULL,
    total_copies INTEGER      NOT NULL,
    version      BIGINT       NOT NULL DEFAULT 0,
    created_at   BIGINT       NOT NULL,
    CONSTRAINT pk_book PRIMARY KEY (id),
    CONSTRAINT uq_book_isbn UNIQUE (isbn),
    CONSTRAINT ck_book_total_copies CHECK (total_copies > 0)
);

CREATE TABLE loan (
    id           VARCHAR(36)  NOT NULL,
    book_id      VARCHAR(36)  NOT NULL,
    user_id      VARCHAR(100) NOT NULL,
    status       VARCHAR(20)  NOT NULL,
    borrowed_at  BIGINT       NOT NULL,
    due_at       BIGINT       NOT NULL,
    returned_at  BIGINT,
    version      BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_loan PRIMARY KEY (id),
    CONSTRAINT fk_loan_book FOREIGN KEY (book_id) REFERENCES book (id),
    CONSTRAINT ck_loan_status CHECK (status IN ('ACTIVE', 'RETURNED')),
    CONSTRAINT ck_loan_returned_at CHECK (
        (status = 'ACTIVE' AND returned_at IS NULL)
        OR (status = 'RETURNED' AND returned_at IS NOT NULL)
    ),
    CONSTRAINT ck_loan_due_date CHECK (due_at = borrowed_at + 1209600000)
);

-- Invariant: at most one ACTIVE loan per user and book. The WHERE clause makes the index partial.
CREATE UNIQUE INDEX uq_loan_active_user_book
    ON loan (user_id, book_id)
    WHERE status = 'ACTIVE';

CREATE INDEX idx_loan_user_status ON loan (user_id, status);
CREATE INDEX idx_loan_user_borrowed ON loan (user_id, borrowed_at DESC);
CREATE INDEX idx_loan_book_status ON loan (book_id, status);
