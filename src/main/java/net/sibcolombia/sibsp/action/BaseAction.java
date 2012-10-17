package net.sibcolombia.sibsp.action;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.google.inject.Inject;
import com.opensymphony.xwork2.ActionSupport;
import com.opensymphony.xwork2.Preparable;
import net.sibcolombia.sibsp.configuration.ApplicationConfig;
import net.sibcolombia.sibsp.struts2.SimpleTextProvider;
import org.apache.log4j.Logger;
import org.apache.struts2.interceptor.ServletRequestAware;
import org.apache.struts2.interceptor.SessionAware;


public class BaseAction extends ActionSupport implements SessionAware, Preparable, ServletRequestAware {

  private static final long serialVersionUID = 2396551545848462123L;
  public static final String NOT_MODIFIED = "304";
  public static final String NOT_FOUND = "404";
  public static final String NOT_ALLOWED = "401";
  public static final String HOME = "home";
  private static final Logger log = Logger.getLogger(BaseAction.class);
  protected HttpServletRequest request;
  protected List<String> warnings = new ArrayList<String>();
  protected Map<String, Object> session;
  protected ApplicationConfig config;
  protected SimpleTextProvider textProvider;

  @Inject
  public BaseAction(SimpleTextProvider textProvider, ApplicationConfig config) {
    this.textProvider = textProvider;
    this.config = config;
  }

  /**
   * Adds an exception message, if not null, to the action warnings.
   * 
   * @param e the exception from which the message is taken
   */
  protected void addActionExceptionWarning(Exception e) {
    String msg = e.getMessage();
    if (msg != null) {
      warnings.add(msg);
    }
  }

  /**
   * Adds a warning similar to the action errors to the user UI, but does not interact with the validation aware
   * workflow interceptor, therefore no changes to the result name of the action are expected.
   * This is the way to present user warnings/errors others than for form validation.
   * If you want form validation with the workflow interceptor, please {@link #addActionError(String)} instead.
   */
  public void addActionWarning(String anErrorMessage) {
    warnings.add(anErrorMessage);
  }

  public void addActionWarning(String anErrorMessage, Exception e) {
    warnings.add(anErrorMessage);
    addActionExceptionWarning(e);
  }

  public String getRootURL() {
    return config.getRootUrl();
  }

  public List<String> getWarnings() {
    return warnings;
  }

  protected boolean isHttpPost() {
    if (request.getMethod().equalsIgnoreCase("post")) {
      return true;
    }
    return false;
  }

  @Override
  public void prepare() throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  public void setServletRequest(HttpServletRequest request) {
    this.request = request;
  }

  @Override
  public void setSession(Map<String, Object> session) {
    this.session = session;
    // always keep sth in the session otherwise the session is not maintained and e.g. the message redirect interceptor
    // doesnt work
    if (session.isEmpty()) {
      session.put("-", true);
    }
  }

}
