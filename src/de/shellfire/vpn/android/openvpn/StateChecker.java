package de.shellfire.vpn.android.openvpn;

import java.net.InetAddress;
import java.net.UnknownHostException;

import android.os.AsyncTask;
import android.util.Log;
import de.shellfire.vpn.android.openvpn.VpnStatus.ConnectionStatus;
import de.shellfire.vpn.android.webservice.ShellfireWebService;

public class StateChecker implements Runnable {

	private ShellfireWebService webService;
	private OpenVpnService openVpnService;
	private Thread thread;
	private boolean doContinue;
	private static StateChecker instance;
	
	public static StateChecker getInstance(OpenVpnService openVpnService) {
		if (instance != null) {
			instance.stop();
		}
			
		instance = new StateChecker(openVpnService);
		return instance;
	}
	
	private void stop() {
		doContinue = false;
		
	}

	private StateChecker(OpenVpnService openVpnService) {
		this.openVpnService = openVpnService;
	}

	public void run() {
		webService = ShellfireWebService.getInstance();
		int numExceptions = 0;
		doContinue = true;
		while (doContinue) {
			ConnectionStatus state = VpnStatus.getLastState();
			if (state == ConnectionStatus.LEVEL_CONNECTED) {
				Log.w("StateChecker", "VpnStatus says we are connected");
				try {
					String localIp = webService.getLocalIpAddress();
					boolean isConnectedToVpn = false;
					if (localIp == null) {
						Log.w("StateChecker", "could not determine local ip - assuming offline");
					}
					else {
						isConnectedToVpn = webService.isVpnServerIp(localIp);	
					}
					

					if (!isConnectedToVpn) {
						Log.w("StateChecker", "but local IP not in list of vpn server ips :(");
						reconnect();
					} else {
						Log.w("StateChecker", "and local IP is in list of vpn server ips - looks good! :-)");
					}

					numExceptions = 0;
				} catch (Exception e) {
					numExceptions++;
					Log.w("StateChecker", "numExceptions: " + numExceptions);
					if (numExceptions > 3) {
						numExceptions = 0;
						reconnect();
					}
				}

			}
			try {
				pause();
			} catch (InterruptedException e) {
				Log.w("StateChecker", "Was interrupted - finishing gracefully");
				doContinue = false;
			}

		}
	}

	private void reconnect() {
		Log.w("StateChecker", "reconnect()");
		if (openVpnService != null) {
			OpenVPNManagement management = openVpnService.getManagement();

			if (management != null) {
				management.reconnect();
				management.releaseHold();
			}
		}
	}

	private void pause() throws InterruptedException {
		Thread.sleep(1000);
	}

	public Thread getThread() {
		if (this.thread == null) {
			this.thread = new Thread(this, "StateCheckerThread");
		}

		return thread;
	}
	
	
}
