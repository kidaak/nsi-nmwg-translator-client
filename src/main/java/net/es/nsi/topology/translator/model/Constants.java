package net.es.nsi.topology.translator.model;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Constants used by the NMWG to NML translator.
 * 
 * @author hacksaw
 */
public class Constants {

    // NSI URN components.
    public static final String NSI_URN_OGF_NETWORK = "urn:ogf:network:";
    public static final String NSI_URN_SEPARATOR = ":";
    public static final String NSI_URN_DOMAIN_YEAR = "2013";
    public static final String NSI_URN_SERVICEDOMAIN = "ServiceDomain";
    public static final String NSI_URN_SERVICEDEFINITION = "ServiceDefinition";
    public static final String NSI_STPID_EXTENSION_PORT_IN = "in";
    public static final String NSI_STPID_EXTENSION_PORT_OUT = "out";


    // NML types.
    public static final String NML_PORT_INBOUND = "http://schemas.ogf.org/nml/2013/05/base#hasInboundPort";
    public static final String NML_PORT_OUTBOUND = "http://schemas.ogf.org/nml/2013/05/base#hasOutboundPort";
    public static final String NML_ETHERNET = "http://schemas.ogf.org/nml/2012/10/ethernet";
    public static final String NML_ETHERNET_VLAN = "http://schemas.ogf.org/nml/2012/10/ethernet#vlan";
    public static final String NML_ISALIAS = "http://schemas.ogf.org/nml/2013/05/base#isAlias";
    public static final String NML_SWITCHINGSERVICE = "http://schemas.ogf.org/nml/2013/05/base#hasService";
    public static final String NML_ETHERNET_VLAN_RANGE = "2-4094";

    // Default A-GOLE EVTS Service Definition.
    public static final String SD_EVTS_AGOLE_ID = "EVTS.A-GOLE";
    public static final String SD_EVTS_AGOLE_NAME = "GLIF Automated GOLE Ethernet VLAN Transfer Service";
    public static final String SD_EVTS_AGOLE_TYPE = "http://services.ogf.org/nsi/2013/07/definitions/EVTS.A-GOLE";

    public static final String LOCAL_CLIENT_PORT = NSI_URN_OGF_NETWORK + "*" + NSI_URN_SEPARATOR + NSI_URN_DOMAIN_YEAR + NSI_URN_SEPARATOR;

    private static final ArrayList<Transform> nmwgTransforms = new ArrayList<>(
        Arrays.asList(
            new Transform("domain=", ""),
            new Transform("node=", ""),
            new Transform("port=", ""),
            new Transform("link=", "")
        )
    );

    private static final ArrayList<Transform> nsiTransforms = new ArrayList<>(
        Arrays.asList(
            new Transform("/", "_"),
            new Transform("*", "+")
        )
    );

    /**
     * @return the nmwgTransforms
     */
    public static ArrayList<Transform> getNmwgTransforms() {
        return nmwgTransforms;
    }

    /**
     * @return the nsiTransforms
     */
    public static ArrayList<Transform> getNsiTransforms() {
        return nsiTransforms;
    }
}
