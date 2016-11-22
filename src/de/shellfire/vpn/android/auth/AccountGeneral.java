package de.shellfire.vpn.android.auth;


import de.shellfire.vpn.android.webservice.ShellfireWebService;

/**
* Created with IntelliJ IDEA.
* User: Udini
* Date: 20/03/13
* Time: 18:11
*/
public class AccountGeneral {

    /**
* Account type id
*/
    public static final String ACCOUNT_TYPE = "de.shellfire.vpn.account";

    /**
* Account name
*/
    public static final String ACCOUNT_NAME = "ShellfireVPN";

    public static final String AUTHTOKEN_TYPE_USE_VPN = "VPN Zugriff";
    public static final String AUTHTOKEN_TYPE_USE_VPN_LABEL = "Zugriff zu ShellfireVPN Verbindungen";

    public static final ShellfireWebService webService = ShellfireWebService.getInstance();
}