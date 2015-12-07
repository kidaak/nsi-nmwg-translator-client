package net.es.nsi.topology.translator.model;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import net.es.nsi.topology.translator.gson.StpType;
import net.es.nsi.topology.translator.http.HttpsConfig;
import net.es.nsi.topology.translator.http.RestClient;
import net.es.nsi.topology.translator.jaxb.JaxbParser;
import net.es.nsi.topology.translator.jaxb.NmwgParser;
import net.es.nsi.topology.translator.jaxb.configuration.ParameterType;
import net.es.nsi.topology.translator.jaxb.configuration.PeeringType;
import net.es.nsi.topology.translator.jaxb.configuration.ServiceDefinitionType;
import net.es.nsi.topology.translator.jaxb.configuration.SourceType;
import net.es.nsi.topology.translator.jaxb.nml.NmlTopologyType;
import net.es.nsi.topology.translator.jaxb.nmwg.CtrlPlaneDomainContent;
import net.es.nsi.topology.translator.jaxb.nmwg.CtrlPlaneLinkContent;
import net.es.nsi.topology.translator.jaxb.nmwg.CtrlPlaneNodeContent;
import net.es.nsi.topology.translator.jaxb.nmwg.CtrlPlanePortContent;
import net.es.nsi.topology.translator.jaxb.nmwg.CtrlPlaneSwcapContent;
import net.es.nsi.topology.translator.jaxb.nmwg.CtrlPlaneSwitchingCapabilitySpecificInfo;
import net.es.nsi.topology.translator.jaxb.nmwg.CtrlPlaneTopologyContent;
import net.es.nsi.topology.translator.utilities.NsiUtilities;
import org.glassfish.jersey.client.ChunkedInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for conversion of NMWG schema to NML.
 *
 * @author hacksaw
 */
public class NmwgTopology {
    private final Logger log = LoggerFactory.getLogger(getClass());

    /* The NMWG schema returned from the Internet2 repository is not a correct
     * instance of CtrlPlane schema so we need to massage it.  These string are
     * used for the conversion.
     */
    private static final String NMTOPO_TOPOLOGY_START = "<nmtopo:topology xmlns:nmtopo=\"http://ogf.org/schema/network/topology/base/20070828/\">";
    private static final String CTRLPLANE_TOPOLOGY_START = "<CtrlPlane:topology xmlns:CtrlPlane=\"http://ogf.org/schema/network/topology/ctrlPlane/20080828/\">";
    private static final String NMTOPO_TOPOLOGY_END = "</nmtopo:topology>";
    private static final String CTRLPLANE_TOPOLOGY_END = "</CtrlPlane:topology>";

    private final WebTarget path;

    // Hold the original parsed NMWG topology just in case.
    private CtrlPlaneTopologyContent topology;

    // Our internal representation of NMWG with normalized identifiers.
    private CtrlDomain parsedDomain;

    private final JaxbParser nmwgParser;

    /**
     *
     * @param nmwg
     * @param secure
     * @param domain
     * @throws UnsupportedEncodingException
     * @throws KeyStoreException
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws KeyManagementException
     * @throws UnrecoverableKeyException
     */
    public NmwgTopology(SourceType nmwg, HttpsConfig secure, String domain) throws UnsupportedEncodingException, KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, KeyManagementException, UnrecoverableKeyException, JAXBException {
        this.nmwgParser = NmwgParser.getInstance();

        // Make sure we have a domain provided.
        if (Strings.isNullOrEmpty(domain)) {
            throw new IllegalArgumentException("Domain cannot be null or empty string.");
        }

        // Get our REST client and set root URL.
        RestClient restClient = new RestClient(secure);
        WebTarget tempPath = restClient.get().target(nmwg.getBaseURL()).queryParam("domain", domain);

        // Now we add additional parameters to the query.
        for (ParameterType parameter : nmwg.getParameters()) {
            try {
                String value = URLEncoder.encode(parameter.getValue(), "UTF-8");
                tempPath = tempPath.queryParam(parameter.getType(), value);
            } catch (UnsupportedEncodingException ex) {
                log.error("Invalid instance URL.", ex);
                throw ex;
            }
        }

        path = tempPath;
    }

    /**
     * Execute the remote NMWG query.
     *
     * @throws IllegalArgumentException
     * @throws JAXBException
     */
    public void process() throws IllegalArgumentException, JAXBException {
        Response response = path.request().accept(MediaType.APPLICATION_XML).get();

        if (Response.Status.OK.getStatusCode() != response.getStatus()) {
            log.error("GET of topology failed " + path.getUri().toString() + ", with STATUS " + response.getStatus());
            return;
        }

        final ChunkedInput<String> chunkedInput = response.readEntity(new GenericType<ChunkedInput<String>>() {});
        String chunk;
        String finalChunk = null;
        while ((chunk = chunkedInput.read()) != null) {
            finalChunk = chunk;
        }
        response.close();

        if (finalChunk == null || finalChunk.isEmpty()) {
            throw new IllegalArgumentException("Topology service returned empty string.");
        }

        parsedDomain = parse(finalChunk);
    }

