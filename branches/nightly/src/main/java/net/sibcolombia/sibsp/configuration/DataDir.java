package net.sibcolombia.sibsp.configuration;

import org.gbif.ipt.utils.InputStreamUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.google.inject.Singleton;
import net.sibcolombia.sibsp.model.Resource;
import net.sibcolombia.sibsp.model.Source;
import net.sibcolombia.sibsp.service.InvalidConfigException;
import net.sibcolombia.sibsp.service.InvalidConfigException.TYPE;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

@Singleton
public class DataDir implements ServletContextListener {

  public static final String CONFIG_DIR = "config";
  public static final String RESOURCES_DIR = "resources";

  public static final String TMP_DIR = "temp";

  public static final String LOGGING_DIR = "logs";
  public static final String EML_XML_FILENAME = "eml.xml";
  private static Logger log = Logger.getLogger(DataDir.class);

  protected File dataDir;

  private File dataDirSettingFile;
  private final InputStreamUtils streamUtils = new InputStreamUtils();
  private int tmpCounter = 0;

  private DataDir() {

  }

  public static DataDir buildFromLocationFile(File dataDirSettingFile) {
    DataDir dataDirectory = new DataDir();
    dataDirectory.dataDirSettingFile = dataDirSettingFile;
    if (dataDirSettingFile != null && dataDirSettingFile.exists()) {
      // A data directory has been configured already. Let´s see where that is
      String dataDirPath = null;
      try {
        dataDirPath = StringUtils.trimToNull(FileUtils.readFileToString(dataDirSettingFile));
        if (dataDirPath != null) {
          log.info("SiB-SP Data directory configured at " + dataDirPath);
          dataDirectory.dataDir = new File(dataDirPath);
        }
      } catch (IOException exception) {
        log.error(
          "Failed to read the datadir location settings file in WEB-INF at " + dataDirSettingFile.getAbsolutePath(),
          exception);
      }
    } else {
      log.warn("Datadir location settings file in WEB-INF not found. Continue without data directory.");
    }
    return dataDirectory;
  }

  /**
   * @return the resourcesDir
   */
  public static String getResourcesDir() {
    return RESOURCES_DIR;
  }

  private void assureDirExists(File f) {
    if (f != null && !f.exists()) {
      f.mkdirs();
    }
  }

  private void assureParentExists(File f) {
    if (f != null && !f.getParentFile().exists()) {
      f.getParentFile().mkdirs();
    }
  }

  protected void clearTmp() throws IOException {
    File tempDir = tmpFile("");
    FileUtils.forceMkdir(tempDir);
    FileUtils.cleanDirectory(tempDir);
    log.debug("Cleared temporary folder");
  }

  /**
   * Constructs an absolute path to a file within the config folder of the data dir.
   * 
   * @param path the relative path within the config folder
   */
  public File configFile(String path) {
    return dataFile(CONFIG_DIR + "/" + path);
  }

