package com.javadog.cgeowear;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

public class cgeoWear extends Activity {
	public static String DEBUG_TAG = "com.javadog.cgeowear";

    private TextView tv_cacheName;
	private TextView tv_geocode;
	private TextView tv_distance;
	private ImageView iv_compass;

	private String cacheName;
	private String geocode;
	private float distance;
	private float direction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_cgeo_wear);

		Intent i = getIntent();
		cacheName = i.getStringExtra(MessageDataSet.KEY_CACHE_NAME);
		geocode = i.getStringExtra(MessageDataSet.KEY_GEOCODE);
		distance = i.getFloatExtra(MessageDataSet.KEY_DISTANCE, 0f);
		direction = i.getFloatExtra(MessageDataSet.KEY_DIRECTION, 0f);

		tv_cacheName = (TextView) findViewById(R.id.textview_cache_name);
		Log.d(DEBUG_TAG, "tv_cacheName: " + tv_cacheName.toString());
		tv_geocode = (TextView) findViewById(R.id.textview_geocode);
		tv_distance = (TextView) findViewById(R.id.textview_distance);
		iv_compass = (ImageView) findViewById(R.id.compass);

		tv_cacheName.setText(cacheName);
		tv_geocode.setText(geocode);
		tv_distance.setText(String.valueOf(distance));
		//TODO: Rotate compass based on direction
    }

}
