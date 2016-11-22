package de.shellfire.vpn.android;

import android.app.Activity;
import android.os.Bundle;

public class SetPreferenceActivity extends Activity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getFragmentManager().beginTransaction().replace(android.R.id.content, new PrefsFragment()).commit();
	}

}
