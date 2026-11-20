-- #15: enrollment cascade. Before this fix, cancelling a session or revoking a bond left the
-- session's CONFIRMED and WAITLISTED enrollments active. This cancels them on existing databases.
-- It does not create notifications: those students were not told at the time, and a late notice
-- for an old session would only be noise. Safe to run more than once.

BEGIN;

UPDATE enrollment e
SET status         = 'CANCELLED',
    queue_position = NULL
FROM session s
WHERE e.session_id = s.id
  AND s.status = 'CANCELLED'
  AND e.status IN ('CONFIRMED', 'WAITLISTED');

COMMIT;
