package net.sibcolombia.sibsp.struts2;

import java.util.Map;

import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.AbstractInterceptor;
import net.sibcolombia.sibsp.action.BaseAction;
import org.apache.log4j.Logger;

// import org.gbif.ipt.model.User;

/**
 * An Interceptor that makes sure an admin user is currently logged in and returns a notAllowed otherwise.
 */
public class RequireAdminInterceptor extends AbstractInterceptor {

  private static Logger log = Logger.getLogger(RequireAdminInterceptor.class);

  @Override
  public String intercept(ActionInvocation invocation) throws Exception {
    Map<String, Object> session = invocation.getInvocationContext().getSession();
    /*
     * User user = (User) session.get(Constants.SESSION_USER);
     * if (user != null && user.hasAdminRights()) {
     * return invocation.invoke();
     * }
     */
    return BaseAction.NOT_ALLOWED;
  }

}
