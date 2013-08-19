package org.blitzortung.android.app.controller;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.blitzortung.android.app.Main;
import org.blitzortung.android.app.R;
import org.blitzortung.android.app.preference.PreferenceKey;
import org.blitzortung.android.app.preference.SharedPreferencesWrapper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Singleton
public class LocationHandler implements SharedPreferencesWrapper.OnSharedPreferenceChangeListener, LocationListener, GpsStatus.Listener {

    public static interface Listener {
        void onLocationChanged(Location location);
    }

    public static enum LocationProvider {
        NETWORK(LocationManager.NETWORK_PROVIDER),
        GPS(LocationManager.GPS_PROVIDER),
        PASSIVE(LocationManager.PASSIVE_PROVIDER),
        MANUAL("manual");

        private String type;

        private LocationProvider(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }

        private static Map<String, LocationProvider> stringToValueMap = new HashMap<String, LocationProvider>();

        static {
            for (LocationProvider key : LocationProvider.values()) {
                String keyString = key.getType();
                if (stringToValueMap.containsKey(keyString)) {
                    throw new IllegalStateException(String.format("key value '%s' already defined", keyString));
                }
                stringToValueMap.put(keyString, key);
            }
        }

        public static LocationProvider fromString(String string) {
            return stringToValueMap.get(string);
        }
    }

    public void onPause() {
        locationManager.removeUpdates(this);
    }

    public void onResume() {
        enableProvider(locationProvider);
    }

    private final LocationManager locationManager;

    private final Provider<Context> contextProvider;

    private LocationProvider locationProvider;

    private final Location location;

    private Set<Listener> listeners = new HashSet<Listener>();

    @Inject
    public LocationHandler(LocationManager locationManager, SharedPreferencesWrapper sharedPreferences, Provider<Context> contextProvider) {
        this.locationManager = locationManager;
        locationManager.addGpsStatusListener(this);
        
        location = new Location("");
        invalidateLocation();

        onSharedPreferenceChanged(sharedPreferences.get(), PreferenceKey.LOCATION_MODE);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        this.contextProvider = contextProvider;
    }

    @Override
    public void onLocationChanged(android.location.Location location) {
        this.location.set(location);
        sendLocationUpdate();
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
    }

    @Override
    public void onProviderEnabled(String s) {
    }

    @Override
    public void onProviderDisabled(String s) {
        invalidateLocation();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferencesWrapper sharedPreferences, PreferenceKey key) {
        switch (key) {
            case LOCATION_MODE:
                LocationProvider newLocationProvider = LocationProvider.fromString(sharedPreferences.getString(key.toString(), LocationProvider.NETWORK.getType()));
                if (newLocationProvider != locationProvider) {
                    updateProvider(newLocationProvider, sharedPreferences);
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
            Log.v(Main.LOG_TAG, "LocationHandler: bad number format for manual latitude setting");
        }
    }

    private void updateManualLongitude(SharedPreferences sharedPreferences) {
        try {
            location.setLongitude(Double.valueOf(sharedPreferences.getString(PreferenceKey.LOCATION_LONGITUDE.toString(), "11.0")));
            sendLocationUpdate();
        } catch (NumberFormatException e) {
            Log.v(Main.LOG_TAG, "LocationHandler: bad number format for manual longitude setting");
        }
    }

    private void updateProvider(LocationProvider newLocationProvider, SharedPreferences sharedPreferences) {
        if (newLocationProvider == LocationProvider.MANUAL) {
            locationManager.removeUpdates(this);
            updateManualLongitude(sharedPreferences);
            updateManualLatitude(sharedPreferences);
            location.setProvider(newLocationProvider.getType());
        } else {
            invalidateLocationAndSendLocationUpdate();
        }
        enableProvider(newLocationProvider);
    }

    private void invalidateLocationAndSendLocationUpdate() {
        sendLocationUpdateToListeners(null);
        invalidateLocation();
    }

    private void invalidateLocation() {
        location.setLongitude(Double.NaN);
        location.setLatitude(Double.NaN);
    }

    private void enableProvider(LocationProvider newLocationProvider) {
        locationManager.removeUpdates(this);
        if (newLocationProvider != null && newLocationProvider != LocationProvider.MANUAL) {
            if (!locationManager.getAllProviders().contains(newLocationProvider.getType())) {
                Toast toast = Toast.makeText(contextProvider.get(), String.format(contextProvider.get().getResources().getText(R.string.location_provider_not_available).toString(), newLocationProvider.toString()), 5000);
                toast.show();
                return;
            }
            locationManager.requestLocationUpdates(newLocationProvider.getType(), locationProvider == LocationProvider.GPS ? 1000 : 10000, 10, this);
        }
        locationProvider = newLocationProvider;
    }

    private void sendLocationUpdate() {
        sendLocationUpdateToListeners(locationIsValid() ? location : null);
    }

    private void sendLocationUpdateToListeners(Location location) {
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
        return location != null && !Double.isNaN(location.getLongitude()) && !Double.isNaN(location.getLatitude());
    }

    public void removeUpdates(LocationHandler.Listener target) {
        listeners.remove(target);
    }

    @Override
    public void onGpsStatusChanged(int event) {

        if (locationProvider == LocationProvider.GPS) {
            switch (event) {
                case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                    Location lastKnownGpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (lastKnownGpsLocation != null) {
                        long secondsElapsedSinceLastFix = (System.currentTimeMillis() - lastKnownGpsLocation.getTime()) / 1000;

                        if (secondsElapsedSinceLastFix < 10) {
                            if (!locationIsValid()) {
                                location.set(lastKnownGpsLocation);
                                onLocationChanged(location);
                            }
                            break;
                        }
                    }
                    if (locationIsValid()) {
                        invalidateLocationAndSendLocationUpdate();
                    }
                    break;
            }
        }
    }

}
