/**
 * VpnAttributeList.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package de.shellfire.vpn.android.webservice;

import java.util.Vector;

import org.ksoap2.serialization.SoapObject;

public class VpnAttributeList implements java.io.Serializable {
	private VpnAttributeContainer[] containers;

	public VpnAttributeList() {
	}

	public VpnAttributeList(VpnAttributeContainer[] containers) {
		this.containers = containers;
	}

	public VpnAttributeList(SoapObject result) {
		Vector<SoapObject> containerSoaps = (Vector<SoapObject>)result.getProperty(0);
		containers = new VpnAttributeContainer[containerSoaps.size()];
		int i = 0;
		for (SoapObject containerSoap : containerSoaps) {
			VpnAttributeContainer container = new VpnAttributeContainer(containerSoap);
			containers[i++] = container;
		}
		
		// TODO Auto-generated constructor stub
	}

	/**
	 * Gets the containers value for this VpnAttributeList.
	 * 
	 * @return containers
	 */
	public VpnAttributeContainer[] getContainers() {
		return containers;
	}

	/**
	 * Sets the containers value for this VpnAttributeList.
	 * 
	 * @param containers
	 */
	public void setContainers(VpnAttributeContainer[] containers) {
		this.containers = containers;
	}


}
