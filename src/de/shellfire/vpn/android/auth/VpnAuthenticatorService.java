package de.shellfire.vpn.android.auth;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class VpnAuthenticatorService extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        VpnAccountAuthenticator authenticator = new VpnAccountAuthenticator(this);
        return authenticator.getIBinder();
    }
}
