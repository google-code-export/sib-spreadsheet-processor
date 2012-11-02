package net.sibcolombia.sibsp.action.administration;

import com.google.inject.Inject;
import net.sibcolombia.sibsp.action.BaseAction;
import net.sibcolombia.sibsp.configuration.ApplicationConfig;
import net.sibcolombia.sibsp.configuration.DataDir;
import net.sibcolombia.sibsp.struts2.SimpleTextProvider;


public class HomeAction extends BaseAction {

  private static final long serialVersionUID = 6845798398258127088L;

  private String rootURL;

  @Inject
  public HomeAction(SimpleTextProvider textProvider, ApplicationConfig config, DataDir dataDir) {
    super(textProvider, config);
  }

  @Override
  public String execute() throws Exception {
    return SUCCESS;
  }

  public String getDataDir() {
    return config.getDataDir().dataFile("").getAbsolutePath();
  }

  @Override
  public String getRootURL() {
    return config.getRootURL();
  }

  public void setRootURL(String rootURL) {
    this.rootURL = rootURL;
  }

}
