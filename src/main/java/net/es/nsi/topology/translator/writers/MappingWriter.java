package net.es.nsi.topology.translator.writers;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.es.nsi.topology.translator.gson.JsonProxy;
import net.es.nsi.topology.translator.gson.LocalType;
import net.es.nsi.topology.translator.gson.StpType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A JSON writer for the OSCARS nsa.json file.
 * 
 * @author hacksaw
 */
public class MappingWriter {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final String file;
    
    /**
     * Constructor creating a writer for the specified file.
     * 
     * @param file File name to write the JSON serialized string.
     */
    public MappingWriter(String file) {
        this.file = file;
    }

    /**
     * Write the specified parameters as a JSON string into the nsa.json file.
     * 
     * @param nsaId NSA identifier of the nsi-bridge instance.
     * @param protocolVersion Supported protocol version of the nsi-bridge instance.
     * @param serviceType Supported service type of the nsi-bridge instance.
     * @param networkId Supported network identifier of the nsi-bridge instance.
     * @param mappings The list of NSI to OSCARS identifiers.
     * @throws IOException File could not be written.
     */
    public void write(String nsaId, String protocolVersion, String serviceType,
            String networkId, List<StpType> mappings) throws IOException {
        
        LocalType local = new LocalType();
        local.setNsaId(nsaId);
        local.setProtocolVersion(protocolVersion);
        local.setServiceType(serviceType);
        local.setNetworkId(networkId);
        local.setStps(mappings);
        
        Map<String, LocalType> json = new HashMap<>();
        json.put("local", local);
        JsonProxy proxy = new JsonProxy();
        try {     
            proxy.write(file, json);
        } catch (IOException ex) {
            log.error("Could not write json file " + file, ex);
            throw ex;
        }       
    }
    
}
