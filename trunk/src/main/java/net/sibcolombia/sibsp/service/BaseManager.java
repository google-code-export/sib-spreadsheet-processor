package net.sibcolombia.sibsp.service;

import net.sibcolombia.sibsp.configuration.ApplicationConfig;
import net.sibcolombia.sibsp.configuration.DataDir;
import org.apache.log4j.Logger;

/**
 * This a base class for management implementation
 * 
 * @author Valentina Grajales
 */
public abstract class BaseManager {

  protected Logger log = Logger.getLogger(this.getClass());
  protected ApplicationConfig config;
  protected DataDir dataDir;

  private BaseManager() {

  }

  public BaseManager(ApplicationConfig config, DataDir dataDir) {
    this.config = config;
    this.dataDir = dataDir;
  }
}
