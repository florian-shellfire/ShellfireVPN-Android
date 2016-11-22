/**
 * WsUpgradeResult.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package de.shellfire.vpn.android.webservice;

import org.ksoap2.serialization.SoapObject;

import de.shellfire.vpn.android.ServerType;

public class WsUpgradeResult implements java.io.Serializable {
	private String error;

	private ServerType eAccountType;

	private int iUpgradeUntil;

	private boolean upgradeSuccesful;

	public WsUpgradeResult(SoapObject responseObject) {
		int i = 0;
		error = (String) responseObject.getProperty(i++);
		
		String accountTypeStr = (String) responseObject.getProperty(i++);
		if (accountTypeStr != null)
			eAccountType = ServerType.valueOf(accountTypeStr);
		
		Object upgradeUntil = responseObject.getProperty(i++);
		if (upgradeUntil != null)
			iUpgradeUntil = (Integer) upgradeUntil;
		
		Object upgradeSuccess = responseObject.getProperty(i++);
		if (upgradeSuccess != null)
			upgradeSuccesful = ((Integer)upgradeSuccess == 1);

	}

	/**
	 * Gets the error value for this WsUpgradeResult.
	 * 
	 * @return error
	 */
	public String getError() {
		return error;
	}

	/**
	 * Sets the error value for this WsUpgradeResult.
	 * 
	 * @param error
	 */
	public void setError(String error) {
		this.error = error;
	}

	/**
	 * Gets the eAccountType value for this WsUpgradeResult.
	 * 
	 * @return eAccountType
	 */
	public ServerType getEAccountType() {
		return eAccountType;
	}

	/**
	 * Sets the eAccountType value for this WsUpgradeResult.
	 * 
	 * @param eAccountType
	 */
	public void setEAccountType(ServerType eAccountType) {
		this.eAccountType = eAccountType;
	}

	/**
	 * Gets the iUpgradeUntil value for this WsUpgradeResult.
	 * 
	 * @return iUpgradeUntil
	 */
	public int getIUpgradeUntil() {
		return iUpgradeUntil;
	}

	/**
	 * Sets the iUpgradeUntil value for this WsUpgradeResult.
	 * 
	 * @param iUpgradeUntil
	 */
	public void setIUpgradeUntil(int iUpgradeUntil) {
		this.iUpgradeUntil = iUpgradeUntil;
	}

	/**
	 * Gets the upgradeSuccesful value for this WsUpgradeResult.
	 * 
	 * @return upgradeSuccesful
	 */
	public boolean getUpgradeSuccesful() {
		return upgradeSuccesful;
	}

	/**
	 * Sets the upgradeSuccesful value for this WsUpgradeResult.
	 * 
	 * @param upgradeSuccesful
	 */
	public void setUpgradeSuccesful(boolean upgradeSuccesful) {
		this.upgradeSuccesful = upgradeSuccesful;
	}


}
