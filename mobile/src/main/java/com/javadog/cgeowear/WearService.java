package com.javadog.cgeowear;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

public class WearService extends Service {
	private static final String INTENT_INIT = "cgeo.geocaching.wear.NAVIGATE_TO";
	private static final String INTENT_STOP = "com.javadog.cgeowear.STOP_APP";

	private static final String EXTRA_CACHE_NAME = "cgeo.geocaching.wear.extra.CACHE_NAME";
	private static final String EXTRA_GEOCODE = "cgeo.geocaching.wear.extra.GEOCODE";
	private static final String EXTRA_LATITUDE = "cgeo.geocaching.wear.extra.LATITUDE";
	private static final String EXTRA_LONGITUDE = "cgeo.geocaching.wear.extra.LONGITUDE";

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
				final String cacheName = intent.getStringExtra(EXTRA_CACHE_NAME);
				final String geocode = intent.getStringExtra(EXTRA_GEOCODE);
				final double latitude = intent.getDoubleExtra(EXTRA_LATITUDE, 0d);
				final double longitude = intent.getDoubleExtra(EXTRA_LONGITUDE, 0d);
			}
		}

		Toast.makeText(getApplicationContext(), getText(R.string.toast_service_started), Toast.LENGTH_SHORT).show();

		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
