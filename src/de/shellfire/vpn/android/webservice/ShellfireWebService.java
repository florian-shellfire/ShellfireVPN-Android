/**
 * ShellfireWebServiceBindingStub.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package de.shellfire.vpn.android.webservice;

import java.io.EOFException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.ksoap2.SoapEnvelope;
import org.ksoap2.SoapFault;
import org.ksoap2.serialization.PropertyInfo;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import android.os.AsyncTask;
import android.util.Log;
import de.shellfire.vpn.android.MainActivity.BillingFragment.ComparisonTableRetrievalFinishedListener;
import de.shellfire.vpn.android.MainActivity.BillingFragment.PremiumStateRetrievalFinishedListener;
import de.shellfire.vpn.android.ProductType;
import de.shellfire.vpn.android.Server;
import de.shellfire.vpn.android.ServerType;
import de.shellfire.vpn.android.Vpn;
import de.shellfire.vpn.android.billing.Base64;
import de.shellfire.vpn.android.billing.Base64DecoderException;
import de.shellfire.vpn.android.openvpn.VpnStatus;
import de.shellfire.vpn.android.openvpn.VpnStatus.LogLevel;

public class ShellfireWebService {

	private static final String WS_NAMESPACE = "https://www.shellfire.de/webservice/sf_soap.php";
	private static final String WS_METHOD_LOGIN = "login";
	private static final String WS_METHOD_GET_SERVER_LIST = "getServerListWithLoad";
	private static final String WS_METHOD_GET_VPN_DETAILS = "getAllVpnDetails";
	private static final String WS_METHOD_GET_PARAMETERS_FOR_OPENVPN = "getParametersForOpenVpn";
	private static final String WS_METHOD_GET_CERTIFICATES_FOR_OPENVPN = "getCertificatesForOpenVpn";
	private static final String WS_METHOD_CREATE_NEW_FREE_VPN = "createNewFreeVpn";
	private static final String WS_METHOD_SET_SERVER_TO = "setServerTo";
	private static final String WS_METHOD_VERIFY_MARKET_INAPPBILLING_PURCHASE = "verifyMarketInAppBillingPurchase";
	private static final String WS_METHOD_REGISTER_NEW_FREE_ACCOUNT = "registerNewFreeAccountAndroid";
	private static final String WS_METHOD_REGISTER_NEW_FREE_ACCOUNT_WITH_GOOGLE_PLAY = "registerNewFreeAccountAndroidWithGooglePlay";

	private static final String WS_METHOD_ACCOUNT_ACTIVE = "accountActive";
	private static final String WS_METHOD_SET_PRODUCT_TYPE_TO_OPENVPN = "setProductTypeToOpenVpn";
	private static final String WS_METHOD_GET_COMPARISON_TABLE_DATA = "getComparisonTableData";
	private static final String WS_METHOD_GET_SKUS = "getSkus";
	private static final String WS_METHOD_GET_SKU_PASSES = "getSkuPasses";
	private static final String WS_METHOD_UPGRADE_VPN_TO_PREMIUM_WITH_GOOGLE_PLAY_PURCHASE = "upgradeVpnToPremiumWithGooglePlayPurchase";
	private static final String WS_METHOD_UPGRADE_VPN_TO_PREMIUM_WITH_COBI_CODE = "upgradeVpnToPremiumWithCobiCode";

	private static final String WS_METHOD_GET_DEVELOPER_PAYLOAD = "getDeveloperPayload";
	private static final String WS_METHOD_SEND_LOG_TO_SHELLFIRE = "sendLogToShellfire";
	private static final String WS_METHOD_GET_MOST_RECENT_VERSIONCODE_ANDROID = "getMostRecentVersionCodeAndroid";

	private static final String TOKEN_SEPARATOR = "_%%%%_";
	private static final String WS_METHOD_GET_LOCAL_IP_ADDRESS = "getLocalIpAddress";
	private static final String WS_METHOD_GET_PUBLIC_VPN_IP_ADDRESS_LIST = "getPublicVpnIpAddressList";

	private static ShellfireWebService instance;
	private String lang = Locale.getDefault().getLanguage();
	private String user;
	private String pass;
	private boolean loggedIn = false;
	private LinkedList<Server> serverList;
	private LinkedList<Vpn> vpnList;
	private Vpn selectedVpn;
	private Server selectedServer;
	private VpnAttributeList vpnAttributeList;
	private List<WsSku> skuList = new LinkedList<WsSku>();
	private List<String> itemSkuList = new LinkedList<String>();
	private List<String> subSkuList = new LinkedList<String>();
	private Boolean isPremium;
	private String parameters;
	private LinkedList<WsFile> certs;
	private List<String> serverIpList;

	private ShellfireWebService() {
	}

	public boolean isLoggedIn() {
		return loggedIn;
	}

	public String getUser() {
		return user;
	}

	public static ShellfireWebService getInstance() {
		if (instance == null) {
			instance = new ShellfireWebService();
		}

		return instance;
	}

	public WsLoginResult login(String user, String pass) throws Exception {

		// set parameters for this session
		this.user = user;
		this.pass = pass;

		String method = WS_METHOD_LOGIN;

		// build request parameter
		LinkedHashMap<String, Object> propertyMap = new LinkedHashMap<String, Object>();

		propertyMap.put("lang", lang);
		propertyMap.put("user", user);
		propertyMap.put("pass", pass);

		SoapObject reponseObject = (SoapObject) makeCall(method, propertyMap);

		Boolean loggedIn = (Boolean) reponseObject.getProperty("loggedIn");
		this.loggedIn = loggedIn;
		String errorMessage = (String) reponseObject.getProperty("errorMessage");

		WsLoginResult result = new WsLoginResult(loggedIn, errorMessage);

		return result;
	}

	public String getLang() {
		return lang;
	}

	private Object makeCall(String method) throws Exception {
		return makeCall(method, new LinkedHashMap<String, Object>());
	}

	private Object makeCall(String method, LinkedHashMap<String, Object> propertyMap) throws Exception {
		SoapObject request = buildRequest(method, propertyMap);

		SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
		envelope.setOutputSoapObject(request);

		HttpTransportSE httpTransport = new HttpTransportSE(WS_NAMESPACE, 60000);

		httpTransport.debug = true;

		int maxTries = 3;
		int currentTry = 1;
		boolean success = false;
		while (currentTry++ <= maxTries && !success) {
			try {
				success = true;
				httpTransport.call(WS_NAMESPACE + method, envelope);
			} catch (EOFException e) {
				// wait a second and try again
				// Thread.sleep(1000);
				String dumpToLog = httpTransport.requestDump;
				if (pass != null && dumpToLog != null)
					dumpToLog = dumpToLog.replace(pass, "xxxxx");

				VpnStatus.logMessage(LogLevel.ERROR, "HTTPTRANSPORT", (currentTry - 1) + " / " + maxTries + " - EOFException at Http call for method=" + method
						+ " HttpRequest: " + dumpToLog);
				success = false;
			}
		}

		String requestDump = httpTransport.requestDump;
		if (requestDump != null && pass != null) {
			requestDump = requestDump.replace(pass, "xxxxx");
		}

		String responseDump = httpTransport.responseDump;
		if (responseDump != null && pass != null) {
			responseDump = responseDump.replace(pass, "xxxxx");
		}

		VpnStatus.logMessage(LogLevel.INFO, "WebServiceCallRequest", requestDump);
		VpnStatus.logMessage(LogLevel.INFO, "WebServiceCallResponse", responseDump);

		if (envelope.bodyIn instanceof SoapObject) { // SoapObject = SUCCESS

			// one of the casts is not valid because an array is returned! :-)
			SoapObject soapObject = (SoapObject) envelope.bodyIn;
			Object ret = soapObject.getProperty("return");
			return ret;

		} else if (envelope.bodyIn instanceof SoapFault) { // SoapFault =
															// FAILURE
			SoapFault soapFault = (SoapFault) envelope.bodyIn;
			throw new Exception(soapFault.getMessage());
		} else {
			throw new Exception("unkown return type in SoapObject" + envelope.bodyIn.getClass().getName());
		}

	}

	private SoapObject buildRequest(String method, LinkedHashMap<String, Object> propertyMap) {
		SoapObject request = new SoapObject(WS_NAMESPACE, method);

		for (String param : propertyMap.keySet()) {
			Object value = propertyMap.get(param);

			PropertyInfo propertyInfo = new PropertyInfo();
			propertyInfo.setName(param);
			propertyInfo.setValue(value);
			propertyInfo.setNamespace(WS_NAMESPACE);

			request.addProperty(propertyInfo);
		}

		return request;
	}

	public LinkedList<Vpn> getAllVpnDetails(boolean loadAlways) throws Exception {
		// should not possibly happen
		if (!isLoggedIn())
			throw new NotLoggedInException("not logged in");

		// vpnList null or empty, need to load from server
		if (this.vpnList == null || this.vpnList.size() == 0 || loadAlways) {
			String method = WS_METHOD_GET_VPN_DETAILS;

			// build request parameter
			LinkedHashMap<String, Object> propertyMap = new LinkedHashMap<String, Object>();

			propertyMap.put("user", user);
			propertyMap.put("pass", pass);

			vpnList = new LinkedList<Vpn>();
			Vector<SoapObject> result = ((Vector<SoapObject>) makeCall(method, propertyMap));
			for (SoapObject soapObj : result) {
				WsVpn wsVpn = new WsVpn(soapObj);
				Vpn vpn = new Vpn(wsVpn);

				this.vpnList.add(vpn);
			}
		}

		return this.vpnList;
	}

	public Vpn createNewFreeVpn() throws Exception {
		// should not possibly happen
		if (!isLoggedIn())
			throw new NotLoggedInException("not logged in");

		// only create a free vpn if no vpn exists yet
		LinkedList<Vpn> vpnList = getAllVpnDetails(true);
		if (vpnList.size() > 0) {
			return vpnList.get(0);
		}

		String method = WS_METHOD_CREATE_NEW_FREE_VPN;

		// build request parameter
		LinkedHashMap<String, Object> propertyMap = new LinkedHashMap<String, Object>();

		propertyMap.put("user", user);
		propertyMap.put("pass", pass);

		vpnList = new LinkedList<Vpn>();
		SoapObject soapObj = (SoapObject) makeCall(method, propertyMap);

		WsVpn wsVpn = new WsVpn(soapObj);
		Vpn vpn = new Vpn(wsVpn);
		this.vpnList.add(vpn);

		return vpn;

	}

	public LinkedList<Server> getServerList() throws Exception {
		return getServerList(false);
	}

	public LinkedList<Server> getServerList(boolean loadAlways) throws Exception {
		if (loadAlways || this.serverList == null || this.serverList.size() == 0) {
			String method = WS_METHOD_GET_SERVER_LIST;

			serverList = new LinkedList<Server>();
			Vector<SoapObject> result = (Vector<SoapObject>) makeCall(method);

			for (SoapObject server : result) {

				WsServer wss = new WsServer(server);
				Server s = new Server(wss);

				this.serverList.add(s);

				if (this.getSelectedServer() != null && this.getSelectedServer().getServerId() == s.getServerId())
					this.setSelectedServer(s);

			}
		}

		return this.serverList;
	}

	public int maySwitchToServer(int vpnProductId, int vpnServerId) {

		return vpnServerId;

	}

	private boolean changeServerTo(int vpnProductId, int vpnServerId) throws Exception {
		String method = WS_METHOD_SET_SERVER_TO;

		// build request parameter
		LinkedHashMap<String, Object> propertyMap = new LinkedHashMap<String, Object>();

		propertyMap.put("user", user);
		propertyMap.put("pass", pass);
		propertyMap.put("vpnProductId", vpnProductId);
		propertyMap.put("vpnServerId", vpnServerId);

		int result = (Integer) makeCall(method, propertyMap);

		return result == 1;
	}

	public boolean setProductTypeToOpenVpn(int vpnProductId) throws Exception {
		// should not possibly happen
		if (!isLoggedIn())
			throw new NotLoggedInException("not logged in");

		String method = WS_METHOD_SET_PRODUCT_TYPE_TO_OPENVPN;

		LinkedHashMap<String, Object> propertyMap = new LinkedHashMap<String, Object>();
		propertyMap.put("user", user);
		propertyMap.put("pass", pass);
		propertyMap.put("vpnProductId", vpnProductId);

		int result = (Integer) makeCall(method, propertyMap);

		return result == 1;

	}

	public int setProtocolTo(int vpnProductId, String protocol) {

		return vpnProductId;
	}

	public String getParametersForOpenVpn() throws Exception {
		if (parameters == null) {
			String method = WS_METHOD_GET_PARAMETERS_FOR_OPENVPN;

			// build request parameter
			LinkedHashMap<String, Object> propertyMap = new LinkedHashMap<String, Object>();

			propertyMap.put("user", user);
			propertyMap.put("pass", pass);
			propertyMap.put("vpnProductId", getSelectedVpn().getVpnId());

			parameters = (String) makeCall(method, propertyMap);
		}

		return parameters;
	}

	public LinkedList<WsFile> getCertificatesForOpenVpn() throws Exception {
		if (certs == null) {
			String method = WS_METHOD_GET_CERTIFICATES_FOR_OPENVPN;

			LinkedHashMap<String, Object> propertyMap = new LinkedHashMap<String, Object>();
			propertyMap.put("user", user);
			propertyMap.put("pass", pass);
			propertyMap.put("vpnProductId", getSelectedVpn().getVpnId());

			LinkedList<WsFile> fileList = new LinkedList<WsFile>();
			Vector<SoapObject> result = (Vector<SoapObject>) makeCall(method, propertyMap);
			for (SoapObject soapObj : result) {
				WsFile file = new WsFile(soapObj);
				fileList.add(file);
			}

			certs = fileList;
		}

		return certs;
	}

	public boolean verifyMarketInAppBillingPurchase(String signedData, String signature) throws Exception {
		String method = WS_METHOD_VERIFY_MARKET_INAPPBILLING_PURCHASE;

		LinkedHashMap<String, Object> propertyMap = new LinkedHashMap<String, Object>();
		propertyMap.put("signedData", signedData);
		propertyMap.put("signature", signature);

		Object resultObj = makeCall(method, propertyMap);
		VpnStatus.logError(resultObj.toString());

		int result = (Integer) resultObj;

		return result == 1;

	}

	public String getLocalIpAddress() throws Exception {

		String myUri = "http://whatismyip.akamai.com/";
		dnsOkay = false;
		if (!resolveDns()) {
			return null;
		}

		HttpGet get = new HttpGet(myUri);

		HttpParams httpParams = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParams, 5000);
		HttpConnectionParams.setSoTimeout(httpParams, 5000);
		HttpClient httpClient = new DefaultHttpClient(httpParams);
		HttpResponse response = httpClient.execute(get);

		// Build up result
		String localIpAddress = EntityUtils.toString(response.getEntity());

		/*
		 * String method = WS_METHOD_GET_LOCAL_IP_ADDRESS; LinkedHashMap<String,
		 * Object> propertyMap = new LinkedHashMap<String, Object>();
		 * 
		 * String localIpAddress = (String) makeCall(method, propertyMap);
		 */

		return localIpAddress;

	}

	public WsGeoPosition getLocalLocation() {
		return null;

	}

	public WsRegistrationResult registerNewFreeAccountWithGooglePlay(String user, String pass, int subscribeToNewsletter) throws Exception {
		String method = WS_METHOD_REGISTER_NEW_FREE_ACCOUNT_WITH_GOOGLE_PLAY;

		LinkedHashMap<String, Object> propertyMap = new LinkedHashMap<String, Object>();
		propertyMap.put("lang", lang);
		propertyMap.put("user", user);
		propertyMap.put("pass", pass);

		propertyMap.put("subscribeToNewsletter", subscribeToNewsletter);
		SoapObject resultObj = (SoapObject) makeCall(method, propertyMap);

		WsRegistrationResult result = new WsRegistrationResult(resultObj);

		return result;
	}

	public WsRegistrationResult registerNewFreeAccount(String user, String pass, int subscribeToNewsletter, int isResend) throws Exception {
		String method = WS_METHOD_REGISTER_NEW_FREE_ACCOUNT;

		LinkedHashMap<String, Object> propertyMap = new LinkedHashMap<String, Object>();
		propertyMap.put("lang", lang);
		propertyMap.put("user", user);
		propertyMap.put("pass", pass);
		propertyMap.put("subscribeToNewsletter", subscribeToNewsletter);
		propertyMap.put("isResend", isResend);
		SoapObject resultObj = (SoapObject) makeCall(method, propertyMap);

		WsRegistrationResult result = new WsRegistrationResult(resultObj);

		return result;
	}

	public boolean accountActive(String token) throws Exception {
		String method = WS_METHOD_ACCOUNT_ACTIVE;

		LinkedHashMap<String, Object> propertyMap = new LinkedHashMap<String, Object>();
		propertyMap.put("token", token);

		int result = (Integer) makeCall(method, propertyMap);

		return result == 1;
	}

	public VpnAttributeList getComparisonTableData() throws Exception {
		if (vpnAttributeList == null) {
			String method = WS_METHOD_GET_COMPARISON_TABLE_DATA;

			LinkedHashMap<String, Object> propertyMap = new LinkedHashMap<String, Object>();
			propertyMap.put("lang", lang);

			SoapObject result = (SoapObject) makeCall(method, propertyMap);

			vpnAttributeList = new VpnAttributeList(result);
		}

		return vpnAttributeList;
	}

	public TrayMessage[] getTrayMessages() {

		return null;
	}

	public String getUrlSuccesfulConnect() {
		return null;

	}

	public String getUrlHelp() {
		return null;

	}

	public String getUrlPremiumInfo() {
		return null;
	}

	public String getUrlPasswordLost() {
		return "https://www.shellfire.de/passwort-verloren/";
	}

	public void setSelectedVpn(int vpnId) throws Exception {
		for (Vpn vpn : this.getAllVpnDetails(false)) {
			if (vpn.getVpnId() == vpnId) {
				this.selectedVpn = vpn;

				if (this.selectedVpn.getProductType() != ProductType.OpenVpn) {
					this.setProductTypeToOpenVpn(vpn.getVpnId());
				}

				this.certs = null;
				this.parameters = null;
				return;
			}
		}
	}

	public Vpn getSelectedVpn() {
		return this.selectedVpn;
	}

	public Server getSelectedServer() {
		return this.selectedServer;
	}

	// required to re wake up after activitiy stopped
	public static void overrideInstance(ShellfireWebService webService) {
		instance = webService;
	}

	public Server getServerById(int serverId) throws Exception {
		serverList = this.getServerList();

		for (Server server : serverList) {
			if (server.getServerId() == serverId) {
				return server;
			}
		}

		return null;
	}

	public void setSelectedServer(Server server) {
		this.selectedServer = server;
	}

	public boolean changeServerTo(Server serverToSelect) throws Exception {
		boolean result = changeServerTo(selectedVpn.getVpnId(), serverToSelect.getServerId());

		if (result)
			selectedServer = serverToSelect;

		this.certs = null;
		this.parameters = null;

		return result;

	}

	public String loginForToken(String name, String password) {
		WsLoginResult result = null;
		try {
			result = this.login(name, password);
		} catch (Exception e) {
			VpnStatus.logError(e);
			e.printStackTrace();
			return "";
			// dont care
		}

		if (result != null && result.isLoggedIn()) {
			return makeToken(name, password);
		}

		return "";
	}

	private String encode(String string) {
		return Base64.encode(string.getBytes());
	}

	private String decode(String string) {
		try {
			return new String(Base64.decode(string));
		} catch (Base64DecoderException e) {
			VpnStatus.logError(e);
			e.printStackTrace();
		}
		return "null";
	}

	private String makeToken(String name, String password) {
		String token = encode(name) + TOKEN_SEPARATOR + encode(password);

		return token;

	}

	public WsLoginResult loginWithToken(String authtoken) throws Exception {
		String creds[] = authtoken.split(TOKEN_SEPARATOR);
		String credUser = decode(creds[0]);
		String credPass = decode(creds[1]);

		return login(credUser, credPass);
	}

	public String getSku(boolean isSubscription, ServerType accountType, int iBillingPeriod) throws Exception {
		if (skuList == null || skuList.size() == 0) {
			this.loadSkuList();
		}

		for (WsSku sku : skuList) {
			if (sku.isSubscription == isSubscription && sku.getServerType() == accountType && sku.getBillingPeriod() == iBillingPeriod)

				return sku.getSkuString();
		}
		return null;
	}

	public List<String> getSubSkuList() throws Exception {
		if (subSkuList == null || subSkuList.size() == 0) {
			this.loadSkuList();
		}

		return this.subSkuList;
	}

	public List<String> getItemSkuList() throws Exception {
		if (itemSkuList == null || itemSkuList.size() == 0) {
			this.loadSkuList();
		}

		return this.itemSkuList;
	}

	public String getDeveloperPayload() throws Exception {
		if (!isLoggedIn())
			throw new NotLoggedInException("not logged in");

		String method = WS_METHOD_GET_DEVELOPER_PAYLOAD;
		LinkedHashMap<String, Object> propertyMap = new LinkedHashMap<String, Object>();
		propertyMap.put("user", user);
		propertyMap.put("pass", pass);

		String payload = (String) makeCall(method, propertyMap);

		return payload;
	}

	public void loadSkuList() throws Exception {
		String method = WS_METHOD_GET_SKUS;

		LinkedHashMap<String, Object> propertyMap = new LinkedHashMap<String, Object>();

		Vector<SoapObject> result = (Vector<SoapObject>) makeCall(method, propertyMap);

		skuList = new LinkedList<WsSku>();
		subSkuList = new LinkedList<String>();
		itemSkuList = new LinkedList<String>();

		for (SoapObject skuObj : result) {
			WsSku sku = new WsSku(skuObj);
			skuList.add(sku);
			subSkuList.add(sku.getSkuString());
		}

		method = WS_METHOD_GET_SKU_PASSES;

		result = (Vector<SoapObject>) makeCall(method, propertyMap);

		for (SoapObject skuObj : result) {
			WsSku sku = new WsSku(skuObj);
			skuList.add(sku);
			itemSkuList.add(sku.getSkuString());
		}
	}

	public WsUpgradeResult upgradeVpnToPremiumWithGooglePlayPurchase(String signed_data, String signature) throws Exception {
		// should not possibly happen
		if (!isLoggedIn())
			throw new NotLoggedInException("not logged in");

		String method = WS_METHOD_UPGRADE_VPN_TO_PREMIUM_WITH_GOOGLE_PLAY_PURCHASE;

		LinkedHashMap<String, Object> propertyMap = new LinkedHashMap<String, Object>();
		propertyMap.put("user", user);
		propertyMap.put("pass", pass);

		Vpn selectedVpn = getSelectedVpn();
		int vpnProductid = selectedVpn.getVpnId();
		propertyMap.put("vpnProductId", vpnProductid);
		propertyMap.put("signed_data", signed_data);
		propertyMap.put("signature", signature);

		SoapObject responseObject = (SoapObject) makeCall(method, propertyMap);

		WsUpgradeResult upgradeResult = new WsUpgradeResult(responseObject);

		if (upgradeResult.getUpgradeSuccesful()) {
			selectedVpn.setAccountType(upgradeResult.getEAccountType());
			selectedVpn.setPremiumUntil(upgradeResult.getIUpgradeUntil());
		}

		return upgradeResult;
	}

	public Boolean isPremium() {
		if (isPremium == null) {
			isPremium = false;
			// only get details if not on main thread!
			try {
				for (Vpn vpn : getAllVpnDetails(false)) {
					if (vpn.getAccountType() != ServerType.Free) {
						isPremium = true;
						break;
					}
				}
			} catch (Exception e) {
				VpnStatus.logError(e);
				e.printStackTrace();
			}
		}

		return isPremium;
	}

	public void setIsPremium(boolean isPremium) {
		this.isPremium = isPremium;
	}

	public boolean sendLogToShellfire(String logString) throws Exception {
		String method = WS_METHOD_SEND_LOG_TO_SHELLFIRE;

		LinkedHashMap<String, Object> propertyMap = new LinkedHashMap<String, Object>();

		propertyMap.put("logString", logString);

		int result = (Integer) makeCall(method, propertyMap);

		return result == 1;
	}

	public void clearParamsAndCerts() {
		this.certs = null;
		this.parameters = null;

	}

	public boolean getParamsLoaded() {
		return certs != null && parameters != null;
	}

	public void isPremiumAsync(final PremiumStateRetrievalFinishedListener ps) {
		new AsyncTask<Void, Void, Boolean>() {

			protected Boolean doInBackground(Void... arg0) {
				Boolean result = isPremium();

				return result;
			}

			protected void onPostExecute(Boolean bol) {
				ps.onPremiumStateRetrievalFinished(bol);
			}

		}.execute();
	}

	public void getComparisonTableDataAsync(final ComparisonTableRetrievalFinishedListener ct) {
		new AsyncTask<Void, Void, VpnAttributeList>() {

			protected VpnAttributeList doInBackground(Void... arg0) {
				VpnAttributeList table = null;
				try {
					table = getComparisonTableData();

				} catch (Exception e) {
					VpnStatus.logError(e);
				}

				return table;
			}

			protected void onPostExecute(VpnAttributeList list) {
				ct.onComparisonTableRetrievalFinished(list);
			}

		}.execute();

	}

	public WsUpgradeResult upgradeVpnToPremiumWithCobiCode(String code) throws Exception {
		// should not possibly happen
		if (!isLoggedIn())
			throw new NotLoggedInException("not logged in");

		String method = WS_METHOD_UPGRADE_VPN_TO_PREMIUM_WITH_COBI_CODE;

		LinkedHashMap<String, Object> propertyMap = new LinkedHashMap<String, Object>();
		propertyMap.put("user", user);
		propertyMap.put("pass", pass);

		Vpn selectedVpn = getSelectedVpn();
		int vpnProductid = selectedVpn.getVpnId();
		propertyMap.put("vpnProductId", vpnProductid);
		propertyMap.put("code", code);

		SoapObject responseObject = (SoapObject) makeCall(method, propertyMap);

		WsUpgradeResult upgradeResult = new WsUpgradeResult(responseObject);

		if (upgradeResult.getUpgradeSuccesful()) {
			selectedVpn.setAccountType(upgradeResult.getEAccountType());
			selectedVpn.setPremiumUntil(upgradeResult.getIUpgradeUntil());
		}

		return upgradeResult;
	}

	public int getMostRecentVersion() throws Exception {
		String method = WS_METHOD_GET_MOST_RECENT_VERSIONCODE_ANDROID;

		LinkedHashMap<String, Object> propertyMap = new LinkedHashMap<String, Object>();

		int result = (Integer) makeCall(method, propertyMap);

		return result;
	}

	public List<String> getVpnServerIpList() throws Exception {

		if (this.serverIpList == null || this.serverIpList.size() == 0) {
			this.serverIpList = new LinkedList<String>();
			String method = WS_METHOD_GET_PUBLIC_VPN_IP_ADDRESS_LIST;

			LinkedHashMap<String, Object> propertyMap = new LinkedHashMap<String, Object>();

			Vector<SoapObject> result = (Vector<SoapObject>) makeCall(method, propertyMap);

			for (SoapObject skuObj : result) {
				String host = (String) skuObj.getProperty(0);
				InetAddress address = InetAddress.getByName(host);
				String ip = address.getHostAddress();

				this.serverIpList.add(ip);
			}
		}

		return this.serverIpList;
	}

	public boolean isVpnServerIp(String localIp) throws Exception {
		return getVpnServerIpList().contains(localIp);
	}

	private boolean dnsOkay = false;
	private static final int DNS_SLEEP_WAIT = 250;

	private synchronized boolean resolveDns() {

		RemoteDnsCheck check = new RemoteDnsCheck();
		check.execute();
		try {
			int timeSlept = 0;
			while (!dnsOkay && timeSlept < 4000) {
				Log.d("RemoteDnsCheck", "sleeping");
				Thread.sleep(DNS_SLEEP_WAIT);
				timeSlept += DNS_SLEEP_WAIT;
				Log.d("RemoteDnsCheck", "slept");
			}
		} catch (InterruptedException e) {

		}

		if (!dnsOkay) {
			Log.d("resolveDns", "cancelling");
			check.cancel(true);
			Log.d("resolveDns", "cancelled");
		}
		return dnsOkay;
	}

	private class RemoteDnsCheck extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			try {
				Log.d("RemoteDnsCheck", "starting");
				dnsOkay = false;
				InetAddress addr = InetAddress.getByName("whatismyip.akamai.com");
				if (addr != null) {
					Log.d("RemoteDnsCheck", "got addr");
					dnsOkay = true;
				}
			} catch (UnknownHostException e) {
				Log.d("RemoteDnsCheck", "UnknownHostException");
			}
			return null;
		}

	}

}
