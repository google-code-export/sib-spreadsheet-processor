package net.sibcolombia.sibsp.action.portal;

import org.gbif.ipt.utils.ActionLogger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import com.google.inject.Inject;
import net.sibcolombia.sibsp.configuration.ApplicationConfig;
import net.sibcolombia.sibsp.configuration.Constants;
import net.sibcolombia.sibsp.configuration.DataDir;
import net.sibcolombia.sibsp.model.Extension;
import net.sibcolombia.sibsp.model.ExtensionMapping;
import net.sibcolombia.sibsp.model.ExtensionProperty;
import net.sibcolombia.sibsp.model.PropertyMapping;
import net.sibcolombia.sibsp.model.RecordFilter;
import net.sibcolombia.sibsp.model.Source;
import net.sibcolombia.sibsp.service.ImportException;
import net.sibcolombia.sibsp.service.InvalidFileExtension;
import net.sibcolombia.sibsp.service.InvalidFileName;
import net.sibcolombia.sibsp.service.admin.ExtensionManager;
import net.sibcolombia.sibsp.service.portal.ResourceManager;
import net.sibcolombia.sibsp.service.portal.SourceManager;
import net.sibcolombia.sibsp.struts2.SimpleTextProvider;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;


public class CreateResourceAction extends ManagerBaseAction {

  private static final long serialVersionUID = 3310022370019075108L;

  // logging
  private static final Logger log = Logger.getLogger(CreateResourceAction.class);

  private final DataDir dataDir; // Directory to save temporal file

  // Data about the file uploades
  private File file;
  private String fileContentType;
  private String fileFileName;
  private String shortname;
  private String onlyFileName;
  private String onlyFileExtension;
  private final ExtensionManager extensionManager;
  private ExtensionMapping mapping;
  private final SourceManager sourceManager;
  private ExtensionProperty coreid;
  private PropertyMapping mappingCoreid;


  @Inject
  public CreateResourceAction(SimpleTextProvider textProvider, ApplicationConfig config,
    ResourceManager resourceManager, DataDir dataDir, ExtensionManager extensionManager, SourceManager sourceManager) {
    super(textProvider, config, resourceManager);
    this.extensionManager = extensionManager;
    this.dataDir = dataDir;
    this.sourceManager = sourceManager;
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

  private boolean isBasicOcurrenceOnly() {
    if (onlyFileName.equalsIgnoreCase("DwC_min_elements_template_version_1.0")) {
      return true;
    } else {
      return false;
    }
  }

  private boolean isEmlOnly() {
    if (onlyFileName.equalsIgnoreCase("GMP_template_version_1.0")) {
      return true;
    } else {
      return false;
    }
  }

  @Override
  public String save() throws IOException {
    ActionLogger actionLogger = new ActionLogger(this.log, this);
    try {
      File tmpFile = uploadToTmp();
      if (tmpFile != null) {
        if (isEmlOnly()) {
          // Process template with metadata only workbook
          UUID uniqueID = UUID.randomUUID();
          this.resource = resourceManager.processMetadataSpreadsheetPart(tmpFile, fileFileName, actionLogger);
          this.resource.setUniqueID(uniqueID);
          saveResource();
          this.resourceManager.saveEml(this.resource);
          tmpFile.delete();
        } else if (isBasicOcurrenceOnly()) {
          UUID uniqueID = UUID.randomUUID();
          this.resource = resourceManager.processMetadataSpreadsheetPart(tmpFile, fileFileName, actionLogger);
          this.resource.setUniqueID(uniqueID);

          Extension extension = extensionManager.get(Constants.DWC_ROWTYPE_OCCURRENCE);
          if (extension != null) {
            mapping = new ExtensionMapping();
            mapping.setExtension(extension);
          }
          if (mapping != null || mapping.getExtension() != null) {
            if (mapping.getSource() == null) {
              Source source = sourceManager.add(this.resource, tmpFile, fileFileName);
              saveResource();
              mapping.setSource(source);
            }
            // set empty filter if not existing
            if (mapping.getFilter() == null) {
              mapping.setFilter(new RecordFilter());
            }
            // determine the core row type
            String coreRowType = resource.getCoreRowType();
            if (coreRowType == null) {
              // not yet set, the current mapping must be the core type
              coreRowType = mapping.getExtension().getRowType();
            }
            // setup the core record id term
            String coreIdTerm = Constants.DWC_OCCURRENCE_ID;
            if (coreRowType.equalsIgnoreCase(Constants.DWC_ROWTYPE_TAXON)) {
              coreIdTerm = Constants.DWC_TAXON_ID;
            }
            coreid = extensionManager.get(coreRowType).getProperty(coreIdTerm);
            mappingCoreid = mapping.getField(coreid.getQualname());
            if (mappingCoreid == null) {
              // no, create bare mapping field
              mappingCoreid = new PropertyMapping();
              mappingCoreid.setTerm(coreid);
              mappingCoreid.setIndex(mapping.getIdColumn());
            }
            this.resource.addMapping(mapping);
            saveResource();
          }


          if (resourceManager.publish(resource, this)) {
            addActionMessage(getText("sibsp.application.portal.overview.publishing.resource.version",
              new String[] {Integer.toString(resource.getEmlVersion())}));
          }
          tmpFile.delete();
          /*
           * // Process template with metadata and basic data of ocurrence file
           * this.resource = processMetadataSpreadsheetPart(sourceFile, fileName, actionLogger);
           * // Get the extension to use for mapper
           * Extension extension = extensionManager.get(RESOURCE_OCURRENCE_NAME);
           * if (extension != null) {
           * mapping = new ExtensionMapping();
           * mapping.setExtension(extension);
           * }
           * if (mapping != null || mapping.getExtension() != null) {
           * // set empty filter if not existing
           * if (mapping.getFilter() == null) {
           * mapping.setFilter(new RecordFilter());
           * }
           * String coreRowType = mapping.getExtension().getRowType();
           * // setup the core record id term
           * String coreIdTerm = Constants.DWC_OCCURRENCE_ID;
           * if (coreRowType.equalsIgnoreCase(Constants.DWC_ROWTYPE_TAXON)) {
           * coreIdTerm = Constants.DWC_TAXON_ID;
           * }
           * coreid = extensionManager.get(coreRowType).getProperty(coreIdTerm);
           * mappingCoreid = mapping.getField(coreid.getQualname());
           * if (mappingCoreid == null) {
           * // no, create bare mapping field
           * mappingCoreid = new PropertyMapping();
           * mappingCoreid.setTerm(coreid);
           * mappingCoreid.setIndex(mapping.getIdColumn());
           * }
           * }
           */
        } else {
          // Process template with metadata and taxonomy file
        }

        // resourceManager.create(tmpFile, fileFileName, onlyFileName, onlyFileExtension, this);
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
    } catch (ImportException e) {
      log.error("File import error.");
      addFieldError("file", getText("sibsp.application.error.importexception"));
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
