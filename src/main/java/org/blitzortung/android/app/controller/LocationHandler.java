package org.blitzortung.android.app.controller;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import org.blitzortung.android.app.view.PreferenceKey;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LocationHandler implements SharedPreferences.OnSharedPreferenceChangeListener, LocationListener {

    public static interface Listener {
        void onLocationChanged(Location location);
    }

    public static enum Provider {
        NETWORK(LocationManager.NETWORK_PROVIDER),
        GPS(LocationManager.GPS_PROVIDER),
        PASSIVE(LocationManager.PASSIVE_PROVIDER),
        MANUAL("manual");

        private String type;

        private Provider(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }

        private static Map<String, Provider> stringToValueMap = new HashMap<String, Provider>();

        static {
            for (Provider key : Provider.values()) {
                String keyString = key.getType();
                if (stringToValueMap.containsKey(keyString)) {
                    throw new IllegalStateException(String.format("key value '%s' already defined", keyString));
                }
                stringToValueMap.put(keyString, key);
            }
        }

        public static Provider fromString(String string) {
            return stringToValueMap.get(string);
        }
    }

    public void onPause() {
        locationManager.removeUpdates(this);
    }

    public void onResume() {
        if (provider != null) {
            enableProvider(provider);
        }
    }

    private final LocationManager locationManager;

    private Provider provider;

    private final Location location;

    private Set<Listener> listeners = new HashSet<Listener>();

    public LocationHandler(Context context) {
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        location = new Location("");
        location.setLongitude(Double.NaN);
        location.setLatitude(Double.NaN);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        onSharedPreferenceChanged(sharedPreferences, PreferenceKey.LOCATION_MODE);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onLocationChanged(android.location.Location location) {
        this.location.set(location);
        sendLocationUpdate();
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void onProviderEnabled(String s) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void onProviderDisabled(String s) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String keyString) {
        onSharedPreferenceChanged(sharedPreferences, PreferenceKey.fromString(keyString));
    }

    private void onSharedPreferenceChanged(SharedPreferences sharedPreferences, PreferenceKey... keys) {
        for (PreferenceKey key : keys) {
            onSharedPreferenceChanged(sharedPreferences, key);
        }
    }

    private void onSharedPreferenceChanged(SharedPreferences sharedPreferences, PreferenceKey key) {
        switch (key) {
            case LOCATION_MODE:
                Provider newProvider = Provider.fromString(sharedPreferences.getString(key.toString(), Provider.NETWORK.toString()));
                if (newProvider != provider) {
                    updateProvider(newProvider, sharedPreferences);
                }
                break;

            case LOCATION_LONGITUDE:
                updateManualLongitude(sharedPreferences);
                break;

            case LOCATION_LATITUDE:
                updateManualLatitude(sharedPreferences);
                break;

        }
    }

    private void updateManualLatitude(SharedPreferences sharedPreferences) {
        try {
            location.setLatitude(Double.valueOf(sharedPreferences.getString(PreferenceKey.LOCATION_LATITUDE.toString(), "49.0")));
            sendLocationUpdate();
        } catch (NumberFormatException e) {
            Log.v("LocationHandler", "bad number format for manual latitude setting");
        }
    }

    private void updateManualLongitude(SharedPreferences sharedPreferences) {
        try {
            location.setLongitude(Double.valueOf(sharedPreferences.getString(PreferenceKey.LOCATION_LONGITUDE.toString(), "11.0")));
            sendLocationUpdate();
        } catch (NumberFormatException e) {
            Log.v("LocationHandler", "bad number format for manual longitude setting");
        }
    }

    private void updateProvider(Provider newProvider, SharedPreferences sharedPreferences) {
        if (newProvider == Provider.MANUAL) {
            locationManager.removeUpdates(this);
            updateManualLongitude(sharedPreferences);
            updateManualLatitude(sharedPreferences);
            location.setProvider(newProvider.getType());
        } else {
            enableProvider(newProvider);
            location.setLongitude(Double.NaN);
            location.setLatitude(Double.NaN);
        }
        provider = newProvider;
    }

    private void enableProvider(Provider newProvider) {
        if (locationManager.isProviderEnabled(newProvider.getType())) {
            locationManager.removeUpdates(this);
            locationManager.requestLocationUpdates(newProvider.getType(), 0, 0, this);
        }
    }

    private void sendLocationUpdate() {
        if (locationIsValid()) {
            sendUpdate(location);
        }
    }

    private void sendUpdate(Location location) {
        for (Listener listener : listeners) {
            listener.onLocationChanged(location);
        }
    }

    public void requestUpdates(LocationHandler.Listener target) {
        listeners.add(target);
        if (locationIsValid()) {
            target.onLocationChanged(location);
        }
    }

    private boolean locationIsValid() {
        return !Double.isNaN(location.getLongitude()) && !Double.isNaN(location.getLatitude());
    }

    public void removeUpdates(LocationHandler.Listener target) {
        listeners.remove(target);
    }

    public boolean isProviderEnabled() {
        if (provider != Provider.MANUAL) {
            return provider == null ? false : locationManager.isProviderEnabled(provider.getType());
        }
        return true;
    }

}