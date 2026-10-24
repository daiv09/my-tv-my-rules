package io.freemytv;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/** Plain-socket HTTP remote on port 8080 for phone browsers on the same Wi‑Fi. */
public class LanRemoteService extends Service {
    static final String ACTION_KEY = "io.freemytv.LAN_KEY";
    static final String EXTRA_KEYCODE = "keyCode";

    private static final byte[] PAGE = (
            "<!DOCTYPE html><html><head><meta name=viewport content=\"width=device-width,initial-scale=1\">"
                    + "<title>Free My TV</title><style>body{font-family:sans-serif;background:#0b1020;color:#fff;"
                    + "text-align:center;padding:1rem}button{font-size:1.4rem;margin:.35rem;padding:.6rem 1rem;"
                    + "min-width:3.5rem}</style></head><body><h2>Remote</h2>"
                    + "<div><button onclick=\"k(19)\">&#9650;</button></div>"
                    + "<div><button onclick=\"k(21)\">&#9664;</button><button onclick=\"k(23)\">OK</button>"
                    + "<button onclick=\"k(22)\">&#9654;</button></div>"
                    + "<div><button onclick=\"k(20)\">&#9660;</button></div>"
                    + "<div><button onclick=\"k(4)\">Back</button></div>"
                    + "<script>function k(c){fetch('/key?code='+c)}</script></body></html>"
    ).getBytes(StandardCharsets.UTF_8);

    private final AtomicBoolean running = new AtomicBoolean();
    private Thread serverThread;

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (!running.compareAndSet(false, true)) return START_STICKY;
        serverThread = new Thread(this::serve, "LanRemote");
        serverThread.setDaemon(true);
        serverThread.start();
        return START_STICKY;
    }

    @Override public void onDestroy() {
        running.set(false);
        if (serverThread != null) serverThread.interrupt();
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) {
        return null;
    }

    private void serve() {
        try (ServerSocket socket = new ServerSocket(8080)) {
            socket.setReuseAddress(true);
            while (running.get()) {
                try (Socket client = socket.accept()) {
                    client.setSoTimeout(5000);
                    handle(client);
                } catch (IOException e) {
                    if (running.get()) AppLog.w("lan remote: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            AppLog.w("lan remote server: " + e.getMessage());
        } finally {
            running.set(false);
        }
    }

    private void handle(Socket client) throws IOException {
        InputStream in = client.getInputStream();
        String request = readRequestLine(in);
        if (request == null) return;
        skipHeaders(in);
        String[] parts = request.split(" ");
        String method = parts[0];
        String target = parts.length > 1 ? parts[1] : "/";
        String path = target.contains("?") ? target.substring(0, target.indexOf('?')) : target;
        String query = target.contains("?") ? target.substring(target.indexOf('?') + 1) : "";
        OutputStream out = client.getOutputStream();
        if ("GET".equals(method) && "/key".equals(path)) {
            int code = parseCode(query);
            if (code != 0) sendKey(code);
            writeResponse(out, 204, "text/plain", new byte[0]);
        } else if ("GET".equals(method) && "/".equals(path)) {
            writeResponse(out, 200, "text/html; charset=utf-8", PAGE);
        } else {
            writeResponse(out, 404, "text/plain", "Not found".getBytes(StandardCharsets.UTF_8));
        }
    }

    private static String readRequestLine(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        int b;
        while ((b = in.read()) != -1) {
            if (b == '\n') {
                if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '\r') sb.setLength(sb.length() - 1);
                return sb.toString();
            }
            sb.append((char) b);
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private static void skipHeaders(InputStream in) throws IOException {
        String line;
        do {
            line = readRequestLine(in);
        } while (line != null && !line.isEmpty());
    }

    private static int parseCode(String query) {
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq <= 0) continue;
            String key = URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8);
            if (!"code".equals(key)) continue;
            try {
                return Integer.parseInt(URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private void sendKey(int code) {
        Intent i = new Intent(ACTION_KEY);
        i.setPackage(getPackageName());
        i.putExtra(EXTRA_KEYCODE, code);
        sendBroadcast(i);
    }

    private static void writeResponse(OutputStream out, int status, String type, byte[] body) throws IOException {
        String head = "HTTP/1.1 " + status + " OK\r\nContent-Type: " + type + "\r\nContent-Length: " + body.length
                + "\r\nConnection: close\r\n\r\n";
        out.write(head.getBytes(StandardCharsets.UTF_8));
        out.write(body);
        out.flush();
    }
}
