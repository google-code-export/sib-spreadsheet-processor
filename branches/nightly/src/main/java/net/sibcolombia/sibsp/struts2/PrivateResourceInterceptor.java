package net.sibcolombia.sibsp.struts2;

import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.AbstractInterceptor;
import org.apache.log4j.Logger;

/**
 * An Interceptor that makes sure a requested resource is either public or the current user has rights to manage the
 * private resource.
 */
public class PrivateResourceInterceptor extends AbstractInterceptor {

  private static Logger log = Logger.getLogger(RequireAdminInterceptor.class);

  @Override
  public String intercept(ActionInvocation invocation) throws Exception {
    String requestedResource = RequireManagerInterceptor.getResourceParam(invocation);
    return invocation.invoke();
  }

}
