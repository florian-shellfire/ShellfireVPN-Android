package de.shellfire.vpn.android.openvpn;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Locale;
import java.util.UUID;
import java.util.Vector;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Base64;
import de.shellfire.vpn.android.R;
import de.shellfire.vpn.android.openvpn.VpnStatus.LogLevel;
import de.shellfire.vpn.android.webservice.WsFile;

public class VpnProfile implements Serializable {
    // Note that this class cannot be moved to core where it belongs since
    // the profile loading depends on it being here
    // The Serializable documentation mentions that class name change are possible
    // but the how is unclear
    //
    transient static final long MAX_EMBED_FILE_SIZE = 2048*1024; // 2048kB
    // Don't change this, not all parts of the program use this constant
    public static final String EXTRA_PROFILEUUID = "de.shellfire.vpn.android.openvpn.profileUUID";
    public static final String INLINE_TAG = "[[INLINE]]";
    public static final String MINIVPN = "pievpn";
    private static final long serialVersionUID = 7085688938959334563L;

    public static final int MAXLOGLEVEL = 4;
    public static String DEFAULT_DNS1 = "8.8.8.8";
    public static String DEFAULT_DNS2 = "8.8.4.4";
	public static VpnProfile current;

    public transient String mTransientPW = null;
    public transient String mTransientPCKS12PW = null;


    public static final int TYPE_CERTIFICATES = 0;
    public static final int TYPE_PKCS12 = 1;
    public static final int TYPE_KEYSTORE = 2;
    public static final int TYPE_USERPASS = 3;
    public static final int TYPE_STATICKEYS = 4;
    public static final int TYPE_USERPASS_CERTIFICATES = 5;
    public static final int TYPE_USERPASS_PKCS12 = 6;
    public static final int TYPE_USERPASS_KEYSTORE = 7;
    public static final int X509_VERIFY_TLSREMOTE = 0;
    public static final int X509_VERIFY_TLSREMOTE_COMPAT_NOREMAPPING = 1;
    public static final int X509_VERIFY_TLSREMOTE_DN = 2;
    public static final int X509_VERIFY_TLSREMOTE_RDN = 3;
    public static final int X509_VERIFY_TLSREMOTE_RDN_PREFIX = 4;
    // variable named wrong and should haven beeen transient
    // but needs to keep wrong name to guarante loading of old
    // profiles
    public transient boolean profileDleted = false;
    public int mAuthenticationType = TYPE_KEYSTORE;
    private String mName;
    public String mAlias;
    public String mClientCertFilename;
    public String mTLSAuthDirection = "";
    public String mTLSAuthFilename;

    public String mCaFilename;
    public boolean mUseLzo = true;
    public String mServerPort = "1194";
    public boolean mUseUdp = true;
    public String mPKCS12Filename;
    public String mPKCS12Password;
    public boolean mUseTLSAuth = false;
    public String mServerName = "openvpn.blinkt.de";
    public String mDNS1 = DEFAULT_DNS1;
    public String mDNS2 = DEFAULT_DNS2;
    public String mIPv4Address;
    public String mIPv6Address;
    public boolean mOverrideDNS = false;
    public String mSearchDomain = "blinkt.de";
    public boolean mUseDefaultRoute = true;
    public boolean mUsePull = true;
    public String mCustomRoutes;
    public boolean mCheckRemoteCN = false;
    public boolean mExpectTLSCert = true;
    public String mRemoteCN = "";
    public String mPassword = "";
    public String mUsername = "";
    public boolean mRoutenopull = false;
    public boolean mUseRandomHostname = false;
    public boolean mUseFloat = false;
    public boolean mUseCustomConfig = false;
    public String mCustomConfigOptions = "";
    public String mVerb = "1";  //ignored
    public String mCipher = "";
    public boolean mNobind = false;
    public boolean mUseDefaultRoutev6 = true;
    public String mCustomRoutesv6 = "";
    public String mKeyPassword = "";
    public boolean mPersistTun = false;
    public String mConnectRetryMax = "5";
    public String mConnectRetry = "5";
    public boolean mUserEditable = true;
    public String mAuth = "";
    public int mX509AuthType = X509_VERIFY_TLSREMOTE_RDN;
    private transient PrivateKey mPrivateKey;
    // Public attributes, since I got mad with getter/setter
    // set members to default values
    private UUID mUuid;
	private LinkedList<WsFile> mCertList;
	private String mParams;

