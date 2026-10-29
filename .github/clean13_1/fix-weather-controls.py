#!/usr/bin/env python3
from pathlib import Path

p = Path('.github/clean13_1/WeatherStatusBarBridge.java')
s = p.read_text()

# Keep weak references to the live Weather page and Home so async refreshes/toggles
# can update the visible UI immediately rather than waiting for a later lifecycle pass.
anchor = '    private static final Map<ViewGroup, AdaptivePillWatcher> PILL_WATCHERS = new WeakHashMap<>();\n'
assert s.count(anchor) == 1
s = s.replace(anchor, anchor + '''    private static WeakReference<Object> ACTIVE_WEATHER_PAGE = new WeakReference<>(null);\n    private static WeakReference<Activity> LAST_HOME = new WeakReference<>(null);\n''', 1)

# Parent Status Bar row: no postcode/temperature subtitle; add a real weather icon.
old = r'''            SharedPreferences p = prefs(context);
            String query = p.getString(KEY_QUERY, "").trim();
            String resolved = p.getString(KEY_RESOLVED, "").trim();
            String subtitle;
            if (p.contains(KEY_TEMP_C)) {
                double tempC = Double.parseDouble(p.getString(KEY_TEMP_C, "0"));
                boolean f = UNIT_F.equals(p.getString(KEY_UNIT, UNIT_C));
                long display = Math.round(f ? tempC * 9.0 / 5.0 + 32.0 : tempC);
                String place = resolved.length() != 0 ? resolved : query;
                subtitle = display + (f ? "°F" : "°C") + (place.length() != 0 ? " • " + place : "");
            } else if (resolved.length() != 0) subtitle = resolved;
            else if (query.length() != 0) subtitle = query;
            else subtitle = "Configure weather";

            Object weather = makeAction(fragment, ID_WEATHER_PAGE, "Weather", subtitle);
'''
new = r'''            Object weather = makeActionWithIcon(fragment, context, ID_WEATHER_PAGE, "Weather", null);
'''
assert s.count(old) == 1
s = s.replace(old, new, 1)

# Track the currently-visible Weather page so completed background refreshes can update it.
old = r'''    public static boolean populateWeatherPage(Object fragment, List list) {
        if (!isWeatherPage(fragment)) return false;
        addWeatherActions(fragment, list);
        Log.i(TAG, "CLEAN13.1B Weather native page populated");
        return true;
    }
'''
new = r'''    public static boolean populateWeatherPage(Object fragment, List list) {
        if (!isWeatherPage(fragment)) return false;
        ACTIVE_WEATHER_PAGE = new WeakReference<>(fragment);
        addWeatherActions(fragment, list);
        Log.i(TAG, "CLEAN13.1D Weather native page populated");
        return true;
    }
'''
assert s.count(old) == 1
s = s.replace(old, new, 1)

# Make Show weather a direct On/Off row instead of a fragile nested dropdown.
old = r'''            list.add(makeDropdown(fragment, ID_SHOW_PARENT, "Show weather",
                    p.getBoolean(KEY_SHOW, true) ? "On" : "Off",
                    new long[]{ID_SHOW_ON, ID_SHOW_OFF},
                    new String[]{"On", "Off"}));
'''
new = r'''            list.add(makeAction(fragment, ID_SHOW_PARENT, "Show weather",
                    p.getBoolean(KEY_SHOW, true) ? "On" : "Off"));
'''
assert s.count(old) == 1
s = s.replace(old, new, 1)

# Direct Show-weather row toggles reliably and immediately re-renders the known Home.
old = r'''            if (id == ID_SHOW_ON || id == ID_SHOW_OFF) {
                p.edit().putBoolean(KEY_SHOW, id == ID_SHOW_ON).apply();
                refreshMenu(fragment);
                Activity a = activityOf(fragment);
                if (a != null) applyToHome(a);
                Log.i(TAG, "CLEAN13.1 show weather=" + (id == ID_SHOW_ON));
                return true;
            }
'''
new = r'''            if (id == ID_SHOW_PARENT || id == ID_SHOW_ON || id == ID_SHOW_OFF) {
                boolean show = id == ID_SHOW_PARENT ? !p.getBoolean(KEY_SHOW, true) : id == ID_SHOW_ON;
                p.edit().putBoolean(KEY_SHOW, show).commit();
                refreshMenu(fragment);
                renderLastHome();
                Activity a = activityOf(fragment);
                if (a != null) render(a);
                Log.i(TAG, "CLEAN13.1D show weather=" + show);
                return true;
            }
'''
assert s.count(old) == 1
s = s.replace(old, new, 1)

