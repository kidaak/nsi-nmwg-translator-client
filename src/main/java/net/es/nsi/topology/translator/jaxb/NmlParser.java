package net.es.nsi.topology.translator.jaxb;

/**
 * A singleton to load the very expensive NMWG JAXBContext once.
 *
 * @author hacksaw
 */
public class NmlParser extends JaxbParser {
    private static final String PACKAGES = "net.es.nsi.topology.translator.jaxb.nml";

    private NmlParser() {
        super(PACKAGES);
    }

    /**
     * An internal static class that invokes our private constructor on object
     * creation.
     */
    private static class ParserHolder {
        public static final NmlParser INSTANCE = new NmlParser();
    }

    /**
     * Returns an instance of this singleton class.
     *
     * @return An object of the NmwgParser.
     */
    public static NmlParser getInstance() {
            return ParserHolder.INSTANCE;
    }
}
