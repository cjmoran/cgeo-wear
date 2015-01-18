/*
	Copyright 2014 Cullin Moran
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

package com.javadog.cgeowear;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.javadog.LocationUtils.LocationUtils;

import java.net.ConnectException;
import java.util.HashSet;
import java.util.NoSuchElementException;

public class MobileService extends Service
		implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
	public static final String DEBUG_TAG = "com.javadog.cgeowear";

	private static final String INTENT_INIT = "cgeo.geocaching.wear.NAVIGATE_TO";
	private static final String INTENT_STOP = "com.javadog.cgeowear.STOP_APP";

	private static final String MESSAGE_NAVIGATING_NOW = "Navigating on Android Wear";
	private static final String MESSAGE_NO_WEAR_DEVICE = "No Android Wear device paired!";
	private static final String MESSAGE_ERROR_COMMUNICATING = "Error communicating with Wear device. Contact support.";

	/**
	 * Constants from c:geo's Android Wear API that I wrote.
	 */
	private static final String PREFIX = "cgeo.geocaching.intent.extra.";
	private static final String EXTRA_CACHE_NAME = PREFIX + "name";
	private static final String EXTRA_GEOCODE = PREFIX + "geocode";
	private static final String EXTRA_LATITUDE = PREFIX + "latitude";
	private static final String EXTRA_LONGITUDE = PREFIX + "longitude";

	private GoogleApiClient apiClient;
	private WearInterface wearInterface;
	private LocationUtils locationUtils;

	private String cacheName;
	private String geocode;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if(intent != null) {
			final String action = intent.getAction();
			if(INTENT_INIT.equals(action)) {
				cacheName = intent.getStringExtra(EXTRA_CACHE_NAME);
				geocode = intent.getStringExtra(EXTRA_GEOCODE);

				final double latitude = intent.getDoubleExtra(EXTRA_LATITUDE, 0d);
				final double longitude = intent.getDoubleExtra(EXTRA_LONGITUDE, 0d);
				final Location geocacheLocation = new Location("c:geo");
				geocacheLocation.setLatitude(latitude);
				geocacheLocation.setLongitude(longitude);
				setupLocationUtils(geocacheLocation);

				connectGoogleApiClient();
			}
		}
		return START_STICKY;
	}

	private void setupLocationUtils(Location geocacheLocation) {
		if(locationUtils != null) {
			locationUtils.stopListeningForUpdates();
		}

		locationUtils = new LocationUtils(getApplicationContext(), geocacheLocation);

		locationUtils.setOnLocationUpdateListener(new LocationUtils.OnLocationUpdateListener() {
			@Override
			public void onDistanceUpdate(float newDistance) {
				wearInterface.sendDistanceUpdate(newDistance);
			}

			@Override
			public void onDirectionUpdate(float newDirection) {
				wearInterface.sendDirectionUpdate(newDirection);
			}
		});
	}

	private void connectGoogleApiClient() {
		if(apiClient != null) {
			apiClient.unregisterConnectionCallbacks(this);
			apiClient.unregisterConnectionFailedListener(this);
			apiClient.disconnect();
		}
		apiClient = new GoogleApiClient.Builder(this, this, this)
				.addApi(Wearable.API)
				.addApi(LocationServices.API)
				.build();
		apiClient.connect();
	}

	@Override
	public void onCreate() {
		super.onCreate();

		//Register listener for INTENT_STOP events
		IntentFilter filter = new IntentFilter(INTENT_STOP);
		registerReceiver(intentReceiver, filter);

		//Register listener for when watch app closes
		IntentFilter localFilter = new IntentFilter(ListenerService.PATH_KILL_APP);
		LocalBroadcastManager.getInstance(this).registerReceiver(localReceiver, localFilter);

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
				Log.d(DEBUG_TAG, "Received 'stop' intent; stopping phone service.");
				stopSelf();
			}
		}
	};

	/**
	 * Handles broadcast originated by watch app closing; stops this service.
	 */
	BroadcastReceiver localReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if(ListenerService.PATH_KILL_APP.equals(action)) {
				Log.d(DEBUG_TAG, "Watch app closed; stopping phone service.");
				stopSelf();
			}
		}
	};

	@Override
	public void onDestroy() {
		stopWearApp();
		stopForeground(true);
		apiClient.disconnect();

		//Stop listeners
		unregisterReceiver(intentReceiver);
		LocalBroadcastManager.getInstance(this).unregisterReceiver(localReceiver);
		locationUtils.stopListeningForUpdates();

		super.onDestroy();
	}

	/**
	 * Stops the Android Wear counterpart to this app.
	 */
	private void stopWearApp() {
		if(wearInterface != null) {
			wearInterface.sendKillRequest();
		}
	}

	final Handler initThreadHandler = new Handler(new Callback() {
		@Override
		public boolean handleMessage(Message msg) {
			//Display any String received as a Toast; quit if there was a fatal error
			String message = (String) msg.obj;
			Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();

			if(message.equals(MESSAGE_NO_WEAR_DEVICE) || message.equals(MESSAGE_ERROR_COMMUNICATING)) {
				stopSelf();
			}
			return true;
		}
	});

	@Override
	public void onConnected(Bundle bundle) {
		Log.d(DEBUG_TAG, "Connected to Play Services");

		locationUtils.startListeningForUpdates(apiClient);

		//Get ID of connected Wear device and send it the initial cache info (in another thread)
		new Thread(new Runnable() {
			@Override
			public void run() {
				HashSet<String> connectedWearDevices = new HashSet<>();
				NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(apiClient).await();
				for(Node node : nodes.getNodes()) {
					connectedWearDevices.add(node.getId());
				}

				Message m = new Message();
				try {
					wearInterface = new WearInterface(apiClient, connectedWearDevices.iterator().next());
					wearInterface.initTracking(cacheName, geocode, 0f, 0f);
					m.obj = MESSAGE_NAVIGATING_NOW;

				} catch(ConnectException e) {
					Log.e(DEBUG_TAG, "Couldn't send initial tracking data.");
					m.obj = MESSAGE_ERROR_COMMUNICATING;

				} catch(NoSuchElementException e) {
					Log.e(DEBUG_TAG, "No Wear devices connected. Killing service...");
					m.obj = MESSAGE_NO_WEAR_DEVICE;

				} finally {
					initThreadHandler.sendMessage(m);
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

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