# Refresh is already a direct action; make the request explicit in logs and refresh the row immediately.
old = r'''            if (id == ID_REFRESH) {
                Activity a = activityOf(fragment);
                if (a != null) {
                    maybeRefresh(a, true);
                    render(a);
                }
                return true;
            }
'''
new = r'''            if (id == ID_REFRESH) {
                Activity a = activityOf(fragment);
                Log.i(TAG, "CLEAN13.1D manual refresh requested");
                if (a != null) {
                    maybeRefresh(a, true);
                    render(a);
                }
                refreshMenu(fragment);
                return true;
            }
'''
assert s.count(old) == 1
s = s.replace(old, new, 1)

# Remember the actual launcher Home activity whenever lifecycle integration calls us.
old = r'''    public static void applyToHome(final Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        final WeakReference<Activity> ref = new WeakReference<>(activity);
'''
new = r'''    public static void applyToHome(final Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        LAST_HOME = new WeakReference<>(activity);
        final WeakReference<Activity> ref = new WeakReference<>(activity);
'''
assert s.count(old) == 1
s = s.replace(old, new, 1)

# After an async network attempt, update both Home and the open Weather page.
old = r'''                    MAIN.post(new Runnable() {
                        @Override public void run() {
                            Activity a = ref.get();
                            if (a != null && !a.isFinishing() && !a.isDestroyed()) render(a);
                        }
                    });
'''
new = r'''                    MAIN.post(new Runnable() {
                        @Override public void run() {
                            Activity a = ref.get();
                            if (a != null && !a.isFinishing() && !a.isDestroyed()) render(a);
                            renderLastHome();
                            refreshActiveWeatherPage();
                        }
                    });
'''
assert s.count(old) == 1
s = s.replace(old, new, 1)

# UK postcodes: use the UK-specific Postcodes.io lookup first. It accepts spaces/case
# insensitively and returns WGS84 lat/lon. City/place names still use Open-Meteo geocoding.
start = s.index('    private static double[] geocode(Context context, String query) throws Exception {')
end = s.index('    private static void fetchCurrent(Context context, String query, double lat, double lon) throws Exception {', start)
old_method = s[start:end]
new_method = r'''    private static double[] geocode(Context context, String query) throws Exception {
        if (looksLikeUkPostcode(query)) {
            try {
                double[] uk = geocodeUkPostcode(context, query);
                if (uk != null) return uk;
            } catch (Throwable t) {
                Log.w(TAG, "CLEAN13.1D UK postcode lookup failed; falling back to Open-Meteo", t);
            }
        }

        String lookup = query;
        String extra = "";
        if (looksLikeUkPostcode(query)) {
            lookup = formatUkPostcode(query);
            extra = "&countryCode=GB";
        }
        String url = "https://geocoding-api.open-meteo.com/v1/search?name="
                + URLEncoder.encode(lookup, "UTF-8") + "&count=5&language=en&format=json" + extra;
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
        saveResolvedLocation(context, query, lat, lon, resolved.toString(), "Open-Meteo");
        return new double[]{lat, lon};
    }

    private static double[] geocodeUkPostcode(Context context, String query) throws Exception {
        String compact = compactUkPostcode(query);
        String url = "https://api.postcodes.io/postcodes/" + URLEncoder.encode(compact, "UTF-8");
        JSONObject root = new JSONObject(readUrl(url));
        JSONObject r = root.optJSONObject("result");
        if (r == null || r.isNull("latitude") || r.isNull("longitude")) return null;
        double lat = r.getDouble("latitude");
        double lon = r.getDouble("longitude");
        String postcode = r.optString("postcode", formatUkPostcode(query));
        String district = r.optString("admin_district", "");
        String country = r.optString("country", "");
        String resolved = postcode;
        if (district.length() != 0) resolved += " • " + district;
        if (country.length() != 0) resolved += " • " + country;
        saveResolvedLocation(context, query, lat, lon, resolved, "Postcodes.io");
        return new double[]{lat, lon};
    }

    private static void saveResolvedLocation(Context context, String query, double lat, double lon,
                                             String resolved, String provider) {
        SharedPreferences p = prefs(context);
        if (!query.equals(p.getString(KEY_QUERY, "").trim())) return;
        p.edit()
                .putString(KEY_LAT, Double.toString(lat))
                .putString(KEY_LON, Double.toString(lon))
                .putString(KEY_RESOLVED, resolved)
                .commit();
        Log.i(TAG, "CLEAN13.1D geocoded via " + provider + " " + query + " -> " + resolved
                + " " + lat + "," + lon);
    }

    private static boolean looksLikeUkPostcode(String query) {
        String c = compactUkPostcode(query);
        return c.matches("^(GIR0AA|[A-Z]{1,2}[0-9][0-9A-Z]?[0-9][A-Z]{2})$");
    }

    private static String compactUkPostcode(String query) {
        return query == null ? "" : query.toUpperCase(Locale.UK).replaceAll("\\s+", "");
    }

    private static String formatUkPostcode(String query) {
        String c = compactUkPostcode(query);
        if (c.length() <= 3) return c;
        return c.substring(0, c.length() - 3) + " " + c.substring(c.length() - 3);
    }

'''
s = s[:start] + new_method + s[end:]

