package net.sibcolombia.sibsp.service.registry.implementation;

import org.gbif.utils.HttpUtil;
import org.gbif.utils.HttpUtil.Response;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import net.sibcolombia.sibsp.action.BaseAction;
import net.sibcolombia.sibsp.configuration.ApplicationConfig;
import net.sibcolombia.sibsp.configuration.ConfigWarnings;
import net.sibcolombia.sibsp.configuration.DataDir;
import net.sibcolombia.sibsp.model.Extension;
import net.sibcolombia.sibsp.service.BaseManager;
import net.sibcolombia.sibsp.service.RegistryException;
import net.sibcolombia.sibsp.service.RegistryException.TYPE;
import net.sibcolombia.sibsp.service.registry.RegistryManager;
import net.sibcolombia.sibsp.struts2.SimpleTextProvider;
import org.xml.sax.SAXException;


public class RegistryManagerImplementation extends BaseManager implements RegistryManager {

  private final HttpUtil http;
  private final SAXParser saxParser;
  private final Gson gson;
  private final ConfigWarnings warnings;
  // create instance of BaseAction - allows class to retrieve i18n terms via getText()
  private final BaseAction baseAction;

  @Inject
  public RegistryManagerImplementation(ApplicationConfig config, DataDir dataDir, HttpUtil httpUtil,
    SAXParserFactory saxFactory, ConfigWarnings warnings, SimpleTextProvider textProvider)
    throws ParserConfigurationException, SAXException {
    super(config, dataDir);
    this.http = httpUtil;
    this.saxParser = saxFactory.newSAXParser();
    this.gson = new Gson();
    this.warnings = warnings;
    baseAction = new BaseAction(textProvider, config);
  }

  /*
   * (non-Javadoc)
   * @see org.gbif.ipt.service.registry.RegistryManager#getExtensions()
   */
  @Override
  public List<Extension> getExtensions() throws RegistryException {
    Map<String, List<Extension>> jSONextensions =
      gson.fromJson(requestHttpGetFromRegistry(getExtensionsURL(true)).content,
        new TypeToken<Map<String, List<Extension>>>() {
        }.getType());
    return (jSONextensions.get("extensions") == null) ? new ArrayList<Extension>() : jSONextensions.get("extensions");
  }

  /**
   * Returns the Extensions url.
   */
  private String getExtensionsURL(boolean json) {
    return String.format("%s%s%s", config.getRegistryUrl(), "/registry/extensions", json ? ".json" : "/");
  }

  /**
   * Executes an HTTP Get Request against the GBIF Registry. If the content is not null, the Response is returned.
   * Otherwise, if the content was null, or an exception occurred, it throws the appropriate type of RegistryException.
   * 
   * @param url Get request URL
   * @return Response if the content was not null, or a RegistryException
   * @throws RegistryException (with RegistryException.type) if the content was null or an exception occurred
   */
  private Response requestHttpGetFromRegistry(String url) throws RegistryException {
    try {
      Response resp = http.get(url);
      if (resp != null && resp.content != null) {
        return resp;
      } else {
        throw new RegistryException(TYPE.BAD_RESPONSE, "Response content is null");
      }
    } catch (ClassCastException e) {
      throw new RegistryException(TYPE.BAD_RESPONSE, e);
    } catch (ConnectException e) {
      // normally happens when a timeout appears - probably a firewall or proxy problem.
      throw new RegistryException(TYPE.PROXY, e);
    } catch (UnknownHostException e) {
      try {
        // if server cannot connect to Google - probably the Internet connection is not active.
        http.get("http://www.google.com");
      } catch (Exception e1) {
        throw new RegistryException(TYPE.NO_INTERNET, e1);
      }
      // if server can connect to Google - probably the GBIF Registry page is down.
      throw new RegistryException(TYPE.SITE_DOWN, e);
    } catch (IOException e) {
      throw new RegistryException(TYPE.IO_ERROR, e);
    } catch (URISyntaxException e) {
      throw new RegistryException(TYPE.BAD_REQUEST, "Please check the request URL: "
        + ((url != null) ? url : "empty URL used!"));
    }
  }

}
