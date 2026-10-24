package io.freemytv;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.MotionEvent;

/**
 * Fire TV home screen (no {@code resumeWhilePausing}). After five minutes without remote input, opens
 * {@link AmbientActivity} (trending backdrops, clock and weather via {@link DreamView}).
 */
public class FireHomeActivity extends HomeActivity {
    private static final long AMBIENT_MS = 5 * 60_000L;
    private static final long CHECK_MS = 30_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private long lastActive = System.currentTimeMillis();
    private boolean ambientOpen;

    private final Runnable idleCheck = new Runnable() {
        @Override public void run() {
            if (ambientOpen) return;
            if (System.currentTimeMillis() - lastActive >= AMBIENT_MS) {
                ambientOpen = true;
                startActivity(new Intent(FireHomeActivity.this, AmbientActivity.class));
            }
            handler.postDelayed(this, CHECK_MS);
        }
    };

    @Override protected void onResume() {
        super.onResume();
        ambientOpen = false;
        lastActive = System.currentTimeMillis();
        handler.removeCallbacks(idleCheck);
        handler.postDelayed(idleCheck, CHECK_MS);
        if (Config.LAN_REMOTE) {
            startService(new Intent(this, LanRemoteService.class));
        }
    }

    @Override protected void onPause() {
        handler.removeCallbacks(idleCheck);
        super.onPause();
    }

    @Override public void onUserInteraction() {
        super.onUserInteraction();
        touch();
    }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) touch();
        return super.dispatchKeyEvent(event);
    }

    @Override public boolean dispatchGenericMotionEvent(MotionEvent ev) {
        touch();
        return super.dispatchGenericMotionEvent(ev);
    }

    private void touch() {
        lastActive = System.currentTimeMillis();
        ambientOpen = false;
    }
}
