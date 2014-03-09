package org.uninottstt.android.util;

import java.util.List;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

public class LocationHandler implements LocationListener {

	private final String TAG = "LiveLocationManager";

	private LocationManager locationManager = null;

	private Location bestCurrentLocation = null;

	public static final int HALF_MIN = 1000 * 30;

	public LocationHandler(Context context) {
		locationManager = (LocationManager) context
				.getSystemService(Context.LOCATION_SERVICE);
	}

	public void onNewBestLocation(Location loc) {

	}

	public void onLocationChanged(Location loc) {

		if (isBetterLocation(loc, bestCurrentLocation)) {
			bestCurrentLocation = loc;

			onNewBestLocation(bestCurrentLocation);

			Log.d(TAG,
					"New location from " + loc.getProvider() + " ["
							+ loc.getLatitude() + " : " + loc.getLongitude()
							+ " : " + loc.getAccuracy() + "]");
		}

	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

	public void onProviderEnabled(String provider) {
	}

	public void onProviderDisabled(String provider) {
	}

	public Location getCurrentLocation() {

		Location lastGPSLocation = locationManager
				.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		Location lastNetworkLocation = locationManager
				.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

		if (lastGPSLocation != null
				&& isBetterLocation(lastGPSLocation, bestCurrentLocation)) {
			bestCurrentLocation = lastGPSLocation;
		}
		if (lastNetworkLocation != null
				&& isBetterLocation(lastNetworkLocation, bestCurrentLocation)) {
			bestCurrentLocation = lastNetworkLocation;
		}

		return bestCurrentLocation;
	}

	public Runnable startListeners = new Runnable() {
		@Override
		public void run() {
			LocationListener handle = LocationHandler.this;
			List<String> providers = locationManager.getProviders(true);
			for (String p : providers) {
				locationManager.requestLocationUpdates(p, 0, 0, handle);
			}
		}
	};

	public Runnable stopListeners = new Runnable() {
		@Override
		public void run() {
			LocationListener handle = LocationHandler.this;
			locationManager.removeUpdates(handle);
		}
	};

	public boolean isBetterLocation(Location location,
			Location currentBestLocation) {
		if (currentBestLocation == null) {
			return true;
		}

		long timeDelta = location.getTime() - currentBestLocation.getTime();
		boolean isSignificantlyNewer = timeDelta > HALF_MIN;
		boolean isSignificantlyOlder = timeDelta < -HALF_MIN;
		boolean isNewer = timeDelta > 0;

		if (isSignificantlyNewer) {
			return true;
		} else if (isSignificantlyOlder) {
			return false;
		}

		int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation
				.getAccuracy());
		boolean isLessAccurate = accuracyDelta > 0;
		boolean isMoreAccurate = accuracyDelta < 0;
		boolean isSignificantlyLessAccurate = accuracyDelta > 200;

		boolean isFromSameProvider = isSameProvider(location.getProvider(),
				currentBestLocation.getProvider());

		if (isMoreAccurate) {
			return true;
		} else if (isNewer && !isLessAccurate) {
			return true;
		} else if (isNewer && !isSignificantlyLessAccurate
				&& isFromSameProvider) {
			return true;
		}
		return false;
	}

	private boolean isSameProvider(String provider1, String provider2) {
		if (provider1 == null) {
			return provider2 == null;
		}
		return provider1.equals(provider2);
	}

}
