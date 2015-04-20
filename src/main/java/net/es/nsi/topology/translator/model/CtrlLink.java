package net.es.nsi.topology.translator.model;

import com.google.common.base.Optional;

/**
 * A NMWG control plane link.
 * 
 * @author hacksaw
 */
public class CtrlLink {
    // Link attributes.
    private String id;         // The normalized NSI identifier for the link.
    private String originalId; // The original NMWG link identifier.

    private CtrlLinkType linkType = CtrlLinkType.UNKNOWN; // We will mark with type of link (ENNI, INNI, UNI).
    private Optional<String> remoteLinkId;
    private Optional<String> vlanRangeAvailability;
    private boolean vlanTranslation;

    // Link attributed inherited from parent port.
    private Optional<String> capacity;
    private Optional<String> maximumReservableCapacity;
    private Optional<String> minimumReservableCapacity;
    private Optional<String> granularity;

    /**
     * Creates an empty link.
     */
    public CtrlLink() {
        remoteLinkId = Optional.absent();
        vlanRangeAvailability = Optional.absent();
        capacity = Optional.absent();
        maximumReservableCapacity = Optional.absent();
        minimumReservableCapacity = Optional.absent();
        granularity = Optional.absent();
        vlanTranslation = false;
    }

    /**
     * Create a link with the provided base values.
     * 
     * @param originalId The original NMWG identifier.
     * @param id The normalized NSI identifier.
     * @param remoteLinkId The remote link identifier (normalized to NSI identifier).
     * @param vlanRangeAvailability The VLAN range allocated to the port.
     * @param vlanTranslation Does the link support VLAN translation.
     */
    public CtrlLink(String originalId, String id, Optional<String> remoteLinkId, Optional<String> vlanRangeAvailability, boolean vlanTranslation) {
        // Store the original id for mapping.
        this.originalId = originalId;

        // Normalize the original id to remove optional labels.
        this.id = id;

        this.remoteLinkId = remoteLinkId;
        this.vlanRangeAvailability = vlanRangeAvailability;
        this.vlanTranslation = vlanTranslation;

        capacity = Optional.absent();
        maximumReservableCapacity = Optional.absent();
        minimumReservableCapacity = Optional.absent();
        granularity = Optional.absent();
    }

    /**
     * Create a link with the provided base values.
     * 
     * @param originalId The original NMWG identifier.
     * @param id The normalized NSI identifier.
     * @param remoteLinkId The remote link identifier (normalized to NSI identifier).
     * @param vlanRangeAvailability The VLAN range allocated to the port.
     * @param vlanTranslation Does the link support VLAN translation.
     * @param capacity The capacity of the link.
     * @param maximumReservableCapacity The maximum reservable capacity of the link.
     * @param minimumReservableCapacity The minimum reservable capacity of the link.
     * @param granularity The granularity of the link in mb/s.
     */
    public CtrlLink(String originalId, String id, Optional<String> remoteLinkId,
            Optional<String> vlanRangeAvailability, boolean vlanTranslation,
            Optional<String> capacity, Optional<String> maximumReservableCapacity,
            Optional<String> minimumReservableCapacity, Optional<String> granularity) {
        // Store the original id for mapping.
        this.originalId = originalId;

        // Normalize the original id to remove optional labels.
        this.id = id;

        this.remoteLinkId = remoteLinkId;
        this.vlanRangeAvailability = vlanRangeAvailability;
        this.vlanTranslation = vlanTranslation;

        this.capacity = capacity;
        this.maximumReservableCapacity = maximumReservableCapacity;
        this.minimumReservableCapacity = minimumReservableCapacity;
        this.granularity = granularity;
    }
    
    /**
     * @return the originalId
     */
    public String getOriginalId() {
        return originalId;
    }

    /**
     * @param originalId the originalId to set
     */
    public void setOriginalId(String originalId) {
        this.originalId = originalId;
    }

    /**
     * Get the normalized NSI identifier.
     * 
     * @return the normalized NSI identifier.
     */
    public String getId() {
        return id;
    }

    /**
     * Set the normalized NSI identifier.
     * 
     * @param id the normalized NSI identifier to set.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Get the remote link identifier.
     * 
     * @return the remoteLinkId
     */
    public Optional<String> getRemoteLinkId() {
        return remoteLinkId;
    }

    /**
     * Set the remote link identifier.
     * 
     * @param remoteLinkId the remoteLinkId to set
     */
    public void setRemoteLinkId(Optional<String> remoteLinkId) {
        this.remoteLinkId = remoteLinkId;
    }

    /**
     * Get the VLAN range available on the link.
     * 
     * @return the vlanRangeAvailability
     */
    public Optional<String> getVlanRangeAvailability() {
        return vlanRangeAvailability;
    }

    /**
     * Set the VLAN range available on the link.
     * 
     * @param vlanRangeAvailability the vlanRangeAvailability to set
     */
    public void setVlanRangeAvailability(Optional<String> vlanRangeAvailability) {
        this.vlanRangeAvailability = vlanRangeAvailability;
    }

    /**
     * Get the link capacity.
     * 
     * @return the capacity
     */
    public Optional<String> getCapacity() {
        return capacity;
    }

    /**
     * Set the link capacity.
     * 
     * @param capacity the capacity to set
     */
    public void setCapacity(Optional<String> capacity) {
        this.capacity = capacity;
    }

    /**
     * Get the maximum reservable capacity on the link.
     * 
     * @return the maximumReservableCapacity
     */
    public Optional<String> getMaximumReservableCapacity() {
        return maximumReservableCapacity;
    }

    /**
     * Set the maximum reservable capacity on the link.
     * 
     * @param maximumReservableCapacity the maximumReservableCapacity to set
     */
    public void setMaximumReservableCapacity(Optional<String> maximumReservableCapacity) {
        this.maximumReservableCapacity = maximumReservableCapacity;
    }

    /**
     * Get the minimum reservable capacity on the link.
     * 
     * @return the minimumReservableCapacity
     */
    public Optional<String> getMinimumReservableCapacity() {
        return minimumReservableCapacity;
    }

    /**
     * Set the minimum reservable capacity on the link.
     * 
     * @param minimumReservableCapacity the minimumReservableCapacity to set
     */
    public void setMinimumReservableCapacity(Optional<String> minimumReservableCapacity) {
        this.minimumReservableCapacity = minimumReservableCapacity;
    }

    /**
     * Get the granularity of the link in mb/s.
     * 
     * @return the granularity
     */
    public Optional<String> getGranularity() {
        return granularity;
    }

    /**
     * Set the granularity of the link in mb/s.
     * 
     * @param granularity the granularity to set
     */
    public void setGranularity(Optional<String> granularity) {
        this.granularity = granularity;
    }

    /**
     * Get the link type (UNKNOWN, INVALID, INNI, ENNI, UNI).
     * 
     * @return the linkType
     */
    public CtrlLinkType getLinkType() {
        return linkType;
    }

    /**
     * Set the link type (UNKNOWN, INVALID, INNI, ENNI, UNI).
     * 
     * @param linkType the linkType to set
     */
    public void setLinkType(CtrlLinkType linkType) {
        this.linkType = linkType;
    }

    /**
     * Is VLAN translation enabled on this link?
     * 
     * @return the vlanTranslation
     */
    public boolean isVlanTranslation() {
        return vlanTranslation;
    }

    /**
     * Set the VLAN translation mode on the link.
     * 
     * @param vlanTranslation the vlanTranslation to set
     */
    public void setVlanTranslation(boolean vlanTranslation) {
        this.vlanTranslation = vlanTranslation;
    }
}
