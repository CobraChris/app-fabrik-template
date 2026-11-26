package com.cobrachris.fahrcockpit;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Read-only foreground check of Android's actual GATT connection list.
 * Saphe Link owns BLE pairing/connection; no scanning, connectGatt, broadcasts,
 * background service, device traffic or network requests by FahrCockpit.
 * Exact device identifier is recorded only after explicit user selection.
 */
final class SapheBluetoothStatus {
    private static final String PREFS = "fahrcockpit_saphe_bluetooth_status_v41";
    private static final String KEY_ADDRESS = "selected_ble_address";
    private static final String KEY_NAME = "selected_ble_label";

    enum Link { UNKNOWN, OBSERVED_CONNECTED, NOT_OBSERVED }

    static final class Observation {
        final Link link;
        final String detail;
        Observation(Link link, String detail) {
            this.link = link;
            this.detail = detail;
        }
    }

    static final class DeviceEntry {
        final String address;
        final String label;
        DeviceEntry(String address, String label) {
            this.address = address;
            this.label = label;
        }
    }

    private SapheBluetoothStatus() {}

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(
            PREFS, Context.MODE_PRIVATE);
    }

    static boolean hasPermission(Context context) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            && context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
               == PackageManager.PERMISSION_GRANTED;
    }

    static boolean configured(Context context) {
        return !prefs(context).getString(KEY_ADDRESS, "").isEmpty();
    }

    static String label(Context context) {
        return prefs(context).getString(KEY_NAME, "Saphe-Gerät");
    }

    /** No action outside Android's documented read-only GATT inventory. */
    private static List<BluetoothDevice> connectedGatt(Context context) {
        // Repeat the exact Android permission check at the sensitive API
        // boundary, not just in a helper; the runtime grant can be revoked.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                || context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) return null;
        try {
            BluetoothManager manager=(BluetoothManager)
                context.getSystemService(Context.BLUETOOTH_SERVICE);
            if(manager==null)return null;
            BluetoothAdapter adapter=manager.getAdapter();
            if(adapter==null||!adapter.isEnabled())return Collections.emptyList();
            return manager.getConnectedDevices(BluetoothProfile.GATT);
        } catch(RuntimeException error) {
            return null; // Could be an OS error, not proof of disconnection.
        }
    }

    static List<DeviceEntry> connectedForSelection(Context context) {
        // Explicit grant check is repeated before getName/getAddress: Android
        // lint cannot infer the runtime grant through the helper method.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                || context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED)
            return Collections.emptyList();
        List<BluetoothDevice> devices=connectedGatt(context);
        if(devices==null)return Collections.emptyList();
        List<DeviceEntry> choices=new ArrayList<>();
        for(BluetoothDevice device:devices) {
            if(device==null)continue;
            try{
                String address=device.getAddress();
                if(address==null)continue;
                address=address.toUpperCase(java.util.Locale.US);
                if(!BluetoothAdapter.checkBluetoothAddress(address))continue;
                String name=device.getName();
                if(name==null||name.trim().isEmpty())name="Unbekanntes BLE-Gerät";
                name=name.trim();
                if(name.length()>64)name=name.substring(0,64);
                // Deduplicate without leaking a device identifier to the UI.
                boolean duplicate=false;
                for(DeviceEntry previous:choices)
                    if(previous.address.equals(address))duplicate=true;
                if(!duplicate)choices.add(new DeviceEntry(address,name));
            }catch(RuntimeException ignored){
                // Do not assume failure to enumerate is disconnection.
            }
        }
        return choices;
    }

    static boolean choose(Context context, DeviceEntry selected) {
        if(!hasPermission(context)||selected==null||selected.label==null
            ||!BluetoothAdapter.checkBluetoothAddress(selected.address))
            return false;
        // Don't save a guess: selected hardware must be observed connected NOW.
        List<BluetoothDevice> actual=connectedGatt(context);
        if(actual==null)return false;
        boolean present=false;
        for(BluetoothDevice device:actual) {
            if(device==null)continue;
            try{
                if(selected.address.equalsIgnoreCase(device.getAddress()))
                    present=true;
            }catch(RuntimeException ignored){}
        }
        if(!present)return false;
        String display=selected.label.trim();
        if(display.isEmpty()||display.length()>64)display="Saphe-Gerät";
        return prefs(context).edit()
            .putString(KEY_ADDRESS, selected.address)
            .putString(KEY_NAME,display)
            .commit();
    }

    static boolean clear(Context context) {
        return prefs(context).edit().remove(KEY_ADDRESS)
            .remove(KEY_NAME).commit();
    }

    static Observation read(Context context) {
        if(!hasPermission(context))
            return new Observation(Link.UNKNOWN,
                "Bluetooth-Zugriff nicht eingerichtet; Verbindung unbekannt");
        String selected=prefs(context).getString(KEY_ADDRESS,"");
        if(selected.isEmpty())
            return new Observation(Link.UNKNOWN,
                "Saphe-Bluetooth-Gerät noch nicht ausgewählt");
        List<BluetoothDevice> devices=connectedGatt(context);
        if(devices==null)
            return new Observation(Link.UNKNOWN,
                "Android-Bluetooth-Abfrage nicht verfügbar; Verbindung unbekannt");
        boolean incompleteInventory = false;
        for(BluetoothDevice device:devices){
            if(device==null) {
                incompleteInventory = true;
                continue;
            }
            try {
                String address = device.getAddress();
                if (address == null || !BluetoothAdapter.checkBluetoothAddress(address)) {
                    incompleteInventory = true;
                    continue;
                }
                if(selected.equalsIgnoreCase(address))
                    return new Observation(Link.OBSERVED_CONNECTED,
                        "Ausgewähltes BLE-Gerät ist laut Android GATT verbunden");
            }catch(RuntimeException ignored){
                // If Android cannot expose an address, the returned inventory
                // is not complete enough to prove the selected device absent.
                incompleteInventory = true;
            }
        }
        if (incompleteInventory)
            return new Observation(Link.UNKNOWN,
                "Android-Geräteliste unvollständig; Verbindung unbekannt");
        return new Observation(Link.NOT_OBSERVED,
            "Ausgewähltes BLE-Gerät derzeit nicht in Androids GATT-Liste. "
            +"Keine Aussage über die Saphe-App-Funktion oder Warnungen.");
    }
}
