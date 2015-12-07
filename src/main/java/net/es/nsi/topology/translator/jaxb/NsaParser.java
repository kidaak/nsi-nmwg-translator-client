package net.es.nsi.topology.translator.jaxb;

/**
 * A singleton to load the very expensive NMWG JAXBContext once.
 *
 * @author hacksaw
 */
public class NsaParser extends JaxbParser {
    private static final String PACKAGES = "net.es.nsi.topology.translator.jaxb.nsa";

    private NsaParser() {
        super(PACKAGES);
    }

    /**
     * An internal static class that invokes our private constructor on object
     * creation.
     */
    private static class ParserHolder {
        public static final NsaParser INSTANCE = new NsaParser();
    }

    /**
     * Returns an instance of this singleton class.
     *
     * @return An object of the NmwgParser.
     */
    public static NsaParser getInstance() {
            return ParserHolder.INSTANCE;
    }
}
