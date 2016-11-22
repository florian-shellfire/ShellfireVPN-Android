package de.shellfire.vpn.android;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

public class VpnPreferences {

	protected final static String REMEMBER_VPN_SELECTION = "REMEMBER_VPN_SELECTION";
	protected final static String REMEMBERED_VPN_SELECTION = "REMEMBERED_VPN_SELECTION";
	protected final static String SHOW_LOG = "SHOW_LOG";
	protected final static String IGNORE_NEW_VERSION_WARNING = "IGNORE_NEW_VERSION_WARNING";
	

	static Boolean getRememberVpnSelection(Context context) {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

		return sp.getBoolean(REMEMBER_VPN_SELECTION, false);
	}

	static boolean containsRememberVpnSelection(Context context) {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
		return sp.contains(REMEMBER_VPN_SELECTION);
	}

	static void setRememberVpnSelection(Context context, Boolean value) {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
		Editor edit = sp.edit();

		if (value == null) {
			edit.remove(REMEMBERED_VPN_SELECTION);
		} else {
			edit.putBoolean(REMEMBER_VPN_SELECTION, value);
		}

		edit.apply();
	}

	static int getRememberedVpnSelection(Context context) {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

		return sp.getInt(REMEMBERED_VPN_SELECTION, 0);
	}

	static void setRememberedVpnSelection(Context context, Integer value) {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
		Editor edit = sp.edit();

		if (value == null) {
			edit.remove(REMEMBERED_VPN_SELECTION);
		} else {
			edit.putInt(REMEMBERED_VPN_SELECTION, value);
		}

		edit.apply();
	}

	public static boolean getShowLog(Activity activity) {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(activity);

		return sp.getBoolean(SHOW_LOG, false);
	}

	public static void setShowLog(Activity activity, Boolean bol) {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(activity);
		Editor edit = sp.edit();

		if (bol == null)
			edit.remove(SHOW_LOG);
		else
			edit.putBoolean(SHOW_LOG, bol);

		edit.apply();
	}

	

	public static boolean getIgnoreNewVersionWarning(Activity activity) {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(activity);

		return sp.getBoolean(IGNORE_NEW_VERSION_WARNING, false);
	}

	public static void setIgnoreNewVersionWarning(Activity activity, Boolean bol) {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(activity);
		Editor edit = sp.edit();

		if (bol == null)
			edit.remove(IGNORE_NEW_VERSION_WARNING);
		else
			edit.putBoolean(IGNORE_NEW_VERSION_WARNING, bol);

		edit.apply();
	}	
	
}
