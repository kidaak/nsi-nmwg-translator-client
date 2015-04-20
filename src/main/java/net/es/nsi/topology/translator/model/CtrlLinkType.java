package net.es.nsi.topology.translator.model;

/**
 * The type of link within the NMWG domain.
 * 
 * @author hacksaw
 */
public enum CtrlLinkType {
    UNKNOWN, // Link could not be classified or has yet to be classified.
    INVALID, // Link has been evaluated as invalid.
    INNI,    // Link is internal (between two nodes in the domain).
    ENNI,    // Link is between two domains.
    UNI      // Link represents a client port.
}
