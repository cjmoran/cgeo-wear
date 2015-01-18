/*
	Copyright 2015 Cullin Moran
	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

		http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
 */

package com.javadog.LocationUtils;

import android.content.Context;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

/**
 * Utility class for reading/filtering sensor values and calculating compass rotation.
 */
public class LocationUtils implements SensorEventListener {

	private static final long COMPASS_UPDATE_INTERVAL = 2000;
	private static final long LOCATION_UPDATE_INTERVAL = 2000;
	private static final long LOCATION_UPDATE_MAX_INTERVAL = 1000;

	private Location currentLocation;
	private Location geocacheLocation;

	private SensorManager sensorManager;
	private Sensor accelerometer;
	private Sensor magnetometer;
	private LocationRequest locationRequest;

	private OnLocationUpdateListener onLocationUpdateListener;

	public LocationUtils(Context applicationContext, final Location cacheLocation) {
		geocacheLocation = cacheLocation;

		//Specify how quickly we want to receive location updates
		locationRequest = LocationRequest.create()
				.setInterval(LOCATION_UPDATE_INTERVAL)
				.setFastestInterval(LOCATION_UPDATE_MAX_INTERVAL)
				.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

		//Listen for updates from the phone compass
		sensorManager = (SensorManager) applicationContext.getSystemService(Context.SENSOR_SERVICE);
		accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
		sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);
	}

	private float[] gravity;
	private float[] geomagnetic;
	private float oldDirection = 0, oldLatitude = 0, oldLongitude = 0, oldAltitude = 0;
	private long prevTime = System.currentTimeMillis();

	@Override
	public final void onSensorChanged(SensorEvent event) {
		if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			gravity = event.values.clone();
		} else if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
			geomagnetic = event.values.clone();
		}

		if(gravity != null && geomagnetic != null) {
			float[] R = new float[9];
			float[] I = new float[9];

			boolean success = SensorManager.getRotationMatrix(R, I, gravity, geomagnetic);
			if(success) {
				float[] orientation = new float[3];
				SensorManager.getOrientation(R, orientation);
				float azimuth = (float) Math.toDegrees(orientation[0]);
				float pitch = (float) Math.toDegrees(orientation[1]);

				if(currentLocation != null) {
					float smoothedLatitude = smoothSensorValues(
							oldLatitude, (float) currentLocation.getLatitude(), 1 / 3f);
					float smoothedLongitude = smoothSensorValues(
							oldLongitude, (float) currentLocation.getLongitude(), 1 / 3f);
					float smoothedAltitude = smoothSensorValues(
							oldAltitude, (float) currentLocation.getAltitude(), 1 / 3f);

					GeomagneticField geomagneticField = new GeomagneticField(
							smoothedLatitude,
							smoothedLongitude,
							smoothedAltitude,
							System.currentTimeMillis()
					);
					azimuth += geomagneticField.getDeclination();

					float bearing = currentLocation.bearingTo(geocacheLocation);

					float direction = smoothSensorValues(oldDirection, -(azimuth - bearing), 1 / 5f);

					//If the user puts the phone in his/her pocket upside-down, invert the compass
					if(pitch > 0) {
						direction += 180f;
					}

					//Set old values to current values (for smoothing)
					oldDirection = direction;
					oldLatitude = smoothedLatitude;
					oldLongitude = smoothedLongitude;
					oldAltitude = smoothedAltitude;

					//Send direction update to Android Wear if update interval has passed
					long currentTime = System.currentTimeMillis();
					if((currentTime - prevTime) > COMPASS_UPDATE_INTERVAL) {
						onLocationUpdateListener.onDirectionUpdate(direction);
						prevTime = currentTime;
					}
				}
			}
		}
	}

	@Override
	public final void onAccuracyChanged(Sensor sensor, int i) {
	}

	/**
	 * Interface for instantiating class to receive distance and direction updates.
	 */
	public interface OnLocationUpdateListener {
		public void onDistanceUpdate(float newDistance);
		public void onDirectionUpdate(float newDirection);
	}

	private LocationListener locationListener = new LocationListener() {
		@Override
		public void onLocationChanged(Location location) {
			//Update stored currentLocation
			currentLocation = location;

			//Calculate new distance (meters) to geocache
			float distance = currentLocation.distanceTo(geocacheLocation);

			//Pass new distance to listener
			onLocationUpdateListener.onDistanceUpdate(distance);
		}
	};

	/**
	 * @param onLocationUpdateListener Client's implementation of
	 */
	public void setOnLocationUpdateListener(OnLocationUpdateListener onLocationUpdateListener) {
		this.onLocationUpdateListener = onLocationUpdateListener;
	}

	/**
	 * Tells the LocationUtils object to start requesting updates from LocationServices.
	 */
	public final void startListeningForUpdates(GoogleApiClient apiClient) {
		LocationServices.FusedLocationApi.requestLocationUpdates(apiClient, locationRequest, locationListener);
	}

	/**
	 * Tells the LocationUtils object to unregister its receivers for sensor updates.
	 */
	public final void stopListeningForUpdates() {
		sensorManager.unregisterListener(this, accelerometer);
		sensorManager.unregisterListener(this, magnetometer);
	}

	/**
	 * A low-pass filter for smoothing out noisy sensor values.
	 *
	 * @param oldVal      The previous value.
	 * @param newVal      The new value.
	 * @param decayFactor Decay factor. (1 / decayFactor) = number of samples to smooth over.
	 * @return The smoothed value.
	 */
	private final float smoothSensorValues(float oldVal, float newVal, float decayFactor) {
		return oldVal * (1 - decayFactor) + newVal * decayFactor;
	}
}
