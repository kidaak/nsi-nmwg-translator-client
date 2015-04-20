package net.es.nsi.topology.translator.writers;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;
import net.es.nsi.topology.translator.http.HttpsConfig;
import net.es.nsi.topology.translator.http.RestClient;
import net.es.nsi.topology.translator.jaxb.dds.AnyType;
import net.es.nsi.topology.translator.jaxb.dds.DocumentType;
import net.es.nsi.topology.translator.jaxb.dds.NsaType;
import net.es.nsi.topology.translator.jaxb.dds.NmlTopologyType;
import net.es.nsi.topology.translator.jaxb.dds.ObjectFactory;
import net.es.nsi.topology.translator.model.NsiConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Write documents to Document Distribution Service.
 * 
 * @author hacksaw
 */
public class DdsWriter {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final ObjectFactory factory = new ObjectFactory();
    private final WebTarget dds;
    
    /**
     * Creates a DdsWriter for DDS at the specified URL using the provided
     * security credentials.
     * 
     * @param url Root URL of the remote DDS.
     * @param secure HTTPS configuration parameters for TLS session with DDS.
     * 
     * @throws KeyStoreException
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws KeyManagementException
     * @throws UnrecoverableKeyException 
     */
    public DdsWriter(String url, HttpsConfig secure) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, KeyManagementException, UnrecoverableKeyException {     
        // Get our REST client and set root URL for documents.
        RestClient restClient = new RestClient(secure);
        dds = restClient.get().target(url).path("documents");
    }
    
    /**
     * Write the provided NSA Description document to the DDS.
     * 
     * @param nsa NSA Description document to add to the DDS.
     * 
     * @throws java.io.UnsupportedEncodingException If the name of the document (nsaId, type, nsaId) cannot be URL encoded.
     * @throws IOException If the document cannot be written to the DDS.
     */
    public void writeNsa(NsaType nsa) throws UnsupportedEncodingException, IOException {
        // Create the document we want to add to DDS.
        DocumentType document = factory.createDocumentType();
        
        // Populate document meta-data based on NSA document.
        document.setNsa(nsa.getId());
        document.setType(NsiConstants.NSI_DOC_TYPE_NSA_V1);
        document.setId(nsa.getId());
        document.setVersion(nsa.getVersion());
        document.setExpires(nsa.getExpires());

        // Add the NSA document into the content element.
        AnyType anyType = factory.createAnyType();
        anyType.getAny().add(factory.createNsa(nsa));
        document.setContent(anyType);
 
        // Write the document to the DDS.
        writeDocument(document);
    }
    
    /**
     * Write the provided NML topology document to the DDS.
     * 
     * @param nsaId The NSA identifier associated with the NML document.
     * @param nml The NML topology document to add to the DDS.
     * 
     * @throws java.io.UnsupportedEncodingException If the name of the document (nsaId, type, networkId) cannot be URL encoded.
     * @throws IOException If the document cannot be written to the DDS.
     */
    public void writeTopology(String nsaId, NmlTopologyType nml) throws UnsupportedEncodingException, IOException {
        // Create the document we want to add to DDS.
        DocumentType document = factory.createDocumentType();
        
        // Populate document meta-data based on NML document.
        document.setNsa(nsaId);
        document.setType(NsiConstants.NSI_DOC_TYPE_TOPOLOGY_V2);
        document.setId(nml.getId());
        document.setVersion(nml.getVersion());
        document.setExpires(nml.getLifetime().getEnd());

        // Add the NML document into the content element.
        AnyType anyType = factory.createAnyType();
        anyType.getAny().add(factory.createTopology(nml));
        document.setContent(anyType);

        // Write the document to the DDS.
        writeDocument(document);
    }
    
    /**
     * Write the specified document to the DDS using RESTful API.
     * 
     * @param document The DDS meta-data wrapped document to write to DDS. 
     * @throws UnsupportedEncodingException If the name of the document (nsaId, type, documentId) cannot be URL encoded.
     * @throws IOException If the document cannot be written to the DDS.
     */
    private void writeDocument(DocumentType document) throws UnsupportedEncodingException, IOException {
        // Wrap the provided DDS document in a JAXB element for sending.
        JAXBElement<DocumentType> request = factory.createDocument(document);
        
        // Build the full document path based on identifiers.
        WebTarget path;
        try {
            path = dds.path(URLEncoder.encode(document.getNsa().trim(), "UTF-8"))
                      .path(URLEncoder.encode(document.getType().trim(), "UTF-8"))
                      .path(URLEncoder.encode(document.getId().trim(), "UTF-8"));
        } catch (UnsupportedEncodingException ex) {
            log.error("Could not format URL " + document.getNsa() + "/" + document.getType() + "/" + document.getId(), ex);
            throw ex;
        }
        
        // Determine of the NSA document already exists.
        Response response = path.request(NsiConstants.NSI_DDS_V1_XML).get();
        int status = response.getStatus();
        response.close();
        
        // If the document exists we modify it through PUT, otherwise we use POST.
        if (Response.Status.OK.getStatusCode() == status) {
            // Document already exists so we will need to modify it.
            Response result = path.request(NsiConstants.NSI_DDS_V1_XML).put(Entity.entity(new GenericEntity<JAXBElement<DocumentType>>(request) {}, NsiConstants.NSI_DDS_V1_XML));
            status = result.getStatus();
            result.close();
            if (Response.Status.OK.getStatusCode() != status) {
                throw new IOException(error("PUT", status, path.getUri().toASCIIString()));
            }
        }
        else if (Response.Status.NOT_FOUND.getStatusCode() == response.getStatus()) {
            // Document does not exist so we will need to add it.  We POST the
            // new document under "/documents" and not the full path of the
            // document.
            Response result = dds.request(NsiConstants.NSI_DDS_V1_XML).post(Entity.entity(new GenericEntity<JAXBElement<DocumentType>>(request) {}, NsiConstants.NSI_DDS_V1_XML));
            status = result.getStatus();
            result.close();
            if (Response.Status.CREATED.getStatusCode() != status) {
                throw new IOException(error("POST", status, path.getUri().toASCIIString()));
            }
        }
        else {
            throw new IOException(error("GET", status, path.getUri().toASCIIString()));
        }
    }

    /**
     * Format error string for exceptions and log.
     * 
     * @param op The HTTP operation being performed.
     * @param code The result code of the operation.
     * @param url The URL being accessed during the operation.
     * @return The formatted error string.
     */
    private String error(String op, int code, String url) {
        StringBuilder sb = new StringBuilder("Error accessing DDS service, op=");
        sb.append(op);
        sb.append(", code=");
        sb.append(code);
        sb.append(", url=");
        sb.append(url);
        log.error(sb.toString());
        return sb.toString();
    }
}
