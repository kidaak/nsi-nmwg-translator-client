/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.topology.translator;

import com.google.common.base.Strings;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;

/**
 * Translator runtime configuration options specified as command line options.
 *
 * @author hacksaw
 */
public class Options {
    // Help line.
    private static final String COMMAND_LINE = "java -jar translator.jar [-basedir <application directory>] [-configdir <configDir>] [-configfile <filename>] [-debug]";

    // Command line arguments.
    private static final String ARGNAME_BASEDIR = "basedir";
    private static final String ARGNAME_CONFIGDIR = "configdir";
    private static final String ARGNAME_CONFIGFILE = "configfile";
    private static final String ARGNAME_DEBUG = "debug";

    // Default properties.
    private static final String DEFAULT_CONFIGDIR = "config/";
    private static final String DEFAULT_CONFIGFILE = "config.xml";
    private static final String DEFAULT_LOGF4J = "log4j.xml";

    // Runtime values.
    private String basedir;
    private String configdir;
    private String configfile;
    private boolean debug;

    /**
     * Process any command line arguments and set up associated system
     * properties for runtime components.
     *
     * @param args
     * @throws ParseException
     * @throws IOException
     */
    public Options(String[] args) throws ParseException, IOException {
        // Process each option and assign normalized value to corresponding
        // system parameters.
        org.apache.commons.cli.Options options = getOptions();
        try {
            // Parse the command line options.
            CommandLineParser parser = new GnuParser();
            CommandLine cmd = parser.parse(options, args);

            // Get the application base directory.
            basedir = getBasedir(cmd);
            System.setProperty(Properties.SYSTEM_PROPERTY_BASEDIR, basedir);

            // Now for the configuration directory path.
            configdir = getConfigdir(cmd);
            System.setProperty(Properties.SYSTEM_PROPERTY_CONFIGDIR, configdir);

            // See if the user overrode the default location of the configuration file.
            configfile = getConfigFile(cmd);
            System.setProperty(Properties.SYSTEM_PROPERTY_CONFIGFILE, configfile);

            // See if we need to enable protocol tracing.
            debug = getDebug(cmd);
            System.setProperty(Properties.SYSTEM_PROPERTY_DEBUG, Boolean.toString(debug));
        } catch (IOException | ParseException pe) {
            System.err.println("You did not provide the correct arguments, see usage below.\n");
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(COMMAND_LINE, options);
            throw pe;
        }
    }

    /**
     * Build supported command line options for parsing of parameter input.
     *
     * @return List of supported command line options.
     */
    private org.apache.commons.cli.Options getOptions() {
        // Create Options object to hold our command line options.
        org.apache.commons.cli.Options options = new org.apache.commons.cli.Options();

        Option basedirOption = new Option(ARGNAME_BASEDIR, true, "The runtime home directory for the application (defaults to \"user.dir\").");
        basedirOption.setRequired(false);
        options.addOption(basedirOption);

        Option configdirOption = new Option(ARGNAME_CONFIGDIR, true, "Location of configuration directory (defaults to ./config).");
        configdirOption.setRequired(false);
        options.addOption(configdirOption);

        Option configfileOption = new Option(ARGNAME_CONFIGFILE, true, "Path to the translation configuration file.");
        configfileOption.setRequired(false);
        options.addOption(configfileOption);

        Option debugOption = new Option(ARGNAME_DEBUG, false, "If specified enables debug tracing in Jersey.");
        debugOption.setRequired(false);
        options.addOption(debugOption);

        return options;
    }

    /**
     * Processes the "basedir" command line and system property option.
     *
     * @param cmd Commands entered by the user.
     * @return The configured basedir.
     * @throws IOException
     */
    private String getBasedir(CommandLine cmd) throws IOException {
        // Get the application base directory.
        String dir = System.getProperty(Properties.SYSTEM_PROPERTY_BASEDIR);
        dir = cmd.getOptionValue(ARGNAME_BASEDIR, dir);
        if(dir == null || dir.isEmpty()) {
            dir = System.getProperty("user.dir");
        }

        try {
            dir = Paths.get(dir).toRealPath().toString();
        } catch (IOException ex) {
            System.err.println("Error: Base directory not found " + dir + "\n");
            throw ex;
        }

        return dir;
    }

