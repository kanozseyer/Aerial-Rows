package com.spocky.projengmenu.embedded;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;

/** CLEAN 03: TV-friendly Configure UI for the embedded Aerial provider. */
public final class AerialSettingsActivity extends Activity {
    private static final String AERIAL_ID = "8c00feaf-c7be-43c2-80f9-d49853aaa99a";
    private static final String PREFS = "clean02_aerial_provider";
    private static final String PARAMS = "params";
    private static final String DEFAULT_PARAMS = "{\"sources\":[\"A\",\"C1\",\"C2\"],\"prefer4k\":true,\"prefer_hdr\":false,\"fallback\":true}";
    private static final String ACTION_UPDATED = "tv.projectivy.plugin.action.WALLPAPER_PROVIDER_UPDATED";
    private static final String EXTRA_PROVIDER_ID = "tv.projectivy.plugin.extra.PROVIDER_ID";
    private static final String EXTRA_UPDATE_REASON = "tv.projectivy.plugin.extra.UPDATE_REASON";

    private final Map<String, CheckBox> sourceChecks = new LinkedHashMap<>();
    private CheckBox prefer4k;
    private CheckBox preferHdr;
    private CheckBox fallback;
    private TextView status;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setTitle("Aerial Rows");

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(46), dp(26), dp(46), dp(36));
        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));

        TextView title = text("Aerial Rows", 28, true);
        root.addView(title);
        TextView sub = text("Choose the sources and video quality used by Projectivy.", 16, false);
        sub.setPadding(0, dp(4), 0, dp(16));
        root.addView(sub);

        root.addView(section("Sources"));
        addSource(root, "A", "Apple TV");
        addSource(root, "AMZ", "Amazon Fire TV");
        addSource(root, "C1", "Community 1");
        addSource(root, "C2", "Community 2");

        root.addView(section("Quality"));
        prefer4k = check("Prefer 4K");
        root.addView(prefer4k);
        preferHdr = check("Prefer HDR");
        root.addView(preferHdr);
        fallback = check("Allow fallback when preferred quality is unavailable");
        root.addView(fallback);

        status = text("", 14, false);
        status.setPadding(0, dp(12), 0, dp(8));
        root.addView(status);

        Button save = button("Save & refresh wallpaper now");
        save.setOnClickListener(v -> {
            save();
            notifyUpdated();
            status.setText("Saved. Projectivy has been asked to refresh Aerial now.");
            save.requestFocus();
        });
        root.addView(save);

        Button done = button("Done");
        done.setOnClickListener(v -> {
            save();
            notifyUpdated();
            finish();
        });
        root.addView(done);

        load();
        setContentView(scroll);
    }

    private SharedPreferences prefs() {
        return getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private void addSource(LinearLayout root, String id, String label) {
        CheckBox c = check(label);
        sourceChecks.put(id, c);
        root.addView(c);
    }

    private void load() {
        try {
            JSONObject o = new JSONObject(prefs().getString(PARAMS, DEFAULT_PARAMS));
            JSONArray a = o.optJSONArray("sources");
            for (CheckBox c : sourceChecks.values()) c.setChecked(false);
            if (a != null) {
                for (int i = 0; i < a.length(); i++) {
                    CheckBox c = sourceChecks.get(a.optString(i));
                    if (c != null) c.setChecked(true);
                }
            }
            prefer4k.setChecked(o.optBoolean("prefer4k", true));
            preferHdr.setChecked(o.optBoolean("prefer_hdr", false));
            fallback.setChecked(o.optBoolean("fallback", true));
            status.setText("Changes are stored inside Projectivy. No separate Aerial app is required.");
        } catch (Throwable ignored) {
            CheckBox apple = sourceChecks.get("A");
            if (apple != null) apple.setChecked(true);
            prefer4k.setChecked(true);
            preferHdr.setChecked(false);
            fallback.setChecked(true);
        }
    }

    private void save() {
        try {
            JSONArray sources = new JSONArray();
            for (Map.Entry<String, CheckBox> e : sourceChecks.entrySet()) {
                if (e.getValue().isChecked()) sources.put(e.getKey());
            }
            if (sources.length() == 0) {
                sources.put("A");
                CheckBox apple = sourceChecks.get("A");
                if (apple != null) apple.setChecked(true);
            }
            JSONObject o = new JSONObject();
            o.put("sources", sources);
            o.put("prefer4k", prefer4k.isChecked());
            o.put("prefer_hdr", preferHdr.isChecked());
            o.put("fallback", fallback.isChecked());
            prefs().edit().putString(PARAMS, o.toString()).apply();
        } catch (Throwable ignored) {}
    }

    private void notifyUpdated() {
        try {
            Intent i = new Intent(ACTION_UPDATED).setPackage(getPackageName());
            i.putExtra(EXTRA_PROVIDER_ID, AERIAL_ID);
            i.putExtra(EXTRA_UPDATE_REASON, 2);
            sendBroadcast(i);
        } catch (Throwable ignored) {}
    }

    private TextView section(String value) {
        TextView v = text(value, 19, true);
        v.setPadding(0, dp(16), 0, dp(4));
        return v;
    }

    private TextView text(String value, int sp, boolean bold) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(sp);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }

    private CheckBox check(String label) {
        CheckBox v = new CheckBox(this);
        v.setText(label);
        v.setTextSize(17);
        v.setGravity(Gravity.CENTER_VERTICAL);
        v.setMinHeight(dp(54));
        v.setFocusable(true);
        return v;
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(16);
        b.setFocusable(true);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(58));
        p.topMargin = dp(10);
        b.setLayoutParams(p);
        return b;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override protected void onPause() {
        save();
        super.onPause();
    }
}
