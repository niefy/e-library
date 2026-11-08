CREATE OR REPLACE FUNCTION elibrary_check_loan_capacity()
RETURNS TRIGGER AS $$
DECLARE
    active_count INTEGER;
    capacity INTEGER;
BEGIN
    IF NEW.status = 'ACTIVE' THEN
        SELECT COUNT(*)
        INTO active_count
        FROM loan
        WHERE book_id = NEW.book_id
          AND status = 'ACTIVE'
          AND id <> NEW.id;

        SELECT total_copies
        INTO capacity
        FROM book
        WHERE id = NEW.book_id;

        IF capacity IS NOT NULL AND active_count >= capacity THEN
            RAISE EXCEPTION 'LOAN_CAPACITY_EXCEEDED' USING ERRCODE = '23514';
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_loan_capacity_guard
BEFORE INSERT OR UPDATE OF status, book_id ON loan
FOR EACH ROW EXECUTE FUNCTION elibrary_check_loan_capacity();
