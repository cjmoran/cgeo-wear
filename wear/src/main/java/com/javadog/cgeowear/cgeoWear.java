package com.javadog.cgeowear;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.DecimalFormat;

public class cgeoWear extends Activity {
	private static final String DEBUG_TAG = "com.javadog.cgeowear";

	private TextView tv_cacheName;
	private TextView tv_geocode;
	private TextView tv_distance;
	private ImageView iv_compass;

	private float distance;
	private float direction;

	private LocalBroadcastManager broadcastManager;

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_cgeo_wear);

		tv_cacheName = (TextView) findViewById(R.id.textview_cache_name);
		tv_geocode = (TextView) findViewById(R.id.textview_geocode);
		tv_distance = (TextView) findViewById(R.id.textview_distance);
		iv_compass = (ImageView) findViewById(R.id.compass);

		Intent i = getIntent();
		initScreen(i);

		//Register BroadcastReceiver for location updates
		broadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
		IntentFilter updateFilter = new IntentFilter(ListenerService.PATH_UPDATE);
		broadcastManager.registerReceiver(broadcastReceiver, updateFilter);
    }

	/**
	 * Initializes the screen, whether the Activity was freshly-launched or onNewIntent was run.
	 *
	 * @param i The launch intent.
	 */
	private void initScreen(Intent i) {
		String cacheName = i.getStringExtra(MessageDataSet.KEY_CACHE_NAME);
		String geocode = i.getStringExtra(MessageDataSet.KEY_GEOCODE);
		distance = i.getFloatExtra(MessageDataSet.KEY_DISTANCE, 0f);
		direction = i.getFloatExtra(MessageDataSet.KEY_DIRECTION, 0f);

		tv_cacheName.setText(cacheName);
		tv_geocode.setText(geocode);
		setDistanceFormatted(distance);
		rotateCompass(direction);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		initScreen(intent);
	}

	/**
	 * Handles location updates, updates UI accordingly.
	 */
	private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if(ListenerService.PATH_UPDATE.equals(intent.getAction())) {
				setDistanceFormatted(intent.getFloatExtra(MessageDataSet.KEY_DISTANCE, 0f));
				rotateCompass(intent.getFloatExtra(MessageDataSet.KEY_DIRECTION, 0f));
			}
		}
	};

	/**
	 * Sets the distance TextView's text with appropriate formatting.
	 *
	 * @param dist The distance to the geocache, in meters.
	 */
	private void setDistanceFormatted(float dist) {
		DecimalFormat format = new DecimalFormat("0.00m");
		tv_distance.setText(format.format(dist));

		distance = dist;
	}

	/**
	 * Handles rotation of the compass to a new direction.
	 * @param newDirection Direction to turn to, in degrees.
	 */
	private void rotateCompass(float newDirection) {
		Log.d(DEBUG_TAG, "Rotating compass from " + direction + " to " + newDirection);

		RotateAnimation anim = new RotateAnimation(
				direction,
				newDirection,
				Animation.RELATIVE_TO_SELF, 0.5f,
				Animation.RELATIVE_TO_SELF, 0.5f);
		anim.setDuration(200l);
		anim.setFillAfter(true);
		iv_compass.startAnimation(anim);

		direction = newDirection;
	}

	@Override
	protected void onStop() {
		//Unregister our BroadcastReceiver
		broadcastManager.unregisterReceiver(broadcastReceiver);

		super.onStop();
	}
}
