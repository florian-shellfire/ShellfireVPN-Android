/**
 * WsLoginResult.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package de.shellfire.vpn.android.webservice;


public class WsLoginResult {
	private boolean loggedIn;

	private String errorMessage;

	public WsLoginResult() {
	}

	public WsLoginResult(boolean loggedIn, String errorMessage) {
		this.loggedIn = loggedIn;
		this.errorMessage = errorMessage;
	}

	/**
	 * Gets the loggedIn value for this WsLoginResult.
	 * 
	 * @return loggedIn
	 */
	public boolean isLoggedIn() {
		return loggedIn;
	}

	/**
	 * Sets the loggedIn value for this WsLoginResult.
	 * 
	 * @param loggedIn
	 */
	public void setLoggedIn(boolean loggedIn) {
		this.loggedIn = loggedIn;
	}

	/**
	 * Gets the errorMessage value for this WsLoginResult.
	 * 
	 * @return errorMessage
	 */
	public String getErrorMessage() {
		return errorMessage;
	}

	/**
	 * Sets the errorMessage value for this WsLoginResult.
	 * 
	 * @param errorMessage
	 */
	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

}
