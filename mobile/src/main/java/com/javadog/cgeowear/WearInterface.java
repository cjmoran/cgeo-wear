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

import android.location.Location;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Wearable;
import com.javadog.WearMessageDataset.MessageDataset;

import java.net.ConnectException;

/**
 * Contains methods used to interface with Android Wear devices.
 */
public class WearInterface implements ResultCallback<MessageApi.SendMessageResult> {
	public static final String PATH_INIT = "/cgeoWear/init";
	public static final String PATH_UPDATE_DISTANCE = "/cgeoWear/update/distance";
	public static final String PATH_UPDATE_DIRECTION = "/cgeoWear/update/direction";
	public static final String PATH_KILL_APP = "/cgeoWear/killApp";

	private GoogleApiClient apiClient;
	private String nodeId;

	/**
	 * @param client A reference to the Google API client.
	 * @param nId    The ID of the paired Wear device to use.
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
	 * @param distance  The distance to the geocache.
	 * @param direction The direction to the geocache.
	 */
	/**
	 * Send initial command to start navigating to a cache using passed values:
	 *
	 * @param cacheName The Geocache's title.
	 * @param geocode   The Geocache's Geocode.
	 * @param distance  The distance to the geocache.
	 * @param direction The direction to the geocache.
	 */
	public void initTracking(String cacheName, String geocode, float distance, float direction, boolean watchCompass,
							 Location cacheLocation) throws ConnectException {
		MessageDataset dataSet = new MessageDataset.Builder()
				.cacheName(cacheName)
				.geocode(geocode)
				.distance(distance)
				.direction(direction)
				.cacheLocation(cacheLocation)
				.useWatchCompass(watchCompass)
				.build();

		MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
				apiClient, nodeId, PATH_INIT, dataSet.putToDataMap().toByteArray()).await();
		if(result.getStatus().isSuccess()) {
			Log.d(MobileService.DEBUG_TAG, "Successfully sent message to Wear device: " + nodeId);
		} else {
			throw new ConnectException("Could not send message to Wear device: " + nodeId);
		}
	}

	public void sendDistanceUpdate(float distance) {
		MessageDataset dataSet = new MessageDataset.Builder()
				.distance(distance)
				.build();

		Wearable.MessageApi.sendMessage(
				apiClient, nodeId, PATH_UPDATE_DISTANCE, dataSet.putToDataMap().toByteArray()).setResultCallback(this);
	}

	public void sendDirectionUpdate(float direction) {
		MessageDataset dataSet = new MessageDataset.Builder()
				.direction(direction)
				.build();

		Wearable.MessageApi.sendMessage(
				apiClient, nodeId, PATH_UPDATE_DIRECTION, dataSet.putToDataMap().toByteArray()).setResultCallback(this);
	}

	public void sendKillRequest() {
		Wearable.MessageApi.sendMessage(
				apiClient, nodeId, PATH_KILL_APP, new byte[0]
		);
	}

	@Override
	public void onResult(MessageApi.SendMessageResult sendMessageResult) {
		if(!sendMessageResult.getStatus().isSuccess()) {
			Log.e(MobileService.DEBUG_TAG, "Failed to send location update to Wear device.");
		}
	}
}
