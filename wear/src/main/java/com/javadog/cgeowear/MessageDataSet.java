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

	public static final String KEY_CACHE_LOCATION = "cacheLocation";
	public static final String KEY_CACHE_LATITUDE = "cacheLatitude";
	public static final String KEY_CACHE_LONGITUDE = "cacheLongitude";

	private final String cacheName, geocode;
	private final float distance, direction;
	private Location cacheLocation;

	/**
	 * Do not call directly, use MessageDataSet.Builder to obtain a new instance.
	 */
	private MessageDataSet(String name, String code, float dist, float dir) {
		cacheName = name;
		geocode = code;
		distance = dist;
		direction = dir;
	}

	/**
	 * Constructs the new object using the supplied DataMap.
	 *
	 * @param map The DataMap containing values to instantiate.
	 */
	public MessageDataSet(DataMap map) {
		this(
				map.getString(KEY_CACHE_NAME),
				map.getString(KEY_GEOCODE),
				map.getFloat(KEY_DISTANCE),
				map.getFloat(KEY_DIRECTION)
		);

		Location cacheLoc = new Location("phoneApp");
		cacheLoc.setLatitude(map.getDouble(KEY_CACHE_LATITUDE, 0d));
		cacheLoc.setLongitude(map.getDouble(KEY_CACHE_LONGITUDE, 0d));
		setCacheLocation(cacheLoc);
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

	public Location getCacheLocation() {
		return cacheLocation;
	}

	private void setCacheLocation(Location loc) {
		cacheLocation = loc;
	}
}
