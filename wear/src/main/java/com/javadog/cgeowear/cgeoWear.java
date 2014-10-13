package com.javadog.cgeowear;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.text.DecimalFormat;
import java.util.HashSet;

public class cgeoWear extends Activity
		implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
		SensorEventListener {
	public static final String DEBUG_TAG = "com.javadog.cgeowear";

	private static final long DIRECTION_UPDATE_SPEED = 2000;

	private TextView tv_cacheName;
	private TextView tv_geocode;
	private TextView tv_distance;
	private ImageView iv_compass;

	private SensorManager sensorManager;
	private Sensor accelerometer;
	private Sensor magnetometer;
	private boolean useWatchCompass;

	private float distance;
	private float direction;
	private Location currentLocation;
	private Location geocacheLocation;

	private LocalBroadcastManager broadcastManager;

	private GoogleApiClient apiClient;
	private String connectedNodeId;

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_cgeo_wear);

		tv_cacheName = (TextView) findViewById(R.id.textview_cache_name);
		tv_geocode = (TextView) findViewById(R.id.textview_geocode);
		tv_distance = (TextView) findViewById(R.id.textview_distance);
		iv_compass = (ImageView) findViewById(R.id.compass);

		Intent i = getIntent();
		initScreen(i);

		//Register BroadcastReceiver for location updates
		broadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
		IntentFilter updateFilter = new IntentFilter();
		updateFilter.addAction(ListenerService.PATH_UPDATE_DISTANCE);
		updateFilter.addAction(ListenerService.PATH_UPDATE_DIRECTION);
		updateFilter.addAction(ListenerService.PATH_UPDATE_LOCATION);
		updateFilter.addAction(ListenerService.PATH_KILL_APP);
		broadcastManager.registerReceiver(broadcastReceiver, updateFilter);

		apiClient = new GoogleApiClient.Builder(this, this, this)
				.addApi(Wearable.API)
				.build();
		apiClient.connect();
    }

	/**
	 * Initializes the screen, whether the Activity was freshly-launched or onNewIntent was run.
	 *
	 * @param i The launch intent.
	 */
	private void initScreen(Intent i) {
		String cacheName = i.getStringExtra(MessageDataSet.KEY_CACHE_NAME);
		String geocode = i.getStringExtra(MessageDataSet.KEY_GEOCODE);
		distance = i.getFloatExtra(MessageDataSet.KEY_DISTANCE, 0f);
		direction = i.getFloatExtra(MessageDataSet.KEY_DIRECTION, 0f);
		geocacheLocation = i.getParcelableExtra(MessageDataSet.KEY_CACHE_LOCATION);

		//Start listening for compass updates, if the user wants
		useWatchCompass = i.getBooleanExtra(MessageDataSet.KEY_WATCH_COMPASS, false);
		if(accelerometer != null && magnetometer != null) {
			sensorManager.unregisterListener(this, accelerometer);
			sensorManager.unregisterListener(this, magnetometer);
		}
		Log.d(DEBUG_TAG, useWatchCompass ? "Using watch compass." : "Using phone compass.");
		if(useWatchCompass) {
			sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
			accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
			magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
			sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
			sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);
		}

		tv_cacheName.setText(cacheName);
		tv_geocode.setText(geocode);
		setDistanceFormatted(distance);
		rotateCompass(direction);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		initScreen(intent);
	}

	/**
	 * Handles location updates, updates UI accordingly.
	 *
	 * Also kills the app if requested by the phone.
	 */
	private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			//Distance update received
			if(ListenerService.PATH_UPDATE_DISTANCE.equals(intent.getAction())) {
				setDistanceFormatted(intent.getFloatExtra(MessageDataSet.KEY_DISTANCE, 0f));

			//Direction update received
			} else if(ListenerService.PATH_UPDATE_DIRECTION.equals(intent.getAction())) {
				rotateCompass(intent.getFloatExtra(MessageDataSet.KEY_DIRECTION, 0f));

			//Location update received
			} else if(ListenerService.PATH_UPDATE_LOCATION.equals(intent.getAction())) {
				Log.d(DEBUG_TAG, "Location update received from phone.");
				currentLocation = intent.getParcelableExtra(MessageDataSet.KEY_LOCATION);

			//Kill app
			} else if(ListenerService.PATH_KILL_APP.equals(intent.getAction())) {
				Log.d(cgeoWear.DEBUG_TAG, "Phone service stopped; killing watch app.");
				cgeoWear.this.finish();
			}
		}
	};

	/**
	 * Sets the distance TextView's text with appropriate formatting.
	 *
	 * @param dist The distance to the geocache, in meters.
	 */
	private void setDistanceFormatted(float dist) {
		DecimalFormat format = new DecimalFormat("0.00m");
		tv_distance.setText(format.format(dist));

		distance = dist;
	}

	/**
	 * Handles rotation of the compass to a new direction.
	 * @param newDirection Direction to turn to, in degrees.
	 */
	private void rotateCompass(float newDirection) {
		if(direction != newDirection) {
			Log.d(DEBUG_TAG, "Rotating compass from " + direction + " to " + newDirection);

			RotateAnimation anim = new RotateAnimation(
					direction,
					newDirection,
					Animation.RELATIVE_TO_SELF, 0.5f,
					Animation.RELATIVE_TO_SELF, 0.5f);
			anim.setDuration(200l);
			anim.setFillAfter(true);
			iv_compass.startAnimation(anim);

			direction = newDirection;
		}
	}

	@Override
	protected void onStop() {
		//Unregister our BroadcastReceiver
		broadcastManager.unregisterReceiver(broadcastReceiver);

		//Attempt to tell the phone service to stop, then disconnect from Google APIs
		if(apiClient.isConnected() && connectedNodeId != null) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					Wearable.MessageApi.sendMessage(
							apiClient, connectedNodeId, ListenerService.PATH_KILL_APP, new byte[0]).await();
					apiClient.disconnect();
				}
			}).start();
		}

		if(useWatchCompass) {
			sensorManager.unregisterListener(this, accelerometer);
			sensorManager.unregisterListener(this, magnetometer);
		}

		super.onStop();
	}

	@Override
	public void onConnected(Bundle bundle) {
		//As soon as the Google APIs are connected, grab the connected Node ID
		new Thread(new Runnable() {
			@Override
			public void run() {
				HashSet<String> connectedWearDevices = new HashSet<String>();
				NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(apiClient).await();
				for(Node node : nodes.getNodes()) {
					connectedWearDevices.add(node.getId());
				}

				connectedNodeId = connectedWearDevices.iterator().next();
			}
		}).start();
	}

	@Override
	public void onConnectionSuspended(int i) {
	}

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
	}

	float[] gravity;
	float[] geomagnetic;
	float oldDirection = 0, oldLatitude = 0, oldLongitude = 0, oldAltitude = 0;
	long prevTime = System.currentTimeMillis();
	/**
	 * Interprets watch compass if user has requested that feature.
	 */
	@Override
	public void onSensorChanged(SensorEvent event) {
		if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			gravity = event.values.clone();
		} else if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
			geomagnetic = event.values.clone(); //TODO: How the hell to access compass?
		}

		if(gravity != null && geomagnetic != null) {
			float[] R = new float[9];
			float[] I = new float[9];

			boolean success = SensorManager.getRotationMatrix(R, I, gravity, geomagnetic);
			if(success) {
				float[] orientation = new float[3];
				SensorManager.getOrientation(R, orientation);
				float azimuth = (float) Math.toDegrees(orientation[0]);

				if(currentLocation != null) {
					float smoothedLatitude = smoothSensorValues(
							oldLatitude, (float) currentLocation.getLatitude(), 1/3f);
					float smoothedLongitude = smoothSensorValues(
							oldLongitude, (float) currentLocation.getLongitude(), 1/3f);
					float smoothedAltitude = smoothSensorValues(
							oldAltitude, (float) currentLocation.getAltitude(), 1/3f);

					GeomagneticField geomagneticField = new GeomagneticField(
							smoothedLatitude,
							smoothedLongitude,
							smoothedAltitude,
							System.currentTimeMillis()
					);
					azimuth += geomagneticField.getDeclination();

					float bearing = currentLocation.bearingTo(geocacheLocation);

					direction = smoothSensorValues(oldDirection, -(azimuth - bearing), 1/5f);

					//Set old values to current values (for smoothing)
					oldDirection = direction;
					oldLatitude = smoothedLatitude;
					oldLongitude = smoothedLongitude;
					oldAltitude = smoothedAltitude;

					//Display direction on compass if update interval has passed
					long currentTime = System.currentTimeMillis();
					if((currentTime - prevTime) > DIRECTION_UPDATE_SPEED) {
						rotateCompass(direction);
						prevTime = currentTime;
					}
				}
			}
		}
	}

	/**
	 * A low-pass filter for smoothing out noisy sensor values.
	 *
	 * @param oldVal The previous value.
	 * @param newVal The new value.
	 * @param decayFactor Decay factor. (1 / decayFactor) = number of samples to smooth over.
	 * @return The smoothed value.
	 */
	private float smoothSensorValues(float oldVal, float newVal, float decayFactor) {
		return oldVal * (1 - decayFactor) + newVal * decayFactor;
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}
}
