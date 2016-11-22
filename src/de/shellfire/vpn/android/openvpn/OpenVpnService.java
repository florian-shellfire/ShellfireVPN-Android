package de.shellfire.vpn.android.openvpn;

import static de.shellfire.vpn.android.openvpn.VpnStatus.ConnectionStatus.LEVEL_CONNECTED;
import static de.shellfire.vpn.android.openvpn.VpnStatus.ConnectionStatus.LEVEL_WAITING_FOR_USER_INPUT;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Vector;

import android.Manifest.permission;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.VpnService;
import android.os.Binder;
import android.os.Build;
import android.os.Handler.Callback;
import android.os.IBinder;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import de.shellfire.vpn.android.MainActivity;
import de.shellfire.vpn.android.R;
import de.shellfire.vpn.android.openvpn.NetworkSpace.ipAddress;
import de.shellfire.vpn.android.openvpn.VpnStatus.ByteCountListener;
import de.shellfire.vpn.android.openvpn.VpnStatus.ConnectionStatus;
import de.shellfire.vpn.android.openvpn.VpnStatus.StateListener;

public class OpenVpnService extends VpnService implements StateListener, Callback, ByteCountListener {
    public static final String START_SERVICE = "de.shellfire.vpn.android.openvpn.START_SERVICE";
    public static final String START_SERVICE_STICKY = "de.shellfire.vpn.android.openvpn.START_SERVICE_STICKY";
    public static final String ALWAYS_SHOW_NOTIFICATION = "de.shellfire.vpn.android.openvpn.NOTIFICATION_ALWAYS_VISIBLE";
    public static final String DISCONNECT_VPN = "de.shellfire.vpn.android.openvpn.DISCONNECT_VPN";
    private static final String PAUSE_VPN = "de.shellfire.vpn.android.openvpn.PAUSE_VPN";
    private static final String RESUME_VPN = "de.shellfire.vpn.android.openvpn.RESUME_VPN";
    private static final int OPENVPN_STATUS = 1;
    private static boolean mNotificationAlwaysVisible = false;
    private final Vector<String> mDnslist = new Vector<String>();
    private final NetworkSpace mRoutes = new NetworkSpace();
    private final NetworkSpace mRoutesv6 = new NetworkSpace();
    private final IBinder mBinder = new LocalBinder();
    private Thread mProcessThread = null;
    private Thread mStateChecker = null;
    private VpnProfile mProfile;
    private String mDomain = null;
    private CIDRIP mLocalIP = null;
    private int mMtu;
    private String mLocalIPv6 = null;
    private DeviceStateReceiver mDeviceStateReceiver;
    private boolean mDisplayBytecount = false;
    private boolean mStarting = false;
    private boolean mOvpn3 = false;
    private OpenVPNManagement mManagement;
	private String mLastTunCfg;
	private Object mRemoteGW;
	private static OpenVpnService sInstance;

	public static OpenVpnService getInstance() {
		return sInstance;
	}
	
