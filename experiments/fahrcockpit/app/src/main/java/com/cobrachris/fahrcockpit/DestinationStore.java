package com.cobrachris.fahrcockpit;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/** Phone-local navigation destination store. No network, telemetry or extra account. */
final class DestinationStore {
    static final String PREFS = "fahrcockpit_destinations_v23";
    private static final String LEGACY = "fahrcockpit_manual_routes_v1";
    private static final int FAVORITES = 20;
    private static final int RECENT = 12;

    static final class Entry {
        final String label;
        final String target;
        final boolean favorite;
        Entry(String label, String target, boolean favorite) {
            this.label = label;
            this.target = target;
            this.favorite = favorite;
        }
    }

    private DestinationStore() {}

    private static List<Entry> load(Context ctx, String prefix, int maximum,
                                    boolean favorites) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        List<Entry> found = new ArrayList<>();
        for (int i = 0; i < maximum; i++) {
            String target = RouteTargetRules.clean(p.getString(prefix + i + "_target", ""));
            if (target == null) continue;
            String title = DestinationMatchRules.label(
                p.getString(prefix + i + "_label", target));
            found.add(new Entry(title == null ? target : title, target, favorites));
        }
        return found;
    }

    static List<Entry> favorites(Context ctx) {
        return load(ctx, "fav_", FAVORITES, true);
    }

    static List<Entry> recent(Context ctx) {
        return load(ctx, "recent_", RECENT, false);
    }

    /** Read the original v18/v19/v22 favorites without modifying their storage. */
    static List<Entry> legacy(Context ctx) {
        SharedPreferences p = ctx.getSharedPreferences(LEGACY, Context.MODE_PRIVATE);
        List<Entry> result = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            String value = RouteTargetRules.clean(p.getString("target_" + i, ""));
            if (value != null && !containsTarget(result, value))
                result.add(new Entry(value, value, true));
        }
        return result;
    }

    static List<Entry> suggestions(Context ctx, String query, int maximum) {
        List<Entry> values = new ArrayList<>();
        if (maximum <= 0) return values;
        appendMatches(values, favorites(ctx), query, maximum);
        appendMatches(values, recent(ctx), query, maximum);
        appendMatches(values, legacy(ctx), query, maximum);
        return values;
    }

    private static boolean containsTarget(List<Entry> list, String target) {
        for (Entry entry : list)
            if (DestinationMatchRules.sameTarget(entry.target, target)) return true;
        return false;
    }

    private static void appendMatches(List<Entry> out, List<Entry> items,
                                      String query, int maximum) {
        for (Entry item : items) {
            if (out.size() >= maximum) return;
            if (!containsTarget(out, item.target)
                    && DestinationMatchRules.matches(query, item.label, item.target))
                out.add(item);
        }
    }

    static boolean saveFavorite(Context context, String label, String destination) {
        String target = RouteTargetRules.clean(destination);
        String title = DestinationMatchRules.label(label);
        if (target == null || title == null) return false;
        List<Entry> list = favorites(context);
        list.removeIf(item -> DestinationMatchRules.sameTarget(item.target, target)
            || item.label.equalsIgnoreCase(title));
        list.add(0, new Entry(title, target, true));
        return write(context, "fav_", FAVORITES, list);
    }

    static boolean removeFavorite(Context context, String target) {
        List<Entry> list = favorites(context);
        if (!list.removeIf(item -> DestinationMatchRules.sameTarget(item.target, target)))
            return false;
        return write(context, "fav_", FAVORITES, list);
    }

    static boolean recordRecent(Context context, String label, String destination) {
        String target = RouteTargetRules.clean(destination);
        if (target == null) return false;
        String title = DestinationMatchRules.label(label);
        List<Entry> list = recent(context);
        list.removeIf(item -> DestinationMatchRules.sameTarget(item.target, target));
        list.add(0, new Entry(title == null ? target : title, target, false));
        return write(context, "recent_", RECENT, list);
    }

    private static boolean write(Context context, String prefix, int maximum,
                                 List<Entry> values) {
        SharedPreferences.Editor edit =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
        for (int i = 0; i < maximum; i++) {
            edit.remove(prefix + i + "_target");
            edit.remove(prefix + i + "_label");
        }
        for (int i = 0; i < Math.min(values.size(), maximum); i++) {
            Entry item = values.get(i);
            edit.putString(prefix + i + "_target", item.target);
            edit.putString(prefix + i + "_label", item.label);
        }
        return edit.commit();
    }
}