    /**
     * Parse NMWG XML instance document.
     *
     * @param xml
     * @throws IllegalArgumentException
     * @throws JAXBException
     */
    private CtrlDomain parse(String xml) throws IllegalArgumentException, JAXBException {
        if (xml == null || xml.isEmpty()) {
            log.error("XML document not specified.");
            throw new IllegalArgumentException("XML document not specified.");
        }

        String processXML = null;
        try {
            processXML = xml.replace(NMTOPO_TOPOLOGY_START, CTRLPLANE_TOPOLOGY_START).replace(NMTOPO_TOPOLOGY_END, CTRLPLANE_TOPOLOGY_END);
        }
        catch (NullPointerException ex) {
            log.error("Failed to process XML document", ex);
        }

        if (processXML == null || processXML.isEmpty()) {
            log.error("Invalid XML");
            throw new IllegalArgumentException("Invalid XML.");
        }

        // Now that we have the XML properly formatted we need to parse it.
        topology = nmwgParser.xml2Jaxb(CtrlPlaneTopologyContent.class, processXML);

        // Process topology.
        if (topology == null || topology.getDomain().isEmpty()) {
            log.error("NMWG topology is empty.");
            throw new IllegalArgumentException("NMWG topology is empty.");
        }

        return convert(topology);
    }

    /**
     * Returns an NML topology structure based on loaded NMWG topology document.
     *
     * @param lifeTime
     * @param serviceDefinitions
     * @param peerings
     * @return
     * @throws DatatypeConfigurationException
     */
    public NmlTopologyType getNmlTopology(long lifeTime, List<ServiceDefinitionType> serviceDefinitions, Map<String, PeeringType> peerings) throws DatatypeConfigurationException {
        NmlTranslator translator = new NmlTranslator();
        return translator.translate(parsedDomain, lifeTime, serviceDefinitions, peerings);
    }

    /**
     * Returns an NSI STP identifier to NMWG mapping of identifiers.
     *
     * @return
     */
    public List<StpType> getMappings() {
        List<StpType> stps = new ArrayList<>();

        for (CtrlLink link : parsedDomain.getLinks()) {
            StpType stp = new StpType();
            stp.setStpId(link.getId());
            stp.setOscarsId(link.getOriginalId());
            stps.add(stp);
        }
        return stps;
    }

    /**
     * Convert the JAXB representation of NMWG topology to an internal mapping.
     *
     * @param topology
     * @return
     */
    private CtrlDomain convert(CtrlPlaneTopologyContent topology) {
        Map<String, CtrlDomain> ctrlDomains = new HashMap<>();

        for (CtrlPlaneDomainContent domain : topology.getDomain()) {
            CtrlDomain ctrlDomain = new CtrlDomain(domain.getId());
            ctrlDomains.put(ctrlDomain.getId(), ctrlDomain);

            for (CtrlPlaneNodeContent node : domain.getNode()) {
                for (CtrlPlanePortContent port : node.getPort()) {
                    // Pull out the port attributes first since these will
                    // be placed in each instance of link we create.
                    Optional<String> portCapacity = Optional.fromNullable(port.getCapacity());
                    Optional<String> portMaxCapacity = Optional.fromNullable(port.getMaximumReservableCapacity());
                    Optional<String> portMinCapacity = Optional.fromNullable(port.getMinimumReservableCapacity());
                    Optional<String> portGranularity = Optional.fromNullable(port.getGranularity());

                    // Now for the link elements.
                    for (CtrlPlaneLinkContent link : port.getLink()) {
                        String id = NsiUtilities.normalizeId(link.getId());

                        Optional<String> remoteLinkId = Optional.fromNullable(NsiUtilities.normalizeId(link.getRemoteLinkId()));
                        Optional<String> vlanRangeAvailability = Optional.absent();
                        Optional<String> encodingType = Optional.absent();
                        boolean vlanTranslation = false;
                        CtrlPlaneSwcapContent switchingCapabilityDescriptors = link.getSwitchingCapabilityDescriptors();
                        if (switchingCapabilityDescriptors != null) {
                            encodingType = Optional.fromNullable(switchingCapabilityDescriptors.getEncodingType());
                            CtrlPlaneSwitchingCapabilitySpecificInfo switchingCapabilitySpecificInfo = switchingCapabilityDescriptors.getSwitchingCapabilitySpecificInfo();
                            if (switchingCapabilitySpecificInfo != null) {
                                vlanRangeAvailability = Optional.of(Optional.fromNullable(switchingCapabilitySpecificInfo.getVlanRangeAvailability()).or(Constants.NML_ETHERNET_VLAN_RANGE));
                                if (switchingCapabilitySpecificInfo.isVlanTranslation() != null) {
                                    vlanTranslation = switchingCapabilitySpecificInfo.isVlanTranslation();
                                }
                            }
                        }

                        // Use any attributes defined on this Link overriding
                        // the Port defined values.
                        Optional<String> capacity = Optional.fromNullable(link.getCapacity());
                        if (!capacity.isPresent()) {
                            capacity = portCapacity;
                        }

                        Optional<String> maxCapacity = Optional.fromNullable(link.getMaximumReservableCapacity());
                        if (!maxCapacity.isPresent()) {
                            maxCapacity = portMaxCapacity;
                        }

                        Optional<String> minCapacity = Optional.fromNullable(link.getMinimumReservableCapacity());
                        if (!minCapacity.isPresent()) {
                            minCapacity = portMinCapacity;
                        }

                        Optional<String> granularity = Optional.fromNullable(link.getGranularity());
                        if (!granularity.isPresent()) {
                            granularity = portGranularity;
                        }

                        // Add this link.
                        CtrlLink ctrlLink = new CtrlLink(link.getId(), id, remoteLinkId,
                                encodingType, vlanRangeAvailability, vlanTranslation, capacity,
                                maxCapacity, minCapacity, granularity);
                        ctrlDomain.addLink(ctrlLink);
                    }
                }
            }
        }

        // We should only have a single domain.
        CtrlDomain finalDomain = null;
        for (CtrlDomain domain : ctrlDomains.values()) {
            finalDomain = domain;
        }

        if (ctrlDomains.size() != 1 || finalDomain == null) {
            StringBuilder sb = new StringBuilder("Unexpected number of domains (");
            sb.append(ctrlDomains.size());
            sb.append(").");
            log.error(sb.toString());
            throw new IllegalArgumentException(sb.toString());
        }

        markLinks(finalDomain.getLinkMap(), finalDomain.getId());

        return finalDomain;
    }

