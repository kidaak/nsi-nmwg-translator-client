package net.es.nsi.topology.translator.jaxb;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

/**
 * A singleton to load the very expensive JAXBContext once.
 *
 * @author hacksaw
 */
public class JaxbParser {
    // Get a logger just in case we encounter a problem.
    private final Logger log = LoggerFactory.getLogger(getClass());
    private static JAXBContext jc;
    private static Marshaller marshaller;
    private static Unmarshaller unmarshaller;
    
    private static final String PACKAGES = "net.es.nsi.topology.translator.jaxb.configuration:net.es.nsi.topology.translator.jaxb.nmwg:net.es.nsi.topology.translator.jaxb.dds";

    /**
     * Private constructor loads the JAXB context once and prevents
     * instantiation from other classes.
     * 
     * @param packages
     * @throws javax.xml.bind.JAXBException
     */
    private JaxbParser() {
        try {
            // Load a JAXB context.
            jc = JAXBContext.newInstance(PACKAGES);
            marshaller = jc.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            unmarshaller = jc.createUnmarshaller();
        }
        catch (JAXBException jaxb) {
            log.error("JaxbParser: Failed to load JAXB instance", jaxb);
        }
    }
    
    /**
     * An internal static class that invokes our private constructor on object
     * creation.
     */
    private static class JaxbParserHolder {
        public static final JaxbParser INSTANCE = new JaxbParser();
    }

    /**
     * Returns an instance of this singleton class.
     *
     * @return An NmlParser object of the NSAType.
     */
    public static JaxbParser getInstance() {
            return JaxbParserHolder.INSTANCE;
    }

