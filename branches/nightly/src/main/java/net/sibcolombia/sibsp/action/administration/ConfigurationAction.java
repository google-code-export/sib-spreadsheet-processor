package net.sibcolombia.sibsp.action.administration;

import java.net.MalformedURLException;
import java.net.URL;

import com.google.inject.Inject;
import net.sibcolombia.sibsp.action.POSTAction;
import net.sibcolombia.sibsp.configuration.ApplicationConfig;
import net.sibcolombia.sibsp.configuration.Constants;
import net.sibcolombia.sibsp.interfaces.ConfigurationManager;
import net.sibcolombia.sibsp.service.InvalidConfigException;
import net.sibcolombia.sibsp.struts2.SimpleTextProvider;
import org.apache.log4j.Logger;


public class ConfigurationAction extends POSTAction {

  private static final long serialVersionUID = 398534098279369149L;

  // Log
  private static final Logger log = Logger.getLogger(ConfigurationAction.class);

  protected ConfigurationManager configurationManager;
  protected String rootURL;

  @Inject
  public ConfigurationAction(SimpleTextProvider textProvider, ApplicationConfig config,
    ConfigurationManager configurationManager) {
    super(textProvider, config);
    this.configurationManager = configurationManager;
  }

  public String getDataDir() {
    return config.getDataDir().dataFile("").getAbsolutePath();
  }

  @Override
  public String getRootURL() {
    return config.getRootURL();
  }

  /**
   * This is called when the new configuration is submitted.
   * 
   * @return SUCCESS if it is valid, or failure with a message if the entered configuration is invalid
   */
  @Override
  public String save() {
    log.info("Changing the SiBSP configuration");
    // base URL
    if (!stringEquals(rootURL, config.getRootURL())) {
      log.info("Changing the installation rootURL from [" + config.getRootURL() + "] to [" + rootURL + "]");
      try {
        URL Rurl = new URL(rootURL);
        configurationManager.setBaseUrl(Rurl);
        log.info("Installation rootURL successfully changed to[" + rootURL + "]");
        addActionMessage(getText("admin.config.rootUrl.changed"));
        addActionMessage(getText("admin.user.login"));
        session.remove(Constants.SESSION_USER);
        if (Rurl.getHost().equalsIgnoreCase("localhost") || Rurl.getHost().equalsIgnoreCase("127.0.0.1")
          || Rurl.getHost().equalsIgnoreCase(configurationManager.getHostName())) {
          addActionWarning(getText("admin.config.error.localhostURL"));
        }
      } catch (MalformedURLException e) {
        addActionError(getText("admin.config.error.invalidRootURL"));
        return INPUT;
      } catch (InvalidConfigException e) {
        if (e.getType() == InvalidConfigException.TYPE.INVALID_BASE_URL) {
          addActionError(getText("admin.config.rootUrl.invalidRootURL") + " " + rootURL);
        } else if (e.getType() == InvalidConfigException.TYPE.INACCESSIBLE_BASE_URL) {
          addActionError(getText("admin.config.rootUrl.inaccessible") + " " + rootURL);
        } else {
          addActionError(getText("admin.error.invalidConfiguration", new String[] {e.getMessage()}));
        }
        return INPUT;
      }
    }
    return SUCCESS;
  }

  public void setRootURL(String rootURL) {
    this.rootURL = rootURL;
  }

}
