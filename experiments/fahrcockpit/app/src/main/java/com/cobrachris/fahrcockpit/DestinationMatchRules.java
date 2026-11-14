package com.cobrachris.fahrcockpit;

import java.util.Locale;

/** Pure local filters: no network request and no Google billing. */
final class DestinationMatchRules {
    private DestinationMatchRules() {}

    static String label(String raw) {
        String cleaned = RouteTargetRules.clean(raw);
        if (cleaned == null) return null;
        return cleaned.length() > 64 ? cleaned.substring(0, 64).trim() : cleaned;
    }

    static boolean matches(String query, String label, String target) {
        String q = RouteTargetRules.clean(query);
        if (q == null) return true;
        String term = q.toLowerCase(Locale.ROOT);
        return (label != null && label.toLowerCase(Locale.ROOT).contains(term))
            || (target != null && target.toLowerCase(Locale.ROOT).contains(term));
    }

    static boolean sameTarget(String one, String two) {
        return one != null && two != null
            && one.trim().equalsIgnoreCase(two.trim());
    }

    static boolean maySuggestOnline(String query) {
        String q = RouteTargetRules.clean(query);
        return q != null && q.length() >= 3 && q.length() <= 160
            && IntegratedNavigationRules.parseCoordinates(q) == null;
    }
}
