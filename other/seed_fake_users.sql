DO $$
DECLARE
  u1 uuid := '00000000-feed-0000-0000-000000000001';
  u2 uuid := '00000000-feed-0000-0000-000000000002';
  u3 uuid := '00000000-feed-0000-0000-000000000003';
  u4 uuid := '00000000-feed-0000-0000-000000000004';
  u5 uuid := '00000000-feed-0000-0000-000000000005';
  u6 uuid := '00000000-feed-0000-0000-000000000006';
BEGIN

INSERT INTO auth.users (
  id, aud, role,
  email, encrypted_password,
  email_confirmed_at,
  created_at, updated_at,
  raw_app_meta_data, raw_user_meta_data,
  is_super_admin
) VALUES
  (u1, 'authenticated', 'authenticated', 'alex_demo@soundaround.app',    '', now(), now(), now(), '{"provider":"email","providers":["email"]}', '{}', false),
  (u2, 'authenticated', 'authenticated', 'maria_demo@soundaround.app',   '', now(), now(), now(), '{"provider":"email","providers":["email"]}', '{}', false),
  (u3, 'authenticated', 'authenticated', 'dan_demo@soundaround.app',     '', now(), now(), now(), '{"provider":"email","providers":["email"]}', '{}', false),
  (u4, 'authenticated', 'authenticated', 'ioana_demo@soundaround.app',   '', now(), now(), now(), '{"provider":"email","providers":["email"]}', '{}', false),
  (u5, 'authenticated', 'authenticated', 'mihai_demo@soundaround.app',   '', now(), now(), now(), '{"provider":"email","providers":["email"]}', '{}', false),
  (u6, 'authenticated', 'authenticated', 'raluca_demo@soundaround.app',  '', now(), now(), now(), '{"provider":"email","providers":["email"]}', '{}', false)
ON CONFLICT (id) DO NOTHING;

INSERT INTO profiles (id, username, lastfm_username, bio, privacy_mode)
VALUES
  (u1, 'alex_m',   'RJ',          'indie & alternative enthusiast',  'public'),
  (u2, 'maria_p',  'Babs_05',     'jazz / soul / everything nice',   'public'),
  (u3, 'dan_r',    NULL,          'metal head, no apologies',        'public'),
  (u4, 'ioana_c',  NULL,          'bedroom pop & lo-fi',             'public'),
  (u5, 'mihai_v',  NULL,          'techno & electronic',             'public'),
  (u6, 'raluca_t', NULL,          'classical + film scores',         'public')
ON CONFLICT (id) DO NOTHING;


INSERT INTO locations (user_id, track_name, artist_name, album_art, is_playing, lat, lng, synced_at, last_seen_at)
VALUES
  (u1, 'Arabella',                 'Arctic Monkeys',      '', true,  44.4524, 26.0779, now(), now()),
  (u2, 'Feeling Good',             'Nina Simone',         '', true,  44.4310, 26.0940, now(), now()),
  (u3, 'Enter Sandman',            'Metallica',           '', false, 44.4621, 26.1098, now(), now()),
  (u4, 'Motion Picture Soundtrack','Radiohead',           '', true,  44.4312, 26.0680, now(), now()),
  (u5, 'Around the World',         'Daft Punk',           '', true,  44.4712, 26.0834, now(), now()),
  (u6, 'Clair de Lune',            'Claude Debussy',      '', false, 44.4355, 26.1008, now(), now())
ON CONFLICT (user_id) DO UPDATE SET
  track_name   = EXCLUDED.track_name,
  artist_name  = EXCLUDED.artist_name,
  album_art    = EXCLUDED.album_art,
  is_playing   = EXCLUDED.is_playing,
  lat          = EXCLUDED.lat,
  lng          = EXCLUDED.lng,
  synced_at    = EXCLUDED.synced_at,
  last_seen_at = EXCLUDED.last_seen_at;

END $$;


-- =============================================================
-- CLEANUP — run this block to remove all fake users
-- =============================================================
/*
DO $$
DECLARE
  fake_ids uuid[] := ARRAY[
    '00000000-feed-0000-0000-000000000001'::uuid,
    '00000000-feed-0000-0000-000000000002'::uuid,
    '00000000-feed-0000-0000-000000000003'::uuid,
    '00000000-feed-0000-0000-000000000004'::uuid,
    '00000000-feed-0000-0000-000000000005'::uuid,
    '00000000-feed-0000-0000-000000000006'::uuid
  ];
BEGIN
  DELETE FROM locations  WHERE user_id = ANY(fake_ids);
  DELETE FROM profiles   WHERE id      = ANY(fake_ids);
  DELETE FROM auth.users WHERE id      = ANY(fake_ids);
END $$;
*/