    public VpnProfile(String params, LinkedList<WsFile> certs) {
        mUuid = UUID.randomUUID();
        
        String fileName = certs.get(1).getName();
        mName = fileName.substring(0, fileName.indexOf("."));
        //mName = certs.get(0).getName();
        mParams = params;
        this.mCertList = certs;

    }


	public static String openVpnEscape(String unescaped) {
        if (unescaped == null)
            return null;
        String escapedString = unescaped.replace("\\", "\\\\");
        escapedString = escapedString.replace("\"", "\\\"");
        escapedString = escapedString.replace("\n", "\\n");

        if (escapedString.equals(unescaped) && !escapedString.contains(" ") && !escapedString.contains("#"))
            return unescaped;
        else
            return '"' + escapedString + '"';
    }

    public UUID getUUID() {
        return mUuid;

    }

    public String getName() {
        if (mName==null)
            return "No profile name";
        return mName;
    }

    public String getParams(Context context) {

        File cacheDir = context.getCacheDir();
        String params = "";
        

     
       // params += mParams.replace("%APPDATA%\\ShellfireVPN\\", filesDir.getAbsolutePath()+ "/");
        params += mParams.replace("%APPDATA%\\ShellfireVPN\\", cacheDir.getAbsolutePath()+ "/").replace("\"", "");
        params = params.trim();
        params += " --management ";

        params += cacheDir.getAbsolutePath() + "/" + "mgmtsocket unix ";
        params += "--management-client --management-query-passwords --management-hold ";
        // --" + getVersionEnvString(context);

        //params += "--parsable-output true";


        
 /* flotodo: think about this
        if (mOverrideDNS || !mUsePull) {
            if (nonNull(mDNS1))
                cfg += "dhcp-option DNS " + mDNS1 + "\n";
            if (nonNull(mDNS2))
                cfg += "dhcp-option DNS " + mDNS2 + "\n";
            if (nonNull(mSearchDomain))
                cfg += "dhcp-option DOMAIN " + mSearchDomain + "\n";
        }
*/

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean usesystemproxy = prefs.getBoolean("usesystemproxy", true);
        if (usesystemproxy) {
            params +=  " --management-query-proxy";
        }
        
        //params = params.replace("--verb 3", "--verb 9");

        return params;
    }


    private Collection<String> getCustomRoutes() {
        Vector<String> cidrRoutes = new Vector<String>();
        if (mCustomRoutes == null) {
            // No routes set, return empty vector
            return cidrRoutes;
        }
        for (String route : mCustomRoutes.split("[\n \t]")) {
            if (!route.equals("")) {
                String cidrroute = cidrToIPAndNetmask(route);
                if (cidrroute == null)
                    return null;

                cidrRoutes.add(cidrroute);
            }
        }

        return cidrRoutes;
    }

    private String cidrToIPAndNetmask(String route) {
        String[] parts = route.split("/");

        // No /xx, assume /32 as netmask
        if (parts.length == 1)
            parts = (route + "/32").split("/");

        if (parts.length != 2)
            return null;
        int len;
        try {
            len = Integer.parseInt(parts[1]);
        } catch (NumberFormatException ne) {
            return null;
        }
        if (len < 0 || len > 32)
            return null;


        long nm = 0xffffffffl;
        nm = (nm << (32 - len)) & 0xffffffffl;

        String netmask = String.format(Locale.ENGLISH, "%d.%d.%d.%d", (nm & 0xff000000) >> 24, (nm & 0xff0000) >> 16, (nm & 0xff00) >> 8, nm & 0xff);
        return parts[0] + "  " + netmask;
    }

    private String[] buildOpenvpnArgv(Context context) {
        File cacheDir = context.getCacheDir();
    	Vector<String> args = new Vector<String>();

        // Add fixed paramenters
        //args.add("/data/data/de.shellfire.vpn.android.openvpn/lib/openvpn");
        args.add(cacheDir.getAbsolutePath() + "/" + VpnProfile.MINIVPN);

        
        String params = getParams(context);
        
        String[] allParams = params.split(" ");
        
        for (int i = 0; i < allParams.length; i++) {
        	if (allParams[i].equals("--service")) {
        		i++;
        	} else if (allParams[i].equals("--redirect-gateway")) {
        		i++;
        	} else {
        		args.add(allParams[i]);
        	}
        }
        
        return args.toArray(new String[args.size()]);
    }

