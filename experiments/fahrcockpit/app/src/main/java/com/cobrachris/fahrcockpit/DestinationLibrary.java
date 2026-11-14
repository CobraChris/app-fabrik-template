package com.cobrachris.fahrcockpit;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Private, offline destination library. Never sends addresses or searches to
 * Google Places. Imports the previous v18/v19 target_0..4 entries once without
 * changing the old preferences so upgrading the signed app does not lose data.
 */
final class DestinationLibrary {
    private static final String PREFS = "fahrcockpit_destination_library_v1";
    private static final String FAVORITES = "favorites";
    private static final String RECENTS = "recent";
    private static final String LEGACY_PREFS = "fahrcockpit_manual_routes_v1";
    static final int MAX_FAVORITES = 30;
    static final int MAX_RECENTS = 15;

    static final class Entry {
        final String label;
        final String target;
        Entry(String label, String target) {
            this.label = label;
            this.target = target;
        }
    }

    private DestinationLibrary() {}

    static synchronized List<Entry> favorites(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!prefs.contains(FAVORITES)) {
            ArrayList<Entry> legacy = new ArrayList<>();
            SharedPreferences old = context.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE);
            for (int i = 0; i < 5; i++) {
                String value = RouteTargetRules.clean(old.getString("target_" + i, ""));
                if (value != null && !containsTarget(legacy, value))
                    legacy.add(new Entry(value, value));
            }
            // A failed migration commit must not conceal the old data.
            if (!write(prefs, FAVORITES, legacy)) return legacy;
        }
        return read(prefs, FAVORITES);
    }

    static synchronized List<Entry> recent(Context context) {
        return read(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE), RECENTS);
    }

    static synchronized boolean isFavorite(Context context, String target) {
        return containsTarget(favorites(context), target);
    }

    static synchronized boolean saveFavorite(Context context, String label, String target) {
        String normalizedTarget = RouteTargetRules.clean(target);
        String normalizedLabel = RouteTargetRules.clean(label);
        if (normalizedTarget == null) return false;
        if (normalizedLabel == null) normalizedLabel = normalizedTarget;
        List<Entry> entries = favorites(context);
        removeTarget(entries, normalizedTarget);
        entries.add(0, new Entry(normalizedLabel, normalizedTarget));
        if (entries.size() > MAX_FAVORITES) entries = entries.subList(0, MAX_FAVORITES);
        return write(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE), FAVORITES, entries);
    }

    static synchronized boolean updateFavorite(Context context, String oldTarget,
                                               String label, String newTarget) {
        String normalizedTarget = RouteTargetRules.clean(newTarget);
        String normalizedLabel = RouteTargetRules.clean(label);
        if (normalizedTarget == null || normalizedLabel == null) return false;
        List<Entry> entries = favorites(context);
        removeTarget(entries, oldTarget);
        removeTarget(entries, normalizedTarget);
        entries.add(0, new Entry(normalizedLabel, normalizedTarget));
        if (entries.size() > MAX_FAVORITES) entries = entries.subList(0, MAX_FAVORITES);
        return write(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE), FAVORITES, entries);
    }

    static synchronized boolean deleteFavorite(Context context, String target) {
        List<Entry> entries = favorites(context);
        removeTarget(entries, target);
        return write(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE), FAVORITES, entries);
    }

    static synchronized boolean remember(Context context, String label, String target) {
        String normalizedTarget = RouteTargetRules.clean(target);
        String normalizedLabel = RouteTargetRules.clean(label);
        if (normalizedTarget == null) return false;
        if (normalizedLabel == null) normalizedLabel = normalizedTarget;
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        List<Entry> entries = read(prefs, RECENTS);
        removeTarget(entries, normalizedTarget);
        entries.add(0, new Entry(normalizedLabel, normalizedTarget));
        if (entries.size() > MAX_RECENTS) entries = entries.subList(0, MAX_RECENTS);
        return write(prefs, RECENTS, entries);
    }

    static synchronized List<Entry> suggestions(Context context, String query) {
        ArrayList<Entry> result = new ArrayList<>();
        List<Entry> favs = favorites(context);
        List<Entry> recent = recent(context);
        Set<String> used = new HashSet<>();
        for (List<Entry> source : new List[] { favs, recent }) {
            for (Entry entry : source) {
                if (!DestinationSuggestionRules.matches(query, entry.label, entry.target))
                    continue;
                String key = DestinationSuggestionRules.normalized(entry.target);
                if (used.add(key)) result.add(entry);
                if (result.size() == DestinationSuggestionRules.MAX_VISIBLE_SUGGESTIONS)
                    return result;
            }
        }
        return result;
    }

    private static boolean containsTarget(List<Entry> entries, String target) {
        String needle = DestinationSuggestionRules.normalized(target);
        for (Entry entry : entries)
            if (DestinationSuggestionRules.normalized(entry.target).equals(needle))
                return true;
        return false;
    }

    private static void removeTarget(List<Entry> entries, String target) {
        String needle = DestinationSuggestionRules.normalized(target);
        entries.removeIf(entry -> DestinationSuggestionRules.normalized(entry.target).equals(needle));
    }

    private static List<Entry> read(SharedPreferences prefs, String key) {
        ArrayList<Entry> result = new ArrayList<>();
        String raw = prefs.getString(key, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            int max = key.equals(FAVORITES) ? MAX_FAVORITES : MAX_RECENTS;
            for (int i = 0; i < array.length() && result.size() < max; i++) {
                JSONObject item = array.optJSONObject(i);
                if (item == null) continue;
                String target = RouteTargetRules.clean(item.optString("target", ""));
                String label = RouteTargetRules.clean(item.optString("label", ""));
                if (target != null && !containsTarget(result, target))
                    result.add(new Entry(label == null ? target : label, target));
            }
        } catch (org.json.JSONException ignored) {
            // Corrupted local data must not crash navigation.
        }
        return result;
    }

    private static boolean write(SharedPreferences prefs, String key, List<Entry> entries) {
        JSONArray array = new JSONArray();
        try {
            for (Entry entry : entries) {
                JSONObject item = new JSONObject();
                item.put("label", entry.label);
                item.put("target", entry.target);
                array.put(item);
            }
        } catch (org.json.JSONException invalid) { return false; }
        return prefs.edit().putString(key, array.toString()).commit();
    }
}
