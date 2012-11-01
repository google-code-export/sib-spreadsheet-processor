package net.sibcolombia.sibsp.struts2;

import com.google.inject.Inject;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.AbstractInterceptor;
import net.sibcolombia.sibsp.configuration.ConfigWarnings;
import net.sibcolombia.sibsp.configuration.ConfigurationAction;
import net.sibcolombia.sibsp.interfaces.ConfigurationManager;
import org.apache.log4j.Logger;


public class ConfigurationAndCancelInterceptor extends AbstractInterceptor {

  private static final long serialVersionUID = 5320234009149369776L;
  public final String CONFIGURATION_RESULT = "configurationIncomplete";
  private static Logger log = Logger.getLogger(ConfigurationAndCancelInterceptor.class);

  @Inject
  private ConfigurationManager configurationManager;
  @Inject
  private ConfigWarnings warnings;

  @Override
  public String intercept(ActionInvocation invocation) throws Exception {
    if (!configurationManager.configurationComplete()) {
      Object action = invocation.getAction();
      if (action instanceof ConfigurationAction) {
        return invocation.invoke();
      } else {
        log.info("Setup incomplete - redirect to setup");
        return CONFIGURATION_RESULT;
      }
    }
    return invocation.invoke();
  }
}
