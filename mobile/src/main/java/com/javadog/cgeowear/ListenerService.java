package com.javadog.cgeowear;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Phone-side service which listens for messages from the watch app.
 */
public class ListenerService extends WearableListenerService {
	public static final String PATH_KILL_APP = "/cgeoWear/killApp";

	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public void onMessageReceived(MessageEvent messageEvent) {
		//Kill app (when watch-side app is stopped)
		if(PATH_KILL_APP.equals(messageEvent.getPath())) {
			Intent killIntent = new Intent(PATH_KILL_APP);
			LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(killIntent);
		}
	}
}
