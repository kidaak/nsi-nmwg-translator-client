package net.es.nsi.topology.translator.model;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;
import net.es.nsi.topology.translator.jaxb.configuration.PeeringType;
import net.es.nsi.topology.translator.jaxb.configuration.ServiceDefinitionType;
import net.es.nsi.topology.translator.jaxb.dds.NmlBidirectionalPortType;
import net.es.nsi.topology.translator.jaxb.dds.NmlLabelGroupType;
import net.es.nsi.topology.translator.jaxb.dds.NmlLifeTimeType;
import net.es.nsi.topology.translator.jaxb.dds.NmlPortGroupRelationType;
import net.es.nsi.topology.translator.jaxb.dds.NmlPortGroupType;
import net.es.nsi.topology.translator.jaxb.dds.NmlSwitchingServiceRelationType;
import net.es.nsi.topology.translator.jaxb.dds.NmlSwitchingServiceType;
import net.es.nsi.topology.translator.jaxb.dds.NmlTopologyRelationType;
import net.es.nsi.topology.translator.jaxb.dds.NmlTopologyType;
import net.es.nsi.topology.translator.jaxb.dds.NsiServiceDefinitionType;
import net.es.nsi.topology.translator.jaxb.dds.ObjectFactory;
import net.es.nsi.topology.translator.utilities.NsiUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts the NMWG topology to an equivalent NSI/NML topology representation.
 *
 * @author hacksaw
 */
public class NmlTranslator {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final ObjectFactory factory = new ObjectFactory();

    // Direction of the port.
    private static enum Direction {
        INBOUND,
        OUTBOUND
    }

    /**
     * Convert the NMWG topology to NSI/NML topology based on provided
     * configuration information.
     *
     * @param domain NMWG domain object for conversion.
     * @param lifetime the lifetime of the generated document in seconds.
     * @param serviceDefinitions the service definitions used to build the NML switching service.
     * @param peerings Port peering information used to override the default isAlias mapping.
     * @return The NML topology document.
     * @throws DatatypeConfigurationException Could not convert the topology.
     */
    public NmlTopologyType translate(CtrlDomain domain, long lifetime, List<ServiceDefinitionType> serviceDefinitions, Map<String, PeeringType> peerings) throws DatatypeConfigurationException {
        // Generate the NML topology.
        NmlTopologyType nmlTopology = factory.createNmlTopologyType();

        // The topology is named after the domain name.
        nmlTopology.setId(domain.getId());
        nmlTopology.setName(NsiUtilities.getNsiDomainName(nmlTopology.getId()));

        // The version is based on the time we generate the topology.
        XMLGregorianCalendar startTime = NsiUtilities.xmlGregorianCalendar();
        nmlTopology.setVersion(startTime);

        // Lifetime goes from now until the configurable end time.
        NmlLifeTimeType life = factory.createNmlLifeTimeType();
        life.setStart(startTime);
        life.setEnd(NsiUtilities.longToXMLGregorianCalendar(System.currentTimeMillis() + lifetime*1000));
        nmlTopology.setLifetime(life);

        // Create the ServiceDefinitions.
        List<Object> sd = getNsiServiceDefinitions(domain.getId(), serviceDefinitions);
        nmlTopology.getAny().addAll(sd);

        // Create the SwitchingService objects that will be populated when
        // creating the individual unidirectional ports.
        List<ServiceDefinitionMap> sdList = getServiceDefinitionMap(domain.getId(), serviceDefinitions);

        // Wrap the switching service in a service relationship.
        for (ServiceDefinitionMap sdm : sdList) {
            NmlTopologyRelationType ssRelation = factory.createNmlTopologyRelationType();
            ssRelation.setType(Constants.NML_SWITCHINGSERVICE);
            ssRelation.getService().add(sdm.getSwitchingService());
            nmlTopology.getRelation().add(ssRelation);
        }

        // Create the inbound port holder we will fill with inbound ports.
        NmlTopologyRelationType inbound = factory.createNmlTopologyRelationType();
        inbound.setType(Constants.NML_PORT_INBOUND);
        nmlTopology.getRelation().add(inbound);

        // Create the outbound port holder we will fill with outbount ports.
        NmlTopologyRelationType outbound = factory.createNmlTopologyRelationType();
        outbound.setType(Constants.NML_PORT_OUTBOUND);
        nmlTopology.getRelation().add(outbound);

        // Convert the NMWG links into NML ports.
        for (CtrlLink link : domain.getLinks()) {
            // Determine which SwitchingService elements this link can
            // potentially match.
            List<NmlSwitchingServiceType> ssList = match(link.getId(), sdList);

            // Now we create the Bidirectional port groups and unidirectional
            // relations for all ENNI and UNI links.
            if (!ssList.isEmpty() && (link.getLinkType() == CtrlLinkType.ENNI ||
                    link.getLinkType() == CtrlLinkType.UNI ||
                    link.getLinkType() == CtrlLinkType.UNKNOWN)) {

                // Bidirectional port group first.
                NmlBidirectionalPortType port = factory.createNmlBidirectionalPortType();
                port.setId(link.getId());

                NmlPortGroupType in = factory.createNmlPortGroupType();
                in.setId(NsiUtilities.getInboundPort(link.getId()));

                NmlPortGroupType out = factory.createNmlPortGroupType();
                out.setId(NsiUtilities.getOutboundPort(link.getId()));

                port.getRest().add(factory.createPortGroup(in));
                port.getRest().add(factory.createPortGroup(out));

                nmlTopology.getGroup().add(port);

                // Now for the unidirectional relations.
                PeeringType peering = peerings.get(link.getId());
                Optional<NmlPortGroupType> inPort = getNmlPortGroup(link, Direction.INBOUND, peering);
                if (inPort.isPresent()) {
                    inbound.getPortGroup().add(inPort.get());
                }

                Optional<NmlPortGroupType> outPort = getNmlPortGroup(link, Direction.OUTBOUND, peering);
                if (outPort.isPresent()) {
                    outbound.getPortGroup().add(outPort.get());
                }

                // Now we put the unidirectional port references in applicable
                // switching services.
                for (NmlSwitchingServiceType ss : ssList) {
                    // We need to determine how a port matches a SwitchingService.
                    for (NmlSwitchingServiceRelationType relation : ss.getRelation()) {
                        if (relation.getType().contentEquals(Constants.NML_PORT_INBOUND) &&
                                ss.getEncoding().equalsIgnoreCase(inPort.get().getEncoding())) {
                            relation.getPortGroup().add(in);
                        }
                        else if (relation.getType().contentEquals(Constants.NML_PORT_OUTBOUND) &&
                                ss.getEncoding().equalsIgnoreCase(outPort.get().getEncoding())) {
                            relation.getPortGroup().add(out);
                        }
                    }
                }
            }
        }

        return nmlTopology;
    }

