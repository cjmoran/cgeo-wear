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

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.content.WakefulBroadcastReceiver;

import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;
import com.javadog.WearMessageDataset.MessageDataset;

/**
 * Watch-side service which listens for messages from the phone app.
 */
public class ListenerService extends WearableListenerService {
	public static final String PATH_INIT = "/cgeoWear/init";
	public static final String PATH_UPDATE_DISTANCE = "/cgeoWear/update/distance";
	public static final String PATH_UPDATE_DIRECTION = "/cgeoWear/update/direction";
	public static final String PATH_UPDATE_LOCATION = "/cgeoWear/update/location";
	public static final String PATH_KILL_APP = "/cgeoWear/killApp";

	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public void onMessageReceived(MessageEvent messageEvent) {

		//Init: Service should start listening for updates from phone, and the main activity should be launched
		if(PATH_INIT.equals(messageEvent.getPath())) {
			//Get dataset from the message
			MessageDataset dataSet = new MessageDataset(DataMap.fromByteArray(messageEvent.getData()));

			//Start the service
			Intent i = new Intent(this, cgeoWearService.class);
			i.setAction(PATH_INIT);
			i.putExtra(MessageDataset.KEY_CACHE_NAME, dataSet.getCacheName());
			i.putExtra(MessageDataset.KEY_GEOCODE, dataSet.getGeocode());
			i.putExtra(MessageDataset.KEY_DISTANCE, dataSet.getDistance());
			i.putExtra(MessageDataset.KEY_DIRECTION, dataSet.getDirection());
			WakefulBroadcastReceiver.startWakefulService(getApplicationContext(), i);

			//Distance update
		} else if(PATH_UPDATE_DISTANCE.equals(messageEvent.getPath())) {
			MessageDataset dataSet = new MessageDataset(DataMap.fromByteArray(messageEvent.getData()));

			Intent updateIntent = new Intent(PATH_UPDATE_DISTANCE);
			updateIntent.putExtra(MessageDataset.KEY_DISTANCE, dataSet.getDistance());

			LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(updateIntent);

			//Direction update
		} else if(PATH_UPDATE_DIRECTION.equals(messageEvent.getPath())) {
			MessageDataset dataSet = new MessageDataset(DataMap.fromByteArray(messageEvent.getData()));

			Intent updateIntent = new Intent(PATH_UPDATE_DIRECTION);
			updateIntent.putExtra(MessageDataset.KEY_DIRECTION, dataSet.getDirection());

			LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(updateIntent);

			//Kill app (when phone-side app is stopped)
		} else if(PATH_KILL_APP.equals(messageEvent.getPath())) {
			Intent killIntent = new Intent(PATH_KILL_APP);
			LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(killIntent);
		}
	}
}
