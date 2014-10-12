package com.javadog.cgeowear;

import android.content.Intent;

import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Watch-side service which listens for messages from the phone app.
 */
public class ListenerService extends WearableListenerService {
	public static final String PATH_INIT = "/cgeoWear/init";
	public static final String PATH_UPDATE = "/cgeoWear/update";

	@Override
	public void onMessageReceived(MessageEvent messageEvent) {
		if(PATH_INIT.equals(messageEvent.getPath())) {
			//Get dataset from the message
			MessageDataSet dataSet = new MessageDataSet(DataMap.fromByteArray(messageEvent.getData()));

			//Start the main Activity
			Intent i = new Intent(this, cgeoWear.class);
			i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			i.putExtra(MessageDataSet.KEY_CACHE_NAME, dataSet.getCacheName());
			i.putExtra(MessageDataSet.KEY_GEOCODE, dataSet.getGeocode());
			i.putExtra(MessageDataSet.KEY_DISTANCE, dataSet.getDistance());
			i.putExtra(MessageDataSet.KEY_DIRECTION, dataSet.getDirection());
			startActivity(i);
		}
	}
}
