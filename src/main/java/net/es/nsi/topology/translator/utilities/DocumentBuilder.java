package net.es.nsi.topology.translator.utilities;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Supplier;
import javax.xml.datatype.XMLGregorianCalendar;
import net.es.nsi.topology.translator.http.Decoder;
import net.es.nsi.topology.translator.http.Encoder;
import net.es.nsi.topology.translator.jaxb.dds.ContentType;
import net.es.nsi.topology.translator.jaxb.dds.DocumentType;
import net.es.nsi.topology.translator.jaxb.dds.ObjectFactory;
import net.es.nsi.topology.translator.signing.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

/**
 *
 * @author hacksaw
 */
public class DocumentBuilder {
    public final static String ContentType = "application/x-gzip";
    public final static String ContentTransferEncoding = "base64";

    private final static Logger log = LoggerFactory.getLogger(DocumentBuilder.class);
    private final ObjectFactory factory = new ObjectFactory();

    protected Optional<String> nsaId = Optional.empty();
    protected Optional<String> type = Optional.empty();
    protected Optional<String> id = Optional.empty();
    protected Optional<String> href = Optional.empty();
    protected Optional<XMLGregorianCalendar> version = Optional.empty();
    protected Optional<XMLGregorianCalendar> expires = Optional.empty();
    protected Optional<Document> doc = Optional.empty();
    protected Optional<Document> sig = Optional.empty();

    public DocumentBuilder withNsaId(String nsaId) {
        this.nsaId = Optional.of(nsaId);
        return this;
    }

    public DocumentBuilder withType(String type) {
        this.type = Optional.of(type);
        return this;
    }

    public DocumentBuilder withId(String id) {
        this.id = Optional.of(id);
        return this;
    }

    public DocumentBuilder withVersion(XMLGregorianCalendar version) {
        this.version = Optional.of(version);
        return this;
    }

    public DocumentBuilder withExpires(XMLGregorianCalendar expires) {
        this.expires = Optional.of(expires);
        return this;
    }

    public DocumentBuilder withContents(Document doc) {
        this.doc = Optional.of(doc);
        return this;
    }

    public DocumentBuilder withSignature(Document sig) {
        this.sig = Optional.of(sig);
        return this;
    }

    private static class ExceptionSupplier implements Supplier<IllegalArgumentException>
    {
        private Optional<String> error = Optional.empty();

        public ExceptionSupplier(String error) {
            this.error = Optional.ofNullable(error);
        }

        public ExceptionSupplier() {
        }

        @Override
        public IllegalArgumentException get()
        {
            return new IllegalArgumentException(error.orElse("Required member data missing"));

        }
    }

    public DocumentType build() throws IllegalArgumentException, IOException {
        // Create the document we want to add to DDS.
        DocumentType document = factory.createDocumentType();

        // Populate document meta-data based on NSA document.
        document.setNsa(nsaId.orElseThrow(new ExceptionSupplier("document nsaId required")));
        document.setType(type.orElseThrow(new ExceptionSupplier("document type required")));
        document.setId(id.orElseThrow(new ExceptionSupplier("document id required")));
        document.setVersion(version.orElseThrow(new ExceptionSupplier("document version required")));
        document.setExpires(expires.orElseThrow(new ExceptionSupplier("document expires time required")));

        // Encode the DOM document and stick it in contents.
        String docEncoded = Encoder.encode(doc.orElseThrow(new ExceptionSupplier("document contents required")));
        ContentType contentHolder = factory.createContentType();
        contentHolder.setValue(docEncoded);
        contentHolder.setContentType(ContentType);
        contentHolder.setContentTransferEncoding(ContentTransferEncoding);
        document.setContent(contentHolder);

        if (sig.isPresent()) {
            try {
                if (!Validate.validateExternal(doc.get(), sig.get())) {
                    log.error("Failed to validate signature.");
                }
            } catch (Exception ex) {
                log.error("Failed to validate signature.", ex);
            }

            String sigEncoded = Encoder.encode(sig.get());
            ContentType sigHolder = factory.createContentType();
            sigHolder.setValue(sigEncoded);
            sigHolder.setContentType(ContentType);
            sigHolder.setContentTransferEncoding(ContentTransferEncoding);
            document.setSignature(sigHolder);
        }

        return document;
    }

    public static Document verify(DocumentType document) throws IllegalArgumentException, IOException {
        log.debug("Verifying document nsaId={}, type={}, id={}", document.getNsa(), document.getType(), document.getId());

        // Get the document contents.
        ContentType contents = document.getContent();
        if (contents == null || contents.getValue().isEmpty()) {
            throw new IllegalArgumentException("verify: No document contents present");
        }

        // Decode the document contents.
        Document contentsDecoded = Decoder.decode(contents.getValue());

        // Get signature if present.
        ContentType signature = document.getSignature();
        if (signature != null && !signature.getValue().isEmpty()) {
            // Validate signature.
            Document signatureDecoded = Decoder.decode(signature.getValue());

            boolean valid = false;
            try {
                valid = Validate.validateExternal(contentsDecoded, signatureDecoded);
            } catch (Exception ex) {
                log.error("validateExternal: failed to validate document", ex);
            }

            if (!valid) {
                throw new SecurityException("verify: signature for document invalid");
            }
        }

        return contentsDecoded;
    }
}
