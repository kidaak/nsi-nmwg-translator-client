package net.es.nsi.topology.translator.utilities;

import com.google.common.base.Optional;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import net.es.nsi.topology.translator.jaxb.dds.EthEncodingTypes;
import net.es.nsi.topology.translator.model.Constants;
import net.es.nsi.topology.translator.model.Transform;

/**
 * A set of utilities for manipulating NMWG and NML XML elements.
 *
 * @author hacksaw
 */
public class NsiUtilities {

    /**
     * Convert an NMWG identifier to the equivalent NML identifier.
     *
     * @param id NMWG identifier to convert.
     * @return Equivalent NML identifier.
     * @throws IllegalArgumentException
     */
    public static String normalizeId(String id) throws IllegalArgumentException {
        // Trim the identifier just in case the XML was improperly formatted.
        id = id.trim();

        // If this is not an OGF URN then we are out of here.
        if (!id.startsWith(Constants.NSI_URN_OGF_NETWORK)) {
            throw new IllegalArgumentException("Domain id (" + id + ") does not contain prefix (" + Constants.NSI_URN_OGF_NETWORK + ").");
        }

        // Remove NMWG labels.
        for (Transform transform : Constants.getNmwgTransforms()) {
            id = id.replace(transform.getFrom(), transform.getTo());
        }

        // Find the end of the domain part.
        String domainId = id.substring(Constants.NSI_URN_OGF_NETWORK.length(), id.length());
        int indexOf = domainId.indexOf(":");
        if (indexOf < 0) {
            // This must be a root domainId so add the year prefix.
            StringBuilder sb = new StringBuilder(id);
            sb.append(Constants.NSI_URN_SEPARATOR);
            sb.append(Constants.NSI_URN_DOMAIN_YEAR);
            sb.append(Constants.NSI_URN_SEPARATOR);
            return sb.toString();
        }

        domainId = domainId.substring(0, indexOf);

        // We have more than just the domainId so convert remainder.
        String theRest = id.substring(Constants.NSI_URN_OGF_NETWORK.length() + domainId.length());
        for (Transform transform : Constants.getNsiTransforms()) {
            theRest = theRest.replace(transform.getFrom(), transform.getTo());
        }

        // Build up the new id.
        StringBuilder result = new StringBuilder(Constants.NSI_URN_OGF_NETWORK);
        result.append(domainId);
        result.append(Constants.NSI_URN_SEPARATOR);
        result.append(Constants.NSI_URN_DOMAIN_YEAR);
        result.append(Constants.NSI_URN_SEPARATOR);
        result.append(theRest);
        return result.toString();
    }

    /**
     * Extract the domain name from the specified OGF NSI identifier.
     *
     * @param id
     * @return Domain name string.
     * @throws IllegalArgumentException
     */
    public static String getNsiDomainName(String id) throws IllegalArgumentException {
        // If this is not an OGF URN then we are out of here.
        if (!id.startsWith(Constants.NSI_URN_OGF_NETWORK)) {
            throw new IllegalArgumentException("Domain id (" + id + ") does not contain prefix (" + Constants.NSI_URN_OGF_NETWORK + ").");
        }

        // Remove the URN OGF prefix and then anything after the domain name.
        String domainId = id.substring(Constants.NSI_URN_OGF_NETWORK.length(), id.length());
        int indexOf = domainId.indexOf(":");
        if (indexOf < 0) {
            return domainId;
        }

        return domainId.substring(0, indexOf);
    }

    /**
     * Append input port identifier to provided root NML port identifier.
     *
     * @param id NML port identifier.
     * @return input NML port identifier.
     */
    public static String getInboundPort(String id) {
        StringBuilder sb = new StringBuilder(id);
        sb.append(Constants.NSI_URN_SEPARATOR);
        sb.append(Constants.NSI_STPID_EXTENSION_PORT_IN);
        return sb.toString();
    }

    /**
     * Append output port identifier to provided root NML port identifier.
     *
     * @param id NML port identifier.
     * @return output NML port identifier.
     */
    public static String getOutboundPort(String id) {
        StringBuilder sb = new StringBuilder(id);
        sb.append(Constants.NSI_URN_SEPARATOR);
        sb.append(Constants.NSI_STPID_EXTENSION_PORT_OUT);
        return sb.toString();
    }

    /**
     * Build an NSI ServiceDefinition identifier from provided networkId.
     *
     * @param networkId The Network identifier to use as a root for the ServiceDefinition identifier.
     * @param id The name of the ServiceDefinition.
     * @return The new NSI ServiceDefinition identifier.
     */
    public static String getServiceDefinitionId(String networkId, String id) {
        StringBuilder sb = new StringBuilder(networkId);
        sb.append(Constants.NSI_URN_SEPARATOR);
        sb.append(Constants.NSI_URN_SERVICEDEFINITION);
        sb.append(Constants.NSI_URN_SEPARATOR);
        sb.append(id);
        return sb.toString();
    }

    /**
     * Build an NML SwitchingService identifier from provided networkId.
     *
     * @param networkId The Network identifier to use as a root for the SwitchingService identifier.
     * @param id The name of the SwitchingService.
     * @return The new NSI ServiceDefinition identifier.
     */
    public static String getSwitchingServiceId(String networkId, String id) {
        StringBuilder sb = new StringBuilder(networkId);
        sb.append(Constants.NSI_URN_SEPARATOR);
        sb.append(Constants.NSI_URN_SERVICEDOMAIN);
        sb.append(Constants.NSI_URN_SEPARATOR);
        sb.append(id);
        return sb.toString();
    }

    /**
     * Return the current time based on GMT time zone.
     *
     * @return The current time relative to GMT.
     * @throws DatatypeConfigurationException
     */
    public static XMLGregorianCalendar xmlGregorianCalendar() throws DatatypeConfigurationException {
        GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        return DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
    }

    /**
     * Return the time in GMT time zone based on provided millisecond time.
     *
     * @param time Millisecond time.
     * @return Time relative to GMT.
     * @throws DatatypeConfigurationException
     */
    public static XMLGregorianCalendar longToXMLGregorianCalendar(long time) throws DatatypeConfigurationException {
        if (time <= 0) {
            return null;
        }

        GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        cal.setTimeInMillis(time);
        XMLGregorianCalendar newXMLGregorianCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
        return newXMLGregorianCalendar;
    }

    public static String getEncodingType(Optional<String> encodingType) {
        if (encodingType.isPresent() && encodingType.get().trim().equalsIgnoreCase("packet")) {
            return EthEncodingTypes.HTTP_SCHEMAS_OGF_ORG_NML_2012_10_ETHERNET.value();
        }

        return "unknown";
    }
}
