package com.spocky.projengmenu.embedded;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

/**
 * Invisible trampoline used for Projectivy-owned Android TV preview programs.
 *
 * Projectivy deliberately pins a preview-program Intent to the package which
 * owns that TV row. Our rows are now owned by Projectivy, so a direct Kodi
 * Intent gets re-pinned to Projectivy before launch. This activity is the
 * Projectivy-owned target; it immediately forwards the original Red Light
 * plugin:// URI to Kodi's exported Splash activity. Kodi then hands that same
 * Intent to its native Main activity, matching an external ACTION_VIEW launch.
 */
public final class RedLightLaunchActivity extends Activity {
    static final String EXTRA_TARGET = "com.spocky.projengmenu.embedded.REDLIGHT_TARGET";
    private static final String KODI_PACKAGE = "org.xbmc.kodi";
    private static final String KODI_SPLASH = "org.xbmc.kodi.Splash";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        forward();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        forward();
    }

    private void forward() {
        try {
            Intent source = getIntent();
            String target = source == null ? null : source.getStringExtra(EXTRA_TARGET);
            if (target == null || target.trim().isEmpty()) {
                Uri data = source == null ? null : source.getData();
                if (data != null && "plugin".equalsIgnoreCase(data.getScheme())) target = data.toString();
            }
            if (target != null && target.startsWith("plugin://")) {
                Intent kodi = new Intent(Intent.ACTION_VIEW);
                kodi.setComponent(new ComponentName(KODI_PACKAGE, KODI_SPLASH));
                kodi.setData(Uri.parse(target));
                kodi.addCategory(Intent.CATEGORY_DEFAULT);
                kodi.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(kodi);
            } else {
                Intent launch = getPackageManager().getLaunchIntentForPackage(KODI_PACKAGE);
                if (launch != null) {
                    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(launch);
                }
            }
        } catch (Throwable ignored) {
            try {
                Intent launch = getPackageManager().getLaunchIntentForPackage(KODI_PACKAGE);
                if (launch != null) startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (Throwable ignoredAgain) {}
        } finally {
            finish();
        }
    }
}
