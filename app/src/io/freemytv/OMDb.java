package io.freemytv;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Optional IMDb / RT ratings from OMDb when using TMDB picks (omdb_key in config.properties). */
final class OMDb {
    private OMDb() {}

    static boolean enabled() {
        return !Config.OMDB_KEY.trim().isEmpty();
    }

    /** Short line for the pick meta, or null if unavailable. */
    static String ratingsLine(String imdbId) {
        if (!enabled() || imdbId == null || imdbId.isEmpty()) return null;
        try {
            JSONObject o = fetchById(imdbId);
            return metaRatings(o);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String metaRatings(JSONObject o) {
        if (!"True".equalsIgnoreCase(o.optString("Response"))) return null;
        List<String> parts = new ArrayList<>();
        String imdb = o.optString("imdbRating");
        if (!imdb.isEmpty() && !"N/A".equals(imdb)) parts.add("IMDb " + imdb);
        JSONArray ratings = o.optJSONArray("Ratings");
        for (int i = 0; ratings != null && i < ratings.length(); i++) {
            JSONObject r = ratings.optJSONObject(i);
            if (r == null) continue;
            String source = r.optString("Source");
            String value = r.optString("Value");
            if (value.isEmpty() || "N/A".equals(value)) continue;
            if (source.contains("Rotten Tomatoes")) parts.add("RT " + value);
            else if (source.contains("Metacritic") && parts.size() < 3) parts.add("MC " + value.replace("/100", ""));
        }
        return parts.isEmpty() ? null : String.join(" · ", parts);
    }

    static JSONObject fetchById(String imdbId) throws IOException, org.json.JSONException {
        String key = URLEncoder.encode(Config.OMDB_KEY.trim(), StandardCharsets.UTF_8.name());
        String id = URLEncoder.encode(imdbId, StandardCharsets.UTF_8.name());
        HttpURLConnection c = (HttpURLConnection) new URL("https://www.omdbapi.com/?apikey=" + key + "&i=" + id).openConnection();
        c.setConnectTimeout(8000);
        c.setReadTimeout(8000);
        try {
            int code = c.getResponseCode();
            if (code != 200) throw new IOException("OMDb returned HTTP " + code);
            return new JSONObject(read(c.getInputStream()));
        } finally {
            c.disconnect();
        }
    }

    private static String read(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int n;
        while ((n = in.read(buffer)) > 0) out.write(buffer, 0, n);
        return out.toString(StandardCharsets.UTF_8.name());
    }
}
