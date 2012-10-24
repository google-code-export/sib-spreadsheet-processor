package net.sibcolombia.sibsp.interfaces;

import java.io.File;
import java.net.URL;

import com.google.inject.ImplementedBy;
import net.sibcolombia.sibsp.configuration.ConfigurationManagerImplementation;
import net.sibcolombia.sibsp.service.InvalidConfigException;

@ImplementedBy(ConfigurationManagerImplementation.class)
public interface ConfigurationManager {

  /**
   * @return true if the configuration is completed, false otherwise
   */
  boolean configurationComplete();

  String getHostName();

  /**
   * Persists the main SiB-SP AppConfig configuration which can be modified for simple properties independently of this
   * manager as its a singleton.
   * Highly recommended is to use the setConfigProperty method in this manager though to edit the configuration.
   */
  void saveConfig() throws InvalidConfigException;

  /**
   * Sets the base URL for the SiB-SP installation.
   * This affects all accessible resources
   * through the SiB-SP. The baseURL cannot be determined programmatically as it is not possible
   * to know things such as virtual host definitions, URL rewriting or proxies that might come
   * into play in the deployment. If any services have been registered, then this will communicate through
   * the registryAPI to update those URLs that have changed.
   * The modified AppConfig is not immediately persisted - remember to call save() at some point!
   * 
   * @param baseURL The new baseURL for the SiB-SP
   * @throws InvalidConfigException If the URL appears to be localhost, 127.0.0.1 or something that clearly
   *         will not be addressable from the internet.
   */
  void setBaseUrl(URL baseURL) throws InvalidConfigException;

  /**
   * Tries to assign a new data directory to the SIBSP.
   * This has huge a impact as all configuration apart the data dirs location itself is stored in the data directory.
   * If the directory provided is empty a new skeleton dir will be setup.
   * If the data dir is valid and writable the configuration is loaded via loadDataDirConfig().
   * 
   * @param dataDir a valid, writable directory. If empty a new skeleton will be used, if its an existing, valid SiB-SP
   *        data dir it will be read.
   * @return true if a new data dir was created, false when an existing was read
   */
  boolean setDataDir(File dataDir) throws InvalidConfigException;
}
