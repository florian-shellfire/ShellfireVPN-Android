package de.shellfire.vpn.android.auth;

import static android.accounts.AccountManager.KEY_BOOLEAN_RESULT;
import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import de.shellfire.vpn.android.webservice.ShellfireWebService;
public class VpnAccountAuthenticator extends AbstractAccountAuthenticator {

	private Context mContext;
	private ShellfireWebService webService = ShellfireWebService.getInstance();

	public VpnAccountAuthenticator(Context context) {
		super(context);
		mContext = context;
	
	}

	@Override
	public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options) throws NetworkErrorException {
	    final Intent intent = new Intent(mContext, VpnAuthenticatorActivity.class);
	    intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, accountType);
	    intent.putExtra(VpnAuthenticatorActivity.ARG_AUTH_TYPE, authTokenType);
	    intent.putExtra(VpnAuthenticatorActivity.ARG_IS_ADDING_NEW_ACCOUNT, true);
	    intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
	    final Bundle bundle = new Bundle();
	    bundle.putParcelable(AccountManager.KEY_INTENT, intent);
	    return bundle;
	}

	@Override
	public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
	 
	    // Extract the username and password from the Account Manager, and ask
	    // the server for an appropriate AuthToken.
	    final AccountManager am = AccountManager.get(mContext);
	 
	    String authToken = am.peekAuthToken(account, authTokenType);
	 
	    // Lets give another try to authenticate the user
	    if (TextUtils.isEmpty(authToken)) {
	        final String password = am.getPassword(account);
	        if (password != null) {
	            authToken = webService.loginForToken(account.name, password);
	        }
	    }
	 
	    // If we get an authToken - we return it
	    if (!TextUtils.isEmpty(authToken)) {
	        final Bundle result = new Bundle();
	        result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
	        result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
	        result.putString(AccountManager.KEY_AUTHTOKEN, authToken);
	        return result;
	    }
	 
	    // If we get here, then we couldn't access the user's password - so we
	    // need to re-prompt them for their credentials. We do that by creating
	    // an intent to display our LoginActivity.
	    final Intent intent = new Intent(mContext, VpnAuthenticatorActivity.class);
	    intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
	    intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, account.type);
	    intent.putExtra(VpnAuthenticatorActivity.ARG_AUTH_TYPE, authTokenType);
	    final Bundle bundle = new Bundle();
	    bundle.putParcelable(AccountManager.KEY_INTENT, intent);
	    return bundle;
	}

	@Override
	public String getAuthTokenLabel(String arg0) {
		
		return AccountGeneral.AUTHTOKEN_TYPE_USE_VPN_LABEL;
	}



    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features) throws NetworkErrorException {
        final Bundle result = new Bundle();
        result.putBoolean(KEY_BOOLEAN_RESULT, false);
        return result;
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
        return null;
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options) throws NetworkErrorException {
        return null;
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
        return null;
    }
}
