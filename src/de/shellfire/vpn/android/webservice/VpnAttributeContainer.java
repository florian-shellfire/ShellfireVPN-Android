/**
 * VpnAttributeContainer.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package de.shellfire.vpn.android.webservice;

import java.util.Vector;

import org.ksoap2.serialization.SoapObject;

public class VpnAttributeContainer implements java.io.Serializable {
	private java.lang.String containerName;

	private VpnAttributeElement[] elements;

	public VpnAttributeContainer() {
	}

	public VpnAttributeContainer(java.lang.String containerName, VpnAttributeElement[] elements) {
		this.containerName = containerName;
		this.elements = elements;
	}

	public VpnAttributeContainer(SoapObject containerSoap) {

		this.containerName = (String) containerSoap.getProperty(0);
		
		Vector<SoapObject> vpnAttributeElementSoapVector = (Vector<SoapObject>) containerSoap.getProperty(1);
		
		int i = 0;
		elements = new VpnAttributeElement[vpnAttributeElementSoapVector.size()];
		for (SoapObject vpnAttributeElementSoap : vpnAttributeElementSoapVector) {
			VpnAttributeElement currentElement = new VpnAttributeElement(vpnAttributeElementSoap);
			elements[i++] = currentElement;
		}
		
	}

	/**
	 * Gets the containerName value for this VpnAttributeContainer.
	 * 
	 * @return containerName
	 */
	public java.lang.String getContainerName() {
		return containerName;
	}

	/**
	 * Sets the containerName value for this VpnAttributeContainer.
	 * 
	 * @param containerName
	 */
	public void setContainerName(java.lang.String containerName) {
		this.containerName = containerName;
	}

	/**
	 * Gets the elements value for this VpnAttributeContainer.
	 * 
	 * @return elements
	 */
	public VpnAttributeElement[] getElements() {
		return elements;
	}

	/**
	 * Sets the elements value for this VpnAttributeContainer.
	 * 
	 * @param elements
	 */
	public void setElements(VpnAttributeElement[] elements) {
		this.elements = elements;
	}


}
