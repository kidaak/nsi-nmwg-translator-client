package net.es.nsi.topology.translator;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import org.apache.commons.cli.ParseException;

import org.apache.log4j.xml.DOMConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The NMWG to NML translator client.
 * 
 * @author hacksaw
 */
public class Client {
    private static Logger log;

    /**
     * Configures the runtime and executes the main NMWG to NML translation workflow.
     * 
     * @param args 
     */
    public static void main(String[] args) {
        // Load the command line options into appropriate system properties.
        Options options;
        try {
            options = new Options(args);
        }
        catch (IOException | ParseException ex) {
            System.err.println("Exiting: Could not read options" + ex.getMessage());
            return;
        }

        // Configure Log4J and use logs from this point forward.
        try {
            DOMConfigurator.configureAndWatch(options.getLog4jConfig(), 45 * 1000);
            log = LoggerFactory.getLogger(Client.class);
        }
        catch (IOException ex) {
            System.err.println("Exiting: could not configure Log4J" + ex.getMessage());
            return;
        }        
        
        // Orchestrate the primary uPA NMWG to NML workflow.
        Orchestrator orchestrator = new Orchestrator(options.getBasedir(), options.getConfigfile());
        try {
            orchestrator.orchestrate();
        } catch (JAXBException | IOException | DatatypeConfigurationException | IllegalArgumentException | KeyStoreException | NoSuchAlgorithmException | CertificateException | KeyManagementException | UnrecoverableKeyException ex) {
            System.err.println("Exiting: Orchestration failed" + ex.getMessage());
        }
    }
}
