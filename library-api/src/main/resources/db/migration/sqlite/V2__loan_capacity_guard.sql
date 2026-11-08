-- SQLite ignores SELECT ... FOR UPDATE. This trigger is the database backstop for
-- "active loans <= total copies" on the local profile; PostgreSQL uses a row lock plus the
-- equivalent trigger in db/migration/postgresql.

CREATE TRIGGER trg_loan_capacity_guard_insert
BEFORE INSERT ON loan
FOR EACH ROW
WHEN NEW.status = 'ACTIVE'
BEGIN
    SELECT RAISE(ABORT, 'LOAN_CAPACITY_EXCEEDED')
    WHERE (
        SELECT COUNT(*)
        FROM loan
        WHERE book_id = NEW.book_id AND status = 'ACTIVE'
    ) >= (
        SELECT total_copies FROM book WHERE id = NEW.book_id
    );
END;

CREATE TRIGGER trg_loan_capacity_guard_update
BEFORE UPDATE OF status, book_id ON loan
FOR EACH ROW
WHEN NEW.status = 'ACTIVE' AND (OLD.status <> 'ACTIVE' OR OLD.book_id <> NEW.book_id)
BEGIN
    SELECT RAISE(ABORT, 'LOAN_CAPACITY_EXCEEDED')
    WHERE (
        SELECT COUNT(*)
        FROM loan
        WHERE book_id = NEW.book_id AND status = 'ACTIVE' AND id <> OLD.id
    ) >= (
        SELECT total_copies FROM book WHERE id = NEW.book_id
    );
END;