    /**
     * Parse the given file into the specified JAXB object.
     * 
     * @param <T> Target type of the JAXB object.
     * @param xmlClass Target class of the JAXB object.
     * @param file The file containing the XML document.
     * @return The target JAXB object.
     * @throws JAXBException Could not parse the specified XML document.
     * @throws IOException Could not open the specified file.
     */
    public <T extends Object> T parseFile(Class<T> xmlClass, String file) throws JAXBException, IOException {
        // Parse the specified file.
        try (FileInputStream fileInputStream = new FileInputStream(file); BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream)) {
            return xml2Jaxb(xmlClass, bufferedInputStream);
        }
    }
    
    /**
     * Write the JAXB object to the specified file.
     * 
     * @param jaxbElement The JAXB element to write to file.
     * @param file The filename of the target file.
     * @throws JAXBException Could not convert the JAXB object to an XML document.
     * @throws IOException The specified file could not be created.
     */
    public void writeFile(JAXBElement<?> jaxbElement, String file) throws JAXBException, IOException {
        // Parse the specified file.
        try (FileOutputStream fileOutputStream = new FileOutputStream(file); BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream)) {
            marshaller.marshal(jaxbElement, fileOutputStream);
        }
    }

    /**
     * Convert the specified JAXB object to a DOM object.
     * 
     * @param jaxbElement JAXB object to convert.
     * @return The converted DOM object.
     * @throws JAXBException JAXB object could not be converted to DOM object.
     * @throws ParserConfigurationException Could not create a DOM document builder.
     */
    public Document jaxb2Dom(JAXBElement<?> jaxbElement) throws JAXBException, ParserConfigurationException {
        // Convert JAXB representation to DOM.
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.newDocument();
        marshaller.marshal(jaxbElement, doc);
        return doc;
    }

    /**
     * Covert specified DOM object to JAXB.
     * 
     * @param doc The DOM object to convert.
     * @return The converted JAXB object.
     * @throws JAXBException JAXBException JAXB object could not be created.
     */
    public JAXBElement<?> dom2Jaxb(Document doc) throws JAXBException {
        return (JAXBElement<?>) unmarshaller.unmarshal(doc);
    }

    /**
     * Convert the specified JAXB object to a string.
     * 
     * @param jaxbElement JAXB object to convert.
     * @return A serialized XML string.
     * @throws JAXBException JAXB object could not be created.
     */
    public String jaxb2Xml(JAXBElement<?> jaxbElement) throws JAXBException {
        // We will write the XML encoding into a string.
        StringWriter writer = new StringWriter();
        String result;
        try {
            marshaller.marshal(jaxbElement, writer);
            result = writer.toString();
        } catch (JAXBException ex) {
            // Something went wrong so get out of here.
            log.error("Failed to serialize JAXB structure.", ex);
            throw ex;
        }
        finally {
            try { writer.close(); } catch (IOException ex) {}
        }

        // Return the XML string.
        return result;
    }
    
    /**
     * Convert the specified string representation of a XML document to a JAXB object.
     * 
     * @param <T> Target type of the JAXB object.
     * @param xmlClass The target class of the JAXB object.
     * @param xml The string containing the XML document to convert.
     * @return JAXB object representing the XML document.
     * @throws JAXBException Could not parse the specified XML document.
     */
    @SuppressWarnings("unchecked")
    public <T extends Object> T xml2Jaxb(Class<T> xmlClass, String xml) throws JAXBException {
        JAXBElement<T> element;
        try (StringReader reader = new StringReader(xml)) {
            element = (JAXBElement<T>) unmarshaller.unmarshal(reader);
        }
        return element.getValue();
    }

    /**
     * Convert the XML document contained in the InputStream to a JAXB object.
     * 
     * @param <T> Target type of the JAXB object.
     * @param xmlClass The target class of the JAXB object.
     * @param is InputStream containing the XML document.
     * @return JAXB object representing the XML document.
     * @throws JAXBException Could not parse the specified XML document.
     * @throws IOException InputStream could not be read.
     */
    @SuppressWarnings("unchecked")
    public <T extends Object> T xml2Jaxb(Class<T> xmlClass, InputStream is) throws JAXBException, IOException {
        JAXBElement<T> element = (JAXBElement<T>) unmarshaller.unmarshal(getReader(is));
        if (element.getDeclaredType() == xmlClass) {
            return element.getValue();
        }

        throw new JAXBException("Expected XML for class " + xmlClass.getCanonicalName() + " but found " + element.getDeclaredType().getCanonicalName());
    }

    /**
     * Convert the XML document contained in the BufferedInputStream to a JAXB object.
     * 
     * @param <T> Target type of the JAXB object.
     * @param xmlClass The target class of the JAXB object.
     * @param is BufferedInputStream containing the XML document.
     * @return JAXB object representing the XML document.
     * @throws JAXBException Could not parse the specified XML document.
     * @throws IOException BufferedInputStream could not be read.
     */
    @SuppressWarnings("unchecked")
    public <T extends Object> T xml2Jaxb(Class<T> xmlClass, BufferedInputStream is) throws JAXBException, IOException {
        JAXBElement<T> element = (JAXBElement<T>) unmarshaller.unmarshal(is);
        if (element.getDeclaredType() == xmlClass) {
            return element.getValue();
        }

        throw new JAXBException("Expected XML for class " + xmlClass.getCanonicalName() + " but found " + element.getDeclaredType().getCanonicalName());
    }

    // Size of the lookahead buffer for reader.
    private final static int LOOKAHEAD = 1024;

    /**
     * The InputStream reader is a workaround to a Grizzly bug on chunked POST
     * operations that leaves the first chunked length in the character stream.
     * This reader will parse out the length value if present and return a
     * Reader with the stream set to the first occurrence of the characters
     * "<?" within the stream.  Will no longer be needed in Jersey 2.19 that
     * now contains the fix.
     * 
     * @param is InputStream to parse for XML document.
     * @return A Reader for the InputStream.
     * @throws IOException Could not read the stream.
     */
    private Reader getReader(InputStream is) throws IOException {
        StringBuilder garbage = new StringBuilder();

        Reader reader = new BufferedReader(new InputStreamReader(is));
        char c[] = "<?".toCharArray();
        int pos = 0;
        reader.mark(LOOKAHEAD);
        while (true) {
            int value = reader.read();
            garbage.append((char) value);

            // Check to see if we hit the end of the stream.
            if (value == -1) {
                throw new IOException("Encounter end of stream before start of XML.");
            }
            else if (value == c[pos]) {
                pos++;
            }
            else {
                if (pos > 0) {
                    pos = 0;
                }
                reader.mark(LOOKAHEAD);
            }

            if (pos == c.length) {
                // We found the character set we were looking for.
                reader.reset();
                break;
            }
        }

        if (garbage.length() > c.length) {
            log.debug("getReader: Dropping characters " + garbage.substring(0, garbage.length() - c.length));
        }
        return reader;
    }
}