package net.es.nsi.topology.translator.http;

import net.es.nsi.topology.translator.model.NsiConstants;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;
import net.es.nsi.topology.translator.Properties;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.apache.connector.ApacheClientProperties;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.RequestEntityProcessing;
import org.glassfish.jersey.filter.LoggingFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple client wrapper around Jersey providing needed configuration for
 * Apache and SSL.
 * 
 * @author hacksaw
 */
public class RestClient {
    private final static Logger log = LoggerFactory.getLogger(RestClient.class);
    private final Client client;
    private final ClientConfig clientConfig;
    
    /**
     * Create a simple RESTful client with no SSL configuration.
     */
    public RestClient() {
        clientConfig = configureClient();
        client = ClientBuilder.newBuilder().withConfig(clientConfig).build();
    }

    /**
     * Create a simple RESTful client with the provided SSL configuration.
     * 
     * @param config The SSL configuration for the RESTful client.
     * 
     * @throws KeyStoreException
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws KeyManagementException
     * @throws UnrecoverableKeyException 
     */
    public RestClient(HttpsConfig config) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, KeyManagementException, UnrecoverableKeyException {
        clientConfig = configureSecureClient(config);
        client = ClientBuilder.newBuilder().withConfig(clientConfig).build();
    }

    /**
     * Create a client configuration for a secure Jersey client backed by Apache.
     * 
     * @param config The SSL configuration information.
     * @return The secure Jersey client configuration.
     * 
     * @throws KeyStoreException
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws KeyManagementException
     * @throws UnrecoverableKeyException 
     */
    public static ClientConfig configureSecureClient(HttpsConfig config) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, KeyManagementException, UnrecoverableKeyException {
        HostnameVerifier hostnameVerifier;
        if (config.isProduction()) {
            hostnameVerifier = new DefaultHostnameVerifier();
        }
        else {
            hostnameVerifier = new NoopHostnameVerifier();
        }

        SSLContext sslContext;
        try {
            sslContext = config.getSSLContext();
        }
        catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException | KeyManagementException | UnrecoverableKeyException ex) {
            log.error("configureSecureClient: failed to create SSL context.", ex);
            throw ex;
        }
        
        LayeredConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(
                sslContext, hostnameVerifier);

        final Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", sslSocketFactory)
                .build();

        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(registry);
        return getClientConfig(connectionManager);
    }

    /**
     * Create a basic client configuration for a Jersey client backed by Apache.
     * @return The specific Jersey client configuration.
     */
    public static ClientConfig configureClient() {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        return getClientConfig(connectionManager);
    }

    /**
     * Configure Jersey and Apache specific client parameters.
     * 
     * @param connectionManager The configured connection manager that will be used by Jersey.
     * @return The Jersey client configuration.
     */
    public static ClientConfig getClientConfig(PoolingHttpClientConnectionManager connectionManager) {
        ClientConfig clientConfig = new ClientConfig();

        // We want to use the Apache connector for chunk POST support.
        clientConfig.connectorProvider(new ApacheConnectorProvider());
        connectionManager.setDefaultMaxPerRoute(20);
        connectionManager.setMaxTotal(80);
        clientConfig.property(ApacheClientProperties.CONNECTION_MANAGER, connectionManager);

        clientConfig.register(GZipEncoder.class);
        //clientConfig.register(new MoxyXmlFeature());
        //clientConfig.register(FollowRedirectFilter.class);
        clientConfig.property(ClientProperties.REQUEST_ENTITY_PROCESSING, RequestEntityProcessing.CHUNKED);

        boolean debug = Boolean.parseBoolean(System.getProperty(Properties.SYSTEM_PROPERTY_DEBUG));
        if (debug) {
            clientConfig.register(new LoggingFilter(java.util.logging.Logger.getGlobal(), true));
        }

        // Apache specific configuration.
        RequestConfig.Builder custom = RequestConfig.custom();
        custom.setExpectContinueEnabled(true);
        custom.setRelativeRedirectsAllowed(true);
        custom.setRedirectsEnabled(true);
        clientConfig.property(ApacheClientProperties.REQUEST_CONFIG, custom.build());

        return clientConfig;
    }

    /**
     * Get the configured Jersey HTTP client.
     * @return The Jersey Client.
     */
    public Client get() {
        return client;
    }

    /**
     * Close the Jersey HTTP client.  This specific instance can no longer be used
     * after this method has been invoked.
     */
    public void close() {
        client.close();
    }

    /**
     * The current configuration for the Jersey client.
     * @return the clientConfig
     */
    public ClientConfig getClientConfig() {
        return clientConfig;
    }

    /**
     * A client redirect filter allowing Jersey to follow HTTP redirects
     * from the remote server.  This is required if the client provider is
     * switched from Apache back to the default HttpConnectionUrl.
     */
    private static class FollowRedirectFilter implements ClientResponseFilter
    {
        private final static Logger log = LoggerFactory.getLogger(FollowRedirectFilter.class);

        @Override
        public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException
        {
            if (requestContext == null || responseContext == null || responseContext.getStatus() != Response.Status.FOUND.getStatusCode()) {
               return;
            }

            log.debug("Processing redirect for " + requestContext.getMethod() + " " + requestContext.getUri().toASCIIString() + " to " + responseContext.getLocation().toASCIIString());

            Client inClient = requestContext.getClient();
            Object entity = requestContext.getEntity();
            MultivaluedMap<String, Object> headers = requestContext.getHeaders();
            String method = requestContext.getMethod();
            Response resp;
            if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) {
                resp = inClient.target(responseContext.getLocation()).request(requestContext.getMediaType()).headers(headers).method(requestContext.getMethod(), Entity.entity(new GenericEntity<JAXBElement<?>>((JAXBElement<?>)entity) {}, NsiConstants.NSI_DDS_V1_XML));
            }
            else {
                resp = inClient.target(responseContext.getLocation()).request(requestContext.getMediaType()).headers(headers).method(requestContext.getMethod());
            }

            responseContext.setEntityStream((InputStream) resp.getEntity());
            responseContext.setStatusInfo(resp.getStatusInfo());
            responseContext.setStatus(resp.getStatus());
            responseContext.getHeaders().putAll(resp.getStringHeaders());

            log.debug("Processing redirect with result " + resp.getStatus());
        }
    }
}
