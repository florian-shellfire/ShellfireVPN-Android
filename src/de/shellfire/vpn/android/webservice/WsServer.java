/**
 * WsServer.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package de.shellfire.vpn.android.webservice;

import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapPrimitive;

public class WsServer {
	private int vpnServerId;

	private String country;

	private String city;

	private String name;

	private String host;

	private String servertype;

	private float longitude;

	private float latitude;
	
	private int loadPercentage;

	public WsServer(SoapObject server) {
		this.vpnServerId = (Integer) server.getProperty(0);
		this.country = (String) server.getProperty(1);
		this.city = (String) server.getProperty(2);
		this.name = (String) server.getProperty(3);
		this.host = (String) server.getProperty(4);
		this.servertype = (String) server.getProperty(5);
		
		SoapPrimitive longitud = (SoapPrimitive)server.getProperty(6);
		this.longitude = Float.parseFloat(longitud.toString());
		
		SoapPrimitive latitud = (SoapPrimitive)server.getProperty(7);
		this.latitude = Float.parseFloat(latitud.toString());
		
		this.loadPercentage = (Integer) server.getProperty(8);
	}

	/**
	 * Gets the vpnServerId value for this WsServer.
	 * 
	 * @return vpnServerId
	 */
	public int getVpnServerId() {
		return vpnServerId;
	}

	/**
	 * Sets the vpnServerId value for this WsServer.
	 * 
	 * @param vpnServerId
	 */
	public void setVpnServerId(int vpnServerId) {
		this.vpnServerId = vpnServerId;
	}
	

	/**
	 * Gets the loadPercentage value for this WsServer.
	 * 
	 * @return vpnServerId
	 */
	public int getLoadPercentage() {
		return loadPercentage;
	}

	/**
	 * Sets the loadPercentage value for this WsServer.
	 * 
	 * @param loadPercentage
	 */
	public void setLoadPercentage(int loadPercentage) {
		this.loadPercentage = loadPercentage;
	}
	

	/**
	 * Gets the country value for this WsServer.
	 * 
	 * @return country
	 */
	public String getCountry() {
		return country;
	}

	/**
	 * Sets the country value for this WsServer.
	 * 
	 * @param country
	 */
	public void setCountry(String country) {
		this.country = country;
	}

	/**
	 * Gets the city value for this WsServer.
	 * 
	 * @return city
	 */
	public String getCity() {
		return city;
	}

	/**
	 * Sets the city value for this WsServer.
	 * 
	 * @param city
	 */
	public void setCity(String city) {
		this.city = city;
	}

	/**
	 * Gets the name value for this WsServer.
	 * 
	 * @return name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the name value for this WsServer.
	 * 
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Gets the host value for this WsServer.
	 * 
	 * @return host
	 */
	public String getHost() {
		return host;
	}

	/**
	 * Sets the host value for this WsServer.
	 * 
	 * @param host
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * Gets the servertype value for this WsServer.
	 * 
	 * @return servertype
	 */
	public String getServertype() {
		return servertype;
	}

	/**
	 * Sets the servertype value for this WsServer.
	 * 
	 * @param servertype
	 */
	public void setServertype(String servertype) {
		this.servertype = servertype;
	}

	/**
	 * Gets the longitude value for this WsServer.
	 * 
	 * @return longitude
	 */
	public float getLongitude() {
		return longitude;
	}

	/**
	 * Sets the longitude value for this WsServer.
	 * 
	 * @param longitude
	 */
	public void setLongitude(float longitude) {
		this.longitude = longitude;
	}

	/**
	 * Gets the latitude value for this WsServer.
	 * 
	 * @return latitude
	 */
	public float getLatitude() {
		return latitude;
	}

	/**
	 * Sets the latitude value for this WsServer.
	 * 
	 * @param latitude
	 */
	public void setLatitude(float latitude) {
		this.latitude = latitude;
	}


}
