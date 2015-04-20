package net.es.nsi.topology.translator.model;

import com.google.common.base.Optional;
import java.io.IOException;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;
import net.es.nsi.topology.translator.jaxb.JaxbParser;
import net.es.nsi.topology.translator.jaxb.dds.InterfaceType;
import net.es.nsi.topology.translator.jaxb.dds.NsaType;
import net.es.nsi.topology.translator.utilities.NsiUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Load an NSA Document from file and reset version and startTime to now, and
 * endTime to now + lifeTime.
 *
 * @author hacksaw
 */
public class NsaDocument {
    private static final Logger log = LoggerFactory.getLogger(NsaDocument.class);
    private final NsaType nsaDocument;
    private final JaxbParser nsaParser;

    /**
     * Load NSA document from specified file, reset version and startTime to
     * now, and endTime to now + lifeTime.
     *
     * @param file file containing NSA description document.
     * @param lifetime lifetime of the NSA description document in seconds
     * @throws IOException Could not read file.
     * @throws JAXBException Could not parse NSA description document.
     * @throws DatatypeConfigurationException Failed to create calendar object.
     */
    public NsaDocument(String file, long lifetime) throws IOException, JAXBException, DatatypeConfigurationException {
        this.nsaParser = JaxbParser.getInstance();

        try {
            nsaDocument = nsaParser.parseFile(NsaType.class, file);
            XMLGregorianCalendar startTime = NsiUtilities.xmlGregorianCalendar();
            nsaDocument.setVersion(startTime);
            nsaDocument.setStartTime(startTime);
            nsaDocument.setExpires(NsiUtilities.longToXMLGregorianCalendar(System.currentTimeMillis() + lifetime*1000));
        }
        catch (IOException io) {
            log.error("Failed to read NSA description file " + file, io);
            throw io;
        }
        catch (JAXBException jaxb) {
            log.error("Invalid XML in file " + file, jaxb);
            throw jaxb;
        }
    }

    /**
     * Get the NSA description document.
     *
     * @return the nsaDocument
     */
    public NsaType getDocument() {
        return nsaDocument;
    }

    /**
     * Get the network identifier associated with the NSA description file.  At
     * the moment we restrict it to only a single networkId based on OSCARS
     * nsi-bridge restriction.
     *
     * @return the networkId.
     * @throws IllegalArgumentException If there is anything other than one networkId.
     */
    public String getNetworkId() throws IllegalArgumentException {
       // The nsi-bridge can only support a single networkId so make sure we
        // do not have multiple identifiers assigned in the NSA description file.
        if (nsaDocument.getNetworkId().size() != 1) {
            StringBuilder error = new StringBuilder("NSA description file (");
            error.append(nsaDocument.getId());
            error.append(") contains multiple networkId (");
            error.append(nsaDocument.getNetworkId().size());
            error.append(").");
            throw new IllegalArgumentException(error.toString());
        }

        return nsaDocument.getNetworkId().get(0);
    }

    /**
     * Find protocol address URL corresponding to the specified protocol type.
     *
     * @param type Protocol type of the interface to search for.
     * @return The address URL for the specified protocol type.
     */
    public Optional<String> getProtocolUrl(String type) {
        for (InterfaceType intf : nsaDocument.getInterface()) {
            if (type.equalsIgnoreCase(intf.getType())) {
                return Optional.fromNullable(intf.getHref());
            }
        }

        return Optional.absent();
    }

    /**
     * Find the highest version of the CS provider protocol supported by the NSA.
     *
     * @return Protocol version string.
     */
    public Optional<String> getProviderVersion() {
        float highest = 0;
        Optional<String> provider = Optional.absent();

        for (InterfaceType intf : nsaDocument.getInterface()) {
            Float version = NsiConstants.NSI_CS_PROVIDERS.get(intf.getType());
            if (version != null && version > highest) {
                highest = version;
                provider = Optional.fromNullable(intf.getType());
            }
        }

        return provider;
    }
}
