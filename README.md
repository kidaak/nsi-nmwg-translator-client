# nsi-nmwg-translator-client

This is a client application used to translate NMWG (OSCARS) topologies to NML (NSI) topologies.  The client also generates the OSCARS nsa.json file, and publishes both NSA description and NML topology documents to the DDS if needed.

## Options
The runtime configuration can be controlled through a combination of command line, system properties, and configuration file options.  The command line options are defined as follows:

```
java -jar translator.jar [-basedir <application directory>] [-configdir <configDir>] [-configfile <filename>] [-debug]

Options
	-basedir <application directory>
		The base directory used as the root for all default relative paths, and any
		relative paths defined in the configuration file.
		
	-configdir <configuration directory>
		The configuration directory holding runtime configuration information.  Default
		is $basedir/config.
		
	-configfile <configuration filename>
		The configuration file.  Default is $configdir/config.xml.
		
	-debug
		Enable Jersey debug for HTTP message tracing.
	
```

## System Properties
The following system properties are supported as an alternative to specifying on the comamnd line or in the configuration file:

  - basedir
  - configdir
  - configfile
  - debug
  - log4j.configuration
  - javax.net.ssl.keyStore
  - javax.net.ssl.keyStorePassword
  - javax.net.ssl.keyStoreType
  - javax.net.ssl.trustStore
  - javax.net.ssl.trustStorePassword
  - javax.net.ssl.trustStoreType


## Configuration file
The following XML document controls the runtime configuration.



```
<?xml version="1.0" encoding="UTF-8"?>
<tns:configuration xmlns:tns="http://schemas.es.net/nsi/2014/12/topology/translator/configuration">
    <!-- Location of the NMGW perfSONAR service. -->
    <nmwg>
        <baseURL>https://ndb7.net.internet2.edu/TopologyViewer/</baseURL>
        <parameters type="ts_instance">http://dcn-ts.internet2.edu:8012/perfSONAR_PS/services/topology</parameters>
    </nmwg>
    
    <!-- [Required] Location of the NSA description document for this uPA. -->
    <nsa>config/nsa-esnet.xml</nsa>
    
    <!-- [Optional] Location of the target DDS server for publishing of documents. -->
    <dds>http://localhost:8401/dds</dds>
    
    <!-- [Optional] Output file of the NML topology document. -->
    <topology>output/topology-esnet.xml</topology>
    
    <!-- [Optional] Output file for the OSCARS nsa.json file.
    <mapping>output/mapping-esnet.json</mapping>
    
    <!-- Lifetime in seconds of the generated documents. -->
    <lifeTime>5184000</lifeTime>
    
    <!-- ServiceDefintion parameters used to drive the NML document generation. -->
    <serviceDefintion id="EVTS.A-GOLE">
        <name>GLIF Automated GOLE Ethernet VLAN Transfer Service</name>
        <encoding>http://schemas.ogf.org/nml/2012/10/ethernet</encoding>
        <labelSwapping>true</labelSwapping>
        <labelType>http://schemas.ogf.org/nml/2012/10/ethernet#vlan</labelType>
        <serviceType>http://services.ogf.org/nsi/2013/07/definitions/EVTS.A-GOLE</serviceType>
        
        <!-- [Optional] Include ports in output based on the following substring matches. Default is to include all ports. -->
        <include>bnl-</include>
        <include>fnal-</include>
        <include>manlan</include>
        <include>xo-osf-rt1</include>
        <include>star-tb1</include>
        <include>lbl-mr2:xe-8_2_0</include>
        <include>pnwg-cr5:10_1_2</include>
        <include>nwg-cr5:10_1_7</include>
        <include>wash-cr5:6_1_1:wix</include>
        <include>star-cr5:10_1_8</include>
        <include>chic-cr5:5_1_1:al2s</include>
        <include>sunn-cr5:10_1_6</include>
        <include>sunn-cr5:8_1_1:caltech</include>
        <include>star-cr5:6_2_1:umich</include>
        <include>amst-cr5:3_1_1</include>
        
        <!-- [Optional] Exclude the following ports from output based on substring match. -->
        <exclude>poop-cr5:3_1_1</exclude>
    </serviceDefintion>
    
    <!-- [Optional] Override the default isAlias information on a port. -->
    <peering id="urn:ogf:network:es.net:2013::amst-cr5:3_1_1:+">
        <inbound>urn:ogf:network:netherlight.net:2013:production7:esnet-out</inbound>
        <outbound>urn:ogf:network:netherlight.net:2013:production7:esnet-in</outbound>
        <labels type="http://schemas.ogf.org/nml/2012/10/ethernet#vlan">1770-1790</labels>
    </peering>
    
    <!-- HTTPS client configuration information. -->
    <client production="true">
        <keyStore type="JKS">
            <file>config/keystore.jks</file>
            <password>changeit</password>
        </keyStore>
        <trustStore type="JKS">
            <file>config/truststore.jks</file>
            <password>changeit</password>
        </trustStore>
    </client>
</tns:configuration>
```