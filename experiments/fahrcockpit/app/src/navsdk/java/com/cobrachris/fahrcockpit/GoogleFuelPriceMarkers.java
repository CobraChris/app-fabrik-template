package com.cobrachris.fahrcockpit;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import java.util.ArrayList;
import java.util.List;

/**
 * Tankerkoenig markers are separate from Google's native map POIs.
 * No extra map, no Google Places request and no route request on map refresh.
 */
final class GoogleFuelPriceMarkers {
    private final Context context;
    private final List<Marker> markers = new ArrayList<>();
    private GoogleMap map;

    GoogleFuelPriceMarkers(Context context) {
        this.context = context;
    }

    void display(GoogleMap active, FuelMapSnapshot.Snapshot snapshot,
                 NavigationEngine.FuelMarkerSelection listener) {
        clear();
        map = active;
        if (active == null || snapshot == null || snapshot.stations.isEmpty()) return;
        boolean stale = FuelMapSnapshot.isStale(snapshot, System.currentTimeMillis());
        String checked = java.text.DateFormat.getDateTimeInstance(
            java.text.DateFormat.SHORT, java.text.DateFormat.SHORT,
            java.util.Locale.GERMANY).format(new java.util.Date(snapshot.fetchedAt));
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (FuelMapSnapshot.Station s : snapshot.stations) {
            min = Math.min(min, s.price);
            max = Math.max(max, s.price);
        }
        for (FuelMapSnapshot.Station s : snapshot.stations) {
            int color = stale ? Color.rgb(117, 122, 133) : tierColor(s, min, max);
            try {
                Marker marker = active.addMarker(new MarkerOptions()
                    .position(new LatLng(s.lat, s.lng))
                    .title(s.name)
                    .snippet((stale ? "VERALTETER PREIS · " : "Preisstand ") + checked
                        + " · " + s.priceLabel() + "/l · " + snapshot.type.toUpperCase(
                        java.util.Locale.GERMANY) + " · " + s.address
                        + "\nTippen: als Navigationsziel auswählen")
                    .icon(BitmapDescriptorFactory.fromBitmap(
                        priceBitmap(s.priceLabel(), color, stale)))
                    .anchor(0.5f, 1.0f)
                    .zIndex(9.0f));
                if (marker != null) {
                    marker.setTag(s);
                    markers.add(marker);
                }
            } catch (RuntimeException incompatibleMap) {
                // Never disrupt guidance just because a nonessential marker fails.
                clear();
                return;
            }
        }
        // The map's normal marker tap displays Google's info window. A second
        // tap on that info window opens a confirmation; never routes silently.
        active.setOnInfoWindowClickListener(marker -> {
            Object tagged = marker.getTag();
            if (tagged instanceof FuelMapSnapshot.Station && listener != null) {
                listener.onStationSelected((FuelMapSnapshot.Station) tagged);
            }
        });
    }

    private int tierColor(FuelMapSnapshot.Station station, double min, double max) {
        if (!station.isOpen) return Color.rgb(109, 116, 126);
        if (max - min < 0.05) return Color.rgb(30, 114, 149);
        double level = (station.price - min) / (max - min);
        if (level <= 0.333) return Color.rgb(25, 126, 80);
        if (level <= 0.666) return Color.rgb(190, 128, 19);
        return Color.rgb(171, 67, 56);
    }

    private Bitmap priceBitmap(String label, int color, boolean stale) {
        float d = context.getResources().getDisplayMetrics().density;
        int width = Math.max(1, Math.round(91 * d));
        int height = Math.max(1, Math.round(37 * d));
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        canvas.drawRoundRect(new RectF(1*d, 1*d, width - 1*d, height - 5*d),
            9*d, 9*d, paint);
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2*d);
        canvas.drawRoundRect(new RectF(2*d, 2*d, width - 2*d, height - 6*d),
            8*d, 8*d, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(15*d);
        paint.setFakeBoldText(true);
        Paint.FontMetrics fm = paint.getFontMetrics();
        float baseline = (height - 5*d)/2f - (fm.ascent + fm.descent)/2f;
        canvas.drawText(label, (width - paint.measureText(label))/2f, baseline, paint);
        if (stale) {
            // Visible monochrome warning; never present a cached old
            // price as a current live quotation on the map.
            paint.setTextSize(8*d);
            paint.setColor(Color.rgb(105, 109, 120));
            canvas.drawText("ALT", 8*d, 9*d, paint);
        }
        return bitmap;
    }

    void clear() {
        for (Marker marker : markers) {
            try { marker.remove(); } catch (RuntimeException ignored) {}
        }
        markers.clear();
        map = null;
    }
}
