-- Demo catalog. This location is included only by the default local profile; tests and
-- production start with an empty catalog.
INSERT INTO book (id, isbn, title, author, total_copies, version, created_at) VALUES
    ('11111111-1111-1111-1111-111111111111', '9780132350884', 'Clean Code', 'Robert C. Martin', 3, 0, 1704067200000),
    ('22222222-2222-2222-2222-222222222222', '9781449373320', 'Designing Data-Intensive Applications', 'Martin Kleppmann', 2, 0, 1704067200000),
    ('33333333-3333-3333-3333-333333333333', '9780321125217', 'Domain-Driven Design', 'Eric Evans', 2, 0, 1704067200000),
    ('44444444-4444-4444-4444-444444444444', '9780134685991', 'Effective Java', 'Joshua Bloch', 1, 0, 1704067200000),
    ('55555555-5555-5555-5555-555555555555', '9780135957059', 'The Pragmatic Programmer', 'David Thomas, Andrew Hunt', 2, 0, 1704067200000),
    ('66666666-6666-6666-6666-666666666666', '9780134757599', 'Refactoring', 'Martin Fowler', 1, 0, 1704067200000),
    ('77777777-7777-7777-7777-777777777777', '9781491929124', 'Site Reliability Engineering', 'Niall Richard Murphy et al.', 2, 0, 1704067200000),
    ('88888888-8888-8888-8888-888888888888', '9787111544937', '深入理解计算机系统', 'Randal E. Bryant, David R. O''Hallaron', 3, 0, 1704067200000);