# Helpers used by toggle/refresh to update the live launcher/page immediately.
anchor = '    private static void render(Activity activity) {\n'
assert s.count(anchor) == 1
helpers = r'''    private static void renderLastHome() {
        Activity home = LAST_HOME.get();
        if (home != null && !home.isFinishing() && !home.isDestroyed()) render(home);
    }

    private static void refreshActiveWeatherPage() {
        Object page = ACTIVE_WEATHER_PAGE.get();
        if (page != null) refreshMenu(page);
    }

'''
s = s.replace(anchor, helpers + anchor, 1)

# Build a proper monochrome weather icon for the parent Status Bar row.
anchor = '    private static Object makeAction(Object fragment, long id, String title, String subtitle) throws Exception {\n'
assert s.count(anchor) == 1
icon_factory = r'''    private static Object makeActionWithIcon(Object fragment, Context context, long id,
                                             String title, String subtitle) throws Exception {
        Class<?> cb3 = Class.forName("cb3");
        Class<?> lu1 = Class.forName("lu1");
        Method t0 = cb3.getDeclaredMethod("t0", cb3, long.class, CharSequence.class, Drawable.class, int.class);
        t0.setAccessible(true);
        Method z0 = cb3.getDeclaredMethod("z0", lu1);
        z0.setAccessible(true);
        Object action = t0.invoke(null, fragment, id, title, new WeatherMenuDrawable(context), 28);
        if (subtitle != null && subtitle.length() != 0) field(action, "d").set(action, subtitle);
        return z0.invoke(null, action);
    }

'''
s = s.replace(anchor, icon_factory + anchor, 1)

anchor = '    private static final class AdaptivePillWatcher implements ViewTreeObserver.OnPreDrawListener {\n'
assert s.count(anchor) == 1
icon_class = r'''    private static final class WeatherMenuDrawable extends Drawable {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path cloud = new Path();

        WeatherMenuDrawable(Context context) {
            p.setColor(0xffffffff);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeJoin(Paint.Join.ROUND);
            p.setStrokeWidth(Math.max(2f, dp(context, 2)));
        }

        @Override public void draw(Canvas c) {
            RectF b = new RectF(getBounds());
            float s = Math.min(b.width(), b.height());
            if (s <= 0f) return;
            float x = b.centerX() - s / 2f;
            float y = b.centerY() - s / 2f;
            float sx = x + s * 0.33f, sy = y + s * 0.34f, sr = s * 0.17f;
            c.drawCircle(sx, sy, sr, p);
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * i / 4.0;
                float x1 = sx + (float)Math.cos(a) * s * 0.24f;
                float y1 = sy + (float)Math.sin(a) * s * 0.24f;
                float x2 = sx + (float)Math.cos(a) * s * 0.31f;
                float y2 = sy + (float)Math.sin(a) * s * 0.31f;
                c.drawLine(x1, y1, x2, y2, p);
            }
            cloud.reset();
            cloud.moveTo(x + s * 0.30f, y + s * 0.72f);
            cloud.cubicTo(x + s * 0.18f, y + s * 0.72f, x + s * 0.16f, y + s * 0.55f, x + s * 0.29f, y + s * 0.51f);
            cloud.cubicTo(x + s * 0.33f, y + s * 0.37f, x + s * 0.55f, y + s * 0.36f, x + s * 0.62f, y + s * 0.50f);
            cloud.cubicTo(x + s * 0.80f, y + s * 0.47f, x + s * 0.88f, y + s * 0.72f, x + s * 0.70f, y + s * 0.72f);
            cloud.close();
            c.drawPath(cloud, p);
        }

        @Override public void setAlpha(int alpha) { p.setAlpha(alpha); invalidateSelf(); }
        @Override public void setColorFilter(android.graphics.ColorFilter cf) { p.setColorFilter(cf); invalidateSelf(); }
        @Override public int getOpacity() { return android.graphics.PixelFormat.TRANSLUCENT; }
        @Override public int getIntrinsicWidth() { return 48; }
        @Override public int getIntrinsicHeight() { return 48; }
    }

'''
s = s.replace(anchor, icon_class + anchor, 1)

for token in [
    'CLEAN13.1D show weather=',
    'CLEAN13.1D manual refresh requested',
    'api.postcodes.io/postcodes/',
    'CLEAN13.1D geocoded via',
    'WeatherMenuDrawable',
    'renderLastHome',
    'refreshActiveWeatherPage',
]:
    assert token in s, token

p.write_text(s)
print('PASS: CLEAN13.1D weather controls fixed: icon, clean parent row, UK postcode, toggle, refresh feedback')
