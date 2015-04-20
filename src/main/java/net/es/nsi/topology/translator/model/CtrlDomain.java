package net.es.nsi.topology.translator.model;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.es.nsi.topology.translator.utilities.NsiUtilities;

/**
 * Models an NMWG control plane domain consisting of Links (Nodes have been
 * dropped as they are not needed to model NML).  Both original NMWG and 
 * normalized NSI identifiers are maintained for proper mapping.
 * 
 * @author hacksaw
 */
public class CtrlDomain {
    private String id; // NSI normalized identifier.
    private String originalId; // Original NMWG identifier.

    // Map containing all the Links within this domain.
    private final Map<String, CtrlLink> links = new HashMap<>();

    /**
     * Empty constructor.
     */
    public CtrlDomain() {
    }

    /**
     * Create a control domain identified by the provided NMWG identifier.
     * 
     * @param originalId Original NMWG identifier for the domain.
     */
    public CtrlDomain(String originalId) {
        // Store the original id for mapping.
        this.originalId = originalId;

        // Normalize the original id to remove optional labels.
        this.id = NsiUtilities.normalizeId(originalId);
    }

    /**
     * Get the original NMWG identifier for the domain.
     * 
     * @return Original NMWG identifier for the domain.
     */
    public String getOrignalId() {
        return originalId;
    }

    /**
     * Set the original NMWG identifier for the domain.
     * 
     * @param orignalId the orignal NMWG identifier for the domain.
     */
    public void setOrignalId(String orignalId) {
        this.originalId = orignalId;
    }
    
    /**
     * Get the normalized NSI identifier for the domain.
     * 
     * @return NSI identifier for the domain.
     */
    public String getId() {
        return id;
    }

    /**
     * Set the normalized NSI identifier for the domain.
     * 
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Add a link to the domain.
     * 
     * @param link Link identifier.
     * @return The old link if one exists.
     */
    public CtrlLink addLink(CtrlLink link) {
        return links.put(link.getId(), link);
    }

    /**
     * Remove the specified link from the domain.
     * 
     * @param id Link identifier to remove.
     * @return The removed link.
     */
    public CtrlLink removeLink(String id) {
        return links.remove(id);
    }

    /**
     * Get the specified link.
     * 
     * @param id Link identifier.
     * @return 
     */
    public CtrlLink getLink(String id) {
        return links.get(id);
    }

    /**
     * Get the domain link map.
     * 
     * @return The link map.
     */
    public Map<String, CtrlLink> getLinkMap() {
        return Collections.unmodifiableMap(links);
    }

    /**
     * Get the domain link collection.
     * 
     * @return The link collection.
     */
    public Collection<CtrlLink> getLinks() {
        return Collections.unmodifiableCollection(links.values());
    }

    /**
     * Get the set of link identifiers within the domain.
     * 
     * @return 
     */
    public Set<String> getLinkIds() {
        return Collections.unmodifiableSet(links.keySet());
    }
}
