package de.shellfire.vpn.android;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceFragment;

public class PrefsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.preferences);

		getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals(VpnPreferences.REMEMBER_VPN_SELECTION)) {
			if (!VpnPreferences.getRememberVpnSelection(getActivity())) {
				VpnPreferences.setRememberedVpnSelection(getActivity(), null);
			}
		}
		if (key.equals(VpnPreferences.SHOW_LOG)) {
			Activity activity = getActivity();
			if (activity != null) {
				if (!VpnPreferences.getShowLog(activity)) {
					VpnPreferences.setShowLog(activity, null);
				}
			}
		}

	}
}
