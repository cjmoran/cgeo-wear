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

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.javadog.LocationUtils.LocationUtils;
import com.javadog.WearMessageDataset.MessageDataset;

import java.util.HashSet;
import java.util.NoSuchElementException;

/**
 * Keeps track of info received from the phone app through {@link com.javadog.cgeowear.ListenerService}.
 *
 * Main activity can bind to this service for access to info that needs to be displayed.
 */
public class cgeoWearService extends Service
		implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

	public static final String DEBUG_TAG = "com.javadog.cgeowear";

	private static final int NOTIFICATION_ID = 1;

	private final IBinder iBinder = new LocalBinder();

	private String cacheName, geocode;
	private float distance, direction;
	private boolean useWatchCompass;

	private Location cacheLocation;

	private GoogleApiClient apiClient;
	private String connectedNodeId;
	private LocationUtils locationUtils;

	private LocalBroadcastManager localBroadcastManager;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		connectGoogleApiClient();
		initLocalVars(intent);
		showOngoingNotification();
		listenForUpdates();
		startCompassActivity();

		if(useWatchCompass) {
			setupLocationUtils(cacheLocation);
			locationUtils.startDirectionalTracking(getApplicationContext());
		}

		WakefulBroadcastReceiver.completeWakefulIntent(intent);

		return START_REDELIVER_INTENT;
	}

	/**
	 * Starts this service in the foreground and supplies the 3-page Android Wear home screen notification.
	 */
	private void showOngoingNotification() {
		//Action 2 - 'Stop Navigating' (kills watch and phone apps)
		Intent killAppIntent = new Intent(ListenerService.PATH_KILL_APP);
		PendingIntent killAppPendingIntent = PendingIntent.getBroadcast(
				this, 0, killAppIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		//Action 1 - 'Show Compass'
		Intent launchIntent = new Intent(this, CompassActivity.class);
		launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		launchIntent.setAction(ListenerService.PATH_INIT);
		PendingIntent launchPendingIntent = PendingIntent.getActivity(
				this, 0, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		//Main Notification
		NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
				.setSmallIcon(R.drawable.ic_launcher)
				.setContentTitle(getString(R.string.app_name))
				.setContentText(getString(R.string.notification1_body))
				.addAction(R.drawable.ic_show_compass, getString(R.string.notification2_label), launchPendingIntent)
				.addAction(R.drawable.ic_stop_navigation, getString(R.string.notification3_label),
						killAppPendingIntent)
				.extend(new NotificationCompat.WearableExtender()
						.setBackground(BitmapFactory.decodeResource(getResources(), R.drawable.background_image)));

		startForeground(NOTIFICATION_ID, notificationBuilder.build());
	}

	private void setupLocationUtils(Location geocacheLocation) {
		if(locationUtils != null) {
			locationUtils.stopListeningForUpdates();
		}

		locationUtils = new LocationUtils(
				geocacheLocation,
				null, // (all location tracking is phone-side for now)
				new LocationUtils.OnDirectionUpdateListener() {
					@Override
					public void onDirectionUpdate(float newDirection) {
						direction = newDirection;
						triggerMinimalUiRefresh();
					}
				},
				null);
	}

	private void connectGoogleApiClient() {
		apiClient = new GoogleApiClient.Builder(this, this, this)
				.addApi(Wearable.API)
				.build();
		apiClient.connect();
	}

	/**
	 * Initializes the watch-side service's local variables from the intent passed to onStartCommand.
	 *
	 * @param startIntent The Intent passed to
	 * {@link com.javadog.cgeowear.cgeoWearService#onStartCommand(android.content.Intent, int, int)}
	 */
	private void initLocalVars(final Intent startIntent) {
		cacheName = startIntent.getStringExtra(MessageDataset.KEY_CACHE_NAME);
		geocode = startIntent.getStringExtra(MessageDataset.KEY_GEOCODE);
		distance = startIntent.getFloatExtra(MessageDataset.KEY_DISTANCE, 0f);
		direction = startIntent.getFloatExtra(MessageDataset.KEY_DIRECTION, 0f);
		useWatchCompass = startIntent.getBooleanExtra(MessageDataset.KEY_WATCH_COMPASS, true);

		if(useWatchCompass) {
			cacheLocation = startIntent.getExtras().getParcelable(MessageDataset.KEY_CACHE_LOCATION);
		}
	}

	/**
	 * Handles location updates, updates UI accordingly.
	 * Also kills the app if requested by the phone.
	 */
	private BroadcastReceiver localBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			//Distance update received
			if(ListenerService.PATH_UPDATE_DISTANCE.equals(intent.getAction())) {
				distance = intent.getFloatExtra(MessageDataset.KEY_DISTANCE, 0f);
				triggerMinimalUiRefresh();

				//Direction update received (ignore if using watch compass)
			} else if(!useWatchCompass && ListenerService.PATH_UPDATE_DIRECTION.equals(intent.getAction())) {
				direction = intent.getFloatExtra(MessageDataset.KEY_DIRECTION, 0f);
				triggerMinimalUiRefresh();

				//User location update received (ignore unless using watch compass)
			} else if(useWatchCompass && ListenerService.PATH_UPDATE_LOCATION.equals(intent.getAction())) {
				locationUtils.setCurrentLocation((Location) intent.getParcelableExtra(MessageDataset.KEY_LOCATION));

				//Kill app
			} else if(ListenerService.PATH_KILL_APP.equals(intent.getAction())) {
				Log.d(DEBUG_TAG, "Phone service stopped; killing watch service.");
				stopSelf();
			}
		}
	};

	/**
	 * Asks CompassActivity to trigger a minimal UI refresh (currently includes distance & direction).
	 */
	private void triggerMinimalUiRefresh() {
		Intent minimalRefresh = new Intent(CompassActivity.ACTION_TRIGGER_MINIMAL_REFRESH);
		localBroadcastManager.sendBroadcast(minimalRefresh);
	}

	/**
	 * A regular (non-local) BroadcastReceiver which listens for 'Kill App' Intent broadcast by PendingIntent from
	 * the persistent notification's 'Stop Navigating' button.
	 */
	private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			stopSelf();
		}
	};

	/**
	 * Registers the service's BroadcastReceivers to listen for updates from the phone app (location, direction, etc.)
	 */
	private void listenForUpdates() {
		localBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
		IntentFilter updateFilter = new IntentFilter();
			updateFilter.addAction(ListenerService.PATH_UPDATE_DISTANCE);
			updateFilter.addAction(ListenerService.PATH_UPDATE_DIRECTION);
			updateFilter.addAction(ListenerService.PATH_UPDATE_LOCATION);
			updateFilter.addAction(ListenerService.PATH_KILL_APP);
		localBroadcastManager.registerReceiver(localBroadcastReceiver, updateFilter);

		IntentFilter bcrFilter = new IntentFilter();
			bcrFilter.addAction(ListenerService.PATH_KILL_APP);
		registerReceiver(broadcastReceiver, bcrFilter);
	}

	/**
	 * Unregister localBroadcastReceiver and stop listening for updates from the phone app.
	 */
	private void stopListeningForUpdates() {
		if(localBroadcastManager != null) {
			localBroadcastManager.unregisterReceiver(localBroadcastReceiver);
		}
		unregisterReceiver(broadcastReceiver);
	}

	/**
	 * Disconnects from Google APIs.
	 *
	 * Also attempts to stop the phone app.
	 */
	private void disconnectGoogleApiClient() {
		if(apiClient != null && apiClient.isConnected() && connectedNodeId != null) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					Wearable.MessageApi.sendMessage(
							apiClient, connectedNodeId, ListenerService.PATH_KILL_APP, new byte[0]).await();
					apiClient.disconnect();
				}
			}).start();
		}
	}

	@Override
	public void onConnected(Bundle bundle) {
		//As soon as the Google APIs are connected, grab the connected Node ID
		new Thread(new Runnable() {
			@Override
			public void run() {
				HashSet<String> connectedWearDevices = new HashSet<>();
				NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(apiClient).await();
				for(Node node : nodes.getNodes()) {
					connectedWearDevices.add(node.getId());
				}

				try {
					connectedNodeId = connectedWearDevices.iterator().next();
				} catch(NoSuchElementException e) {
					Log.wtf(DEBUG_TAG, "No paired devices found.");
					connectedNodeId = null;
				}
			}
		}).start();
	}

	@Override
	public void onConnectionSuspended(int i) {
	}

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
	}

	private void startCompassActivity() {
		Intent startIntent = new Intent(this, CompassActivity.class);
		startIntent.setAction(ListenerService.PATH_INIT);
		startIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(startIntent);
	}

	@Override
	public void onDestroy() {
		disconnectGoogleApiClient();
		stopListeningForUpdates();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return iBinder;
	}

	/**
	 * Utility class used to let clients bind to this service.
	 */
	public class LocalBinder extends Binder {
		cgeoWearService getService() {
			return cgeoWearService.this;
		}
	}

	public String getCacheName() {
		return cacheName;
	}

	public String getGeocode() {
		return geocode;
	}

	public float getDistance() {
		return distance;
	}

	public float getDirection() {
		return direction;
	}
}
