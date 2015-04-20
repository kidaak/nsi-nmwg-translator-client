package net.es.nsi.topology.translator.http;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import javax.net.ssl.SSLContext;
import net.es.nsi.topology.translator.utilities.PathBuilder;
import net.es.nsi.topology.translator.Properties;
import net.es.nsi.topology.translator.jaxb.configuration.KeyStoreType;
import net.es.nsi.topology.translator.jaxb.configuration.ObjectFactory;
import net.es.nsi.topology.translator.jaxb.configuration.SecureType;
import org.glassfish.jersey.SslConfigurator;

/**
 * HTTPs configuration information for the REST Jersey client.
 * 
 * @author hacksaw
 */
public class HttpsConfig {
    private final ObjectFactory factory = new ObjectFactory();
    private final SecureType config;

    /**
     * 
     * @param basedir
     * @param config
     * @throws IOException 
     */
    public HttpsConfig(String basedir, SecureType config) throws IOException {
        if (config == null) {
            throw new IllegalArgumentException("HttpConfig: server configuration not provided");
        }

        // Determine the keystore configuration.
        KeyStoreType keyStore = config.getKeyStore();
        if (keyStore == null) {
            // Check to see if the keystore was provided on the commandline.
            keyStore = factory.createKeyStoreType();
            keyStore.setFile(System.getProperty(Properties.SYSTEM_PROPERTY_SSL_KEYSTORE, Properties.DEFAULT_SSL_KEYSTORE));
            keyStore.setPassword(System.getProperty(Properties.SYSTEM_PROPERTY_SSL_KEYSTORE_PASSWORD, Properties.DEFAULT_SSL_KEYSTORE_PASSWORD));
            keyStore.setType(System.getProperty(Properties.SYSTEM_PROPERTY_SSL_KEYSTORE_TYPE, Properties.DEFAULT_SSL_KEYSTORE_TYPE));
        }

        PathBuilder pb = new PathBuilder(basedir);
        
        keyStore.setFile(pb.getRealPath(keyStore.getFile()));

        KeyStoreType trustStore = config.getTrustStore();
        if (trustStore == null) {
            trustStore = factory.createKeyStoreType();
            trustStore.setFile(System.getProperty(Properties.SYSTEM_PROPERTY_SSL_TRUSTSTORE, Properties.DEFAULT_SSL_TRUSTSTORE));
            trustStore.setPassword(System.getProperty(Properties.SYSTEM_PROPERTY_SSL_TRUSTSTORE_PASSWORD, Properties.DEFAULT_SSL_TRUSTSTORE_PASSWORD));
            trustStore.setType(System.getProperty(Properties.SYSTEM_PROPERTY_SSL_TRUSTSTORE_TYPE, Properties.DEFAULT_SSL_TRUSTSTORE_TYPE));
        }

        trustStore.setFile(pb.getRealPath(trustStore.getFile()));

        this.config = config;
    }

    /**
     * 
     * @return
     * @throws KeyStoreException
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws KeyManagementException
     * @throws UnrecoverableKeyException 
     */
    public SSLContext getSSLContext() throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, KeyManagementException, UnrecoverableKeyException {
        SslConfigurator sslConfig = SslConfigurator.newInstance()
            .trustStoreFile(config.getTrustStore().getFile())
            .trustStorePassword(config.getTrustStore().getPassword())
            .trustStoreType(config.getTrustStore().getType())
            .keyStoreFile(config.getKeyStore().getFile())
            .keyPassword(config.getKeyStore().getPassword())
            .keyStoreType(config.getKeyStore().getType())
            .securityProtocol("TLS");
        return sslConfig.createSSLContext();
    }

    /**
     * 
     * @return 
     */
    public boolean isProduction() {
        return config.isProduction();
    }
}