    /**
     * Processes the "configdir" command line and system property option.
     * @param cmd Commands entered by the user.
     * @param basedir The base directory for the application (install directory).
     * @return The configured configdir.
     * @throws IOException
     */
    private String getConfigdir(CommandLine cmd) throws IOException {
        String dir = System.getProperty(Properties.SYSTEM_PROPERTY_CONFIGDIR);
        dir = cmd.getOptionValue(ARGNAME_CONFIGDIR, dir);

        Path configPath;
        if(dir == null || dir.isEmpty()) {
            configPath = Paths.get(basedir, DEFAULT_CONFIGDIR);
        }
        else {
            configPath = Paths.get(dir);
            if (!configPath.isAbsolute()) {
                configPath = Paths.get(basedir, configPath.toString());
            }
        }

        try {
            dir = configPath.toRealPath().toString();
        } catch (IOException ex) {
            System.err.println("Error: Configuration directory not found " + dir + "\n");
            throw ex;
        }

        return dir;
    }

    /**
     * Processes the "configfile" command line and system property option.
     *
     * @param cmd Commands entered by the user.
     * @param configdir The application configuration directory.
     * @return The configuration file path.
     * @throws IOException
     */
    private String getConfigFile(CommandLine cmd) throws IOException {
        String file = System.getProperty(Properties.SYSTEM_PROPERTY_CONFIGFILE);
        file = cmd.getOptionValue(ARGNAME_CONFIGFILE, file);

        System.out.println("file=" + file);

        Path path;
        if (Strings.isNullOrEmpty(file)) {
            path = Paths.get(basedir, DEFAULT_CONFIGFILE);
        }
        else {
            path = Paths.get(file);
            if (!path.isAbsolute()) {
                path = Paths.get(basedir, path.toString());
            }
        }

        try {
            file = path.toRealPath().toString();
        } catch (IOException ex) {
            System.err.println("Error: Configuration file not found " + path.toString() + "\n");
            throw ex;
        }

        return file;
    }

    /**
     * Process the "debug" command line and system property option.
     *
     * @param cmd Commands entered by the user.
     * @return true if debug is enabled, false otherwise.
     */
    private boolean getDebug(CommandLine cmd) {
        boolean sys = Boolean.getBoolean(System.getProperty(Properties.SYSTEM_PROPERTY_DEBUG, "false"));
        boolean com = cmd.hasOption(ARGNAME_DEBUG);
        return (sys | com);
    }

    /**
     * Get the Log4J configuration file.
     *
     * @return Log4J configuration file.
     * @throws IOException If path to configuration files is invalid.
     */
    public String getLog4jConfig() throws IOException {
        Path realPath;

        // Check the log4j system property first.
        String log4jConfig = System.getProperty(Properties.SYSTEM_PROPERTY_LOG4J);
        if (log4jConfig == null) {
            // No system property so we take a guess based on local information.
            realPath = Paths.get(configdir, DEFAULT_LOGF4J).toRealPath();
        }
        else {
            realPath = Paths.get(log4jConfig).toRealPath();
        }

        return realPath.toString();
    }

    /**
     * Get the base directory property.
     *
     * @return the basedir
     */
    public String getBasedir() {
        return basedir;
    }

    /**
     * Set the base directory property.
     *
     * @param basedir the basedir to set
     */
    public void setBasedir(String basedir) {
        this.basedir = basedir;
    }

    /**
     * Get the configuration directory property.
     *
     * @return the configdir
     */
    public String getConfigdir() {
        return configdir;
    }

    /**
     * Set the configuration directory property.
     *
     * @param configdir the configdir to set
     */
    public void setConfigdir(String configdir) {
        this.configdir = configdir;
    }

    /**
     * Get the configuration file property.
     *
     * @return the configfile
     */
    public String getConfigfile() {
        return configfile;
    }

    /**
     * Set the configuration file property.
     *
     * @param configfile the file to set
     */
    public void setConfigfile(String configfile) {
        this.configfile = configfile;
    }

    /**
     * Is debug enabled?
     *
     * @return the debug
     */
    public boolean isDebug() {
        return debug;
    }

    /**
     * Set debug status.
     *
     * @param debug the debug to set
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }
}
