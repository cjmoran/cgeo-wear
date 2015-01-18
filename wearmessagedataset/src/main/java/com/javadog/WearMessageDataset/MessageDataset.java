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
 
package com.javadog.WearMessageDataset;

import com.google.android.gms.wearable.DataMap;

/**
 * A Bundle-like solution to passing a set of objects between the phone and Wear device.
 */
public class MessageDataset {
	public static final String KEY_CACHE_NAME = "cacheName";
	public static final String KEY_GEOCODE = "geocode";
	public static final String KEY_DISTANCE = "distance";
	public static final String KEY_DIRECTION = "direction";

	private final String cacheName, geocode;
	private final float distance, direction;

	/**
	 * Do not call directly, use MessageDataSet.Builder to obtain a new instance.
	 */
	private MessageDataset(String name, String code, float dist, float dir) {
		cacheName = name;
		geocode = code;
		distance = dist;
		direction = dir;
	}

	public DataMap putToDataMap() {
		DataMap map = new DataMap();

		map.putString(KEY_CACHE_NAME, cacheName);
		map.putString(KEY_GEOCODE, geocode);
		map.putFloat(KEY_DISTANCE, distance);
		map.putFloat(KEY_DIRECTION, direction);

		return map;
	}

	/**
	 * Constructs the new object using the supplied DataMap.
	 *
	 * @param map The DataMap containing values to instantiate.
	 */
	public MessageDataset(DataMap map) {
		this(
				map.getString(KEY_CACHE_NAME),
				map.getString(KEY_GEOCODE),
				map.getFloat(KEY_DISTANCE),
				map.getFloat(KEY_DIRECTION)
		);
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

	public static class Builder {
		private String nestedCacheName, nestedGeocode;
		private float nestedDistance, nestedDirection;

		public Builder cacheName(String name) {
			nestedCacheName = name;
			return this;
		}

		public Builder geocode(String code) {
			nestedGeocode = code;
			return this;
		}

		public Builder distance(float dist) {
			nestedDistance = dist;
			return this;
		}

		public Builder direction(float dir) {
			nestedDirection = dir;
			return this;
		}

		public MessageDataset build() {
			return new MessageDataset(nestedCacheName, nestedGeocode, nestedDistance, nestedDirection);
		}
	}
}
