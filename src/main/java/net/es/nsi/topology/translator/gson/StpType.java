package net.es.nsi.topology.translator.gson;

/**
 * Models a NSI to OSCARS identifier mapping and gets serialized into
 * a JSON string. 
 * 
 * @author hacksaw
 */
public class StpType {
    private String stpId;
    private String oscarsId;

    /**
     * Get NSI identifier associated with the mapping.
     * 
     * @return the stpId
     */
    public String getStpId() {
        return stpId;
    }

    /**
     * Set the NSI identifier associated with the mapping.
     * 
     * @param stpId the stpId to set
     */
    public void setStpId(String stpId) {
        this.stpId = stpId;
    }

    /**
     * Get OSCARS identifier associated with the mapping.
     * 
     * @return the oscarsId
     */
    public String getOscarsId() {
        return oscarsId;
    }

    /**
     * Set the OSCARS identifier associated with the mapping.
     * @param oscarsId the oscarsId to set
     */
    public void setOscarsId(String oscarsId) {
        this.oscarsId = oscarsId;
    }
}
