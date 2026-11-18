package com.cobrachris.fahrcockpit;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Phone-local navigation destination store. No network, telemetry or extra account. */
final class DestinationStore {
    static final String PREFS = "fahrcockpit_destinations_v23";
    private static final String LEGACY = "fahrcockpit_manual_routes_v1";
    private static final int FAVORITES = 20;
    private static final int RECENT = 12;
    private static final int LEGACY_SLOTS = 5;

    enum Source { FAVORITE, LEGACY_FAVORITE, RECENT }

    static final class Entry {
        final String label;
        final String target;
        final boolean favorite;
        final Source source;

        Entry(String label, String target, boolean favorite) {
            this(label, target, favorite ? Source.FAVORITE : Source.RECENT);
        }

        Entry(String label, String target, Source source) {
            this.label = label;
            this.target = target;
            this.source = source;
            this.favorite = source != Source.RECENT;
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

    /** Read original v18/v19/v22 favorites without silently migrating or deleting them. */
    static List<Entry> legacy(Context ctx) {
        SharedPreferences p = ctx.getSharedPreferences(LEGACY, Context.MODE_PRIVATE);
        List<Entry> result = new ArrayList<>();
        for (int i = 0; i < LEGACY_SLOTS; i++) {
            String value = RouteTargetRules.clean(p.getString("target_" + i, ""));
            if (value != null && !containsTarget(result, value))
                result.add(new Entry(value, value, Source.LEGACY_FAVORITE));
        }
        return result;
    }

    static List<Entry> suggestions(Context ctx, String query, int maximum) {
        List<Entry> values = new ArrayList<>();
        if (maximum <= 0) return values;
        appendMatches(values, favorites(ctx), query, maximum);
        appendMatches(values, legacy(ctx), query, maximum);
        appendMatches(values, recent(ctx), query, maximum);
        return values;
    }

    /**
     * Display the same destinations in the manager that users can select.
     * Favor older saved favorites over recent history, and newer custom labels
     * over both. Duplicate targets are shown only once.
     */
    static List<Entry> manageable(Context ctx) {
        return combineManageable(favorites(ctx), legacy(ctx), recent(ctx));
    }

    /** Pure composition logic, shared with unit tests (no Android state). */
    static List<Entry> combineManageable(List<Entry> saved, List<Entry> older,
                                         List<Entry> history) {
        List<Entry> result = new ArrayList<>();
        appendMatches(result, saved, "", FAVORITES + LEGACY_SLOTS + RECENT);
        appendMatches(result, older, "", FAVORITES + LEGACY_SLOTS + RECENT);
        appendMatches(result, history, "", FAVORITES + LEGACY_SLOTS + RECENT);
        return result;
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

    /**
     * Rename and/or relocate a favorite in one committed transaction.
     * Reject accidental collisions instead of silently overwriting another
     * custom destination. A failure leaves the old entry intact.
     */
    static boolean updateFavorite(Context context, String oldTarget,
                                  String newLabel, String newAddress) {
        String target = RouteTargetRules.clean(newAddress);
        String title = DestinationMatchRules.label(newLabel);
        if (target == null || title == null) return false;
        List<Entry> entries = favorites(context);
        int original = -1;
        for (int i = 0; i < entries.size(); i++) {
            Entry current = entries.get(i);
            if (DestinationMatchRules.sameTarget(current.target, oldTarget)) {
                original = i;
            } else if (DestinationMatchRules.sameTarget(current.target, target)
                    || current.label.equalsIgnoreCase(title)) {
                return false;
            }
        }
        if (original == -1) return false;
        entries.remove(original);
        entries.add(0, new Entry(title, target, true));
        return write(context, "fav_", FAVORITES, entries);
    }

    /**
     * Move an older saved target into the editable v23 store only after a valid
     * new favorite has been committed. Never overwrite an unrelated favorite.
     * If the second write fails, the old target remains (no loss of data).
     */
    static boolean updateLegacyFavorite(Context context, String oldTarget,
                                        String newLabel, String newAddress) {
        String target = RouteTargetRules.clean(newAddress);
        String title = DestinationMatchRules.label(newLabel);
        if (target == null || title == null || !containsTarget(legacy(context), oldTarget))
            return false;
        for (Entry entry : favorites(context)) {
            if (DestinationMatchRules.sameTarget(entry.target, target)
                    || entry.label.equalsIgnoreCase(title)) return false;
        }
        for (Entry entry : legacy(context)) {
            if (!DestinationMatchRules.sameTarget(entry.target, oldTarget)
                    && DestinationMatchRules.sameTarget(entry.target, target)) return false;
        }
        if (!saveFavorite(context, title, target)) return false;
        return removeLegacy(context, oldTarget);
    }

    static boolean removeFavorite(Context context, String target) {
        List<Entry> list = favorites(context);
        if (!list.removeIf(item -> DestinationMatchRules.sameTarget(item.target, target)))
            return false;
        return write(context, "fav_", FAVORITES, list);
    }

    /**
     * Remove an explicitly selected destination from all three local stores.
     * Deleting only the modern favorite would let an older duplicate reappear
     * immediately in "Meine Ziele". No other target is touched.
     */
    static boolean removeManaged(Context context, String destination) {
        String target = RouteTargetRules.clean(destination);
        if (target == null) return false;
        List<Entry> saved = favorites(context);
        List<Entry> history = recent(context);
        boolean hadSaved = saved.removeIf(item ->
            DestinationMatchRules.sameTarget(item.target, target));
        boolean hadHistory = history.removeIf(item ->
            DestinationMatchRules.sameTarget(item.target, target));
        SharedPreferences oldPrefs = context.getSharedPreferences(LEGACY, Context.MODE_PRIVATE);
        List<String> legacyKeys = matchingLegacyKeys(oldPrefs.getAll(), target);
        if (!hadSaved && !hadHistory && legacyKeys.isEmpty()) return false;

        // One atomic write for modern favorites and recent history.
        if (hadSaved || hadHistory) {
            SharedPreferences.Editor editor =
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
            if (hadSaved) writeInto(editor, "fav_", FAVORITES, saved);
            if (hadHistory) writeInto(editor, "recent_", RECENT, history);
            if (!editor.commit()) return false;
        }
        if (legacyKeys.isEmpty()) return true;
        SharedPreferences.Editor oldEditor = oldPrefs.edit();
        for (String key : legacyKeys) oldEditor.remove(key);
        return oldEditor.commit();
    }

    /** Identify only matching legacy slots; never clear the whole preference file. */
    static List<String> matchingLegacyKeys(Map<String, ?> prefs, String destination) {
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < LEGACY_SLOTS; i++) {
            String key = "target_" + i;
            Object raw = prefs.get(key);
            if (raw instanceof String
                    && DestinationMatchRules.sameTarget((String) raw, destination))
                keys.add(key);
        }
        return keys;
    }

    private static boolean removeLegacy(Context context, String target) {
        SharedPreferences prefs = context.getSharedPreferences(LEGACY, Context.MODE_PRIVATE);
        List<String> keys = matchingLegacyKeys(prefs.getAll(), target);
        if (keys.isEmpty()) return false;
        SharedPreferences.Editor editor = prefs.edit();
        for (String key : keys) editor.remove(key);
        return editor.commit();
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

    private static void writeInto(SharedPreferences.Editor edit, String prefix,
                                  int maximum, List<Entry> values) {
        for (int i = 0; i < maximum; i++) {
            edit.remove(prefix + i + "_target");
            edit.remove(prefix + i + "_label");
        }
        for (int i = 0; i < Math.min(values.size(), maximum); i++) {
            Entry item = values.get(i);
            edit.putString(prefix + i + "_target", item.target);
            edit.putString(prefix + i + "_label", item.label);
        }
    }

    private static boolean write(Context context, String prefix, int maximum,
                                 List<Entry> values) {
        SharedPreferences.Editor edit =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
        writeInto(edit, prefix, maximum, values);
        return edit.commit();
    }
}
