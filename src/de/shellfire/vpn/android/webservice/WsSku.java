package de.shellfire.vpn.android.webservice;

import org.ksoap2.serialization.SoapObject;

import de.shellfire.vpn.android.ServerType;

public class WsSku {
	boolean isSubscription;
	ServerType serverType;
	int billingPeriod;
	String skuString;
	
	public WsSku(SoapObject skuObj) {
		int i = 0;
		String eAccountType = (String) skuObj.getProperty(i++);
		this.serverType = ServerType.valueOf(eAccountType);
		
		billingPeriod = (Integer) skuObj.getProperty(i++);
		skuString = (String) skuObj.getProperty(i++);

		this.isSubscription = false;
		if (skuString.startsWith("sub")) {
			this.isSubscription = true;
		}

	}

	public boolean isSubscription() {
		return isSubscription;
	}

	public ServerType getServerType() {
		return serverType;
	}

	public int getBillingPeriod() {
		return billingPeriod;
	}

	public String getSkuString() {
		return skuString;
	}

	
	
}
