package net.es.nsi.topology.translator.writers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Optional;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import net.es.nsi.topology.translator.Configuration;
import net.es.nsi.topology.translator.http.RestClient;
import net.es.nsi.topology.translator.jaxb.NmlParser;
import net.es.nsi.topology.translator.jaxb.NsaParser;
import net.es.nsi.topology.translator.jaxb.configuration.SignatureType;
import net.es.nsi.topology.translator.jaxb.dds.DocumentType;
import net.es.nsi.topology.translator.jaxb.nml.NmlTopologyType;
import net.es.nsi.topology.translator.jaxb.nsa.NsaType;
import net.es.nsi.topology.translator.model.NsiConstants;
import net.es.nsi.topology.translator.signing.KeyStoreHandler;
import net.es.nsi.topology.translator.signing.SignatureFactory;
import net.es.nsi.topology.translator.utilities.DocumentBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

/**
 * Write documents to Document Distribution Service.
 *
 * @author hacksaw
 */
public class DdsWriter {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final net.es.nsi.topology.translator.jaxb.dds.ObjectFactory ddsFactory = new net.es.nsi.topology.translator.jaxb.dds.ObjectFactory();
    private final net.es.nsi.topology.translator.jaxb.nsa.ObjectFactory nsaFactory = new net.es.nsi.topology.translator.jaxb.nsa.ObjectFactory();
    private final net.es.nsi.topology.translator.jaxb.nml.ObjectFactory nmlFactory = new net.es.nsi.topology.translator.jaxb.nml.ObjectFactory();
    private final WebTarget dds;
    private Optional<String> alias = Optional.empty();
    private Optional<KeyStoreHandler> keyStoreHandler = Optional.empty();

    /**
     * Creates a DdsWriter for DDS at the specified URL using the provided
     * security credentials.
     *
     * @param url Root URL of the remote DDS.
     * @param config
     *
     * @throws KeyStoreException
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws KeyManagementException
     * @throws UnrecoverableKeyException
     */
    public DdsWriter(String url, Configuration config) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, KeyManagementException, UnrecoverableKeyException {
        // Get our REST client and set root URL for documents.
        RestClient restClient = new RestClient(config.getHttpsConfig());
        dds = restClient.get().target(url).path("documents");

        SignatureType signature = config.getSignature();
        if (signature.isSign()) {
            alias = Optional.of(signature.getAlias());
            keyStoreHandler = Optional.of(new KeyStoreHandler(signature.getKeyStore().getFile(), signature.getKeyStore().getPassword(), signature.getKeyStore().getType()));
        }
    }

    /**
     * Write the provided NSA Description document to the DDS.
     *
     * @param nsa NSA Description document to add to the DDS.
     *
     * @throws java.io.UnsupportedEncodingException If the name of the document (nsaId, type, nsaId) cannot be URL encoded.
     * @throws IOException If the document cannot be written to the DDS.
     */
    public void writeNsa(NsaType nsa) throws IllegalArgumentException, IOException {
        // Convert the JAXB NSA description document to DOM format.
        Optional<Document> doc;
        try {
            // Convert JAXB representation to DOM for signing and encoding.
            doc = Optional.of(NsaParser.getInstance().jaxb2Dom(nsaFactory.createNsa(nsa)));
        } catch (NullPointerException | JAXBException | ParserConfigurationException ex) {
            log.error("writeNsa: invalid NSA document", ex);
            throw new IllegalArgumentException(ex);
        }

        // Sign the document if configured to do so.
        // Generate an external signature on the document.
        Optional<Document> signature = Optional.empty();
        if (keyStoreHandler.isPresent()) {
            try {
                SignatureFactory signatureFactory;
                signatureFactory = new SignatureFactory();
                signature = Optional.of(signatureFactory.generateExternalSignature(doc.get(), alias.get()));
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchAlgorithmException | InvalidAlgorithmParameterException | KeyException | MarshalException | XMLSignatureException | TransformerException | ParserConfigurationException | KeyStoreException | FileNotFoundException | CertificateException | UnrecoverableEntryException ex) {
                log.error("build: invalid signature document", ex);
                throw new IllegalArgumentException(ex);
            }
        }

        // Create the document we want to add to DDS.
        DocumentBuilder db = new DocumentBuilder()
                .withNsaId(nsa.getId())
                .withType(NsiConstants.NSI_DOC_TYPE_NSA_V1)
                .withId(nsa.getId())
                .withVersion(nsa.getVersion())
                .withExpires(nsa.getExpires())
                .withContents(doc.get());

        // Add signature if required.
        if (signature.isPresent()) {
            db.withSignature(signature.get());
        }

        DocumentType document = db.build();

        // Write the document to the DDS.
        try {
            writeDocument(document);
        } catch (IOException ex) {
            log.error("writeNsa: could not write NSA document to DDS server", ex);
            throw ex;
        }
    }

    /**
     * Write the provided NML topology document to the DDS.
     *
     * @param nsaId The NSA identifier associated with the NML document.
     * @param nml The NML topology document to add to the DDS.
     *
     * @throws java.io.UnsupportedEncodingException If the name of the document (nsaId, type, networkId) cannot be URL encoded.
     * @throws IOException If the document cannot be written to the DDS.
     * @throws javax.xml.bind.JAXBException
     */
    public void writeTopology(String nsaId, NmlTopologyType nml) throws UnsupportedEncodingException, IOException, JAXBException {
        // Convert the JAXB NML Topology document to DOM format.
        Optional<Document> doc;
        try {
            doc = Optional.of(NmlParser.getInstance().jaxb2Dom(nmlFactory.createTopology(nml)));
        } catch (NullPointerException | JAXBException | ParserConfigurationException ex) {
            log.error("writeTopology: invalid NML topology document", ex);
            throw new IllegalArgumentException(ex);
        }

        // Sign the document if configured to do so.
        // Generate an external signature on the document.
        Optional<Document> signature = Optional.empty();
        if (keyStoreHandler.isPresent()) {
            try {
                SignatureFactory signatureFactory;
                signatureFactory = new SignatureFactory();
                signature = Optional.of(signatureFactory.generateExternalSignature(doc.get(), alias.get()));
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchAlgorithmException | InvalidAlgorithmParameterException | KeyException | MarshalException | XMLSignatureException | TransformerException | ParserConfigurationException | KeyStoreException | FileNotFoundException | CertificateException | UnrecoverableEntryException ex) {
                log.error("build: invalid signature document", ex);
                throw new IllegalArgumentException(ex);
            }
        }

        // Create the document we want to add to DDS.
        DocumentBuilder db = new DocumentBuilder()
                .withNsaId(nsaId)
                .withType(NsiConstants.NSI_DOC_TYPE_TOPOLOGY_V2)
                .withId(nml.getId())
                .withVersion(nml.getVersion())
                .withExpires(nml.getLifetime().getEnd())
                .withContents(doc.get());

        // Add signature if required.
        if (signature.isPresent()) {
            db.withSignature(signature.get());
        }

        DocumentType document = db.build();

        // Write the document to the DDS.
        try {
            writeDocument(document);
        } catch (IOException ex) {
            log.error("writeNsa: could not write NSA document to DDS server", ex);
            throw ex;
        }
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
        JAXBElement<DocumentType> request = ddsFactory.createDocument(document);

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
