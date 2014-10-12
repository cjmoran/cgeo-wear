package com.javadog.cgeowear;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

public class SettingsActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getFragmentManager().beginTransaction().replace(android.R.id.content, new PrefsFragment()).commit();
	}

	public static class PrefsFragment extends PreferenceFragment {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.layout_preferences);

			//=====================Debugging code===================
			Preference debugButton = findPreference("button_debug");
			debugButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					Intent intent = new Intent("cgeo.geocaching.wear.NAVIGATE_TO");
					intent.putExtra("cgeo.geocaching.wear.extra.CACHE_NAME", "Sample Geocache");
					intent.putExtra("cgeo.geocaching.wear.extra.GEOCODE", "GC10101");
					intent.putExtra("cgeo.geocaching.wear.extra.LATITUDE", 35.28884);
					intent.putExtra("cgeo.geocaching.wear.extra.LONGITUDE", -80.73667);
					getActivity().startService(intent);

					return true;
				}
			});
			//====================End debugging code================
		}
	}
}