    /**
     * Traverses list of links marking each as one of INNI (internal),
     * ENNI (inter-domain), UNI (client), or INVALID (type cannot be
     * determined).
     *
     * How it works: The remoteLinkId associated with a link is checked to see
     * if there is a valid known link entry with the same identifier.  If there
     * is, and that remote link references the link being checked, then it is
     * classified as in INNI (internal) link.  If there is a mismatch, the
     * checked link is marked as INVALID.
     *
     * If there is no matching remote link, then the remoteLinkId is parsed to
     * determine if it is a member of the local domain (contains our domain
     * identifier).  If it is then we classify the link as UNI (client).
     *
     * Finally, if neither of the previous two criteria is met, we mark the link
     * as ENNI (inter-domain).
     *
     * @param links
     * @param domain
     */
    private void markLinks(Map<String, CtrlLink> links, String domain) {
        // Mark link types.
        for (CtrlLink link : links.values()) {
            // Only process if we have not already visited this link (UNKNOWN),
            // and we have a remoteLinkId to look up.
            if (link.getLinkType() == CtrlLinkType.UNKNOWN &&
                    link.getRemoteLinkId().isPresent()) {
                // Lookup the remote link to determine if it is an entry in our
                // NMWG topology.
                CtrlLink remoteLink = links.get(link.getRemoteLinkId().get());

                // If we have a remote link process it.
                if (remoteLink != null) {
                    // If it has not yet been processed (UNKNOWN).
                    if (remoteLink.getLinkType() == CtrlLinkType.UNKNOWN &&
                            remoteLink.getRemoteLinkId().isPresent() &&
                            remoteLink.getRemoteLinkId().get().equalsIgnoreCase(link.getId())) {
                        // Both ends agree so tag as an internal link.
                        link.setLinkType(CtrlLinkType.INNI);
                        remoteLink.setLinkType(CtrlLinkType.INNI);
                    }
                    else {
                        // We have a mismatch and need to flag local link
                        // as bad.  The remote link may still be valid
                        // so wait until it is processed to validate.
                        link.setLinkType(CtrlLinkType.INVALID);
                        log.error("remoteLink mismatch linkId=" + link.getId() + ", remoteLinkId=" + remoteLink.getId());
                    }
                }
                // Check to see if this is an inter-domain or client port.
                else if (link.getRemoteLinkId().isPresent()) {
                    String remoteLinkString = link.getRemoteLinkId().get();

                    // Check to see if this is a client port.
                    if (remoteLinkString.startsWith(domain) || remoteLinkString.startsWith(Constants.LOCAL_CLIENT_PORT)) {
                        link.setLinkType(CtrlLinkType.UNI);
                    }
                    // Must be an external link.
                    else {
                        link.setLinkType(CtrlLinkType.ENNI);
                    }
                }
            }
        }
    }
}
