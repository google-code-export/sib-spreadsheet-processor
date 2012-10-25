package net.sibcolombia.sibsp.action.portal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.google.inject.Inject;
import net.sibcolombia.sibsp.action.POSTAction;
import net.sibcolombia.sibsp.configuration.ApplicationConfig;
import net.sibcolombia.sibsp.configuration.DataDir;
import net.sibcolombia.sibsp.service.InvalidFileExtension;
import net.sibcolombia.sibsp.service.InvalidFileName;
import net.sibcolombia.sibsp.service.portal.ResourceManager;
import net.sibcolombia.sibsp.struts2.SimpleTextProvider;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;


public class CreateResourceAction extends POSTAction {

  private static final long serialVersionUID = 3310022370019075108L;

  // logging
  private static final Logger log = Logger.getLogger(CreateResourceAction.class);

  private final ResourceManager resourceManager;
  private final DataDir dataDir; // Directory to save temporal file

  // Data about the file uploades
  private File file;
  private String fileContentType;
  private String fileFileName;
  private String shortname;
  private String onlyFileName;
  private String onlyFileExtension;


  @Inject
  public CreateResourceAction(SimpleTextProvider textProvider, ApplicationConfig cfg, ResourceManager resourceManager,
    DataDir dataDir) {
    super(textProvider, cfg);
    this.resourceManager = resourceManager;
    this.dataDir = dataDir;
  }


  private void explodeFileExtension() {
    String fileName = fileFileName;
    int dotPosition = fileName.lastIndexOf('.');
    if (dotPosition > 0 && dotPosition < fileName.length() - 1) {
      onlyFileExtension = fileName.substring(dotPosition + 1).toLowerCase();
      onlyFileName = fileName.substring(0, dotPosition).toLowerCase();
    }
  }


  public String getShortname() {
    return shortname;
  }

  @Override
  public String save() throws IOException {
    try {
      File tmpFile = uploadToTmp();
      if (tmpFile != null) {
        resourceManager.create(tmpFile, fileFileName, onlyFileName, onlyFileExtension, this);
        log.info("File uploaded");
      } else {
        log.error("Error no file to upload");
      }
    } catch (InvalidFileExtension error) {
      log.error("Spreadsheet template file extension is invalid.");
      addFieldError("file", getText("sibsp.application.error.invalidextension"));
      return INPUT;
    } catch (InvalidFileName error) {
      log.error("Spreadsheet template file name is invalid.");
      addFieldError("file", getText("sibsp.application.error.invalidfilename"));
      return INPUT;
    } catch (InvalidFormatException error) {
      log.error("Spreadsheet template file format error.");
      addFieldError("file", getText("sibsp.application.error.invalidfiletype"));
      return INPUT;
    }
    return SUCCESS;
  }

  public void setFile(File file) {
    this.file = file;
  }

  public void setFileContentType(String fileContentType) {
    this.fileContentType = fileContentType;
  }

  public void setFileFileName(String fileFileName) {
    this.fileFileName = fileFileName;
  }

  public void setShortname(String shortname) {
    this.shortname = shortname;
  }

  private File uploadToTmp() throws InvalidFileExtension, InvalidFileName {
    if (fileFileName == null) {
      return null;
    }
    explodeFileExtension();
    if (validExtension()) {
      if (validFileName()) {
        // the file to upload to
        File tmpFile = dataDir.tmpFile("temp", fileFileName);
        log.debug("Uploading template file for " + tmpFile.getAbsolutePath());
        // retrieve the file data
        InputStream input = null;
        OutputStream output = null;
        try {
          input = new FileInputStream(file);
          // write the file to the file specified
          output = new FileOutputStream(tmpFile);
          IOUtils.copy(input, output);
          output.flush();
          log.debug("Uploaded file " + fileFileName + " with content-type " + fileContentType);
        } catch (IOException e) {
          log.error(e);
          return null;
        } finally {
          if (output != null) {
            IOUtils.closeQuietly(output);
          }
          if (input != null) {
            IOUtils.closeQuietly(input);
          }
        }
        return tmpFile;
      } else {
        throw new InvalidFileName("invalid file name");
      }
    } else {
      throw new InvalidFileExtension("invalid file extension");
    }
  }


  /**
   * Check if the source file is a valid spreadsheet template file extension
   * 
   * @return
   */
  private boolean validExtension() {
    if (onlyFileExtension == null) {
      return false;
    } else {
      if (onlyFileExtension.equalsIgnoreCase("xls") || onlyFileExtension.equalsIgnoreCase("xlsx")) {
        return true;
      } else {
        return false;
      }
    }
  }

  /**
   * Check if the source file is a valid spreadsheet template file name
   * 
   * @return
   */
  private boolean validFileName() {
    if (onlyFileName == null) {
      return false;
    } else {
      if (onlyFileName.equalsIgnoreCase("GMP_template_version_1.0")) {
        return true;
      }
      if (onlyFileName.equalsIgnoreCase("DwC_min_elements_template_version_1.0")) {
        return true;
      } else {
        return false;
      }
    }
  }

}
