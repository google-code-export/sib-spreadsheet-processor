package net.sibcolombia.sibsp.action.portal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import com.google.inject.Inject;
import net.sibcolombia.sibsp.action.BaseAction;
import net.sibcolombia.sibsp.configuration.ApplicationConfig;
import net.sibcolombia.sibsp.configuration.Constants;
import net.sibcolombia.sibsp.configuration.DataDir;
import net.sibcolombia.sibsp.struts2.SimpleTextProvider;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;


public class ResourceFileAction extends BaseAction {

  private static final long serialVersionUID = 1469722447044769727L;

  // logging
  private static final Logger log = Logger.getLogger(ResourceFileAction.class);

  private final DataDir dataDir;
  protected String paramR;
  protected File data;
  protected String mimeType = "text/plain";
  protected String filename;
  private InputStream inputStream;

  @Inject
  public ResourceFileAction(SimpleTextProvider textProvider, ApplicationConfig config, DataDir dataDir) {
    super(textProvider, config);
    this.dataDir = dataDir;
  }

  public String dwca() {
    // serve file as set in prepare method
    data = dataDir.resourceDwcaFile(paramR);
    filename = "dwca-" + paramR + ".zip";
    mimeType = "application/zip";
    return execute();
  }

  public String eml() {
    data = dataDir.resourceEmlFile(paramR, null);
    mimeType = "text/xml";
    filename = "eml-" + paramR + ".xml";
    return execute();
  }

  @Override
  public String execute() {
    // make sure we have a downlaod filename
    if (filename == null) {
      filename = data.getName();
    }
    try {
      inputStream = new FileInputStream(data);
    } catch (FileNotFoundException e) {
      log.warn("Data dir file not found", e);
      return NOT_FOUND;
    }
    return SUCCESS;
  }

  public File getData() {
    return data;
  }

  public String getFilename() {
    return filename;
  }

  public InputStream getInputStream() {
    return inputStream;
  }

  public String getMimeType() {
    return mimeType;
  }

  public String getR() {
    return this.paramR;
  }

  @Override
  public void prepare() {
    paramR = StringUtils.trimToNull(request.getParameter(Constants.REQ_PARAM_RESOURCE));
    if (paramR == null) {
      // try session instead
      try {
        paramR = (String) session.get(Constants.SESSION_RESOURCE);
      } catch (Exception e) {
        // swallow. if session is not yet opened we get an exception here...
      }
    }
  }

  public String rtf() {
    data = dataDir.resourceRtfFile(paramR);
    mimeType = "application/rtf";
    filename = paramR + "-metadata.rtf";
    return execute();
  }

}
