package com.javadog.cgeowear;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.net.ConnectException;
import java.util.HashSet;
import java.util.NoSuchElementException;

public class WearService extends Service
		implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
	public static final String DEBUG_TAG = "com.javadog.cgeowear";

	private static final String INTENT_INIT = "cgeo.geocaching.wear.NAVIGATE_TO";
	private static final String INTENT_STOP = "com.javadog.cgeowear.STOP_APP";

	private static final String EXTRA_CACHE_NAME = "cgeo.geocaching.wear.extra.CACHE_NAME";
	private static final String EXTRA_GEOCODE = "cgeo.geocaching.wear.extra.GEOCODE";
	private static final String EXTRA_LATITUDE = "cgeo.geocaching.wear.extra.LATITUDE";
	private static final String EXTRA_LONGITUDE = "cgeo.geocaching.wear.extra.LONGITUDE";

	private GoogleApiClient apiClient;
	private WearInterface wearInterface;

	private String cacheName;
	private String geocode;
	private double latitude;
	private double longitude;
	private float distance;
	private float direction;

	@Override
	public void onCreate() {
		super.onCreate();
		handleInit();
	}

	/**
	 * Starts service & watch app.
	 */
	private void handleInit() {
		//TODO: Ensure GPS/compass/etc. is available
		//TODO: Ensure an Android Wear device is paired
		//TODO: Register location receiver
		//TODO: Start Wear app

		//Register listener for INTENT_STOP events
		IntentFilter filter = new IntentFilter(INTENT_STOP);
		registerReceiver(intentReceiver, filter);

		//Show a persistent notification
		Intent stopServiceIntent = new Intent(INTENT_STOP);
		PendingIntent nIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, stopServiceIntent, 0);
		Notification notification = new NotificationCompat.Builder(getApplicationContext())
				.setOngoing(true)
				.setContentIntent(nIntent)
				.setSmallIcon(R.drawable.ic_launcher)
				.setContentTitle(getText(R.string.app_name))
				.setContentText(getText(R.string.notification_text))
				.build();

		//Connect to Google APIs
		apiClient = new GoogleApiClient.Builder(getApplicationContext(), WearService.this, WearService.this)
				.addApi(Wearable.API)
				.build();
		apiClient.connect();

		//Start service in foreground
		startForeground(R.string.app_name, notification);
	}

	/**
	 * Handles INTENT_STOP event.
	 */
	BroadcastReceiver intentReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if(INTENT_STOP.equals(action)) {
				stopApp();
			}
		}
	};

	@Override
	public void onDestroy() {
		unregisterReceiver(intentReceiver);
		stopWearApp();
		stopForeground(true);
		apiClient.disconnect();

		//TODO: Unregister location listener

		super.onDestroy();
	}

	/**
	 * Stops both this phone service and the Wear app.
	 */
	private void stopApp() {
		stopSelf();
		stopWearApp();
	}

	/**
	 * Stops the Android Wear counterpart to this app.
	 */
	private void stopWearApp() {
		//TODO: Implement
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if(intent != null) {
			final String action = intent.getAction();
			if(INTENT_INIT.equals(action)) {
				cacheName = intent.getStringExtra(EXTRA_CACHE_NAME);
				geocode = intent.getStringExtra(EXTRA_GEOCODE);
				latitude = intent.getDoubleExtra(EXTRA_LATITUDE, 0d);
				longitude = intent.getDoubleExtra(EXTRA_LONGITUDE, 0d);

				Toast.makeText(
						getApplicationContext(), getText(R.string.toast_service_started), Toast.LENGTH_SHORT).show();
			}
		}

		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onConnected(Bundle bundle) {
		Log.d(DEBUG_TAG, "Connected to Play Services");

		//Get ID of connected Wear device
		new Thread(new Runnable() {
			@Override
			public void run() {
				HashSet<String> connectedWearDevices = new HashSet<String>();
				NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(apiClient).await();
				for(Node node : nodes.getNodes()) {
					connectedWearDevices.add(node.getId());
				}

				wearInterface = new WearInterface(apiClient);

				try {
					wearInterface.initTracking(
							connectedWearDevices.iterator().next(), cacheName, geocode, 12.34f, 0.12f);
				} catch(ConnectException e) {
					Log.e(DEBUG_TAG, "Couldn't send initial tracking data.");
				} catch(NoSuchElementException e) {
					//TODO: Handle this with a warning in the UI
					Log.e(DEBUG_TAG, "No Wear devices connected. Killing service...");
					stopApp();
				}
			}
		}).start();
	}

	@Override
	public void onConnectionSuspended(int i) {
		Log.d(DEBUG_TAG, "Play Services connection suspended.");
	}

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		Log.e(DEBUG_TAG, "Failed to connect to Google Play Services.");
	}
}
