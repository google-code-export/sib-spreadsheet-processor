package net.sibcolombia.sibsp.configuration;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import com.google.inject.Inject;
import net.sibcolombia.sibsp.action.BaseAction;
import net.sibcolombia.sibsp.interfaces.ConfigurationManager;
import net.sibcolombia.sibsp.model.Extension;
import net.sibcolombia.sibsp.service.InvalidConfigException;
import net.sibcolombia.sibsp.service.admin.ExtensionManager;
import net.sibcolombia.sibsp.struts2.SimpleTextProvider;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;


public class ConfigurationAction extends BaseAction {

  private static final long serialVersionUID = -5631929626796102335L;

  // logging
  protected static Logger log = Logger.getLogger(ConfigurationAction.class);

  protected ConfigurationManager configurationManager;
  private final DataDir dataDir;

  private final ExtensionManager extensionManager;

  // Action attributes
  protected String dataDirPath;
  protected String urlPath;
  protected String rootURL;

  @Inject
  public ConfigurationAction(SimpleTextProvider textProvider, ApplicationConfig config,
    ConfigurationManager configurationManager, DataDir dataDir, ExtensionManager extensionManager) {
    super(textProvider, config);
    this.configurationManager = configurationManager;
    this.dataDir = dataDir;
    this.extensionManager = extensionManager;
  }

  /**
   * Try to detect actual application root folder
   * 
   * @return rootURL as String
   */
  public String findRootURL() {
    // try to detect the baseURL if not configured yet!
    String applicationRoot = request.getRequestURL().toString().replaceAll(request.getServletPath(), "");
    log.info(getText("sibsp.application.log.autodetectRootURLMessage") + applicationRoot);
    return applicationRoot;
  }

  public String getDataDirPath() {
    return dataDirPath;
  }

  @Override
  public String getRootURL() {
    // try to detect default values if not yet configured
    if (StringUtils.trimToNull(config.getRootURL()) == null) {
      rootURL = findRootURL();
    } else {
      rootURL = config.getRootURL();
    }
    return rootURL;
  }


  public String getUrlPath() {
    return urlPath;
  }


  public void setDataDirPath(String dataDirPath) {
    this.dataDirPath = dataDirPath;
  }


  /**
   * This method is used to configure SIBSP for the first time.
   * 
   * @return Request status
   */
  public String setup() {
    boolean isValidUrl = false;
    if (isHttpPost() && dataDirPath != null && urlPath != null) {
      if (urlPath.isEmpty()) {
        addActionError(getText("admin.config.setup.urlpath.notnull"));
        return INPUT;
      }
      File dataDirectory = new File(dataDirPath.trim());
      try {
        if (dataDirectory.isAbsolute()) {
          boolean directoryCreated = configurationManager.setDataDir(dataDirectory);
          if (directoryCreated) {
            addActionMessage(getText("admin.config.setup.datadir.created"));
            configurationManager.loadDataDirConfig();
            List<Extension> list = extensionManager.listCore();

            if (list.isEmpty()) {
              // load all registered extensions from registry, and install core extensions
              extensionManager.installCoreTypes();
            }
          } else {
            addActionMessage(getText("admin.config.setup.datadir.reused"));
          }
          if (!urlPath.isEmpty()) {
            try {
              URL rootURL = new URL(urlPath);
              configurationManager.setBaseUrl(rootURL);
              configurationManager.saveConfig();
              isValidUrl = true;
            } catch (MalformedURLException exception) {

            }
          }
        } else {
          addActionError(getText("admin.config.setup.datadir.absolute", new String[] {dataDirPath}));
        }
      } catch (InvalidConfigException exception) {
        log.warn(getText("admin.config.setup.datadir.failed") + ": " + exception.getMessage(), exception);
        if (exception.getType() == InvalidConfigException.TYPE.NON_WRITABLE_DATA_DIR) {
          addActionError(getText("admin.config.setup.datadir.writable", new String[] {dataDirPath}));
        } else {
          addActionError(getText("admin.config.setup.datadir.error"));
        }
      }
    }
    if (dataDir.isConfigured() && isValidUrl) {
      // Configuration sucessfull
      return SUCCESS;
    }
    return INPUT;
  }


  public void setUrlPath(String urlPath) {
    this.urlPath = urlPath;
  }
}
