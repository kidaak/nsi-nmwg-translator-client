package net.es.nsi.topology.translator.utilities;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A file system path building helper class that will convert relative path
 * strings into equivalent absolute paths.
 * 
 * @author hacksaw
 */
public class PathBuilder {
    private final String basedir;
    
    /**
     * Creates the PathBuilder using the base directory that will anchor any
     * relatives paths provided.
     * 
     * @param basedir The based directory that anchors any relative paths resolved.
     * @throws IOException If the base directory does not exist.
     */
    public PathBuilder(String basedir) throws IOException {
        this.basedir = Paths.get(basedir).toRealPath().toString();
    }
    
    /**
     * Builds absolute path if required and verifies the specified file system
     * entity exists.
     * 
     * @param inPath That to validate.
     * @return String representing the absolute path.
     * @throws IOException 
     */
    public String getRealPath(String inPath) throws IOException {
        Path outPath = Paths.get(inPath);
        if (!outPath.isAbsolute()) {
            outPath = Paths.get(basedir, inPath);
        }

        return outPath.toRealPath().toString();
    }
    
    /**
     * Builds absolute path if required but does not verify if the entity
     * exists on the file system.  
     * 
     * @param inPath That to validate.
     * @return String representing the absolute path.
     * @throws IOException 
     */
    public String getAbsolutePath(String inPath) throws IOException {
        Path outPath = Paths.get(inPath);
        if (!outPath.isAbsolute()) {
            outPath = Paths.get(basedir, inPath);
        }

        return outPath.toString();
    }   
}
