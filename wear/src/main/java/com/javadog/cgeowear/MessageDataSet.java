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

	public static final String KEY_LOCATION = "location";
	public static final String KEY_LATITUDE = "latitude";
	public static final String KEY_LONGITUDE = "longitude";
	public static final String KEY_ALTITUDE = "altitude";

	public static final String KEY_CACHE_LOCATION = "cacheLocation";
	public static final String KEY_CACHE_LATITUDE = "cacheLatitude";
	public static final String KEY_CACHE_LONGITUDE = "cacheLongitude";

	private final String cacheName, geocode;
	private final float distance, direction;
	private Location location, cacheLocation;
	private boolean useWatchCompass;

	/**
	 * Do not call directly, use MessageDataSet.Builder to obtain a new instance.
	 */
	private MessageDataSet(String name, String code, float dist, float dir, boolean watchCompass) {
		cacheName = name;
		geocode = code;
		distance = dist;
		direction = dir;
		useWatchCompass = watchCompass;
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
}
