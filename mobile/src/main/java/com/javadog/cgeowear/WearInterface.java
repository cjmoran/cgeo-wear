package com.javadog.cgeowear;

import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.net.ConnectException;
import java.util.HashSet;
import java.util.NoSuchElementException;

/**
 * Contains methods used to interface with Android Wear devices.
 */
public class WearInterface {
	public static final String PATH_INIT = "/cgeoWear/init";
	public static final String PATH_UPDATE = "/cgeoWear/update";

	private GoogleApiClient apiClient;

	public WearInterface(GoogleApiClient client) {
		apiClient = client;

		//Start finding Wear devices
		try {
			getWearDevives();
		} catch(ConnectException e) {
			Log.e(WearService.DEBUG_TAG, "Couldn't search for Wear devices.");
		}
	}

	/**
	 * Send initial command to start navigating to a cache using passed values:
	 *
	 * @param nodeId The ID of the paired Wear device to use.
	 * @param cacheName The Geocache's title.
	 * @param geocode   The Geocache's Geocode.
	 * @param distance The distance to the geocache.
	 * @param direction The direction to the geocache.
	 */
	public void initTracking(String nodeId, String cacheName, String geocode, float distance, float direction)
			throws ConnectException {
		MessageDataSet dataSet = new MessageDataSet.Builder(distance, direction)
				.cacheName(cacheName)
				.geocode(geocode)
				.build();

		MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
				apiClient, nodeId, PATH_INIT, dataSet.putToDataMap().toByteArray()).await();
		if(result.getStatus().isSuccess()) {
			Log.d(WearService.DEBUG_TAG, "Successfully sent message to Wear device: " + nodeId);
		} else {
			throw new ConnectException("Could not send message to Wear device: " + nodeId);
		}
	}

	private void getWearDevives() throws ConnectException {

	}
}
