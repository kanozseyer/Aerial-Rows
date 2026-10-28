package com.spocky.projengmenu.embedded;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

/**
 * CLEAN 09 Projectivy-owned trampoline for playable Red Light preview cards.
 * Projectivy owns the preview row, so clicks land here first and are then
 * forwarded unchanged to Kodi's exported Splash activity as ACTION_VIEW.
 */
public final class RedLightLaunchActivity extends Activity {
    public static final String EXTRA_TARGET = "com.spocky.projengmenu.embedded.REDLIGHT_TARGET";
    public static final String EXTRA_KIND = "com.spocky.projengmenu.embedded.REDLIGHT_KIND";

    private static final String TAG = "ProjectivyRedLightLaunch";
    private static final String KODI_PACKAGE = "org.xbmc.kodi";
    private static final String KODI_SPLASH = "org.xbmc.kodi.Splash";

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        forward();
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        forward();
    }

    private void forward() {
        try {
            Intent source = getIntent();
            String target = source == null ? null : source.getStringExtra(EXTRA_TARGET);
            String kind = source == null ? "" : source.getStringExtra(EXTRA_KIND);
            if (target == null || target.trim().isEmpty()) {
                Uri data = source == null ? null : source.getData();
                if (data != null && "plugin".equalsIgnoreCase(data.getScheme())) target = data.toString();
            }

            if (target == null || !target.startsWith("plugin://plugin.video.redlight/")) {
                Log.i(TAG, "CLEAN09 rejected non-Red-Light target");
                return;
            }

            Intent kodi = new Intent(Intent.ACTION_VIEW);
            kodi.setComponent(new ComponentName(KODI_PACKAGE, KODI_SPLASH));
            kodi.setData(Uri.parse(target));
            kodi.addCategory(Intent.CATEGORY_DEFAULT);
            kodi.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            Log.i(TAG, "CLEAN09 forwarding kind=" + (kind == null ? "" : kind));
            startActivity(kodi);
        } catch (Throwable t) {
            Log.e(TAG, "CLEAN09 forward failed", t);
        } finally {
            finish();
        }
    }
}
