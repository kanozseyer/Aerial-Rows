package com.spocky.projengmenu.embedded;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** CLEAN13.1: cached Open-Meteo weather badge integrated into Projectivy status bar. */
public final class WeatherStatusBarBridge {
    private static final String TAG = "ProjectivyWeather";
    private static final String PREFS = "clean13_1_weather";
    private static final String CLOCK_PREFS = "clean13d_clock_date_options";

    private static final String KEY_SHOW = "show_weather";
    private static final String KEY_QUERY = "location_query";
    private static final String KEY_RESOLVED = "location_resolved";
    private static final String KEY_LAT = "latitude";
    private static final String KEY_LON = "longitude";
    private static final String KEY_UNIT = "temperature_unit";
    private static final String KEY_TEMP_C = "temperature_c";
    private static final String KEY_CODE = "weather_code";
    private static final String KEY_IS_DAY = "is_day";
    private static final String KEY_FETCHED = "fetched_at";

    private static final String UNIT_C = "c";
    private static final String UNIT_F = "f";
    private static final String BADGE_TAG = "clean13_1_weather_badge";
    private static final long REFRESH_MS = 30L * 60L * 1000L;

    private static final long ID_SHOW_PARENT = 0x131100L;
    private static final long ID_SHOW_ON = 0x131101L;
    private static final long ID_SHOW_OFF = 0x131102L;
    private static final long ID_LOCATION = 0x131200L;
    private static final long ID_UNIT_PARENT = 0x131300L;
    private static final long ID_UNIT_C = 0x131301L;
    private static final long ID_UNIT_F = 0x131302L;
    private static final long ID_REFRESH = 0x131400L;
    private static final long ID_ATTRIBUTION = 0x131500L;

    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final AtomicBoolean IN_FLIGHT = new AtomicBoolean(false);

    private WeatherStatusBarBridge() {}

    public static void addWeatherActions(Object fragment, List list) {
        try {
            if (fragment == null || list == null) return;
            Context context = contextOf(fragment);
            if (context == null) return;
            SharedPreferences p = prefs(context);

            list.add(makeDropdown(fragment, ID_SHOW_PARENT, "Show weather",
                    p.getBoolean(KEY_SHOW, true) ? "On" : "Off",
                    new long[]{ID_SHOW_ON, ID_SHOW_OFF},
                    new String[]{"On", "Off"}));

            String query = p.getString(KEY_QUERY, "").trim();
            String resolved = p.getString(KEY_RESOLVED, "").trim();
            String locationLabel = resolved.length() != 0 ? resolved : (query.length() != 0 ? query : "Not set");
            list.add(makeAction(fragment, ID_LOCATION, "Weather location", locationLabel));

            list.add(makeDropdown(fragment, ID_UNIT_PARENT, "Temperature unit",
                    UNIT_F.equals(p.getString(KEY_UNIT, UNIT_C)) ? "°F" : "°C",
                    new long[]{ID_UNIT_C, ID_UNIT_F},
                    new String[]{"Celsius (°C)", "Fahrenheit (°F)"}));

            list.add(makeAction(fragment, ID_REFRESH, "Refresh weather", lastUpdatedLabel(p)));
            list.add(makeAction(fragment, ID_ATTRIBUTION, "Weather data", "Open-Meteo • CC BY 4.0"));

            Log.i(TAG, "CLEAN13.1 weather actions added");
        } catch (Throwable t) {
            Log.e(TAG, "CLEAN13.1 add actions failed", t);
        }
    }

