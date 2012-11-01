package net.sibcolombia.sibsp.action;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.google.inject.Inject;
import com.opensymphony.xwork2.ActionSupport;
import com.opensymphony.xwork2.Preparable;
import net.sibcolombia.sibsp.configuration.ApplicationConfig;
import net.sibcolombia.sibsp.struts2.SimpleTextProvider;
import org.apache.commons.lang3.StringUtils;
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
  // a generic identifier for loading an object BEFORE the param interceptor sets values
  protected String id;

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

  /**
   * Return a list of action warning strings.
   * 
   * @return list of action warning strings.
   */
  public List<String> getActionWarnings() {
    return warnings;
  }

  /**
   * Custom simple text provider for much faster lookups.
   * This increases page rendering with lots of <@s:text> tags by nearly 100%.
   * Struts2 manages the locale in the session param WW_TRANS_I18N_LOCALE via the i18n interceptor.
   * If the Locale is null, the default language "en" is returned.
   * 
   * @return Locale language, or default language string "en" if Locale was null
   */
  public String getLocaleLanguage() {
    return (getLocale() == null) ? Locale.ENGLISH.getLanguage() : getLocale().getLanguage();
  }

  public String getRootURL() {
    return config.getRootURL();
  }

  public String getSopas() {
    log.info("URL esta en: " + config.getRootURL());
    return "hola";
    // return config.getRootURL();
  }

  public String getTextWithDynamicArgs(String key, String... args) {
    return textProvider.getText(this, key, null, args);
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
  public void prepare() {
    id = StringUtils.trimToNull(request.getParameter("id"));
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

  /**
   * Utility to compare 2 objects for comparison when both converted to strings useful to compare if a submitted value
   * is the same as the persisted value.
   * 
   * @return true only if o1.equals(o2)
   */
  protected boolean stringEquals(Object o1, Object o2) {
    // both null
    if (o1 == null && o2 == null) {
      return true;
    }
    if (o1 != null && o2 != null) {
      return o1.toString().equals(o2.toString());
    }
    return false;
  }

}
