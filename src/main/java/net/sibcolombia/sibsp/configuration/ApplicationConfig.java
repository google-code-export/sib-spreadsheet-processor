package net.sibcolombia.sibsp.configuration;

import org.gbif.ipt.utils.InputStreamUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

@Singleton
public class ApplicationConfig {

  protected enum REGISTRY_TYPE {
    PRODUCTION, DEVELOPMENT
  }

  protected static final String DATADIR_PROPFILE = "sibsp.properties";
  private static final String CLASSPATH_PROPFILE = "application.properties";
  public static final String ROOTURL = "sibsp.rootURL";
  public static final String PROXY = "proxy";
  public static final String DEBUG = "debug";
  private DataDir dataDir;
  private Properties properties = new Properties();
  private static final Logger LOG = Logger.getLogger(ApplicationConfig.class);
  private REGISTRY_TYPE type;

  private ApplicationConfig() {
  }

  @Inject
  public ApplicationConfig(DataDir dataDir) {
    this.dataDir = dataDir;
    loadConfigurationSettings();
  }

  public boolean debug() {
    return "true".equalsIgnoreCase(properties.getProperty(DEBUG));
  }

  public DataDir getDataDir() {
    return dataDir;
  }

  public String getProperty(String key) {
    return properties.getProperty(key);
  }

  public REGISTRY_TYPE getRegistryType() {
    return type;
  }

  public String getRegistryUrl() {
    if (REGISTRY_TYPE.PRODUCTION == type) {
      return getProperty("dev.registry.url");
    }
    return getProperty("dev.registrydev.url");
  }

  public String getRootURL() {
    String root = properties.getProperty(ROOTURL);
    while (root != null && root.endsWith("/")) {
      root = root.substring(0, root.length() - 1);
    }
    return root;
  }

  protected void loadConfigurationSettings() {
    InputStreamUtils streamUtils = new InputStreamUtils();
    InputStream configStream = streamUtils.classpathStream(CLASSPATH_PROPFILE);
    try {
      Properties properties = new Properties();
      if (configStream == null) {
        LOG.error("Could not load default configuration from application.properties in classpath");
      } else {
        properties.load(configStream);
        LOG.debug("Loaded default configuration from application.properties in classpath");
      }
      if (dataDir.dataDir != null && dataDir.dataDir.exists()) {
        // Read configuration data
        File userConfigurationFile = new File(dataDir.dataDir, "config/" + DATADIR_PROPFILE);
        if (userConfigurationFile.exists()) {
          try {
            properties.load(new FileInputStream(userConfigurationFile));
            LOG.debug("Loaded user configuration from " + userConfigurationFile.getAbsolutePath());
          } catch (IOException exception) {
            LOG.warn(
              "DataDir configured, but failed to load user configuration from "
                + userConfigurationFile.getAbsolutePath(), exception);
          }
        }
      }
      this.properties = properties;
    } catch (IOException exception) {
      LOG.error("Failed to load application configuration from application.properties", exception);
    }
  }

  protected void saveConfig() throws IOException {
    // save property config file
    OutputStream out = null;
    try {
      File userCfgFile = new File(dataDir.dataDir, "config/" + DATADIR_PROPFILE);
      out = new FileOutputStream(userCfgFile);

      Properties props = (Properties) properties.clone();
      Enumeration<?> e = props.propertyNames();
      while (e.hasMoreElements()) {
        String key = (String) e.nextElement();
        if (key.startsWith("dev.")) {
          props.remove(key);
        }
      }
      props.store(out, "SiB-SP configuration, last saved " + new Date().toString());
      out.close();
    } finally {
      if (out != null) {
        out.close();
      }
    }
  }

  public void setProperty(String key, String value) {
    properties.setProperty(key, StringUtils.trimToEmpty(value));
  }
}