    /**
     * Create the NSI service definition.
     *
     * @param networdId create a service domain as a member of this network.
     * @param serviceDefinitions The service definition configuration information used to generate the NML ServiceDefinition.
     * @return The JAXB encoded ServiceDefinition objects.
     */
    private List<Object> getNsiServiceDefinitions(String networdId, List<ServiceDefinitionType> serviceDefinitions) {
        List<Object> list = new ArrayList<>();
        for (ServiceDefinitionType sd : serviceDefinitions) {
            NsiServiceDefinitionType sdt = factory.createNsiServiceDefinitionType();
            sdt.setId(NsiUtilities.getServiceDefinitionId(networdId, sd.getId()));
            sdt.setName(sd.getName());
            sdt.setServiceType(sd.getServiceType());

            // Create inbound port relationships.
            JAXBElement<NsiServiceDefinitionType> jaxb = factory.createServiceDefinition(sdt);
            list.add(jaxb);
        }
        return list;
    }

    /**
     * Create a NML SwitchingService for each ServiceDefintion and create a
     * mapping between the NSI ServiceDefintion and NML Switching service.
     *
     * @param networkId NML SwitchingService is a member of this network.
     * @param serviceDefinitions the NSI ServiceDefinitions used to generate the SwitchingService.
     * @return the list of ServiceDefinition/SwitchingService mappings.
     */
    private List<ServiceDefinitionMap> getServiceDefinitionMap(String networkId, List<ServiceDefinitionType> serviceDefinitions) {
        List<ServiceDefinitionMap> sdList = new ArrayList<>();
        for (ServiceDefinitionType sd : serviceDefinitions) {
            NmlSwitchingServiceType ss = factory.createNmlSwitchingServiceType();
            ss.setId(NsiUtilities.getSwitchingServiceId(networkId, sd.getId()));
            ss.setEncoding(sd.getEncoding());
            ss.setLabelType(sd.getLabelType());
            ss.setLabelSwapping(sd.isLabelSwapping());

            // Create inbound port relationships.
            NmlSwitchingServiceRelationType inbound = factory.createNmlSwitchingServiceRelationType();
            inbound.setType(Constants.NML_PORT_INBOUND);
            ss.getRelation().add(inbound);

            // Create outbound port relationships.
            NmlSwitchingServiceRelationType outbound = factory.createNmlSwitchingServiceRelationType();
            outbound.setType(Constants.NML_PORT_OUTBOUND);
            ss.getRelation().add(outbound);

            // Wrap the switching service in a service relationship.
            NmlTopologyRelationType ssRelation = factory.createNmlTopologyRelationType();
            ssRelation.setType(Constants.NML_SWITCHINGSERVICE);
            ssRelation.getService().add(ss);

            // Add the ServiceDefinition reference.
            NsiServiceDefinitionType sdt = factory.createNsiServiceDefinitionType();
            sdt.setId(NsiUtilities.getServiceDefinitionId(networkId, sd.getId()));
            JAXBElement<NsiServiceDefinitionType> jaxb = factory.createServiceDefinition(sdt);
            ss.getAny().add(jaxb);

            ServiceDefinitionMap sdMap = new ServiceDefinitionMap();
            sdMap.setServiceDefinition(sd);
            sdMap.setSwitchingService(ss);
            sdList.add(sdMap);
        }
        return sdList;
    }

