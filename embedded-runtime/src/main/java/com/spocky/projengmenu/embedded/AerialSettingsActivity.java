package com.spocky.projengmenu.embedded;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;

/** Simple TV-friendly settings screen for the Aerial provider embedded in Projectivy. */
public final class AerialSettingsActivity extends Activity {
    private final Map<String, CheckBox> sourceChecks = new LinkedHashMap<>();
    private CheckBox prefer4k;
    private CheckBox preferHdr;
    private CheckBox fallback;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Aerial Rows");

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(42), dp(28), dp(42), dp(36));
        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));

        TextView title = text("Aerial Rows", 28, true);
        root.addView(title);
        TextView sub = text("Video wallpaper settings built directly into Projectivy", 16, false);
        sub.setPadding(0, dp(4), 0, dp(18));
        root.addView(sub);

        root.addView(section("Sources"));
        addSource(root, "A", "Apple TV Aerials");
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
        status.setPadding(0, dp(14), 0, dp(10));
        root.addView(status);

        Button refresh = button("Save & refresh wallpaper now");
        refresh.setOnClickListener(v -> {
            save();
            EmbeddedRuntime.notifyAerial(getApplicationContext());
            status.setText("Saved. Projectivy has been asked to refresh Aerial immediately.");
        });
        root.addView(refresh);

        Button done = button("Done");
        done.setOnClickListener(v -> {
            save();
            EmbeddedRuntime.notifyAerial(getApplicationContext());
            finish();
        });
        root.addView(done);

        load();
        setContentView(scroll);
    }

    private void addSource(LinearLayout root, String id, String label) {
        CheckBox c = check(label);
        sourceChecks.put(id, c);
        root.addView(c);
    }

    private void load() {
        try {
            String raw = EmbeddedRuntime.projectivyPrefs(this).getString(
                    "appearance_wallpaper_provider_params_" + EmbeddedRuntime.AERIAL_ID,
                    "{\"sources\":[\"A\",\"C1\",\"C2\"],\"prefer4k\":true,\"prefer_hdr\":false,\"fallback\":true}");
            JSONObject o = new JSONObject(raw);
            JSONArray a = o.optJSONArray("sources");
            if (a != null) {
                for (CheckBox c : sourceChecks.values()) c.setChecked(false);
                for (int i = 0; i < a.length(); i++) {
                    CheckBox c = sourceChecks.get(a.optString(i));
                    if (c != null) c.setChecked(true);
                }
            }
            prefer4k.setChecked(o.optBoolean("prefer4k", true));
            preferHdr.setChecked(o.optBoolean("prefer_hdr", false));
            fallback.setChecked(o.optBoolean("fallback", true));
            status.setText("Changes are saved to Projectivy; no separate Aerial app is required.");
        } catch (Throwable ignored) {
            CheckBox apple = sourceChecks.get("A");
            if (apple != null) apple.setChecked(true);
            prefer4k.setChecked(true);
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
            EmbeddedRuntime.projectivyPrefs(this).edit()
                    .putString("appearance_wallpaper_provider_params_" + EmbeddedRuntime.AERIAL_ID, o.toString())
                    .apply();
        } catch (Throwable ignored) {}
    }

    private TextView section(String value) {
        TextView v = text(value, 19, true);
        v.setPadding(0, dp(18), 0, dp(6));
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

    @Override
    protected void onPause() {
        save();
        super.onPause();
    }
}
