package net.sibcolombia.sibsp.configuration;

import org.gbif.ipt.utils.InputStreamUtils;
import org.gbif.ipt.utils.LogFileAppender;
import org.gbif.utils.HttpUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.sibcolombia.sibsp.interfaces.ConfigurationManager;
import net.sibcolombia.sibsp.scheduler.SchedulerJob;
import net.sibcolombia.sibsp.service.BaseManager;
import net.sibcolombia.sibsp.service.InvalidConfigException;
import net.sibcolombia.sibsp.service.InvalidConfigException.TYPE;
import net.sibcolombia.sibsp.service.admin.ExtensionManager;
import net.sibcolombia.sibsp.service.admin.VocabulariesManager;
import net.sibcolombia.sibsp.service.portal.ResourceManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.LogManager;
import org.apache.log4j.xml.DOMConfigurator;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

@Singleton
public class ConfigurationManagerImplementation extends BaseManager implements ConfigurationManager {

  private final DefaultHttpClient client;
  private final InputStreamUtils streamUtils;
  private final ResourceManager resourceManager;
  private final ExtensionManager extensionManager;
  private final VocabulariesManager vocabularyManager;
  private final ConfigWarnings warnings;
  private final HttpUtil http;
  private static final String PATH_TO_CSS = "/css/style.css";

  @Inject
  public ConfigurationManagerImplementation(ApplicationConfig config, InputStreamUtils streamUtils, DataDir dataDir,
    ResourceManager resourceManager, ExtensionManager extensionManager, VocabulariesManager vocabularyManager,
    ConfigWarnings warnings, DefaultHttpClient client) {
    super(config, dataDir);
    this.streamUtils = streamUtils;
    this.resourceManager = resourceManager;
    this.extensionManager = extensionManager;
    this.vocabularyManager = vocabularyManager;
    this.warnings = warnings;
    this.client = client;
    this.http = new HttpUtil(client);
    if (dataDir.isConfigured()) {
      log.info("SiB-SP DataDir configured - loading its configuration");
      try {
        loadDataDirConfig();
      } catch (InvalidConfigException e) {
        log.error("Configuration problems existing. Watch your data dir! " + e.getMessage(), e);
      }
    } else {
      log.debug("SiB-SP DataDir not configured - no configuration loaded");
    }
  }

