package net.es.nsi.topology.translator.gson;

import java.util.ArrayList;
import java.util.List;

/**
 * Models a nsi-bridge nsa.json file contents. 
 * 
 * @author hacksaw
 */
public class LocalType {
    private String nsaId;
    private String protocolVersion;
    private String serviceType;
    private String networkId;
    private List<StpType> stps = new ArrayList<>();

    /**
     * @return the nsaId
     */
    public String getNsaId() {
        return nsaId;
    }

    /**
     * @param nsaId the nsaId to set
     */
    public void setNsaId(String nsaId) {
        this.nsaId = nsaId;
    }

    /**
     * @return the protocolVersion
     */
    public String getProtocolVersion() {
        return protocolVersion;
    }

    /**
     * @param protocolVersion the protocolVersion to set
     */
    public void setProtocolVersion(String protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    /**
     * @return the serviceType
     */
    public String getServiceType() {
        return serviceType;
    }

    /**
     * @param serviceType the serviceType to set
     */
    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    /**
     * @return the networkId
     */
    public String getNetworkId() {
        return networkId;
    }

    /**
     * @param networkId the networkId to set
     */
    public void setNetworkId(String networkId) {
        this.networkId = networkId;
    }

    /**
     * @return the stps
     */
    public List<StpType> getStps() {
        return stps;
    }

    /**
     * @param stps the stps to set
     */
    public void setStps(List<StpType> stps) {
        this.stps = stps;
    }
    
}
