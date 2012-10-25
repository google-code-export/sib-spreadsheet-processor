package net.sibcolombia.sibsp.action.portal;

import net.sibcolombia.sibsp.service.portal.ResourceManager;

import com.google.inject.Inject;
import net.sibcolombia.sibsp.action.BaseAction;
import net.sibcolombia.sibsp.configuration.ApplicationConfig;
import net.sibcolombia.sibsp.struts2.SimpleTextProvider;


public class HomeAction extends BaseAction {

  private static final long serialVersionUID = -3330941416834699271L;

  private final ResourceManager resourceManager;

  @Inject
  public HomeAction(SimpleTextProvider textProvider, ApplicationConfig config, ResourceManager resourceManager) {
    super(textProvider, config);
    this.resourceManager = resourceManager;
  }

  @Override
  public String execute() throws Exception {
    return SUCCESS;
  }

}
