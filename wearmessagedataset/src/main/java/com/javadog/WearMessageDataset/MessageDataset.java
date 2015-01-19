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

import android.location.Location;

import com.google.android.gms.wearable.DataMap;

/**
 * A Bundle-like solution to passing a set of objects between the phone and Wear device.
 */
public class MessageDataset {
	public static final String KEY_CACHE_NAME = "cacheName";
	public static final String KEY_GEOCODE = "geocode";
	public static final String KEY_WATCH_COMPASS = "useWatchCompass";

	public static final String KEY_LATITUDE = "latitude";
	public static final String KEY_LONGITUDE = "longitude";
	public static final String KEY_ALTITUDE = "altitude";

	public static final String KEY_DISTANCE = "distance";
	public static final String KEY_DIRECTION = "direction";
	public static final String KEY_CACHE_LATITUDE = "cacheLatitude";
	public static final String KEY_CACHE_LONGITUDE = "cacheLongitude";

	private final String cacheName, geocode;
	private final float distance, direction;
	private Location location;
	private Location cacheLocation;
	private boolean useWatchCompass;

	/**
	 * Do not call constructors directly, use MessageDataSet.Builder to obtain a new instance.
	 */
	private MessageDataset(String name, String code, float dist, float dir, boolean watchCompass) {
		cacheName = name;
		geocode = code;
		distance = dist;
		direction = dir;
		useWatchCompass = watchCompass;
	}

	private MessageDataset(String name, String code, float dist, float dir, boolean watchCompass, Location loc,
						   Location cacheLoc) {
		cacheName = name;
		geocode = code;
		distance = dist;
		direction = dir;
		useWatchCompass = watchCompass;
		location = loc;
		cacheLocation = cacheLoc;
	}

	public DataMap putToDataMap() {
		DataMap map = new DataMap();

		map.putString(KEY_CACHE_NAME, cacheName);
		map.putString(KEY_GEOCODE, geocode);
		map.putFloat(KEY_DISTANCE, distance);
		map.putFloat(KEY_DIRECTION, direction);
		map.putBoolean(KEY_WATCH_COMPASS, useWatchCompass);
		map.putDouble(KEY_LATITUDE, (location != null) ? location.getLatitude() : 0d);
		map.putDouble(KEY_LONGITUDE, (location != null) ? location.getLongitude() : 0d);
		map.putDouble(KEY_ALTITUDE, (location != null) ? location.getAltitude() : 0d);
		map.putDouble(KEY_CACHE_LATITUDE, (cacheLocation != null) ? cacheLocation.getLatitude() : 0d);
		map.putDouble(KEY_CACHE_LONGITUDE, (cacheLocation != null) ? cacheLocation.getLongitude() : 0d);

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
				map.getFloat(KEY_DIRECTION),
				map.getBoolean(KEY_WATCH_COMPASS)
		);

		Location loc = new Location("phoneApp");
		loc.setLatitude(map.getDouble(KEY_LATITUDE, 0d));
		loc.setLongitude(map.getDouble(KEY_LONGITUDE, 0d));
		loc.setAltitude(map.getDouble(KEY_ALTITUDE, 0d));
		setLocation(loc);

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

	public boolean getWatchCompassPref() {
		return useWatchCompass;
	}

	public Location getLocation() {
		return location;
	}

	public Location getCacheLocation() {
		return cacheLocation;
	}

	private void setLocation(Location loc) {
		location = loc;
	}

	private void setCacheLocation(Location loc) {
		cacheLocation = loc;
	}

	public static class Builder {
		private String nestedCacheName, nestedGeocode;
		private float nestedDistance, nestedDirection;
		private boolean nestedUseWatchCompass;
		private Location nestedLocation, nestedCacheLocation;

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

		public Builder useWatchCompass(boolean w) {
			nestedUseWatchCompass = w;
			return this;
		}

		public Builder location(Location l) {
			nestedLocation = l;
			return this;
		}

		public Builder cacheLocation(Location l) {
			nestedCacheLocation = l;
			return this;
		}

		public MessageDataset build() {
			return new MessageDataset(
					nestedCacheName, nestedGeocode, nestedDistance, nestedDirection, nestedUseWatchCompass,
					nestedLocation, nestedCacheLocation);
		}
	}
}
