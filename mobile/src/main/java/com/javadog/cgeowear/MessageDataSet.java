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
	public static final String KEY_WATCH_COMPASS = "useWatchCompass";

	public static final String KEY_LATITUDE = "latitude";
	public static final String KEY_LONGITUDE = "longitude";
	public static final String KEY_ALTITUDE = "altitude";
	public static final String KEY_CACHE_LATITUDE = "cacheLatitude";
	public static final String KEY_CACHE_LONGITUDE = "cacheLongitude";

	private final String cacheName, geocode;
	private final float distance, direction;
	private Location location;
	private Location cacheLocation;
	private boolean useWatchCompass;

	/**
	 * Do not call directly, use MessageDataSet.Builder to obtain a new instance.
	 */
	private MessageDataSet(String name, String code, float dist, float dir, boolean watchCompass, Location loc,
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

		public MessageDataSet build() {
			return new MessageDataSet(
					nestedCacheName, nestedGeocode, nestedDistance, nestedDirection, nestedUseWatchCompass,
					nestedLocation, nestedCacheLocation);
		}
	}
}
