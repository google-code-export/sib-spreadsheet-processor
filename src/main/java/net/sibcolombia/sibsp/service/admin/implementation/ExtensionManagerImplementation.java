package net.sibcolombia.sibsp.service.admin.implementation;

import org.gbif.utils.HttpUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.sibcolombia.sibsp.action.BaseAction;
import net.sibcolombia.sibsp.configuration.ApplicationConfig;
import net.sibcolombia.sibsp.configuration.ConfigWarnings;
import net.sibcolombia.sibsp.configuration.Constants;
import net.sibcolombia.sibsp.configuration.DataDir;
import net.sibcolombia.sibsp.interfaces.ResourceManager;
import net.sibcolombia.sibsp.model.Extension;
import net.sibcolombia.sibsp.model.Resource;
import net.sibcolombia.sibsp.model.factory.ExtensionFactory;
import net.sibcolombia.sibsp.service.BaseManager;
import net.sibcolombia.sibsp.service.DeletionNotAllowedException;
import net.sibcolombia.sibsp.service.DeletionNotAllowedException.Reason;
import net.sibcolombia.sibsp.service.InvalidConfigException;
import net.sibcolombia.sibsp.service.InvalidConfigException.TYPE;
import net.sibcolombia.sibsp.service.RegistryException;
import net.sibcolombia.sibsp.service.admin.ExtensionManager;
import net.sibcolombia.sibsp.service.registry.RegistryManager;
import net.sibcolombia.sibsp.struts2.SimpleTextProvider;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

@Singleton
public class ExtensionManagerImplementation extends BaseManager implements ExtensionManager {

  public static class RegisteredExtensions {

    private List<Extension> extensions = new ArrayList<Extension>();
    private final RegistryManager registryManager;

    @Inject
    public RegisteredExtensions(RegistryManager registryManager) {
      this.registryManager = registryManager;
    }

    public List<Extension> getCoreTypes() {
      List<Extension> coreTypes = new ArrayList<Extension>();
      for (Extension ext : extensions) {
        if (Constants.DWC_ROWTYPE_OCCURRENCE.equals(normalizeRowType(ext.getRowType()))) {
          coreTypes.add(ext);
        }
        if (Constants.DWC_ROWTYPE_TAXON.equals(normalizeRowType(ext.getRowType()))) {
          coreTypes.add(ext);
        }
      }
      return coreTypes;
    }

    public List<Extension> getExtensions() {
      return extensions;
    }

    public boolean isLoaded() {
      return !extensions.isEmpty();
    }

    /**
     * Load list of registered extensions from Registry.
     */
    public void load() throws RegistryException {
      extensions = registryManager.getExtensions();
    }
  }

  // logging
  private static final Logger log = Logger.getLogger(ExtensionManagerImplementation.class);
  private final Map<String, Extension> extensionsByRowtype = new HashMap<String, Extension>();
  protected static final String CONFIG_FOLDER = ".extensions";
  private final ExtensionFactory factory;
  private final HttpUtil downloader;
  private final String TAXON_KEYWORD = "dwc:taxon";
  private final String OCCURRENCE_KEYWORD = "dwc:occurrence";
  private final ResourceManager resourceManager;
  private final ConfigWarnings warnings;

  private final RegisteredExtensions registered;

  // create instance of BaseAction - allows class to retrieve i18n terms via getText()
  private final BaseAction baseAction;

  @Inject
  public ExtensionManagerImplementation(ApplicationConfig config, DataDir dataDir, ExtensionFactory factory,
    ResourceManager resourceManager, HttpUtil httpUtil, RegisteredExtensions registered, ConfigWarnings warnings,
    SimpleTextProvider textProvider) {
    super(config, dataDir);
    this.factory = factory;
    this.resourceManager = resourceManager;
    this.downloader = httpUtil;
    this.registered = registered;
    this.warnings = warnings;
    baseAction = new BaseAction(textProvider, config);
  }

