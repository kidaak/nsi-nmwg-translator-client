package net.es.nsi.topology.translator.jaxb;

/**
 * A singleton to load the very expensive JAXBContext once.
 *
 * @author hacksaw
 */
public class ConfigurationParser extends JaxbParser {
    private static final String PACKAGES = "net.es.nsi.topology.translator.jaxb.configuration";

    private ConfigurationParser() {
        super(PACKAGES);
    }

    /**
     * An internal static class that invokes our private constructor on object
     * creation.
     */
    private static class ParserHolder {
        public static final ConfigurationParser INSTANCE = new ConfigurationParser();
    }

    /**
     * Returns an instance of this singleton class.
     *
     * @return An NmlParser object of the NSAType.
     */
    public static ConfigurationParser getInstance() {
            return ParserHolder.INSTANCE;
    }
}
