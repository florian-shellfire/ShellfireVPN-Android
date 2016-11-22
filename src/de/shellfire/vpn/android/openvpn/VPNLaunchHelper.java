package de.shellfire.vpn.android.openvpn;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import de.shellfire.vpn.android.R;

public class VPNLaunchHelper {
	static private boolean writeMiniVPN(Context context) {
		File mvpnout = new File(context.getCacheDir(),VpnProfile.MINIVPN);
		if (mvpnout.exists() && mvpnout.canExecute()) {
			try {
				System.out.println("myvpnout is alright: " + mvpnout.getCanonicalPath());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return true;

		}
			
		IOException e2 = null;

		try {
			InputStream mvpn;
			
			try {
				mvpn = context.getAssets().open("minivpn." + Build.CPU_ABI);
			}
			catch (IOException errabi) {
				VpnStatus.logInfo("Failed getting assets for archicture " + Build.CPU_ABI);
				e2=errabi;
				mvpn = context.getAssets().open("minivpn." + Build.CPU_ABI2);
			}

			FileOutputStream fout = new FileOutputStream(mvpnout);

			byte buf[]= new byte[4096];

			int lenread = mvpn.read(buf);
			while(lenread> 0) {
				fout.write(buf, 0, lenread);
				lenread = mvpn.read(buf);
			}
			fout.close();

			if(!mvpnout.setExecutable(true)) {
				VpnStatus.logError("Failed to set minivpn executable");
				return false;
			}
				
			
			return true;
		} catch (IOException e) {
			if(e2!=null)
				VpnStatus.logError( e2.getLocalizedMessage());
			VpnStatus.logError(e.getLocalizedMessage());
			e.printStackTrace();
			return false;
		}
	}
	

	public static void startOpenVpn(VpnProfile startprofile, Context context) {
		if(!writeMiniVPN(context)) {
			VpnStatus.logError("Error writing minivpn binary");
			return;
		}

		VpnStatus.logInfo(R.string.building_configration);

		if (startprofile != null) {
			Intent startVPN = startprofile.prepareIntent(context);
			if(startVPN!=null)
				context.startService(startVPN);
		}

	}
}