  public static String normalizeRowType(String rowType) {
    // occurrence alternatives
    if ("http://rs.tdwg.org/dwc/terms/DarwinCore".equalsIgnoreCase(rowType)
      || "http://rs.tdwg.org/dwc/xsd/simpledarwincore/".equalsIgnoreCase(rowType)
      || "http://rs.tdwg.org/dwc/terms/SimpleDarwinCore".equalsIgnoreCase(rowType)
      || "http://rs.tdwg.org/dwc/dwctype/Occurrence".equalsIgnoreCase(rowType)
      || "http://rs.tdwg.org/dwc/xsd/simpledarwincore/SimpleDarwinRecord".equalsIgnoreCase(rowType)) {
      return Constants.DWC_ROWTYPE_OCCURRENCE;
    }

    // taxon alternatives
    if ("http://rs.tdwg.org/dwc/dwctype/Taxon".equalsIgnoreCase(rowType)) {
      return Constants.DWC_ROWTYPE_TAXON;
    }

    return rowType;
  }

  @Override
  public void delete(String rowType) throws DeletionNotAllowedException {
    if (extensionsByRowtype.containsKey(rowType)) {
      // check if its used by some resources
      for (Resource r : resourceManager.list()) {
        if (!r.getMappings(rowType).isEmpty()) {
          String msg = "Extension mapped in resource " + r.getShortname();
          log.warn(msg);
          throw new DeletionNotAllowedException(Reason.EXTENSION_MAPPED, msg);
        }
      }
      // delete
      extensionsByRowtype.remove(rowType);
      File f = getExtensionFile(rowType);
      if (f.exists()) {
        f.delete();
      } else {
        log.warn("Extension doesnt exist locally, cant delete " + rowType);
      }
    } else {
      log.warn("Extension not installed locally, cant delete " + rowType);
    }
  }

  @Override
  public Extension get(String rowType) {
    return extensionsByRowtype.get(normalizeRowType(rowType));
  }

  private File getExtensionFile(String rowType) {
    String filename = rowType.replaceAll("[/.:]+", "_") + ".xml";
    return dataDir.configFile(CONFIG_FOLDER + "/" + filename);
  }

  @Override
  public synchronized Extension install(URL url) throws InvalidConfigException {
    Extension ext = null;
    // download extension into local file first for subsequent IPT startups
    // final filename is based on rowType which we dont know yet - create a tmp file first
    File tmpFile = dataDir.configFile(CONFIG_FOLDER + "/tmp-extension.xml");
    try {
      // TODO: use StatusLine to determine if download was successful
      downloader.download(url, tmpFile);
      log.info("Successfully downloaded Extension " + url);
      // finally read in the new file and create the extension object
      ext = loadFromFile(tmpFile);
      if (ext != null && ext.getRowType() != null) {
        // rename tmp file into final version
        File localFile = getExtensionFile(ext.getRowType());
        FileUtils.moveFile(tmpFile, localFile);
      } else {
        log.error("Extension lacking required rowType!");
      }
    } catch (InvalidConfigException e) {
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      log.error(e);
      throw new InvalidConfigException(TYPE.INVALID_EXTENSION, "Error installing extension " + url, e);
    }
    return ext;
  }

  /**
   * Load all registered extensions from registry (if they haven't been loaded yet), and install core extensions.
   */
  @Override
  public void installCoreTypes() {
    List<Extension> extensions;
    try {
      if (!registered.isLoaded()) {
        registered.load();
      }
      extensions = registered.getCoreTypes();
      for (Extension ext : extensions) {
        try {
          install(ext.getUrl());
        } catch (Exception e) {
          log.debug(e);
        }
      }
    } catch (RegistryException e) {
      // log as specific error message as possible about why the Registry error occurred
      String msg = RegistryException.logRegistryException(e.getType(), baseAction);
      // add startup error message about Registry error
      warnings.addStartupError(msg);
      log.error(msg);

      // add startup error message that explains the consequence of the Registry error
      msg = baseAction.getText("admin.config.extensions.couldnt.load", new String[] {config.getRegistryUrl()});
      warnings.addStartupError(msg);
      log.error(msg);
    }
  }

