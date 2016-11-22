package de.shellfire.vpn.android;


import java.io.Serializable;
import java.util.Date;

import de.shellfire.vpn.android.webservice.ShellfireWebService;
import de.shellfire.vpn.android.webservice.WsVpn;



public class Vpn implements Serializable {

    private int vpnId;
    private int serverId;
    private ServerType accountType;
    private String listenHost;
    private Protocol protocol;
    private Server server;
    private ProductType productType;
    private Date premiumUntil;

    public Server getServer() throws Exception {
    	if (server == null && serverId != 0) {
    		server = ShellfireWebService.getInstance().getServerById(serverId);
    	}
        return server;
    }

    public void setServer(Server server) {
        this.server = server;
    }

    public Vpn(WsVpn vpn) {
        this.vpnId = vpn.getIVpnId();
        this.serverId = vpn.getIServerId();
        this.accountType = Enum.valueOf(ServerType.class, vpn.getEAccountType());
        this.listenHost = vpn.getSListenHost();
        if (vpn.getEProtocol() != null && vpn.getEProtocol().length() > 0)
        	this.protocol = Enum.valueOf(Protocol.class, vpn.getEProtocol());

        ProductType[] vals = ProductType.values();
        this.productType = vals[vpn.getIProductTypeId()-1];
        
        if (vpn.getIPremiumUntil() != null) {
            long lngUntil = (long)vpn.getIPremiumUntil() * 1000;
            this.premiumUntil = new Date(lngUntil);
        } else {
        	this.premiumUntil = null;
        }
    }

    public int getVpnId() {
        return vpnId;
    }

    public void setVpnId(int vpnId) {
        this.vpnId = vpnId;
    }

    public int getServerId() {
        return serverId;
    }

    public void setServerId(int serverId) {
        this.serverId = serverId;
    }

    public ServerType getAccountType() {
        return accountType;
    }

    public void setAccountType(ServerType accountType) {
        this.accountType = accountType;
    }

    public String getListenHost() {
        return listenHost;
    }

    public void setListenHost(String listenHost) {
        this.listenHost = listenHost;
    }

    public Protocol getProtocol() {
        return protocol;
    }

    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    public void loadServerObject(ServerList serverList) {

        this.server = serverList.getServerByServerId(this.serverId);
    }

    public ProductType getProductType() {
        return this.productType;
    }

    public Date getPremiumUntil() {
        return this.premiumUntil;
    }
    

    public String toString() {
        return "int vpnId: " + vpnId +
		        "int serverId: " + serverId +
		        "ServerType accountType: " + accountType +
		        "String listenHost: " + listenHost +
		        "Protocol protocol: " + protocol +
		        "Server server: " + server +
		        "ProductType productType: " + productType +
		        "final Date premiumUntil: " + premiumUntil;
    }

	public String getName() {
		return "sf" + vpnId;
	}
	
	public void setPremiumUntil(Date premiumUntil) {
		this.premiumUntil = premiumUntil;
	}
	
	public void setPremiumUntil(int premiumUntil) {
		setPremiumUntil(new Date(premiumUntil));
	}
}