    public static boolean handleWeatherAction(Object fragment, Object action) {
        try {
            if (fragment == null || action == null) return false;
            long id = ((Number) field(action, "a").get(action)).longValue();
            Context context = contextOf(fragment);
            if (context == null) return false;
            SharedPreferences p = prefs(context);

            if (id == ID_SHOW_ON || id == ID_SHOW_OFF) {
                p.edit().putBoolean(KEY_SHOW, id == ID_SHOW_ON).apply();
                refreshMenu(fragment);
                Activity a = activityOf(fragment);
                if (a != null) applyToHome(a);
                Log.i(TAG, "CLEAN13.1 show weather=" + (id == ID_SHOW_ON));
                return true;
            }
            if (id == ID_UNIT_C || id == ID_UNIT_F) {
                p.edit().putString(KEY_UNIT, id == ID_UNIT_F ? UNIT_F : UNIT_C).apply();
                refreshMenu(fragment);
                Activity a = activityOf(fragment);
                if (a != null) render(a);
                Log.i(TAG, "CLEAN13.1 temperature unit=" + (id == ID_UNIT_F ? "F" : "C"));
                return true;
            }
            if (id == ID_LOCATION) {
                openLocationDialog(fragment);
                return true;
            }
            if (id == ID_REFRESH) {
                Activity a = activityOf(fragment);
                if (a != null) {
                    maybeRefresh(a, true);
                    render(a);
                }
                return true;
            }
            if (id == ID_ATTRIBUTION) return true;
            return false;
        } catch (Throwable t) {
            Log.e(TAG, "CLEAN13.1 handle action failed", t);
            return false;
        }
    }

    public static void applyToHome(final Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        final WeakReference<Activity> ref = new WeakReference<>(activity);
        activity.getWindow().getDecorView().post(new Runnable() {
            @Override public void run() {
                Activity a = ref.get();
                if (a == null || a.isFinishing() || a.isDestroyed()) return;
                try {
                    render(a);
                    maybeRefresh(a, false);
                } catch (Throwable t) {
                    Log.e(TAG, "CLEAN13.1 Home apply failed", t);
                }
            }
        });
    }

