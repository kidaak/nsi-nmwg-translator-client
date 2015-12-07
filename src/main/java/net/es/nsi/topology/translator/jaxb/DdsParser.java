package net.es.nsi.topology.translator.jaxb;

/**
 * A singleton to load the very expensive NMWG JAXBContext once.
 *
 * @author hacksaw
 */
public class DdsParser extends JaxbParser {
    private static final String PACKAGES = "net.es.nsi.topology.translator.jaxb.dds";

    private DdsParser() {
        super(PACKAGES);
    }

    /**
     * An internal static class that invokes our private constructor on object
     * creation.
     */
    private static class ParserHolder {
        public static final DdsParser INSTANCE = new DdsParser();
    }

    /**
     * Returns an instance of this singleton class.
     *
     * @return An object of the NmwgParser.
     */
    public static DdsParser getInstance() {
            return ParserHolder.INSTANCE;
    }
}