    /**
     * Generate an NML unidirectional PortGroup for the specified NMWG link.
     *
     * @param link the NMWG link to convert.
     * @param direction the direction of the port.
     * @param peer The peer isAlias configuration information.
     * @return the NML port group if one was created.
     */
    private Optional<NmlPortGroupType> getNmlPortGroup(CtrlLink link, Direction direction, PeeringType peer) {
        // Do we have peer configuration information for this port?
        boolean usePeerLink = (peer != null && (!Strings.isNullOrEmpty(peer.getInbound()) || !Strings.isNullOrEmpty(peer.getOutbound())));

        // We skip processing internal (INNI) links as these are not modelled
        // in NSI topology.
        NmlPortGroupType port = null;
        if (link.getLinkType() == CtrlLinkType.ENNI ||
                link.getLinkType() == CtrlLinkType.UNI ||
                link.getLinkType() == CtrlLinkType.UNKNOWN) {
            // We need to model this link so create the containing port group.
            port = factory.createNmlPortGroupType();

            // Are we creating an inbound or outbound NML port relation?
            if (direction == Direction.INBOUND) {
                port.setId(NsiUtilities.getInboundPort(link.getId()));
            }
            else {
                port.setId(NsiUtilities.getOutboundPort(link.getId()));
            }

            // Do we need to override the labels associated with the port?
            if (peer != null && peer.getLabels() != null) {
                NmlLabelGroupType labelGroupType = factory.createNmlLabelGroupType();
                labelGroupType.setLabeltype(peer.getLabels().getType());
                labelGroupType.setValue(peer.getLabels().getValue());
                port.getLabelGroup().add(labelGroupType);
            }
            else if (link.getVlanRangeAvailability().isPresent()) {
                NmlLabelGroupType labelGroupType = factory.createNmlLabelGroupType();
                labelGroupType.setLabeltype(Constants.NML_ETHERNET_VLAN);
                labelGroupType.setValue(link.getVlanRangeAvailability().get());
                port.getLabelGroup().add(labelGroupType);
            }

            // Add the encoding type for the port.
            port.setEncoding(NsiUtilities.getEncodingType(link.getEncodingType()));

            // Add bandwitdh metrics.
            port.getAny().addAll(createCtrElements(link));

            // We add isAlias information if this is an ENNI link, or if there
            // is specific configuration information asking us to do so.
            if (link.getLinkType() == CtrlLinkType.ENNI || usePeerLink) {
                String ali = NsiUtilities.getOutboundPort(link.getRemoteLinkId().get());
                NmlPortGroupRelationType portRelationType;
                if (direction == Direction.INBOUND) {
                    // We add the outbound port isAlias.
                    if (peer != null && !Strings.isNullOrEmpty(peer.getInbound())) {
                        portRelationType = getNmlIsAlias(peer.getInbound());
                    }
                    else {
                        portRelationType = getNmlIsAlias(NsiUtilities.getOutboundPort(link.getRemoteLinkId().get()));
                    }
                }
                else {
                    // We add the inbound port isAlias.
                    if (peer != null && !Strings.isNullOrEmpty(peer.getOutbound())) {
                        portRelationType = getNmlIsAlias(peer.getOutbound());
                    }
                    else {
                        portRelationType = getNmlIsAlias(NsiUtilities.getInboundPort(link.getRemoteLinkId().get()));
                    }
                }
                port.getRelation().add(portRelationType);
            }
        }

        return Optional.fromNullable(port);
    }

