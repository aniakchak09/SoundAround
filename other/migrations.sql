-- =============================================================
-- migrations.sql — run these once against the Supabase DB
-- =============================================================


-- #1  Unique constraint on lastfm_username in profiles
-- Prevents two accounts from linking the same last.fm username.
-- NULLs are allowed (users without a last.fm account).
-- Run only if the constraint doesn't already exist.
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint
    WHERE conname = 'profiles_lastfm_username_key'
  ) THEN
    ALTER TABLE profiles
      ADD CONSTRAINT profiles_lastfm_username_key
      UNIQUE (lastfm_username);
  END IF;
END $$;