  @Override
  public List<Extension> list() {
    return new ArrayList<Extension>(extensionsByRowtype.values());
  }

  @Override
  public List<Extension> list(String coreRowType) {
    if (coreRowType != null && coreRowType.equalsIgnoreCase(Constants.DWC_ROWTYPE_OCCURRENCE)) {
      return search(OCCURRENCE_KEYWORD, true, false);
    } else if (coreRowType != null && coreRowType.equalsIgnoreCase(Constants.DWC_ROWTYPE_TAXON)) {
      return search(TAXON_KEYWORD, true, false);
    } else {
      return list();
    }
  }

  @Override
  public List<Extension> listCore() {
    List<Extension> list = new ArrayList<Extension>();
    Extension e = get(Constants.DWC_ROWTYPE_OCCURRENCE);
    if (e != null) {
      list.add(e);
    }
    e = get(Constants.DWC_ROWTYPE_TAXON);
    if (e != null) {
      list.add(e);
    }
    return list;
  }

  @Override
  public int load() {
    File extensionDir = dataDir.configFile(CONFIG_FOLDER);
    int counter = 0;
    if (extensionDir.isDirectory()) {
      List<File> extensionFiles = new ArrayList<File>();
      FilenameFilter ff = new SuffixFileFilter(".xml", IOCase.INSENSITIVE);
      extensionFiles.addAll(Arrays.asList(extensionDir.listFiles(ff)));
      for (File ef : extensionFiles) {
        try {
          loadFromFile(ef);
          counter++;
        } catch (InvalidConfigException e) {
          warnings.addStartupError("Cant load local extension definition " + ef.getAbsolutePath(), e);
        }
      }
    }
    return counter;
  }

  /**
   * Reads a local extension file into manager cache.
   */
  private Extension loadFromFile(File localFile) throws InvalidConfigException {
    InputStream fileIn = null;
    Extension ext = null;
    try {
      fileIn = new FileInputStream(localFile);
      ext = factory.build(fileIn);
      // normalise rowtype
      ext.setRowType(normalizeRowType(ext.getRowType()));
      // keep vocab in local lookup
      extensionsByRowtype.put(ext.getRowType(), ext);
      log.info("Successfully loaded extension " + ext.getRowType());
    } catch (FileNotFoundException e) {
      log.error("Cant find local extension file", e);
      throw new InvalidConfigException(TYPE.INVALID_EXTENSION, "Cant find local extension file");
    } catch (IOException e) {
      log.error("Cant access local extension file", e);
      throw new InvalidConfigException(TYPE.INVALID_EXTENSION, "Cant access local extension file");
    } catch (SAXException e) {
      log.error("Cant parse local extension file", e);
      throw new InvalidConfigException(TYPE.INVALID_EXTENSION, "Cant parse local extension file");
    } catch (ParserConfigurationException e) {
      log.error("Cant create sax parser", e);
      throw new InvalidConfigException(TYPE.INVALID_EXTENSION, "Cant create sax parser");
    } finally {
      if (fileIn != null) {
        try {
          fileIn.close();
        } catch (IOException e) {
        }
      }
    }
    return ext;
  }

  @Override
  public List<Extension> search(String keyword) {
    return search(keyword, false, false);
  }

  private List<Extension> search(String keyword, boolean includeEmptySubject, boolean includeCoreExtensions) {
    List<Extension> list = new ArrayList<Extension>();
    keyword = StringUtils.trimToNull(keyword);
    if (keyword != null) {
      keyword = keyword.toLowerCase();
      for (Extension e : extensionsByRowtype.values()) {
        if (!includeCoreExtensions && e.isCore()) {
          continue;
        }
        if (includeEmptySubject && StringUtils.trimToNull(e.getSubject()) == null
          || StringUtils.containsIgnoreCase(e.getSubject(), keyword)) {
          list.add(e);
        }
      }
    }
    return list;
  }
}