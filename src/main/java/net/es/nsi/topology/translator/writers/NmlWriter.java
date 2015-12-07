package net.es.nsi.topology.translator.writers;

import java.io.IOException;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import net.es.nsi.topology.translator.jaxb.NmlParser;
import net.es.nsi.topology.translator.jaxb.nml.NmlTopologyType;
import net.es.nsi.topology.translator.jaxb.nml.ObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Write an NML JAXB object to the specified file.
 *
 * @author hacksaw
 */
public class NmlWriter {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final String file;

    private final NmlParser nmlParser;

    /**
     * Create a writer for the specified file.
     *
     * @param file file to store XML document.
     */
    public NmlWriter(String file) {
        this.nmlParser = NmlParser.getInstance();
        this.file = file;
    }

    /**
     * Write NML JAXB object to file.
     *
     * @param nml NML JAXB object.
     * @throws IOException If writing of document to file fails.
     */
    public void writeFile(NmlTopologyType nml) throws IOException {
        ObjectFactory factory = new ObjectFactory();
        JAXBElement<NmlTopologyType> createTopology = factory.createTopology(nml);

        try {
            nmlParser.writeFile(createTopology, file);
        } catch (JAXBException ex) {
            log.error("Failed to write NML topology to file " + file, ex);
        }
    }
}