    private static void openLocationDialog(final Object fragment) {
        final Activity activity = activityOf(fragment);
        if (activity == null || activity.isFinishing()) return;
        final SharedPreferences p = prefs(activity);
        final EditText input = new EditText(activity);
        input.setSingleLine(true);
        input.setHint("City or postcode");
        input.setText(p.getString(KEY_QUERY, ""));
        input.setSelectAllOnFocus(true);
        int pad = dp(activity, 24);
        input.setPadding(pad, dp(activity, 8), pad, dp(activity, 8));

        final AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("Weather location")
                .setMessage("Enter a city or postcode")
                .setView(input)
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int which) {
                        String q = input.getText() == null ? "" : input.getText().toString().trim();
                        SharedPreferences.Editor e = p.edit();
                        e.putString(KEY_QUERY, q);
                        e.remove(KEY_RESOLVED).remove(KEY_LAT).remove(KEY_LON)
                                .remove(KEY_TEMP_C).remove(KEY_CODE).remove(KEY_IS_DAY).remove(KEY_FETCHED);
                        e.apply();
                        refreshMenu(fragment);
                        render(activity);
                        if (q.length() != 0) maybeRefresh(activity, true);
                        Log.i(TAG, "CLEAN13.1 location changed query=" + q);
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override public void onShow(DialogInterface d) {
                input.requestFocus();
                if (dialog.getWindow() != null) {
                    dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }
        });
        dialog.show();
    }

    private static void maybeRefresh(final Activity activity, final boolean force) {
        final Context app = activity.getApplicationContext();
        final SharedPreferences p = prefs(app);
        if (!p.getBoolean(KEY_SHOW, true)) return;
        final String query = p.getString(KEY_QUERY, "").trim();
        if (query.length() == 0) return;
        long fetched = p.getLong(KEY_FETCHED, 0L);
        boolean haveCoords = p.contains(KEY_LAT) && p.contains(KEY_LON);
        if (!force && haveCoords && fetched > 0L && System.currentTimeMillis() - fetched < REFRESH_MS) return;
        if (!IN_FLIGHT.compareAndSet(false, true)) return;

        Log.i(TAG, "CLEAN13.1 refresh start force=" + force + " query=" + query);
        final WeakReference<Activity> ref = new WeakReference<>(activity);
        EXEC.execute(new Runnable() {
            @Override public void run() {
                try {
                    SharedPreferences current = prefs(app);
                    if (!query.equals(current.getString(KEY_QUERY, "").trim())) return;
                    double lat;
                    double lon;
                    if (current.contains(KEY_LAT) && current.contains(KEY_LON)) {
                        lat = Double.parseDouble(current.getString(KEY_LAT, "0"));
                        lon = Double.parseDouble(current.getString(KEY_LON, "0"));
                    } else {
                        double[] coords = geocode(app, query);
                        if (coords == null) throw new IOException("No geocoding result for " + query);
                        lat = coords[0];
                        lon = coords[1];
                    }
                    fetchCurrent(app, query, lat, lon);
                } catch (Throwable t) {
                    Log.w(TAG, "CLEAN13.1 refresh failed", t);
                } finally {
                    IN_FLIGHT.set(false);
                    MAIN.post(new Runnable() {
                        @Override public void run() {
                            Activity a = ref.get();
                            if (a != null && !a.isFinishing() && !a.isDestroyed()) render(a);
                        }
                    });
                }
            }
        });
    }

    private static double[] geocode(Context context, String query) throws Exception {
        String url = "https://geocoding-api.open-meteo.com/v1/search?name="
                + URLEncoder.encode(query, "UTF-8") + "&count=1&language=en&format=json";
        JSONObject root = new JSONObject(readUrl(url));
        JSONArray results = root.optJSONArray("results");
        if (results == null || results.length() == 0) return null;
        JSONObject r = results.getJSONObject(0);
        double lat = r.getDouble("latitude");
        double lon = r.getDouble("longitude");
        String name = r.optString("name", query);
        String admin1 = r.optString("admin1", "");
        String country = r.optString("country", "");
        StringBuilder resolved = new StringBuilder(name);
        if (admin1.length() != 0 && !admin1.equalsIgnoreCase(name)) resolved.append(", ").append(admin1);
        if (country.length() != 0) resolved.append(", ").append(country);

        SharedPreferences p = prefs(context);
        if (!query.equals(p.getString(KEY_QUERY, "").trim())) return null;
        p.edit()
                .putString(KEY_LAT, Double.toString(lat))
                .putString(KEY_LON, Double.toString(lon))
                .putString(KEY_RESOLVED, resolved.toString())
                .apply();
        Log.i(TAG, "CLEAN13.1 geocoded " + query + " -> " + resolved + " " + lat + "," + lon);
        return new double[]{lat, lon};
    }

    private static void fetchCurrent(Context context, String query, double lat, double lon) throws Exception {
        String url = String.format(Locale.US,
                "https://api.open-meteo.com/v1/forecast?latitude=%.6f&longitude=%.6f&current=temperature_2m,weather_code,is_day&timezone=auto",
                lat, lon);
        JSONObject root = new JSONObject(readUrl(url));
        JSONObject current = root.getJSONObject("current");
        double temp = current.getDouble("temperature_2m");
        int code = current.getInt("weather_code");
        boolean isDay = current.optInt("is_day", 1) != 0;

        SharedPreferences p = prefs(context);
        if (!query.equals(p.getString(KEY_QUERY, "").trim())) return;
        p.edit()
                .putString(KEY_TEMP_C, Double.toString(temp))
                .putInt(KEY_CODE, code)
                .putBoolean(KEY_IS_DAY, isDay)
                .putLong(KEY_FETCHED, System.currentTimeMillis())
                .apply();
        Log.i(TAG, "CLEAN13.1 weather refreshed tempC=" + temp + " code=" + code + " isDay=" + isDay);
    }

    private static String readUrl(String address) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(address).openConnection();
        c.setConnectTimeout(8000);
        c.setReadTimeout(8000);
        c.setRequestMethod("GET");
        c.setRequestProperty("Accept", "application/json");
        c.setRequestProperty("User-Agent", "Aerial-Rows Projectivy CLEAN13.1");
        int status = c.getResponseCode();
        InputStream in = status >= 200 && status < 300 ? c.getInputStream() : c.getErrorStream();
        if (in == null) throw new IOException("HTTP " + status);
        StringBuilder out = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"))) {
            String line;
            while ((line = r.readLine()) != null) out.append(line);
        } finally {
            c.disconnect();
        }
        if (status < 200 || status >= 300) throw new IOException("HTTP " + status + " " + out);
        return out.toString();
    }

    private static void render(Activity activity) {
        try {
            int clockId = activity.getResources().getIdentifier("title_text", "id", activity.getPackageName());
            int iconsId = activity.getResources().getIdentifier("statusbar_icons_container", "id", activity.getPackageName());
            View clockView = activity.findViewById(clockId);
            View iconsView = activity.findViewById(iconsId);
            if (!(clockView instanceof TextView) || !(iconsView instanceof ViewGroup)) return;
            TextView clock = (TextView) clockView;
            ViewGroup icons = (ViewGroup) iconsView;
            if (!(clock.getParent() instanceof ViewGroup) || clock.getParent() != icons.getParent()) return;
            ViewGroup parent = (ViewGroup) clock.getParent();

            WeatherBadgeView badge = (WeatherBadgeView) parent.findViewWithTag(BADGE_TAG);
            if (badge == null) {
                badge = new WeatherBadgeView(activity);
                badge.setTag(BADGE_TAG);
                int index = Math.min(parent.indexOfChild(clock) + 1, parent.getChildCount());
                ViewGroup.LayoutParams lp;
                if (parent instanceof LinearLayout) {
                    LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
                    llp.gravity = Gravity.CENTER_VERTICAL;
                    lp = llp;
                } else {
                    lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                }
                parent.addView(badge, index, lp);
                Log.i(TAG, "CLEAN13.1 weather badge attached");
            }

            SharedPreferences p = prefs(activity);
            String query = p.getString(KEY_QUERY, "").trim();
            boolean show = p.getBoolean(KEY_SHOW, true) && query.length() != 0;
            badge.setVisibility(show ? View.VISIBLE : View.GONE);
            if (!show) return;

            boolean have = p.contains(KEY_TEMP_C) && p.contains(KEY_CODE);
            double tempC = have ? Double.parseDouble(p.getString(KEY_TEMP_C, "0")) : 0.0;
            boolean fahrenheit = UNIT_F.equals(p.getString(KEY_UNIT, UNIT_C));
            double display = fahrenheit ? tempC * 9.0 / 5.0 + 32.0 : tempC;
            String tempText = have ? Long.toString(Math.round(display)) + "°" : "--°";
            int code = have ? p.getInt(KEY_CODE, 2) : 2;
            boolean isDay = p.getBoolean(KEY_IS_DAY, true);
            badge.setWeather(tempText, code, isDay, clock.getTextSize(), clock.getCurrentTextColor());
            badge.setContentDescription(have ? condition(code) + ", " + tempText : "Weather loading");
            applyBadgeBackground(activity, badge, icons);
        } catch (Throwable t) {
            Log.w(TAG, "CLEAN13.1 render failed", t);
        }
    }

    private static void applyBadgeBackground(Context context, WeatherBadgeView badge, ViewGroup icons) {
        String mode = context.getSharedPreferences(CLOCK_PREFS, Context.MODE_PRIVATE)
                .getString("background_mode", "unified");
        if ("unified".equals(mode) || "none".equals(mode)) {
            badge.setBackground(null);
            badge.setPadding(dp(context, 8), 0, dp(context, 8), 0);
            return;
        }
        Drawable source = icons.getBackground();
        Drawable bg = null;
        if (source != null && source.getConstantState() != null) {
            bg = source.getConstantState().newDrawable(context.getResources()).mutate();
        }
        ColorStateList tint = icons.getBackgroundTintList();
        if (bg == null) {
            GradientDrawable g = new GradientDrawable();
            g.setColor(tint != null ? tint.getDefaultColor() : 0x66000000);
            g.setCornerRadius(dp(context, 40));
            bg = g;
        } else if (tint != null) {
            bg.setTintList(tint);
        }
        badge.setBackground(bg);
        badge.setPadding(dp(context, 12), 0, dp(context, 12), 0);
    }

    private static String lastUpdatedLabel(SharedPreferences p) {
        long ts = p.getLong(KEY_FETCHED, 0L);
        if (ts <= 0L) return "Not updated yet";
        long mins = Math.max(0L, (System.currentTimeMillis() - ts) / 60000L);
        if (mins < 1L) return "Just now";
        if (mins < 60L) return mins + " min ago";
        long hours = mins / 60L;
        return hours + (hours == 1L ? " hour ago" : " hours ago");
    }

    private static String condition(int code) {
        if (code == 0) return "Clear";
        if (code == 1) return "Mainly clear";
        if (code == 2) return "Partly cloudy";
        if (code == 3) return "Cloudy";
        if (code == 45 || code == 48) return "Fog";
        if ((code >= 51 && code <= 67) || (code >= 80 && code <= 82)) return "Rain";
        if ((code >= 71 && code <= 77) || code == 85 || code == 86) return "Snow";
        if (code >= 95) return "Thunderstorm";
        return "Weather";
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static Object makeDropdown(Object fragment, long id, String title, String selected,
                                       long[] childIds, String[] childTitles) throws Exception {
        Class<?> cb3 = Class.forName("cb3");
        Class<?> lu1 = Class.forName("lu1");
        Method t0 = cb3.getDeclaredMethod("t0", cb3, long.class, CharSequence.class, Drawable.class, int.class);
        t0.setAccessible(true);
        Method z0 = cb3.getDeclaredMethod("z0", lu1);
        z0.setAccessible(true);
        Object parent = t0.invoke(null, fragment, id, title, null, 28);
        field(parent, "d").set(parent, selected);
        ArrayList<Object> children = new ArrayList<>();
        for (int i = 0; i < childIds.length; i++) {
            Object child = t0.invoke(null, fragment, childIds[i], childTitles[i], null, 28);
            children.add(z0.invoke(null, child));
        }
        field(parent, "k").set(parent, children);
        return z0.invoke(null, parent);
    }

    private static Object makeAction(Object fragment, long id, String title, String subtitle) throws Exception {
        Class<?> cb3 = Class.forName("cb3");
        Class<?> lu1 = Class.forName("lu1");
        Method t0 = cb3.getDeclaredMethod("t0", cb3, long.class, CharSequence.class, Drawable.class, int.class);
        t0.setAccessible(true);
        Method z0 = cb3.getDeclaredMethod("z0", lu1);
        z0.setAccessible(true);
        Object action = t0.invoke(null, fragment, id, title, null, 28);
        field(action, "d").set(action, subtitle);
        return z0.invoke(null, action);
    }

    private static Context contextOf(Object fragment) {
        try {
            Object c = findMethod(fragment.getClass(), "k").invoke(fragment);
            return c instanceof Context ? (Context) c : null;
        } catch (Throwable ignored) { return null; }
    }

    private static Activity activityOf(Object fragment) {
        Context c = contextOf(fragment);
        return c instanceof Activity ? (Activity) c : null;
    }

    private static void refreshMenu(Object fragment) {
        try { findMethod(fragment.getClass(), "K0").invoke(fragment); }
        catch (Throwable t) { Log.w(TAG, "CLEAN13.1 menu refresh deferred", t); }
    }

    private static Field field(Object object, String name) throws Exception {
        return field(object.getClass(), name);
    }

    private static Field field(Class<?> type, String name) throws Exception {
        Class<?> c = type;
        while (c != null) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException e) { c = c.getSuperclass(); }
        }
        throw new NoSuchFieldException(name);
    }

    private static Method findMethod(Class<?> type, String name, Class<?>... params) throws Exception {
        Class<?> c = type;
        while (c != null) {
            try {
                Method m = c.getDeclaredMethod(name, params);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException e) { c = c.getSuperclass(); }
        }
        throw new NoSuchMethodException(name);
    }

    private static int dp(Context c, int v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    private static final class WeatherBadgeView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private String temperature = "--°";
        private int code = 2;
        private boolean day = true;
        private float textSize = 22f;
        private int color = 0xffffffff;

        WeatherBadgeView(Context context) {
            super(context);
            setFocusable(false);
            setClickable(false);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
        }

        void setWeather(String temperature, int code, boolean day, float textSize, int color) {
            this.temperature = temperature;
            this.code = code;
            this.day = day;
            this.textSize = Math.max(16f, textSize);
            this.color = color;
            requestLayout();
            invalidate();
        }

        @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            paint.setTextSize(textSize);
            float icon = textSize * 1.15f;
            float gap = dp(getContext(), 7);
            int desiredW = getPaddingLeft() + getPaddingRight() + Math.round(icon + gap + paint.measureText(temperature));
            int desiredH = Math.max(dp(getContext(), 36), Math.round(textSize * 1.65f));
            setMeasuredDimension(resolveSize(desiredW, widthMeasureSpec), resolveSize(desiredH, heightMeasureSpec));
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            paint.setColor(color);
            paint.setTextSize(textSize);
            paint.setStrokeWidth(Math.max(2f, dp(getContext(), 2)));
            paint.setStyle(Paint.Style.STROKE);
            float iconSize = textSize * 1.15f;
            float left = getPaddingLeft();
            float centerY = getHeight() / 2f;
            drawWeatherIcon(canvas, left, centerY - iconSize / 2f, iconSize, code, day);

            paint.setStyle(Paint.Style.FILL);
            Paint.FontMetrics fm = paint.getFontMetrics();
            float baseline = centerY - (fm.ascent + fm.descent) / 2f;
            canvas.drawText(temperature, left + iconSize + dp(getContext(), 7), baseline, paint);
        }

        private void drawWeatherIcon(Canvas c, float x, float y, float s, int code, boolean day) {
            if (code == 0) {
                if (day) drawSun(c, x, y, s); else drawMoon(c, x, y, s);
                return;
            }
            if (code == 1 || code == 2) {
                if (day) drawSun(c, x + s * 0.03f, y, s * 0.58f); else drawMoon(c, x + s * 0.03f, y, s * 0.58f);
                drawCloud(c, x + s * 0.18f, y + s * 0.28f, s * 0.78f);
                return;
            }
            if (code == 45 || code == 48) {
                drawFog(c, x, y, s);
                return;
            }
            if ((code >= 71 && code <= 77) || code == 85 || code == 86) {
                drawCloud(c, x, y, s * 0.82f);
                drawSnow(c, x + s * 0.15f, y + s * 0.63f, s * 0.7f);
                return;
            }
            if (code >= 95) {
                drawCloud(c, x, y, s * 0.82f);
                drawLightning(c, x + s * 0.42f, y + s * 0.52f, s * 0.38f);
                return;
            }
            if ((code >= 51 && code <= 67) || (code >= 80 && code <= 82)) {
                drawCloud(c, x, y, s * 0.82f);
                drawRain(c, x + s * 0.16f, y + s * 0.60f, s * 0.68f);
                return;
            }
            drawCloud(c, x, y + s * 0.08f, s * 0.9f);
        }

        private void drawSun(Canvas c, float x, float y, float s) {
            float cx = x + s / 2f, cy = y + s / 2f, r = s * 0.22f;
            c.drawCircle(cx, cy, r, paint);
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * i / 4.0;
                float x1 = cx + (float)Math.cos(a) * s * 0.34f;
                float y1 = cy + (float)Math.sin(a) * s * 0.34f;
                float x2 = cx + (float)Math.cos(a) * s * 0.46f;
                float y2 = cy + (float)Math.sin(a) * s * 0.46f;
                c.drawLine(x1, y1, x2, y2, paint);
            }
        }

        private void drawMoon(Canvas c, float x, float y, float s) {
            RectF r = new RectF(x + s * 0.18f, y + s * 0.08f, x + s * 0.78f, y + s * 0.88f);
            c.drawArc(r, 65f, 235f, false, paint);
            RectF inner = new RectF(x + s * 0.36f, y + s * 0.15f, x + s * 0.86f, y + s * 0.77f);
            c.drawArc(inner, 95f, 190f, false, paint);
        }

        private void drawCloud(Canvas c, float x, float y, float s) {
            path.reset();
            path.moveTo(x + s * 0.16f, y + s * 0.72f);
            path.cubicTo(x + s * 0.02f, y + s * 0.70f, x + s * 0.02f, y + s * 0.48f, x + s * 0.20f, y + s * 0.45f);
            path.cubicTo(x + s * 0.23f, y + s * 0.23f, x + s * 0.48f, y + s * 0.18f, x + s * 0.58f, y + s * 0.37f);
            path.cubicTo(x + s * 0.80f, y + s * 0.31f, x + s * 0.96f, y + s * 0.45f, x + s * 0.91f, y + s * 0.64f);
            path.cubicTo(x + s * 0.89f, y + s * 0.72f, x + s * 0.80f, y + s * 0.75f, x + s * 0.70f, y + s * 0.75f);
            path.lineTo(x + s * 0.18f, y + s * 0.75f);
            c.drawPath(path, paint);
        }

        private void drawRain(Canvas c, float x, float y, float s) {
            for (int i = 0; i < 3; i++) {
                float px = x + s * (0.15f + i * 0.32f);
                c.drawLine(px + s * 0.05f, y, px - s * 0.02f, y + s * 0.24f, paint);
            }
        }

        private void drawSnow(Canvas c, float x, float y, float s) {
            for (int i = 0; i < 3; i++) {
                float cx = x + s * (0.12f + i * 0.36f);
                float cy = y + s * 0.12f;
                float r = s * 0.09f;
                c.drawLine(cx - r, cy, cx + r, cy, paint);
                c.drawLine(cx, cy - r, cx, cy + r, paint);
                c.drawLine(cx - r * 0.7f, cy - r * 0.7f, cx + r * 0.7f, cy + r * 0.7f, paint);
                c.drawLine(cx - r * 0.7f, cy + r * 0.7f, cx + r * 0.7f, cy - r * 0.7f, paint);
            }
        }

        private void drawFog(Canvas c, float x, float y, float s) {
            drawCloud(c, x, y, s * 0.86f);
            for (int i = 0; i < 3; i++) {
                float yy = y + s * (0.68f + i * 0.12f);
                c.drawLine(x + s * 0.12f, yy, x + s * 0.82f, yy, paint);
            }
        }

        private void drawLightning(Canvas c, float x, float y, float s) {
            path.reset();
            path.moveTo(x + s * 0.48f, y);
            path.lineTo(x + s * 0.18f, y + s * 0.48f);
            path.lineTo(x + s * 0.47f, y + s * 0.45f);
            path.lineTo(x + s * 0.25f, y + s);
            path.lineTo(x + s * 0.78f, y + s * 0.35f);
            path.lineTo(x + s * 0.50f, y + s * 0.38f);
            c.drawPath(path, paint);
        }
    }
}
