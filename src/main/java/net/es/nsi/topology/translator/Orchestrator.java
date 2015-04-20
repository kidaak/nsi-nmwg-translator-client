package net.es.nsi.topology.translator;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import net.es.nsi.topology.translator.jaxb.dds.NmlTopologyType;
import net.es.nsi.topology.translator.model.NmwgTopology;
import net.es.nsi.topology.translator.model.NsaDocument;
import net.es.nsi.topology.translator.utilities.NsiUtilities;
import net.es.nsi.topology.translator.writers.DdsWriter;
import net.es.nsi.topology.translator.writers.MappingWriter;
import net.es.nsi.topology.translator.writers.NmlWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the NMWG to NML translation workflow including creation of target
 * output files and writing of NSA description and NML topology files to 
 * associated DDS service.
 * 
 * @author hacksaw
 */
public class Orchestrator {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final String baseDir;
    private final String configFile;
    
    /**
     * Create an orchestrator using the provided configuration.
     * 
     * @param baseDir Base directory for relative file name in configFile and defaults.
     * @param configFile Runtime configuration for the orchestrator.
     */
    public Orchestrator(String baseDir, String configFile) {
        this.baseDir = baseDir;
        this.configFile = configFile;
    }

    /**
     * Invoke the workflow orchestration.
     * 
     * @throws JAXBException
     * @throws IOException
     * @throws DatatypeConfigurationException
     * @throws IllegalArgumentException
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws KeyManagementException
     * @throws UnrecoverableKeyException 
     */
    public void orchestrate() throws JAXBException, IOException, DatatypeConfigurationException, IllegalArgumentException, KeyStoreException, NoSuchAlgorithmException, CertificateException, KeyManagementException, UnrecoverableKeyException {
        // Read the configuration file.
        Configuration conf;
        try {
            conf = new Configuration(baseDir, configFile);
        }
        catch (JAXBException | IOException ex) {
            log.error("Could not read configuration.", ex);
            throw ex;
        }
        
        // Read the NSA description file.
        NsaDocument document;
        try {
            document = new NsaDocument(conf.getNsaFile(), conf.getLifeTime());
        }
        catch (JAXBException | IOException | DatatypeConfigurationException ex) {
            log.error("Could not read NSA description document.", ex);
            throw ex;
        }

        // Write NSA description document update to DDS if required.
        DdsWriter dds = null;
        if (!Strings.isNullOrEmpty(conf.getDds())) {
            try {
                dds = new DdsWriter(conf.getDds(), conf.getClient());
                dds.writeNsa(document.getDocument());
            }
            catch (IllegalArgumentException | KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException | KeyManagementException | UnrecoverableKeyException ex) {
                log.error("Exiting: Could not process NSA description file " + document.getDocument().getId(), ex);
                throw ex;
            }
        }    
                
        // The nsi-bridge can only support a single networkId so make sure we
        // do not have multiple identifiers assigned in the NSA description file.
        String networkId;
        try {
            networkId = document.getNetworkId();
        }
        catch (IllegalArgumentException ex) {
            log.error("Exiting: " + ex.getMessage());
            throw ex;
        }

        try {            
            // Get the NMWG topology for the specified domain.
            NmwgTopology nmwg = new NmwgTopology(conf.getNmwg(), conf.getClient(), NsiUtilities.getNsiDomainName(networkId));
            nmwg.process();

            // Convert NMWG to NML topology.
            NmlTopologyType nml = nmwg.getNmlTopology(conf.getLifeTime(), conf.getServiceDefintion(), conf.getPeerings());

            // Write NML topology to local file is required.
            if (!Strings.isNullOrEmpty(conf.getTopologyFile())) {
                NmlWriter nmlWriter = new NmlWriter(conf.getTopologyFile());
                nmlWriter.writeFile(nml);
            }

            // Write topology update to DDS if required.
            if (dds != null) {
                dds.writeTopology(document.getDocument().getId(), nml);
            }

            // Write the nsi-bridge nsa.json file (this is very hacky).
            if (!Strings.isNullOrEmpty(conf.getMappingFile())) {
                // Get the highest supported version of the CS protocol defined
                // in the NSA description file.
                Optional<String> version = document.getProviderVersion();
                if (!version.isPresent()) {
                    String error = "No connection service version defined in NSA description file " + document.getDocument().getId();
                    log.error(error);
                    throw new IllegalArgumentException(error);
                }

                // Get a single serviceType from the configuration information.
                // This will change when multiple services are supported in the
                // nsi-bridge.
                Optional<String> serviceType = conf.getServiceType();
                if (!serviceType.isPresent()) {
                    String error = "No serviceTypes defined " + document.getDocument().getId();
                    log.error(error);
                    throw new IllegalArgumentException(error);
                }
                
                MappingWriter mw = new MappingWriter(conf.getMappingFile());
                mw.write(document.getDocument().getId(), version.get(),
                        serviceType.get(), networkId, nmwg.getMappings());
            }            
        }
        catch (DatatypeConfigurationException | JAXBException | IllegalArgumentException | KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException | KeyManagementException | UnrecoverableKeyException ex) {
            log.error("Could not process topology for networkId " + networkId, ex);
            throw ex;
        }      
    }
}
