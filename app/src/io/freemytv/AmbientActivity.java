package io.freemytv;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;

/** Full-screen ambient UI after idle on Fire TV (same content as the system screensaver). */
public class AmbientActivity extends Activity {
    private DreamView dream;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Ui.init(this);
        dream = new DreamView(this);
        setContentView(dream);
        dream.start();
    }

    @Override protected void onDestroy() {
        if (dream != null) dream.stop();
        super.onDestroy();
    }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            finish();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }
}
