package io.freemytv;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** Picks a random trending title and previews its trailer (YouTube app or WebView). */
public class RouletteActivity extends Activity {
    private WebView web;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Ui.init(this);
        Recs.Pick pick = Recs.get(this).roulettePick();
        if (pick == null) {
            finish();
            return;
        }
        String query = pick.title + " trailer";
        if (openYouTubeSearch(query)) {
            finish();
            return;
        }
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        TextView title = new TextView(this);
        title.setText(pick.title);
        title.setTextColor(Color.WHITE);
        title.setTextSize(22);
        title.setPadding(48, 36, 48, 0);
        root.addView(title, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient());
        String enc = URLEncoder.encode(query, StandardCharsets.UTF_8);
        web.loadUrl("https://www.youtube.com/results?search_query=" + enc);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        lp.topMargin = 120;
        root.addView(web, lp);
        setContentView(root);
    }

    private boolean openYouTubeSearch(String query) {
        String enc = URLEncoder.encode(query, StandardCharsets.UTF_8);
        Uri uri = Uri.parse("https://www.youtube.com/results?search_query=" + enc);
        for (String pkg : new String[] {"com.amazon.firetv.youtube", "com.google.android.youtube.tv"}) {
            Intent i = new Intent(Intent.ACTION_VIEW, uri).setPackage(pkg);
            try {
                startActivity(i);
                return true;
            } catch (ActivityNotFoundException ignored) {
            }
        }
        return false;
    }

    @Override protected void onDestroy() {
        if (web != null) {
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            finish();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }
}