    public Intent prepareIntent(Context context) {
        String prefix = context.getPackageName();

        Intent intent = new Intent(context, OpenVpnService.class);
        current = this;
        String[] args = buildOpenvpnArgv(context);
        
        
        VpnStatus.logMessage(LogLevel.INFO, "SFVPN", "command line args: " + Arrays.asList(args).toString());
        intent.putExtra(prefix + ".ARGV", buildOpenvpnArgv(context));
        intent.putExtra(prefix + ".profileUUID", mUuid.toString());
        
        ApplicationInfo info = context.getApplicationInfo();
        intent.putExtra(prefix + ".nativelib", info.nativeLibraryDir);

        String abspath = "";
        try {
            // write certificates
            for (WsFile file : this.mCertList) {
            	abspath = context.getCacheDir().getAbsolutePath() + "/" + file.getName();
            	VpnStatus.logInfo("writing file to: " + abspath);
                FileWriter writer = new FileWriter(abspath);
                writer.write(file.getContent());
                writer.flush();
                writer.close();       
                
                File f = new File(abspath);
                if (!f.exists()) {
                	VpnStatus.logError("does not exist: " + abspath);
                }
            }
            
        } catch (IOException e) {
            e.printStackTrace();
            VpnStatus.logError("error while writing file to: " + abspath +  " " +  e.getMessage());
        }

        return intent;
    }




    // Used by the Array Adapter
    @Override
    public String toString() {
        return mName;
    }

    public String getUUIDString() {
        return mUuid.toString();
    }

    public PrivateKey getKeystoreKey() {
        return mPrivateKey;
    }

    public String getSignedData(String b64data) {
        PrivateKey privkey = getKeystoreKey();
        Exception err;

        byte[] data = Base64.decode(b64data, Base64.DEFAULT);

        // The Jelly Bean *evil* Hack
        // 4.2 implements the RSA/ECB/PKCS1PADDING in the OpenSSLprovider
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN) {
            return processSignJellyBeans(privkey, data);
        }


        try {


            Cipher rsasinger = Cipher.getInstance("RSA/ECB/PKCS1PADDING");

            rsasinger.init(Cipher.ENCRYPT_MODE, privkey);

            byte[] signed_bytes = rsasinger.doFinal(data);
            return Base64.encodeToString(signed_bytes, Base64.NO_WRAP);

        } catch (NoSuchAlgorithmException e) {
            err = e;
        } catch (InvalidKeyException e) {
            err = e;
        } catch (NoSuchPaddingException e) {
            err = e;
        } catch (IllegalBlockSizeException e) {
            err = e;
        } catch (BadPaddingException e) {
            err = e;
        }

        VpnStatus.logError(R.string.error_rsa_sign, err.getClass().toString(), err.getLocalizedMessage());

        return null;

    }

    private String processSignJellyBeans(PrivateKey privkey, byte[] data) {
        Exception err;
        try {
            Method getKey = privkey.getClass().getSuperclass().getDeclaredMethod("getOpenSSLKey");
            getKey.setAccessible(true);

            // Real object type is OpenSSLKey
            Object opensslkey = getKey.invoke(privkey);

            getKey.setAccessible(false);

            Method getPkeyContext = opensslkey.getClass().getDeclaredMethod("getPkeyContext");

            // integer pointer to EVP_pkey
            getPkeyContext.setAccessible(true);
            int pkey = (Integer) getPkeyContext.invoke(opensslkey);
            getPkeyContext.setAccessible(false);

            // 112 with TLS 1.2 (172 back with 4.3), 36 with TLS 1.0
            byte[] signed_bytes = NativeUtils.rsasign(data, pkey);
            return Base64.encodeToString(signed_bytes, Base64.NO_WRAP);

        } catch (NoSuchMethodException e) {
            err = e;
        } catch (IllegalArgumentException e) {
            err = e;
        } catch (IllegalAccessException e) {
            err = e;
        } catch (InvocationTargetException e) {
            err = e;
        } catch (InvalidKeyException e) {
            err = e;
        }
        VpnStatus.logError(R.string.error_rsa_sign, err.getClass().toString(), err.getLocalizedMessage());

        return null;

    }


}




