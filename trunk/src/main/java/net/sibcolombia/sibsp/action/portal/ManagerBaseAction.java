package net.sibcolombia.sibsp.action.portal;

import com.google.inject.Inject;
import net.sibcolombia.sibsp.action.POSTAction;
import net.sibcolombia.sibsp.configuration.ApplicationConfig;
import net.sibcolombia.sibsp.model.Resource;
import net.sibcolombia.sibsp.service.portal.ResourceManager;
import net.sibcolombia.sibsp.struts2.SimpleTextProvider;

public class ManagerBaseAction extends POSTAction {

  private static final long serialVersionUID = 8182458395538498832L;

  // the resourceManager session is populated by the resource interceptor and kept alive for an entire manager session
  protected ResourceManager resourceManager;
  protected Resource resource;

  @Inject
  public ManagerBaseAction(SimpleTextProvider textProvider, ApplicationConfig config, ResourceManager resourceManager) {
    super(textProvider, config);
    this.resourceManager = resourceManager;
  }

  public Resource getResource() {
    return resource;
  }

  protected void saveResource() {
    resourceManager.save(resource);
  }

  public void setResource(Resource resource) {
    this.resource = resource;
  }

}