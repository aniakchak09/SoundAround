import "@supabase/functions-js/edge-runtime.d.ts";

const LASTFM_API = "https://ws.audioscrobbler.com/2.0/";
const API_KEY = Deno.env.get("LASTFM_API_KEY") ?? "";

interface UserMusicProfile {
  artistVector: Record<string, number>;
  trackVector: Record<string, number>;
  genreVector: Record<string, number>;
}

async function getTopArtists(username: string): Promise<{ name: string; playcount: string }[]> {
  const url = `${LASTFM_API}?method=user.gettopartists&user=${encodeURIComponent(username)}&api_key=${API_KEY}&format=json&limit=50`;
  const res = await fetch(url);
  if (!res.ok) return [];
  const data = await res.json();
  return data?.topartists?.artist ?? [];
}

async function getTopTracks(username: string): Promise<{ name: string; artist: { name: string }; playcount: string }[]> {
  const url = `${LASTFM_API}?method=user.gettoptracks&user=${encodeURIComponent(username)}&api_key=${API_KEY}&format=json&limit=50`;
  const res = await fetch(url);
  if (!res.ok) return [];
  const data = await res.json();
  return data?.toptracks?.track ?? [];
}

async function getUserTopTags(username: string): Promise<{ name: string; count: string }[]> {
  const url = `${LASTFM_API}?method=user.gettopartags&user=${encodeURIComponent(username)}&api_key=${API_KEY}&format=json&limit=50`;
  const res = await fetch(url);
  if (!res.ok) return [];
  const data = await res.json();
  return data?.toptags?.tag ?? [];
}

async function buildProfile(username: string): Promise<UserMusicProfile | null> {
  const [artists, tracks, tags] = await Promise.all([
    getTopArtists(username),
    getTopTracks(username),
    getUserTopTags(username),
  ]);

  if (artists.length === 0) return null;

  const artistVector: Record<string, number> = {};
  for (const a of artists) {
    artistVector[a.name.toLowerCase()] = parseFloat(a.playcount) || 0;
  }

  const trackVector: Record<string, number> = {};
  for (const t of tracks) {
    trackVector[`${t.name.toLowerCase()}_${t.artist.name.toLowerCase()}`] = parseFloat(t.playcount) || 0;
  }

  const genreVector: Record<string, number> = {};
  for (const tag of tags) {
    genreVector[tag.name.toLowerCase()] = parseFloat(tag.count) || 0;
  }

  return { artistVector, trackVector, genreVector };
}

function cosineSimilarity(a: Record<string, number>, b: Record<string, number>): number {
  let dot = 0, magA = 0, magB = 0;
  for (const [k, v] of Object.entries(a)) {
    dot += v * (b[k] ?? 0);
    magA += v * v;
  }
  for (const v of Object.values(b)) magB += v * v;
  const denom = Math.sqrt(magA) * Math.sqrt(magB);
  return denom === 0 ? 0 : Math.min(1, Math.max(0, dot / denom));
}

function computeScore(mine: UserMusicProfile, theirs: UserMusicProfile): number {
  const artistScore = cosineSimilarity(mine.artistVector, theirs.artistVector);
  const trackScore = cosineSimilarity(mine.trackVector, theirs.trackVector);
  const genreScore = cosineSimilarity(mine.genreVector, theirs.genreVector);
  return 0.40 * artistScore + 0.35 * trackScore + 0.25 * genreScore;
}

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response(null, {
      headers: {
        "Access-Control-Allow-Origin": "*",
        "Access-Control-Allow-Headers": "authorization, content-type",
      },
    });
  }

  try {
    const { myUsername, usernames } = await req.json() as {
      myUsername: string;
      usernames: { userId: string; lastFmUsername: string }[];
    };

    if (!myUsername || !usernames?.length) {
      return Response.json({ scores: {} });
    }

    // Build all profiles in parallel — my profile + all nearby users
    const allUsernames = [myUsername, ...usernames.map((u) => u.lastFmUsername)];
    const profiles = await Promise.all(allUsernames.map((u) => buildProfile(u)));

    const myProfile = profiles[0];
    if (!myProfile) return Response.json({ scores: {} });

    const scores: Record<string, number> = {};
    for (let i = 0; i < usernames.length; i++) {
      const theirProfile = profiles[i + 1];
      scores[usernames[i].userId] = theirProfile
        ? computeScore(myProfile, theirProfile)
        : 0;
    }

    return Response.json({ scores }, {
      headers: { "Access-Control-Allow-Origin": "*" },
    });
  } catch (e) {
    return Response.json({ error: String(e) }, { status: 500 });
  }
});
