/**
 * WsVpn.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package de.shellfire.vpn.android.webservice;

import org.ksoap2.serialization.SoapObject;

public class WsVpn implements java.io.Serializable {
	private int iVpnId;

	private int iProductTypeId;

	private int iServerId;

	private java.lang.String sUserName;

	private java.lang.String sPassword;

	private java.lang.String eAccountType;

	private java.lang.String sListenHost;

	private java.lang.String eProtocol;

	private Integer iPremiumUntil;

	public WsVpn() {
	}

	public WsVpn(SoapObject vpn) {

		int i = 0;
		this.iVpnId = (Integer) vpn.getProperty(i++);
		this.iProductTypeId = (Integer) vpn.getProperty(i++);
		this.iServerId = (Integer) vpn.getProperty(i++);
		this.sUserName = (String) vpn.getProperty(i++);
		this.sPassword = (String) vpn.getProperty(i++);
		this.eAccountType = (String) vpn.getProperty(i++);
		this.sListenHost = (String) vpn.getProperty(i++);
		this.eProtocol = (String) vpn.getProperty(i++);
		this.iPremiumUntil = (Integer) vpn.getProperty(i++);
		
	}

	/**
	 * Gets the iVpnId value for this WsVpn.
	 * 
	 * @return iVpnId
	 */
	public int getIVpnId() {
		return iVpnId;
	}

	/**
	 * Sets the iVpnId value for this WsVpn.
	 * 
	 * @param iVpnId
	 */
	public void setIVpnId(int iVpnId) {
		this.iVpnId = iVpnId;
	}

	/**
	 * Gets the iProductTypeId value for this WsVpn.
	 * 
	 * @return iProductTypeId
	 */
	public int getIProductTypeId() {
		return iProductTypeId;
	}

	/**
	 * Sets the iProductTypeId value for this WsVpn.
	 * 
	 * @param iProductTypeId
	 */
	public void setIProductTypeId(int iProductTypeId) {
		this.iProductTypeId = iProductTypeId;
	}

	/**
	 * Gets the iServerId value for this WsVpn.
	 * 
	 * @return iServerId
	 */
	public int getIServerId() {
		return iServerId;
	}

	/**
	 * Sets the iServerId value for this WsVpn.
	 * 
	 * @param iServerId
	 */
	public void setIServerId(int iServerId) {
		this.iServerId = iServerId;
	}

	/**
	 * Gets the sUserName value for this WsVpn.
	 * 
	 * @return sUserName
	 */
	public java.lang.String getSUserName() {
		return sUserName;
	}

	/**
	 * Sets the sUserName value for this WsVpn.
	 * 
	 * @param sUserName
	 */
	public void setSUserName(java.lang.String sUserName) {
		this.sUserName = sUserName;
	}

	/**
	 * Gets the sPassword value for this WsVpn.
	 * 
	 * @return sPassword
	 */
	public java.lang.String getSPassword() {
		return sPassword;
	}

	/**
	 * Sets the sPassword value for this WsVpn.
	 * 
	 * @param sPassword
	 */
	public void setSPassword(java.lang.String sPassword) {
		this.sPassword = sPassword;
	}

	/**
	 * Gets the eAccountType value for this WsVpn.
	 * 
	 * @return eAccountType
	 */
	public java.lang.String getEAccountType() {
		return eAccountType;
	}

	/**
	 * Sets the eAccountType value for this WsVpn.
	 * 
	 * @param eAccountType
	 */
	public void setEAccountType(java.lang.String eAccountType) {
		this.eAccountType = eAccountType;
	}

	/**
	 * Gets the sListenHost value for this WsVpn.
	 * 
	 * @return sListenHost
	 */
	public java.lang.String getSListenHost() {
		return sListenHost;
	}

	/**
	 * Sets the sListenHost value for this WsVpn.
	 * 
	 * @param sListenHost
	 */
	public void setSListenHost(java.lang.String sListenHost) {
		this.sListenHost = sListenHost;
	}

	/**
	 * Gets the eProtocol value for this WsVpn.
	 * 
	 * @return eProtocol
	 */
	public java.lang.String getEProtocol() {
		return eProtocol;
	}

	/**
	 * Sets the eProtocol value for this WsVpn.
	 * 
	 * @param eProtocol
	 */
	public void setEProtocol(java.lang.String eProtocol) {
		this.eProtocol = eProtocol;
	}

	/**
	 * Gets the iPremiumUntil value for this WsVpn.
	 * 
	 * @return iPremiumUntil
	 */
	public Integer getIPremiumUntil() {
		return iPremiumUntil;
	}

	/**
	 * Sets the iPremiumUntil value for this WsVpn.
	 * 
	 * @param iPremiumUntil
	 */
	public void setIPremiumUntil(int iPremiumUntil) {
		this.iPremiumUntil = iPremiumUntil;
	}
}
