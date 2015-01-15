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

import com.google.android.gms.wearable.DataMap;

/**
 * A Bundle-like solution to passing a set of objects between the phone and Wear device.
 */
public class MessageDataSet {
	public static final String KEY_CACHE_NAME = "cacheName";
	public static final String KEY_GEOCODE = "geocode";
	public static final String KEY_DISTANCE = "distance";
	public static final String KEY_DIRECTION = "direction";

	public static final String KEY_CACHE_LATITUDE = "cacheLatitude";
	public static final String KEY_CACHE_LONGITUDE = "cacheLongitude";

	private final String cacheName, geocode;
	private final float distance, direction;
	private Location cacheLocation;

	/**
	 * Do not call directly, use MessageDataSet.Builder to obtain a new instance.
	 */
	private MessageDataSet(String name, String code, float dist, float dir, Location cacheLoc) {
		cacheName = name;
		geocode = code;
		distance = dist;
		direction = dir;
		cacheLocation = cacheLoc;
	}

	public DataMap putToDataMap() {
		DataMap map = new DataMap();

		map.putString(KEY_CACHE_NAME, cacheName);
		map.putString(KEY_GEOCODE, geocode);
		map.putFloat(KEY_DISTANCE, distance);
		map.putFloat(KEY_DIRECTION, direction);
		map.putDouble(KEY_CACHE_LATITUDE, (cacheLocation != null) ? cacheLocation.getLatitude() : 0d);
		map.putDouble(KEY_CACHE_LONGITUDE, (cacheLocation != null) ? cacheLocation.getLongitude() : 0d);

		return map;
	}

	public static class Builder {
		private String nestedCacheName, nestedGeocode;
		private float nestedDistance, nestedDirection;
		private Location nestedCacheLocation;

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

		public Builder cacheLocation(Location l) {
			nestedCacheLocation = l;
			return this;
		}

		public MessageDataSet build() {
			return new MessageDataSet(
					nestedCacheName, nestedGeocode, nestedDistance, nestedDirection, nestedCacheLocation);
		}
	}
}
