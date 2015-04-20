package net.es.nsi.topology.translator;

import com.google.common.base.Optional;
import net.es.nsi.topology.translator.utilities.PathBuilder;
import net.es.nsi.topology.translator.http.HttpsConfig;
import com.google.common.base.Strings;
import net.es.nsi.topology.translator.jaxb.JaxbParser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.bind.JAXBException;
import net.es.nsi.topology.translator.jaxb.configuration.ConfigurationType;
import net.es.nsi.topology.translator.jaxb.configuration.PeeringType;
import net.es.nsi.topology.translator.jaxb.configuration.ObjectFactory;
import net.es.nsi.topology.translator.jaxb.configuration.ServiceDefinitionType;
import net.es.nsi.topology.translator.jaxb.configuration.SourceType;
import net.es.nsi.topology.translator.model.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Models configuration information for the NMWG to NML orchestration.
 * 
 * @author hacksaw
 */
public class Configuration {
    private static final Logger log = LoggerFactory.getLogger(Configuration.class);
    
    // Contents of the configuration file.
    private final ConfigurationType conf;
    
    // Path of the NSA description file used to drive orchestration.
    private final String nsaFile;
    
    // Output files.
    private final String mappingFile;
    private final String topologyFile;
    
    // Runtime configuration of the HTTP client.
    private final HttpsConfig client;
    
    // Lifetime of the NSA description and NML topology files.  Lifetime is
    // generated based on now + lifeTime.
    private static final long DEFAULT_LIFETIME = 5184000L;
    private final long lifeTime;
    
    // The list of NSI service defintiions used to map NMWG links into NML
    // ports and SwitchingServices.
    List<ServiceDefinitionType> serviceDefinition;
    
    // isAlias information for overriding the default workflow generation for
    // NML port information.
    Map<String, PeeringType> peerings = new ConcurrentHashMap<>();
    
    // Parser for reading XML from configuration file.
    private final JaxbParser configParser;
    
    /**
     * Loads specified configuration file and validates values.
     * 
     * @param basedir All relative paths will be qualified under this base directory.
     * @param file File containing the configuration document.
     * @throws IOException Could not read the specified file.
     * @throws JAXBException Contents of the configuration file were not valid XML.
     */
    public Configuration(String basedir, String file) throws IOException, JAXBException {
        this.configParser = JaxbParser.getInstance();

        // Read the configuration file.
        conf = readConfiguration(file);
        
        // Normalize file paths.
        PathBuilder pb = new PathBuilder(basedir);
        try {
            nsaFile = pb.getRealPath(conf.getNsa());
            mappingFile = pb.getAbsolutePath(conf.getMapping());
            topologyFile = pb.getAbsolutePath(conf.getTopology());
        }
        catch (IOException ex) {
            log.error("Could not get absolute path for file " + ex.getMessage());
            throw ex;
        }
        
        // Normalize TLS security configuration.
        client = new HttpsConfig(basedir, conf.getClient());
        
        // Now parameters for converting NMWG to NML.
        lifeTime = (conf.getLifeTime() == 0) ? DEFAULT_LIFETIME : conf.getLifeTime();
        
        // Create default service definition if one is not provided.
        if (conf.getServiceDefintion().isEmpty()) {
            serviceDefinition = new ArrayList<>();
            ObjectFactory factory = new ObjectFactory();
            ServiceDefinitionType sd = factory.createServiceDefinitionType();
            sd.setId(Constants.SD_EVTS_AGOLE_ID);
            sd.setName(Constants.SD_EVTS_AGOLE_NAME);
            sd.setServiceType(Constants.SD_EVTS_AGOLE_TYPE);
            sd.setEncoding(Constants.NML_ETHERNET);
            sd.setLabelType(Constants.NML_ETHERNET_VLAN);
            sd.setLabelSwapping(false);
            serviceDefinition.add(sd);
        }
        else {
            serviceDefinition = conf.getServiceDefintion();
        }
        
        // Add specified peerings into HashMap for indexed access.
        for (PeeringType peer : conf.getPeering()) {
            if (!Strings.isNullOrEmpty(peer.getId())) {
                peerings.put(peer.getId(), peer);
            }
        }
    }
    
    /**
     * Read configuration file.
     * 
     * @param file Configuration file.
     * @return JAXB object representing configuration file contents.
     * @throws IOException Could not read the specified file.
     * @throws JAXBException Contents of the configuration file were not valid XML.
     */
    private ConfigurationType readConfiguration(String file) throws IOException, JAXBException {
        try {
            return configParser.parseFile(ConfigurationType.class, file);
        }
        catch (IOException io) {
            log.error("Failed to read configuration file " + file, io);
            throw io;
        }
        catch (JAXBException jaxb) {
            log.error("Invalid XML in file " + file, jaxb);
            throw jaxb;            
        }
    }
    
    /**
     * Get the NMWG/perfSONAR topology location information.
     * 
     * @return Source address of the NMWG/perfSONAR topology server.
     */
    public SourceType getNmwg() {
        return conf.getNmwg();
    }
    
    /**
     * Get the DDS server URL.
     * 
     * @return DDS server URL.
     */
    public String getDds() {
        return conf.getDds();
    }

    /**
     * Location of the file containing the NSA description document.
     * 
     * @return the nsaFile
     */
    public String getNsaFile() {
        return nsaFile;
    }

    /**
     * Location of the file for output of the nsa.json mapping.
     * 
     * @return the mappingFile
     */
    public String getMappingFile() {
        return mappingFile;
    }

    /**
     * Location of the file for output of the NML topology document.
     * 
     * @return the topologyFile
     */
    public String getTopologyFile() {
        return topologyFile;
    }

    /**
     * Get the service definition used to convert NMWG to NML.
     * 
     * @return List of service definitions.
     */
    public List<ServiceDefinitionType> getServiceDefintion() {
        return Collections.unmodifiableList(serviceDefinition);
    }
    
    /**
     * Get the isAlias port peering information for overwriting the default
     * isAlias value generation.
     * 
     * @return Map of peer elements indexed by NML port identifier.
     */
    public Map<String, PeeringType> getPeerings() {
        return  Collections.unmodifiableMap(peerings);
    }
            
    /**
     * Get the HTTPS client configuration information.
     * 
     * @return the client
     */
    public HttpsConfig getClient() {
        return client;
    }

    /**
     * Get the lifetime value for generating document expiry dates.
     * 
     * @return the lifeTime in seconds.
     */
    public long getLifeTime() {
        return lifeTime;
    }
    
    /**
     * Get the first service type specified in the service definition.
     * 
     * @return serviceType string identifier.
     */
    public Optional<String> getServiceType() {
        for (ServiceDefinitionType sd : serviceDefinition) {
            return Optional.fromNullable(sd.getServiceType());
        }
        
        return Optional.absent();
    }
}
