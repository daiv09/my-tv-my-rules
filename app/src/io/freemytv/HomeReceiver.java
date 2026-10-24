package io.freemytv;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

/** System notices for the main process: the TV booted, or this app was updated. Not exported. */
public class HomeReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            HomeMode.bootCheck(context);
            if (Config.BOOT_LAUNCH_HOME) {
                Context app = context.getApplicationContext();
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    try {
                        Class<?> home = HomeMode.AMAZON ? FireHomeActivity.class : HomeActivity.class;
                        Intent launch = new Intent(app, home);
                        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        app.startActivity(launch);
                    } catch (Exception e) {
                        AppLog.w("boot launch: " + e.getMessage());
                    }
                }, 25_000);
            }
        } else if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            HomeMode.verify(context);   // app updated: complete Free My TV mode if something is missing
            HomeMode.ensureRemoteService(context, 3_000);
        }
    }
}
