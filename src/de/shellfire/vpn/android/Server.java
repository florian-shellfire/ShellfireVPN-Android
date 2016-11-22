package de.shellfire.vpn.android;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import de.shellfire.vpn.android.webservice.WsServer;

public class Server implements Serializable {

	private int serverId;
	private Country country;
	private String name;
	private String host;
	private ServerType serverType;
	private double longitude;
	private double latitude;
	private int loadPercentage;

	public Server(WsServer wss) {
		this.serverId = wss.getVpnServerId();
		String country = wss.getCountry();

		country = country.replace(" ", "");

		try {
			this.country = Enum.valueOf(Country.class, country);
		} catch (Exception e) {
			this.country = Country.Germany;
		}

		this.name = wss.getName();
		this.host = wss.getHost();
		this.serverType = Enum.valueOf(ServerType.class, wss.getServertype());
		this.longitude = wss.getLongitude();
		this.latitude = wss.getLatitude();
		this.loadPercentage = wss.getLoadPercentage();
	}

	public int getServerId() {
		return serverId;
	}
	
	public void setVpnServerId(int serverId) {
		this.serverId = serverId;
	}


	public int getLoadPercentage() {
		return loadPercentage;
	}

	public void setLoadPercentage(int loadPercentage) {
		this.loadPercentage = loadPercentage;
	}
	
	public Country getCountry() {
		return country;
	}

	public void setCountry(Country country) {
		this.country = country;
	}

	public String getName() {
		String result = CountryMap.get(country).toUpperCase();
		result = country.toString();
		return result;
		//return result + " - " + name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public ServerType getServerType() {
		return serverType;
	}

	public void setServerType(ServerType serverType) {
		this.serverType = serverType;
	}

	public VpnStar getServerSpeed() {
		switch (this.serverType) {
		case PremiumPlus:
			return new VpnStar(5, R.string.unlimited);
		case Premium:
			return new VpnStar(3, R.string.upto10000);
		case Free:
		default:
			return new VpnStar(1, R.string.upto786);
		}
	}

	@Override
	public boolean equals(Object server) {
		if (server == null)
			return false;
		else if (!(server instanceof Server))
			return false;
		else {
			Server s = (Server) server;
			return s.getServerId() == this.getServerId();
		}

	}

	public double getLatitude() {
		return this.latitude;
	}

	public double getLongitude() {
		return this.longitude;
	}

	public String getCountryString() {
		return this.getCountry().toString();
	}

	public VpnStar getSecurity() {
		switch (this.serverType) {
		case PremiumPlus:
			return new VpnStar(5, R.string._256bit);
		case Premium:
			return new VpnStar(3, R.string._192bit);
		case Free:
		default:
			return new VpnStar(2, R.string._128bit);
		}
	}

	public String toString() {
		return "\n int serverId: " + serverId + "\n Country country: " + country + "\n String name: " + name + "\n String host: " + host
				+ "\n ServerType serverType: " + serverType + "\n double longitude: " + longitude + "\n double latitude: " + latitude;

	}

}
