-- ============================================================
-- V19: Assign ADMIN role to the initial user
-- ============================================================
--
-- IMPORTANT:
-- Replace the email below with the actual initial/admin user's
-- email before running this migration.
--
-- If no matching user exists, this migration does nothing.
-- ============================================================


INSERT INTO model_has_roles (
    role_id,
    model_id,
    model_type
)
SELECT
    r.id,
    u.id,
    'App\\Models\\User'
FROM roles r
CROSS JOIN users u
WHERE r.name = 'ADMIN'
  AND r.guard_name = 'web'

  -- ----------------------------------------------------------
  -- CHANGE THIS EMAIL TO YOUR ACTUAL ADMIN USER
  -- ----------------------------------------------------------

  AND u.email = 'jaisurya1399@gmail.com'

  AND u.deleted_at IS NULL

  -- ----------------------------------------------------------
  -- Prevent duplicate composite-key mapping
  -- ----------------------------------------------------------

  AND NOT EXISTS (
      SELECT 1
      FROM model_has_roles mr
      WHERE mr.role_id = r.id
        AND mr.model_id = u.id
        AND mr.model_type = 'App\\Models\\User'
  );