  @Override
  public void contextDestroyed(ServletContextEvent arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void contextInitialized(ServletContextEvent arg0) {
    // TODO Auto-generated method stub

  }

  private void createDefaultDir() throws IOException {
    // create config, resources and lucene directories
    File configDir = dataFile(CONFIG_DIR);
    File resourcesDir = dataFile(RESOURCES_DIR);
    File loggingDir = dataFile(LOGGING_DIR);
    FileUtils.forceMkdir(configDir);
    FileUtils.forceMkdir(resourcesDir);
    FileUtils.forceMkdir(loggingDir);
    // copy default config files
    InputStream input = streamUtils.classpathStream("configurationFiles/sibsp.properties");
    if (input == null) {
      throw new InvalidConfigException(TYPE.CONFIG_WRITE,
        "Cannot read required classpath resources to create new data dir!");
    }
    org.gbif.ipt.utils.FileUtils.copyStreamToFile(input, configFile(ApplicationConfig.DATADIR_PROPFILE));

    input = streamUtils.classpathStream("configurationFiles/about.ftl");
    if (input == null) {
      throw new InvalidConfigException(TYPE.CONFIG_WRITE,
        "Cannot read required classpath resources to create new data dir!");
    }
    org.gbif.ipt.utils.FileUtils.copyStreamToFile(input, configFile("about.ftl"));

    input = streamUtils.classpathStream("configurationFiles/columncoreocurrencemapping.csv");
    if (input == null) {
      throw new InvalidConfigException(TYPE.CONFIG_WRITE,
        "Cannot read required classpath resources to create new data dir!");
    }
    org.gbif.ipt.utils.FileUtils.copyStreamToFile(input, configFile("columncoreocurrencemapping.csv"));

    input = streamUtils.classpathStream("configurationFiles/columncoretaxonmapping.csv");
    if (input == null) {
      throw new InvalidConfigException(TYPE.CONFIG_WRITE,
        "Cannot read required classpath resources to create new data dir!");
    }
    org.gbif.ipt.utils.FileUtils.copyStreamToFile(input, configFile("columncoretaxonmapping.csv"));

    input = streamUtils.classpathStream("configurationFiles/columnextensionmeasurementorfactsmapping.csv");
    if (input == null) {
      throw new InvalidConfigException(TYPE.CONFIG_WRITE,
        "Cannot read required classpath resources to create new data dir!");
    }
    org.gbif.ipt.utils.FileUtils.copyStreamToFile(input, configFile("columnextensionmeasurementorfactsmapping.csv"));

    input = streamUtils.classpathStream("configurationFiles/columnextensionresourcerelationshipmapping.csv");
    if (input == null) {
      throw new InvalidConfigException(TYPE.CONFIG_WRITE,
        "Cannot read required classpath resources to create new data dir!");
    }
    org.gbif.ipt.utils.FileUtils.copyStreamToFile(input, configFile("columnextensionresourcerelationshipmapping.csv"));

    log.info("Creating new default data dir");
  }

  /**
   * Basic method to convert a relative path within the data dir to an absolute path on the filesystem.
   * 
   * @param path the relative path within the data dir
   */
  public File dataFile(String path) {
    if (dataDir == null) {
      throw new IllegalStateException("No data dir has been configured yet");
    }
    // if (path.startsWith("/")){
    // return new File(path);
    // }
    File f = new File(dataDir, path);
    assureParentExists(f);
    return f;
  }

  public File getDataDir() {
    return dataDir;
  }

  /**
   * @return true if a working data directory is configured
   */
  public boolean isConfigured() {
    return dataDir != null && dataDir.exists();
  }

  /**
   * Constructs an absolute path to the logs folder of the data dir.
   */
  public File loggingDir() {
    return dataFile(LOGGING_DIR);
  }

  private void persistLocation() throws IOException {
    // persist location in WEB-INF
    FileUtils.writeStringToFile(dataDirSettingFile, dataDir.getAbsolutePath());
    log.info("SiB-SP DataDir location file in /WEB-INF changed to " + dataDir.getAbsolutePath());
  }

  public File resourceDwcaFile(String resourceName) {
    return dataFile(RESOURCES_DIR + "/" + resourceName + "/dwca.zip");
  }

  public File resourceEmlFile(String resourceName, @Nullable Integer version) {
    String fn;
    if (version == null) {
      fn = EML_XML_FILENAME;
    } else {
      fn = "eml-" + version + ".xml";
    }
    return dataFile(RESOURCES_DIR + "/" + resourceName + "/" + fn);
  }

  public File resourceFile(Resource resource, String path) {
    if (resource == null) {
      return null;
    }
    return resourceFile(resource.getUniqueID().toString(), path);
  }

  /**
   * Constructs an absolute path to a file within a resource folder inside the data dir
   * 
   * @param path the relative path within the individual resource folder
   */
  public File resourceFile(String resourceName, String path) {
    return dataFile(RESOURCES_DIR + "/" + resourceName + "/" + path);
  }

  public File resourcePublicationLogFile(String resourceName) {
    return dataFile(RESOURCES_DIR + "/" + resourceName + "/publication.log");
  }

  /**
   * File for the only & current rtf file representing the eml metadata for data publishers in RTF format.
   */
  public File resourceRtfFile(String resourceName) {
    String fn = resourceName + ".rtf";
    return dataFile(RESOURCES_DIR + "/" + resourceName + "/" + fn);
  }

  public File resourceRtfFile(String resourceName, @Nullable Integer version) {
    String fn;
    if (version == null) {
      fn = "filertf.rtf";
    } else {
      fn = "filertf" + "-" + version + ".rtf";
    }
    return dataFile(RESOURCES_DIR + "/" + resourceName + "/" + fn);
  }

  /**
   * Sets the path to the data directory for the entire application and persists it in the /WEB-INF folder. This method
   * does not reload any configuration though - so normally setting the dataDir should be done through the
   * ConfigManager
   * which calls this method but also reloads all user configurations.
   * 
   * @return true if a new data dir was created, false when an existing was read
   */
  public boolean setDataDir(File dataDir) throws InvalidConfigException {
    if (dataDir == null) {
      throw new NullPointerException("DataDir file required");
    } else {

      this.dataDir = dataDir;
      File configDir = configFile("");

      if (dataDir.exists() && (!dataDir.isDirectory() || dataDir.list().length > 0)) {
        // EXISTING file or directory with content: make sure its an SiB-SP datadir - otherwise break
        if (dataDir.isDirectory()) {
          // check if this directory contains a config folder - if not copy empty default dir from classpath
          if (!configDir.exists() || !configDir.isDirectory()) {
            this.dataDir = null;
            throw new InvalidConfigException(TYPE.INVALID_DATA_DIR, "DataDir " + dataDir.getAbsolutePath()
              + " exists already and is no SiB-SP data dir.");
          }
          log.info("Reusing existing data dir.");
          // persist location in WEB-INF
          try {
            persistLocation();
          } catch (IOException e) {
            log.error("Cant persist datadir location in WEBINF webapp folder", e);
          }
          return false;
        } else {
          this.dataDir = null;
          throw new InvalidConfigException(TYPE.INVALID_DATA_DIR, "DataDir " + dataDir.getAbsolutePath()
            + " is not a directory");
        }

      } else {
        // NEW datadir
        try {
          // create new main data dir. Populate later
          FileUtils.forceMkdir(dataDir);
          // test if we can write to the directory
          File testFile = new File(dataDir, "test.tmp");
          FileUtils.touch(testFile);
          // remove test file
          testFile.delete();
          // create new default data dir
          createDefaultDir();
          // all works fine - persist location in WEB-INF
          persistLocation();
          return true;
        } catch (IOException e) {
          log.error("New DataDir " + dataDir.getAbsolutePath() + " not writable", e);
          this.dataDir = null;
          throw new InvalidConfigException(InvalidConfigException.TYPE.NON_WRITABLE_DATA_DIR, "DataDir "
            + dataDir.getAbsolutePath() + " is not writable");
        }
      }
    }
  }

  public File sourceExcelFile(Resource resource, String fileName) {
    if (resource == null) {
      return null;
    }
    return resourceFile(resource.getUniqueID().toString(), "sources/" + fileName + ".txt");
  }

  public File sourceFile(Resource resource, Source source) {
    if (resource == null) {
      return null;
    }
    return resourceFile(resource.getUniqueID().toString(), "sources/" + source.getName() + ".txt");
  }

  public File sourceLogFile(String resourceName, String sourceName) {
    return dataFile(RESOURCES_DIR + "/" + resourceName + "/sources/" + sourceName + ".log");
  }

  public File tmpDir() {
    tmpCounter++;
    File dir = tmpFile("dir-" + tmpCounter);
    assureDirExists(dir);
    return dir;
  }

  public File tmpFile() {
    tmpCounter++;
    return tmpFile("file-" + tmpCounter + ".tmp");
  }

  public File tmpFile(String path) {
    return dataFile(TMP_DIR + "/" + path);
  }

  public File tmpFile(String prefix, String fileFileName) {
    UUID idOne = UUID.randomUUID();
    return tmpFile(prefix + idOne.toString() + fileFileName);
  }

}