    private List<Object> createCtrElements(CtrlLink link) {
        List<Object> any = new ArrayList<>();

        // Add the maximum reservable capacity if set.
        try {
            Optional<String> maximumReservableCapacity = link.getMaximumReservableCapacity();
            if (maximumReservableCapacity.isPresent()) {
                Long max = Long.parseUnsignedLong(maximumReservableCapacity.get());
                any.add(factory.createMaximumReservableCapacity(max));
            }
        }
        catch (NumberFormatException nfe) {
            log.error("maximumReservableCapacity is present for " + link.getId() + " but not a valid long value " + link.getMaximumReservableCapacity().get());
        }

        try {
            Optional<String> minimumReservableCapacity = link.getMinimumReservableCapacity();
            if (minimumReservableCapacity.isPresent()) {
                Long min = Long.parseUnsignedLong(minimumReservableCapacity.get());
                any.add(factory.createMinimumReservableCapacity(min));
            }
        }
        catch (NumberFormatException nfe) {
            log.error("minimumReservableCapacity is present for " + link.getId() + " but not a valid long value " + link.getMinimumReservableCapacity().get());
        }

        try {
            Optional<String> capacity = link.getCapacity();
            if (capacity.isPresent()) {
                Long cap = Long.parseUnsignedLong(capacity.get());
                any.add(factory.createCapacity(cap));
            }
        }
        catch (NumberFormatException nfe) {
            log.error("Capacity is present for " + link.getId() + " but not a valid long value " + link.getCapacity().get());
        }

        try {
            Optional<String> granularity = link.getGranularity();
            if (granularity.isPresent()) {
                Long gran = Long.parseUnsignedLong(granularity.get());
                any.add(factory.createGranularity(gran));
            }
        }
        catch (NumberFormatException nfe) {
            log.error("Granularity is present for " + link.getId() + " but not a valid long value " + link.getGranularity().get());
        }

        return any;
    }

    /**
     *
     * @param id
     * @return
     */
    private NmlPortGroupRelationType getNmlIsAlias(String id) {

        NmlPortGroupRelationType isAlias = factory.createNmlPortGroupRelationType();

        isAlias.setType(Constants.NML_ISALIAS);

        List<NmlPortGroupType> portGroupTypeList = isAlias.getPortGroup();
        NmlPortGroupType portGroupType = factory.createNmlPortGroupType();
        portGroupType.setId(id);
        portGroupTypeList.add(portGroupType);

        return isAlias;
    }

    /**
     *
     * @param id
     * @param sdList
     * @return
     */
    private List<NmlSwitchingServiceType> match(String id, List<ServiceDefinitionMap> sdList) {
        List<NmlSwitchingServiceType> result = new ArrayList<>();

        for (ServiceDefinitionMap sd : sdList) {
            boolean match = false;
            List<String> includes = sd.getServiceDefinition().getInclude();
            if (includes.isEmpty()) {
                match = true;
            }
            else {
                for (String pattern : includes) {
                    if (id.contains(pattern)) {
                        match = true;
                        break;
                    }
                }
            }

            List<String> excludes = sd.getServiceDefinition().getExclude();
            if (!excludes.isEmpty()) {
                for (String pattern : excludes) {
                    if (id.contains(pattern)) {
                        match = false;
                        break;
                    }
                }
            }

            if (match) {
                result.add(sd.getSwitchingService());
            }
        }

        return result;
    }
}
