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

/** Simkl trending lists (movies + TV). Needs simkl_client_id in config.properties. */
final class Simkl {
    private static final String API = "https://api.simkl.com";
    private static final String APP = "freemytv";
    private static final String VERSION = "1.1";

    private Simkl() {}

    static boolean enabled() {
        return !Config.SIMKL_CLIENT_ID.trim().isEmpty();
    }

    static JSONArray trendingMovies() throws IOException, org.json.JSONException {
        return new JSONArray(get("/movies/trending/"));
    }

    static JSONArray trendingTv() throws IOException, org.json.JSONException {
        return new JSONArray(get("/tv/trending/"));
    }

    static String fanartUrl(String path) {
        if (path == null || path.isEmpty() || "null".equals(path)) return null;
        return "https://simkl.in/fanart/" + path + "_w.jpg";
    }

    static String posterUrl(String path) {
        if (path == null || path.isEmpty() || "null".equals(path)) return null;
        return "https://simkl.in/posters/" + path + "_m.webp";
    }

    static String metaLine(JSONObject o, String kind) {
        List<String> parts = new ArrayList<>();
        if ("tv".equals(kind)) parts.add(Ui.tr("Series"));
        String runtime = o.optString("runtime");
        if (!runtime.isEmpty() && !"N/A".equals(runtime)) parts.add(runtime);
        JSONObject ratings = o.optJSONObject("ratings");
        if (ratings != null) {
            JSONObject imdb = ratings.optJSONObject("imdb");
            if (imdb != null) {
                String r = imdb.optString("rating");
                if (!r.isEmpty() && !"N/A".equals(r)) parts.add("IMDb " + r);
            }
            JSONObject simkl = ratings.optJSONObject("simkl");
            if (simkl != null) {
                String r = simkl.optString("rating");
                if (!r.isEmpty() && !"N/A".equals(r)) parts.add("Simkl " + r);
            }
        }
        return String.join(" · ", parts);
    }

    private static String get(String path) throws IOException {
        String key = Config.SIMKL_CLIENT_ID.trim();
        String q = "client_id=" + enc(key) + "&app-name=" + enc(APP) + "&app-version=" + enc(VERSION);
        String url = API + path + (path.contains("?") ? "&" : "?") + q;
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(12_000);
        c.setReadTimeout(20_000);
        c.setRequestProperty("Accept", "application/json");
        c.setRequestProperty("simkl-api-key", key);
        c.setRequestProperty("User-Agent", APP + "/" + VERSION);
        try {
            int code = c.getResponseCode();
            if (code == 401) throw new IOException("Simkl rejected the client id");
            if (code != 200) throw new IOException("Simkl returned HTTP " + code);
            return read(c.getInputStream());
        } finally {
            c.disconnect();
        }
    }

    private static String enc(String s) {
        try {
            return URLEncoder.encode(s, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return s;
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