    // From: http://stackoverflow.com/questions/3758606/how-to-convert-byte-size-into-human-readable-format-in-java
    public static String humanReadableByteCount(long bytes, boolean mbit) {
        if (mbit)
            bytes = bytes * 8;
        int unit = mbit ? 1000 : 1024;
        if (bytes < unit)
            return bytes + (mbit ? " bit" : " B");

        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (mbit ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (mbit ? "" : "");
        if (mbit)
            return String.format(Locale.getDefault(), "%.1f %sbit", bytes / Math.pow(unit, exp), pre);
        else
            return String.format(Locale.getDefault(), "%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    @Override
    public IBinder onBind(Intent intent) {
    	sInstance = this;
        String action = intent.getAction();
        if (action != null && action.equals(START_SERVICE))
            return mBinder;
        else
            return super.onBind(intent);
    }
    


    @Override
    public void onRevoke() {
        mManagement.stopVPN();
        endVpnService();
    }

    // Similar to revoke but do not try to stop process
    public void processDied() {
        endVpnService();
    }

    private void endVpnService() {
        mProcessThread = null;
        VpnStatus.removeByteCountListener(this);
        unregisterDeviceStateReceiver();
        
        if (!mStarting) {
            stopForeground(!mNotificationAlwaysVisible);

            if (!mNotificationAlwaysVisible) {
                stopSelf();
                VpnStatus.removeStateListener(this);
            }
        }
    }


    private int getIconByConnectionStatus(ConnectionStatus level) {
        switch (level) {
            case LEVEL_CONNECTED:
                return R.drawable.ic_stat_vpn;
            case LEVEL_AUTH_FAILED:
            case LEVEL_NONETWORK:
            case LEVEL_NOTCONNECTED:
                return R.drawable.ic_stat_vpn_offline;
            case LEVEL_CONNECTING_NO_SERVER_REPLY_YET:
            case LEVEL_WAITING_FOR_USER_INPUT:
                return R.drawable.ic_stat_vpn_outline;
            case LEVEL_CONNECTING_SERVER_REPLIED:
                return R.drawable.ic_stat_vpn_empty_halo;
            case LEVEL_VPNPAUSED:
                return android.R.drawable.ic_media_pause;
            case UNKNOWN_LEVEL:
            default:
                return R.drawable.ic_stat_vpn;

        }
    }

    PendingIntent getLogPendingIntent() {
        // Let the configure Button show the Log
        Intent intent = new Intent(getBaseContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        PendingIntent startLW = PendingIntent.getActivity(this, 0, intent, 0);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        return startLW;

    }

    synchronized void registerDeviceStateReceiver(OpenVPNManagement magnagement) {
        // Registers BroadcastReceiver to track network connection changes.
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        mDeviceStateReceiver = new DeviceStateReceiver(magnagement);
        registerReceiver(mDeviceStateReceiver, filter);
        VpnStatus.addByteCountListener(mDeviceStateReceiver);
    }

    synchronized void unregisterDeviceStateReceiver() {
        if (mDeviceStateReceiver != null)
            try {
                VpnStatus.removeByteCountListener(mDeviceStateReceiver);
                this.unregisterReceiver(mDeviceStateReceiver);
            } catch (IllegalArgumentException iae) {
                // I don't know why  this happens:
                // java.lang.IllegalArgumentException: Receiver not registered: de.shellfire.vpn.android.openvpn.NetworkSateReceiver@41a61a10
                // Ignore for now ...
                //iae.printStackTrace();
            }
        mDeviceStateReceiver = null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null && intent.getBooleanExtra(ALWAYS_SHOW_NOTIFICATION, false))
            mNotificationAlwaysVisible = true;

        VpnStatus.addStateListener(this);
        VpnStatus.addByteCountListener(this);

        if (intent != null && PAUSE_VPN.equals(intent.getAction())) {
            if (mDeviceStateReceiver != null)
                mDeviceStateReceiver.userPause(true);
            return START_NOT_STICKY;
        }

        if (intent != null && RESUME_VPN.equals(intent.getAction())) {
            if (mDeviceStateReceiver != null)
                mDeviceStateReceiver.userPause(false);
            return START_NOT_STICKY;
        }


        if (intent != null && START_SERVICE.equals(intent.getAction()))
            return START_NOT_STICKY;
        if (intent != null && START_SERVICE_STICKY.equals(intent.getAction())) {
            return START_REDELIVER_INTENT;
        }

        assert (intent != null);

        // Extract information from the intent.
        String prefix = getPackageName();
        String[] argv = intent.getStringArrayExtra(prefix + ".ARGV");
        String nativelibdir = intent.getStringExtra(prefix + ".nativelib");
        
        mProfile = VpnProfile.current;
        
        
        String startTitle = getString(R.string.start_vpn_title, mProfile.getName());
        String startTicker = getString(R.string.start_vpn_ticker, mProfile.getName());
        // showNotification(startTitle, startTicker, false, 0, LEVEL_CONNECTING_NO_SERVER_REPLY_YET);

        // Set a flag that we are starting a new VPN
        mStarting = true;
        // Stop the previous session by interrupting the thread.
        if (mManagement != null && mManagement.stopVPN())
            // an old was asked to exit, wait 1s
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


        if (mProcessThread != null) {
            mProcessThread.interrupt();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        // An old running VPN should now be exited
        mStarting = false;


        // Open the Management Interface
        if (!mOvpn3) {

            // start a Thread that handles incoming messages of the managment socket
            OpenVpnManagementThread ovpnManagementThread = new OpenVpnManagementThread(mProfile, this);
            if (ovpnManagementThread.openManagementInterface(this)) {

                Thread mSocketManagerThread = new Thread(ovpnManagementThread, "OpenVPNManagementThread");
                mSocketManagerThread.start();
                mManagement = ovpnManagementThread;
                VpnStatus.logInfo("started Socket Thread");
            }
        }

        // Start a new session by creating a new thread.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        mOvpn3 = prefs.getBoolean("ovpn3", false);
        mOvpn3 = false;

        Runnable processThread;
        if (mOvpn3) {

            OpenVPNManagement mOpenVPN3 = instantiateOpenVPN3Core();
            processThread = (Runnable) mOpenVPN3;
            mManagement = mOpenVPN3;


        } else {
            HashMap<String, String> env = new HashMap<String, String>();
            processThread = new OpenVPNThread(this, argv, env, nativelibdir);
        }

        mProcessThread = new Thread(processThread, "OpenVPNProcessThread");
        mProcessThread.start();

        if (mDeviceStateReceiver != null)
            unregisterDeviceStateReceiver();

        registerDeviceStateReceiver(mManagement);

       /* 
        this.mStateChecker = StateChecker.getInstance(this).getThread();
        if (!this.mStateChecker.isAlive())
        	this.mStateChecker.start();
*/
        return START_NOT_STICKY;
    }

    private OpenVPNManagement instantiateOpenVPN3Core() {
        return null;
    }

    @Override
    public void onDestroy() {
        if (mProcessThread != null) {
            mManagement.stopVPN();

            if (mProcessThread != null) {
            	mProcessThread.interrupt();	
            }
            
        }
        if (mDeviceStateReceiver != null) {
            this.unregisterReceiver(mDeviceStateReceiver);
        }
        // Just in case unregister for state
        VpnStatus.removeStateListener(this);

    }

    public ParcelFileDescriptor openTun() {

        //Debug.startMethodTracing(getExternalFilesDir(null).toString() + "/opentun.trace", 40* 1024 * 1024);

        Builder builder = new Builder();

        VpnStatus.logInfo(R.string.last_openvpn_tun_config);


        if (mLocalIP == null && mLocalIPv6 == null) {
            VpnStatus.logError(getString(R.string.opentun_no_ipaddr));
            return null;
        }

        if (mLocalIP != null) {
            try {
                builder.addAddress(mLocalIP.mIp, mLocalIP.len);
            } catch (IllegalArgumentException iae) {
                VpnStatus.logError(R.string.dns_add_error, mLocalIP, iae.getLocalizedMessage());
                return null;
            }
        }

        if (mLocalIPv6 != null) {
            String[] ipv6parts = mLocalIPv6.split("/");
            try {
                builder.addAddress(ipv6parts[0], Integer.parseInt(ipv6parts[1]));
            } catch (IllegalArgumentException iae) {
                VpnStatus.logError(R.string.ip_add_error, mLocalIPv6, iae.getLocalizedMessage());
                return null;
            }

        }


        for (String dns : mDnslist) {
            try {
                builder.addDnsServer(dns);
            } catch (IllegalArgumentException iae) {
                VpnStatus.logError(R.string.dns_add_error, dns, iae.getLocalizedMessage());
            }
        }

        String release = Build.VERSION.RELEASE;
        if ((Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT && !release.startsWith("4.4.3")
                &&  !release.startsWith("4.4.4") &&  !release.startsWith("4.4.5") && !release.startsWith("4.4.6"))
                && mMtu < 1280) {
            VpnStatus.logInfo(String.format(Locale.US, "Forcing MTU to 1280 instead of %d to workaround Android Bug #70916", mMtu));
            builder.setMtu(1280);
        } else {
            builder.setMtu(mMtu);
        }

        Collection<ipAddress> positiveIPv4Routes = mRoutes.getPositiveIPList();
        Collection<ipAddress> positiveIPv6Routes = mRoutesv6.getPositiveIPList();

        for (NetworkSpace.ipAddress route : positiveIPv4Routes) {
            try {
                builder.addRoute(route.getIPv4Address(), route.networkMask);
            } catch (IllegalArgumentException ia) {
                VpnStatus.logError(getString(R.string.route_rejected) + route + " " + ia.getLocalizedMessage());
            }
        }

        for (NetworkSpace.ipAddress route6 : positiveIPv6Routes) {
            try {
                builder.addRoute(route6.getIPv6Address(), route6.networkMask);
            } catch (IllegalArgumentException ia) {
                VpnStatus.logError(getString(R.string.route_rejected) + route6 + " " + ia.getLocalizedMessage());
            }
        }

        if (mDomain != null)
            builder.addSearchDomain(mDomain);

        VpnStatus.logInfo(R.string.local_ip_info, mLocalIP.mIp, mLocalIP.len, mLocalIPv6, mMtu);
        VpnStatus.logInfo(R.string.dns_server_info, TextUtils.join(", ", mDnslist), mDomain);
        VpnStatus.logInfo(R.string.routes_info_incl, TextUtils.join(", ", mRoutes.getNetworks(true)), TextUtils.join(", ", mRoutesv6.getNetworks(true)));
        VpnStatus.logInfo(R.string.routes_info_excl, TextUtils.join(", ", mRoutes.getNetworks(false)),TextUtils.join(", ", mRoutesv6.getNetworks(false)));
        //VpnStatus.logDebug(R.string.routes_debug, TextUtils.join(", ", positiveIPv4Routes), TextUtils.join(", ", positiveIPv6Routes));

        String session = "ShellfireVPN";
        if (mLocalIP != null && mLocalIPv6 != null)
            session = getString(R.string.session_ipv6string, session, mLocalIP, mLocalIPv6);
        else if (mLocalIP != null)
            session = getString(R.string.session_ipv4string, session, mLocalIP);

        builder.setSession(session);

        // No DNS Server, log a warning
        if (mDnslist.size() == 0)
            VpnStatus.logInfo(R.string.warn_no_dns);

        mLastTunCfg = getTunConfigString();

        // Reset information
        mDnslist.clear();
        mRoutes.clear();
        mRoutesv6.clear();
        mLocalIP = null;
        mLocalIPv6 = null;
        mDomain = null;

        builder.setConfigureIntent(getLogPendingIntent());

        try {
            //Debug.stopMethodTracing();
            return builder.establish();
        } catch (Exception e) {
            VpnStatus.logError(R.string.tun_open_error);
            VpnStatus.logError(getString(R.string.error) + e.getLocalizedMessage());
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                VpnStatus.logError(R.string.tun_error_helpful);
            }
            return null;
        }

    }

    // Ugly, but java has no such method
    private <T> String joinString(Vector<T> vec) {
        String ret = "";
        if (vec.size() > 0) {
            ret = vec.get(0).toString();
            for (int i = 1; i < vec.size(); i++) {
                ret = ret + ", " + vec.get(i).toString();
            }
        }
        return ret;
    }

    public void addDNS(String dns) {
        mDnslist.add(dns);
    }

    public void setDomain(String domain) {
        if (mDomain == null) {
            mDomain = domain;
        }
    }
    
    /** Route that is always included, used by the v3 core */
    public void addRoute (CIDRIP route) {
        mRoutes.addIP(route, true);
    }


    public void addRoute (String dest, String mask, String gateway, String device) {
        CIDRIP route = new CIDRIP(dest, mask);
        boolean include = isAndroidTunDevice(device);

        NetworkSpace.ipAddress gatewayIP = new NetworkSpace.ipAddress(new CIDRIP(gateway, 32),false);

        if (mLocalIP==null) {
            VpnStatus.logError("Local IP address unset but adding route?! This is broken! Please contact author with log");
            return;
        }
        NetworkSpace.ipAddress localNet = new NetworkSpace.ipAddress(mLocalIP,true);
        if (localNet.containsNet(gatewayIP))
            include=true;

        if (gateway!= null &&
                (gateway.equals("255.255.255.255") || gateway.equals(mRemoteGW)))
            include=true;


        if (route.len == 32 && !mask.equals("255.255.255.255")) {
            VpnStatus.logWarning(R.string.route_not_cidr, dest, mask);
        }

        if (route.normalise())
            VpnStatus.logWarning(R.string.route_not_netip, dest, route.len, route.mIp);

        mRoutes.addIP(route, include);
    }
    public void addRoutev6(String network, String device) {
        String[] v6parts = network.split("/");
        boolean included = isAndroidTunDevice(device);

        // Tun is opened after ROUTE6, no device name may be present

        try {
            Inet6Address ip = (Inet6Address) InetAddress.getAllByName(v6parts[0])[0];
            int mask = Integer.parseInt(v6parts[1]);
            mRoutesv6.addIPv6(ip, mask, included);

        } catch (UnknownHostException e) {
            VpnStatus.logError(e);
        }


    }
    

    private boolean isAndroidTunDevice(String device) {
        return device!=null &&
                (device.startsWith("tun") || "(null)".equals(device) || "vpnservice-tun".equals(device));
    }

    public void setMtu(int mtu) {
        mMtu = mtu;
    }

    public void setLocalIP(CIDRIP cdrip) {
        mLocalIP = cdrip;
    }

    public void setLocalIP(String local, String netmask, int mtu, String mode) {
        mLocalIP = new CIDRIP(local, netmask);
        mMtu = mtu;
        mRemoteGW=null;

        if (mLocalIP.len == 32 && !netmask.equals("255.255.255.255")) {
            // get the netmask as IP
            long netint = CIDRIP.getInt(netmask);
            if (Math.abs(netint - mLocalIP.getInt()) == 1) {
                if ("net30".equals(mode))
                    mLocalIP.len = 30;
                else
                    mLocalIP.len = 31;
            } else {
                VpnStatus.logWarning(R.string.ip_not_cidr, local, netmask, mode);
            }
        }
        
        // Configurations are sometimes really broken...
        mRemoteGW=netmask;
    }

    public void setLocalIPv6(String ipv6addr) {
        mLocalIPv6 = ipv6addr;
    }

    @Override
    public void updateState(String state, String logmessage, int resid, ConnectionStatus level) {
        // If the process is not running, ignore any state,
        // Notification should be invisible in this state
        doSendBroadcast(state, level);
        if (mProcessThread == null && !mNotificationAlwaysVisible)
            return;

        // Display byte count only after being connected

        {
            if (level == LEVEL_WAITING_FOR_USER_INPUT) {
                // The user is presented a dialog of some kind, no need to inform the user
                // with a notifcation
                return;
            } else if (level == LEVEL_CONNECTED) {
                mDisplayBytecount = true;
                System.currentTimeMillis();
            } else {
                mDisplayBytecount = false;
            }

            // Other notifications are shown,
            // This also mean we are no longer connected, ignore bytecount messages until next
            // CONNECTED
            String ticker = getString(resid);
            // showNotification(getString(resid) + " " + logmessage, ticker, false, 0, level);

        }
    }

    private void doSendBroadcast(String state, ConnectionStatus level) {
        Intent vpnstatus = new Intent();
        vpnstatus.setAction("de.shellfire.vpn.android.openvpn.VPN_STATUS");
        vpnstatus.putExtra("status", level.toString());
        vpnstatus.putExtra("detailstatus", state);
        sendBroadcast(vpnstatus, permission.ACCESS_NETWORK_STATE);
    }

    @Override
    public void updateByteCount(long in, long out, long diffIn, long diffOut) {
        if (mDisplayBytecount) {
            String netstat = String.format(getString(R.string.statusline_bytecount),
                    humanReadableByteCount(in, false),
                    humanReadableByteCount(diffIn / OpenVPNManagement.mBytecountInterval, true),
                    humanReadableByteCount(out, false),
                    humanReadableByteCount(diffOut / OpenVPNManagement.mBytecountInterval, true));

            //boolean lowpriority = !mNotificationAlwaysVisible;
            // showNotification(netstat, null, lowpriority, mConnecttime, LEVEL_CONNECTED);
        }

    }

    @Override
    public boolean handleMessage(Message msg) {
        Runnable r = msg.getCallback();
        if (r != null) {
            r.run();
            return true;
        } else {
            return false;
        }
    }

    public OpenVPNManagement getManagement() {
        return mManagement;
    }

    public class LocalBinder extends Binder {
        public OpenVpnService getService() {
            // Return this instance of LocalService so clients can call public methods
            return OpenVpnService.this;
        }
    }
    

    public String getTunReopenStatus() {
        String currentConfiguration = getTunConfigString();
        if (currentConfiguration.equals(mLastTunCfg)) {
            return "NOACTION";
        } else {
            String release = Build.VERSION.RELEASE;
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT && !release.startsWith("4.4.3")
                    &&  !release.startsWith("4.4.4") &&  !release.startsWith("4.4.5") && !release.startsWith("4.4.6"))
                // There will be probably no 4.4.4 or 4.4.5 version, so don't waste effort to do parsing here
                return "OPEN_AFTER_CLOSE";
            else
                return "OPEN_BEFORE_CLOSE";
        }
    }    
    

    private String getTunConfigString() {
        // The format of the string is not important, only that
        // two identical configurations produce the same result
        String cfg = "TUNCFG UNQIUE STRING ips:";

        if (mLocalIP != null)
            cfg += mLocalIP.toString();
        if (mLocalIPv6 != null)
            cfg += mLocalIPv6;

        cfg += "routes: " + TextUtils.join("|", mRoutes.getNetworks(true)) + TextUtils.join("|", mRoutesv6.getNetworks(true));
        cfg += "excl. routes:" + TextUtils.join("|", mRoutes.getNetworks(false)) + TextUtils.join("|", mRoutesv6.getNetworks(false));
        cfg += "dns: " + TextUtils.join("|", mDnslist);
        cfg += "domain: " + mDomain;
        cfg += "mtu: " + mMtu;
        return cfg;
    }
}
