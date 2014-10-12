package com.javadog.cgeowear;

import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Wearable;

import java.net.ConnectException;

/**
 * Contains methods used to interface with Android Wear devices.
 */
public class WearInterface implements ResultCallback<MessageApi.SendMessageResult> {
	public static final String PATH_INIT = "/cgeoWear/init";
	public static final String PATH_UPDATE = "/cgeoWear/update";
	public static final String PATH_KILL_APP = "/cgeoWear/killApp";

	private GoogleApiClient apiClient;
	private String nodeId;

	/**
	 * @param client A reference to the Google API client.
	 * @param nId The ID of the paired Wear device to use.
	 */
	public WearInterface(GoogleApiClient client, String nId) {
		apiClient = client;
		nodeId = nId;
	}

	/**
	 * Send initial command to start navigating to a cache using passed values:
	 *
	 * @param cacheName The Geocache's title.
	 * @param geocode   The Geocache's Geocode.
	 * @param distance The distance to the geocache.
	 * @param direction The direction to the geocache.
	 */
	public void initTracking(String cacheName, String geocode, float distance, float direction)
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

	public void sendLocationUpdate(float distance, float direction) {
		MessageDataSet dataSet = new MessageDataSet.Builder(distance, direction).build();

		Wearable.MessageApi.sendMessage(
				apiClient, nodeId, PATH_UPDATE, dataSet.putToDataMap().toByteArray()).setResultCallback(this);
	}

	public void sendKillRequest() {
		Wearable.MessageApi.sendMessage(
				apiClient, nodeId, PATH_KILL_APP, new byte[0]
		);
	}

	@Override
	public void onResult(MessageApi.SendMessageResult sendMessageResult) {
		if(!sendMessageResult.getStatus().isSuccess()) {
			Log.e(WearService.DEBUG_TAG, "Failed to send location update to Wear device.");
		}
	}
}