  /**
   * It Creates a HttpHost object with the string given by the user and verifies if there is a connection with this
   * host. If there is a connection with this host, it changes the current proxy host with this host. If don't it keeps
   * the current proxy.
   * 
   * @param proxy an URL with the format http://proxy.my-institution.com:8080.
   * @param hostTemp the actual proxy.
   * @throws InvalidConfigException If it can not connect to the proxy host or if the port number is no integer or if
   *         the proxy URL is not with the valid format http://proxy.my-institution.com:8080
   */
  private boolean changeProxy(HttpHost hostTemp, String proxy) {
    try {
      URL url = new URL(proxy);
      String[] var = proxy.split(":");
      HttpHost host;
      if (var.length > 2) {
        host = new HttpHost(url.getHost(), url.getPort());
      } else {
        host = new HttpHost(url.getHost());
      } // test that host really exists
      if (!http.verifyHost(host)) {
        if (hostTemp != null) {
          client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, hostTemp);
        }
        throw new InvalidConfigException(TYPE.INVALID_PROXY, "admin.config.error.connectionRefused");
      }
      log.info("Updating the proxy setting to: " + proxy);
      client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, host);
    } catch (NumberFormatException e) {
      if (hostTemp != null) {
        client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, hostTemp);
      }
      throw new InvalidConfigException(TYPE.INVALID_PROXY, "admin.config.error.invalidPort");
    } catch (MalformedURLException e) {
      if (hostTemp != null) {
        client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, hostTemp);
      }
      throw new InvalidConfigException(TYPE.INVALID_PROXY, "admin.config.error.invalidProxyURL");
    }
    return true;
  }

  @Override
  public boolean configurationComplete() {
    if (dataDir.isConfigured()) {
      createFolderCleaningJob();
    }
    return dataDir.isConfigured();
  }

  private void createFolderCleaningJob() {
    try {
      Scheduler scheduler = new StdSchedulerFactory().getScheduler();
      JobDetail currentJob = scheduler.getJobDetail(new JobKey("folderCleaning", "group1"));
      if (currentJob == null) {
        JobDetail job = JobBuilder.newJob(SchedulerJob.class).withIdentity("folderCleaning", "group1").build();
        job.getJobDataMap().put("setupFolder", this.dataDir);
        Trigger trigger =
          TriggerBuilder.newTrigger().withIdentity("folderCleaningTrigger", "group1")
            .withSchedule(CronScheduleBuilder.cronSchedule(config.getProperty("defaultCronExpression"))).build();
        scheduler.start();
        scheduler.scheduleJob(job, trigger);
      }
    } catch (SchedulerException e) {
      e.printStackTrace();
    }

  }

  /**
   * Returns the local host name.
   */
  @Override
  public String getHostName() {
    String hostName = "";
    try {
      hostName = InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      log.info("No IP address for the local hostname could be found", e);
    }
    return hostName;
  }

  @Override
  public void loadDataDirConfig() throws InvalidConfigException {
    log.info("Reading DATA DIRECTORY: " + dataDir.dataDir.getAbsolutePath());
    log.info("Loading SiB-SP config ...");
    config.loadConfigurationSettings();

    // log.info("Reloading log4j settings ...");
    // reloadLogger();

    log.info("Loading vocabularies ...");
    vocabularyManager.load();

    log.info("Loading dwc extensions ...");
    extensionManager.load();

  }

  private void reloadLogger() {
    LogFileAppender.LOGDIR = dataDir.loggingDir().getAbsolutePath();
    log.info("Setting logging dir to " + LogFileAppender.LOGDIR);

    InputStream log4j;
    // use different log4j settings files for production or debug mode
    if (config.debug()) {
      log4j = streamUtils.classpathStream("log4j.xml");
    } else {
      log4j = streamUtils.classpathStream("log4j-production.xml");
    }
    LogManager.resetConfiguration();
    DOMConfigurator domConfig = new DOMConfigurator();
    domConfig.doConfigure(log4j, LogManager.getLoggerRepository());
    log.info("Reloaded log4j for " + (config.debug() ? "debugging" : "production"));
    log.info("Logging to " + LogFileAppender.LOGDIR);
    log.info("SiBSP Data Directory: " + dataDir.dataFile(".").getAbsolutePath());
  }

  @Override
  public void saveConfig() throws InvalidConfigException {
    try {
      config.saveConfig();
    } catch (IOException e) {
      log.debug("Cant save SiB-SP configuration: " + e.getMessage(), e);
      throw new InvalidConfigException(TYPE.CONFIG_WRITE, "Cant save SiB-SP configuration: " + e.getMessage());
    }
  }

  @Override
  public void setBaseUrl(URL baseURL) throws InvalidConfigException {
    log.info("Updating the baseURL to: " + baseURL);
    boolean validate = true;
    if ("localhost".equals(baseURL.getHost()) || "127.0.0.1".equals(baseURL.getHost())
      || baseURL.getHost().equalsIgnoreCase(this.getHostName())) {
      log.warn("Localhost used as base url, SiB-SP will not be visible to the outside!");

      HttpHost hostTemp = (HttpHost) client.getParams().getParameter(ConnRoutePNames.DEFAULT_PROXY);
      if (hostTemp != null) {
        // if local URL is configured, SiB-SP should do the validation without a proxy.
        setProxy(null);
        validate = false;
        if (!validateBaseURL(baseURL)) {
          setProxy(hostTemp.toString());
          throw new InvalidConfigException(TYPE.INACCESSIBLE_BASE_URL, "No SiB-SP found at new base URL");
        }
        setProxy(hostTemp.toString());
      }

    }

    if (validate && !validateBaseURL(baseURL)) {
      throw new InvalidConfigException(TYPE.INACCESSIBLE_BASE_URL, "No SiB-SP found at new base URL");
    }

    // store in properties file
    config.setProperty(ApplicationConfig.ROOTURL, baseURL.toString());
  }

  @Override
  public boolean setDataDir(File dataDir) {
    boolean created = this.dataDir.setDataDir(dataDir);
    return created;
  }

  /**
   * It validates if is the first time that the user saves a proxy, if this is true, the proxy is saved normally (the
   * first time that the proxy is saved is in the setup page), if not (the second time that the user saves a proxy is
   * in
   * the config page), it validates if this proxy is the same as current proxy, if this is true, nothing changes, if
   * not, it removes the current proxy and save the new proxy.
   * 
   * @param proxy an URL with the format http://proxy.my-institution.com:8080.
   */
  public void setProxy(String proxy) throws InvalidConfigException {
    proxy = StringUtils.trimToNull(proxy);
    // save the current proxy
    HttpHost hostTemp = null;
    if (StringUtils.trimToNull(config.getProperty(ApplicationConfig.PROXY)) != null) {
      try {
        URL urlTemp = new URL(config.getProperty(ApplicationConfig.PROXY));
        hostTemp = new HttpHost(urlTemp.getHost(), urlTemp.getPort());
      } catch (MalformedURLException e) {
        // This exception should not be shown, the urlTemp was validated before being saved.
        log.info("the proxy URL is invalid", e);
      }
    }

    if (proxy == null) {
      // remove proxy from http client
      // Suddenly the client didn't have proxy host.
      log.info("Removing proxy setting");
      client.getParams().removeParameter(ConnRoutePNames.DEFAULT_PROXY);
    } else {
      // Changing proxy host
      if (hostTemp == null) {
        // First time, before Setup
        changeProxy(null, proxy);
      } else {
        // After Setup
        // Validating if the current proxy in the same proxy given by the user
        if (hostTemp.toString().equals(proxy)) {
          changeProxy(hostTemp, proxy);
        } else {
          // remove proxy from http client
          log.info("Removing proxy setting");
          client.getParams().removeParameter(ConnRoutePNames.DEFAULT_PROXY);
          changeProxy(hostTemp, proxy);
        }
      }
    }
    // store in properties file
    config.setProperty(ApplicationConfig.PROXY, proxy);
  }

  /**
   * It validates if the there is a connection with the baseURL, it executes a request using the baseURL.
   * 
   * @param baseURL a URL to validate.
   * @return true if the response to the request has a status code equal to 200.
   */
  public boolean validateBaseURL(URL baseURL) {
    if (baseURL == null) {
      return false;
    }
    // ensure there is an sibsp listening at the target
    boolean valid = false;
    try {
      HttpGet get = new HttpGet(baseURL.toString() + PATH_TO_CSS);
      HttpResponse response = http.executeGetWithTimeout(get, 4000);
      valid = response.getStatusLine().getStatusCode() == 200;
    } catch (ClientProtocolException e) {
      log.info("Protocol error connecting to new base URL [" + baseURL.toString() + "]", e);
    } catch (IOException e) {
      log.info("IO error connecting to new base URL [" + baseURL.toString() + "]", e);
    } catch (Exception e) {
      log.info("Unknown error connecting to new base URL [" + baseURL.toString() + "]", e);
    }

    return valid;
  }

}
