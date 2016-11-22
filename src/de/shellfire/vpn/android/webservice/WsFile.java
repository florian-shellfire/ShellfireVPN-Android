/**
 * WsFile.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package de.shellfire.vpn.android.webservice;

import org.ksoap2.serialization.SoapObject;

public class WsFile {
	private String name;

	private String content;

	public WsFile(SoapObject soapObj) {
		this.name = (String) soapObj.getProperty(0);;
		this.content = (String) soapObj.getProperty(1);;
	}


	/**
	 * Gets the name value for this WsFile.
	 * 
	 * @return name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the name value for this WsFile.
	 * 
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Gets the content value for this WsFile.
	 * 
	 * @return content
	 */
	public String getContent() {
		return content;
	}

	/**
	 * Sets the content value for this WsFile.
	 * 
	 * @param content
	 */
	public void setContent(String content) {
		this.content = content;
	}

}
