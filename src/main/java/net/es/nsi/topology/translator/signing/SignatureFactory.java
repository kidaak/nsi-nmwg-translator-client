package net.es.nsi.topology.translator.signing;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.Collections;
import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

/**
 *
 * @author hacksaw
 */
public class SignatureFactory {
    private final Logger log = LoggerFactory.getLogger(SignatureFactory.class);
    private final XMLSignatureFactory fac;

    public SignatureFactory() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        // We are going to use the jsr105 provider to generate our digital signature.
        String providerName = System.getProperty("jsr105Provider", "org.jcp.xml.dsig.internal.dom.XMLDSigRI");

        // Create a DOM XMLSignatureFactory that will be used to
        // generate the enveloped signature.
        fac = XMLSignatureFactory.getInstance("DOM", (Provider) Class.forName(providerName).newInstance());
    }

    public Document generateEnvelopedSignature(Document doc, String alias) throws NoSuchAlgorithmException,
            InvalidAlgorithmParameterException, KeyException, javax.xml.crypto.MarshalException,
            XMLSignatureException, TransformerConfigurationException, TransformerException,
            KeyStoreException, IOException, CertificateException, UnrecoverableEntryException {

        // Create a Reference to the enveloped document (in this case,
        // you are signing the whole document, so a URI of "" signifies
        // that, and also specify the SHA1 digest algorithm and
        // the ENVELOPED Transform.
        Reference ref = fac.newReference("", fac.newDigestMethod(DigestMethod.SHA512, null), Collections.singletonList(fac.newTransform(Transform.ENVELOPED, (XMLStructure) null)), null, null);

        // Open the keystore and load the private key corresponding to alias.
        KeyStoreHandler keyStoreHandler = new KeyStoreHandler();
        KeyStore.PrivateKeyEntry keyEntry = keyStoreHandler.getPrivateKeyEntry(alias);

        // Determine the signature method we will use.
        String algorithm = keyEntry.getPrivateKey().getAlgorithm();
        String method = SignatureMethod.HMAC_SHA1;
        if (algorithm.equalsIgnoreCase("RSA")) {
            method = SignatureMethod.RSA_SHA1;
        }
        else if (algorithm.equalsIgnoreCase("DSA")) {
            method = SignatureMethod.DSA_SHA1;
        }

        // Create the SignedInfo.
        SignedInfo si = fac.newSignedInfo(fac.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE_WITH_COMMENTS, (C14NMethodParameterSpec) null), fac.newSignatureMethod(method, null), Collections.singletonList(ref));

        // Create a KeyInfo and add the KeyValue to it.
        KeyInfo ki = keyStoreHandler.getKeyInfo(alias, fac);

        // Create a DOMSignContext and set the signing Key to the DSA
        // PrivateKey and specify where the XMLSignature should be inserted
        // in the target document (in this case, the document root)
        DOMSignContext dsc = new DOMSignContext(keyEntry.getPrivateKey(), doc.getDocumentElement());

        // Marshal, generate (and sign) the eveloped XMLSignature. The DOM
        // Document will contain the XML Signature if this method returns
        // successfully.
        XMLSignature signature = fac.newXMLSignature(si, ki);
        signature.sign(dsc);

        // Validate document before returning.
        try {
            Validate.validateEnveloped(doc);
        }
        catch (Exception ex) {
            log.error("Failed to validate enveloped signature", ex);
        }

        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer trans = tf.newTransformer();
        trans.transform(new DOMSource(doc), new StreamResult(System.out));

        return doc;
    }

    public Document generateExternalSignature(Document dom, String alias)
            throws NoSuchAlgorithmException, InvalidAlgorithmParameterException,
            KeyException, javax.xml.crypto.MarshalException,
            XMLSignatureException, TransformerConfigurationException,
            TransformerException, ParserConfigurationException, KeyStoreException,
            FileNotFoundException, IOException, CertificateException,
            UnrecoverableEntryException {

        // Create a Reference to the enveloped document (in this case,
        // you are signing the whole document, so a URI of "" signifies
        // that, and also specify the SHA1 digest algorithm and
        // the ENVELOPED Transform.
        Reference ref = fac.newReference("http://www.w3.org/TR/xml-stylesheet", fac.newDigestMethod(DigestMethod.SHA512, null));

        // Open the keystore and load the private key corresponding to alias.
        KeyStoreHandler keyStoreHandler = new KeyStoreHandler();
        KeyStore.PrivateKeyEntry keyEntry = keyStoreHandler.getPrivateKeyEntry(alias);

        // Determine the signature method we will use.
        String algorithm = keyEntry.getPrivateKey().getAlgorithm();
        String method = SignatureMethod.HMAC_SHA1;
        if (algorithm.equalsIgnoreCase("RSA")) {
            method = SignatureMethod.RSA_SHA1;
        }
        else if (algorithm.equalsIgnoreCase("DSA")) {
            method = SignatureMethod.DSA_SHA1;
        }

        // Create the SignedInfo.
        SignedInfo si = fac.newSignedInfo(fac.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE_WITH_COMMENTS, (C14NMethodParameterSpec) null), fac.newSignatureMethod(method, null), Collections.singletonList(ref));

        // Create a KeyInfo and add the KeyValue to it.
        KeyInfo ki = keyStoreHandler.getKeyInfo(alias, fac);

        // Create the XMLSignature (but don't sign it yet)
        XMLSignature signature = fac.newXMLSignature(si, ki);

        // Create the Document that will hold the resulting XMLSignature
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true); // must be set
        dbf.setValidating(true);
        dbf.setExpandEntityReferences(true);
        dbf.setXIncludeAware(true);
        Document doc = dbf.newDocumentBuilder().newDocument();

        // Create a DOMSignContext and set the signing Key to the DSA
        // PrivateKey and specify where the XMLSignature should be inserted
        // in the target document (in this case, the document root)
        DOMSignContext signContext = new DOMSignContext(keyEntry.getPrivateKey(), doc);

        // Marshal, generate (and sign) the detached XMLSignature. The DOM
        // Document will contain the XML Signature if this method returns
        // successfully.
        signature.sign(signContext);

        return doc;
    }
}
