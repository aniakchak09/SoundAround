DO $$
DECLARE
  u1 uuid := '00000000-feed-0000-0000-000000000001';
  u2 uuid := '00000000-feed-0000-0000-000000000002';
  u3 uuid := '00000000-feed-0000-0000-000000000003';
  u4 uuid := '00000000-feed-0000-0000-000000000004';
  u5 uuid := '00000000-feed-0000-0000-000000000005';
  u6  uuid := '00000000-feed-0000-0000-000000000006';
  u7  uuid := '00000000-feed-0000-0000-000000000007';
  u8  uuid := '00000000-feed-0000-0000-000000000008';
  u9  uuid := '00000000-feed-0000-0000-000000000009';
  u10 uuid := '00000000-feed-0000-0000-000000000010';
  u11 uuid := '00000000-feed-0000-0000-000000000011';
  u12 uuid := '00000000-feed-0000-0000-000000000012';
  u13 uuid := '00000000-feed-0000-0000-000000000013';
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
  (u6,  'authenticated', 'authenticated', 'raluca_demo@soundaround.app',   '', now(), now(), now(), '{"provider":"email","providers":["email"]}', '{}', false),
  (u7,  'authenticated', 'authenticated', 'andrei_demo@soundaround.app',   '', now(), now(), now(), '{"provider":"email","providers":["email"]}', '{}', false),
  (u8,  'authenticated', 'authenticated', 'elena_demo@soundaround.app',    '', now(), now(), now(), '{"provider":"email","providers":["email"]}', '{}', false),
  (u9,  'authenticated', 'authenticated', 'vlad_demo@soundaround.app',     '', now(), now(), now(), '{"provider":"email","providers":["email"]}', '{}', false),
  (u10, 'authenticated', 'authenticated', 'cristina_demo@soundaround.app', '', now(), now(), now(), '{"provider":"email","providers":["email"]}', '{}', false),
  (u11, 'authenticated', 'authenticated', 'bogdan_demo@soundaround.app',   '', now(), now(), now(), '{"provider":"email","providers":["email"]}', '{}', false),
  (u12, 'authenticated', 'authenticated', 'diana_demo@soundaround.app',    '', now(), now(), now(), '{"provider":"email","providers":["email"]}', '{}', false),
  (u13, 'authenticated', 'authenticated', 'radu_demo@soundaround.app',     '', now(), now(), now(), '{"provider":"email","providers":["email"]}', '{}', false)
ON CONFLICT (id) DO NOTHING;

INSERT INTO profiles (id, username, lastfm_username, bio, privacy_mode)
VALUES
  (u1, 'alex_m',   'RJ',          'indie & alternative enthusiast',  'public'),
  (u2, 'maria_p',  'Babs_05',     'jazz / soul / everything nice',   'public'),
  (u3, 'dan_r',    NULL,          'metal head, no apologies',        'public'),
  (u4, 'ioana_c',  NULL,          'bedroom pop & lo-fi',             'public'),
  (u5, 'mihai_v',  NULL,          'techno & electronic',             'public'),
  (u6,  'raluca_t',   NULL,          'classical + film scores',         'public'),
  (u7,  'andrei_b',  'andrei_b_lfm','hip-hop & trap all day',          'public'),
  (u8,  'elena_d',   NULL,          'pop & singer-songwriter vibes',   'public'),
  (u9,  'vlad_n',    NULL,          'punk rock & hardcore',            'public'),
  (u10, 'cristina_m',NULL,          'R&B and neo-soul lover',          'public'),
  (u11, 'bogdan_s',  NULL,          'ambient & post-rock explorer',    'public'),
  (u12, 'diana_f',   NULL,          'latin & reggaeton nonstop',       'public'),
  (u13, 'radu_c',    NULL,          'folk & acoustic fingerpicking',   'public')
ON CONFLICT (id) DO NOTHING;


INSERT INTO locations (user_id, track_name, artist_name, album_art, is_playing, lat, lng, synced_at, last_seen_at)
VALUES
  (u1, 'Arabella',                 'Arctic Monkeys',      '', true,  44.4524, 26.0779, now(), now()),
  (u2, 'Feeling Good',             'Nina Simone',         '', true,  44.4310, 26.0940, now(), now()),
  (u3, 'Enter Sandman',            'Metallica',           '', false, 44.4621, 26.1098, now(), now()),
  (u4, 'Motion Picture Soundtrack','Radiohead',           '', true,  44.4312, 26.0680, now(), now()),
  (u5, 'Around the World',         'Daft Punk',           '', true,  44.4712, 26.0834, now(), now()),
  (u6,  'Clair de Lune',            'Claude Debussy',      '', false, 44.4355, 26.1008, now(), now()),
  (u7,  'HUMBLE.',                  'Kendrick Lamar',      '', true,  44.4480, 26.0720, now(), now()),
  (u8,  'Anti-Hero',                'Taylor Swift',        '', true,  44.4390, 26.0860, now(), now()),
  (u9,  'Should I Stay or Should I Go','The Clash',        '', true,  44.4560, 26.1150, now(), now()),
  (u10, 'Golden',                   'Jill Scott',          '', true,  44.4270, 26.0990, now(), now()),
  (u11, 'Svefn-g-englar',           'Sigur Rós',           '', false, 44.4640, 26.0650, now(), now()),
  (u12, 'Shakira: Bzrp Music Sessions, Vol. 53','Bizarrap', '', true,  44.4420, 26.1040, now(), now()),
  (u13, 'Fast Car',                 'Tracy Chapman',       '', false, 44.4510, 26.0780, now(), now())
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
    '00000000-feed-0000-0000-000000000006'::uuid,
    '00000000-feed-0000-0000-000000000007'::uuid,
    '00000000-feed-0000-0000-000000000008'::uuid,
    '00000000-feed-0000-0000-000000000009'::uuid,
    '00000000-feed-0000-0000-000000000010'::uuid,
    '00000000-feed-0000-0000-000000000011'::uuid,
    '00000000-feed-0000-0000-000000000012'::uuid,
    '00000000-feed-0000-0000-000000000013'::uuid
  ];
BEGIN
  DELETE FROM locations  WHERE user_id = ANY(fake_ids);
  DELETE FROM profiles   WHERE id      = ANY(fake_ids);
  DELETE FROM auth.users WHERE id      = ANY(fake_ids);
END $$;
*/
