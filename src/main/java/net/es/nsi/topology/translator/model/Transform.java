package net.es.nsi.topology.translator.model;

/**
 * Simple mapping object for tracking String transforms (from -> to).
 * 
 * @author hacksaw
 */
public class Transform {
    private String from;
    private String to;

    /**
     * Create a transform mapping the string "from" to the string "to".
     * 
     * @param from The string that will be replaced.
     * @param to The string to replace with.
     */
    public Transform(String from, String to) {
        this.from = from;
        this.to = to;
    }
    
    /**
     * Empty constructor that will require "from" and "to" to be set using setters.
     */
    public Transform() {
    }

    /**
     * Get value of transform string in "from".
     * 
     * @return the from
     */
    public String getFrom() {
        return from;
    }

    /**
     * Set value of transform string "from".
     * 
     * @param from the from to set
     */
    public void setFrom(String from) {
        this.from = from;
    }

    /**
     * Get value of transform string in "to".
     * 
     * @return the to
     */
    public String getTo() {
        return to;
    }

    /**
     * Set value of transform string "to".
     * 
     * @param to the to to set
     */
    public void setTo(String to) {
        this.to = to;
    }
}
