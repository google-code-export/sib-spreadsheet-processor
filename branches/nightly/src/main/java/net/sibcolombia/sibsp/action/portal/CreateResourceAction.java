package net.sibcolombia.sibsp.action.portal;

import org.gbif.ipt.task.Xls2Csv;
import org.gbif.ipt.utils.ActionLogger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import com.google.common.base.Strings;
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
import net.sibcolombia.sibsp.model.Source.FileSource;
import net.sibcolombia.sibsp.service.ImportException;
import net.sibcolombia.sibsp.service.InvalidFileExtension;
import net.sibcolombia.sibsp.service.InvalidFileName;
import net.sibcolombia.sibsp.service.admin.ExtensionManager;
import net.sibcolombia.sibsp.service.portal.ResourceManager;
import net.sibcolombia.sibsp.service.portal.SourceManager;
import net.sibcolombia.sibsp.struts2.SimpleTextProvider;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;


public class CreateResourceAction extends ManagerBaseAction {

  private static final long serialVersionUID = 3310022370019075108L;

  // logging
  private static final Logger log = Logger.getLogger(CreateResourceAction.class);

  private static final Pattern NORM_TERM = Pattern.compile("[\\W\\s_0-9]+");

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
  private final Xls2Csv excelToCsvConverter;

  private List<String> columns;
  private List<PropertyMapping> fields;
  private List<String[]> peek;

  private final Map<String, Map<String, String>> vocabTerms = new HashMap<String, Map<String, String>>();


  @Inject
  public CreateResourceAction(SimpleTextProvider textProvider, ApplicationConfig config,
    ResourceManager resourceManager, DataDir dataDir, ExtensionManager extensionManager, SourceManager sourceManager,
    Xls2Csv excelToCsvConverter) {
    super(textProvider, config, resourceManager);
    this.extensionManager = extensionManager;
    this.dataDir = dataDir;
    this.sourceManager = sourceManager;
    this.excelToCsvConverter = excelToCsvConverter;
  }


  /**
   * This method automaps a source's columns. First it tries to automap the mappingCoreId column, and then it tries
   * to automap the source's remaining fields against the core/extension.
   * 
   * @return the number of terms that have been automapped
   */
  int automap() {
    // keep track of how many terms were automapped
    int automapped = 0;

    // start by trying to automap the mappingCoreId (occurrenceId/taxonId) to a column in source
    int idx1 = 0;
    for (String col : columns) {
      col = normalizeColumnName(col);
      if (col != null && mappingCoreid.getTerm().simpleNormalisedName().equalsIgnoreCase(col)) {
        // mappingCoreId and mapping id column must both be set (and have the same index) to automap successfully.
        mappingCoreid.setIndex(idx1);
        mapping.setIdColumn(idx1);
        // we have automapped the core id column, so increment automapped counter and exit
        automapped++;
        break;
      }
      idx1++;
    }

    // next, try to automap the source's remaining columns against the extensions fields
    for (PropertyMapping f : fields) {
      int idx2 = 0;
      for (String col : columns) {
        col = normalizeColumnName(col);
        if (col != null && f.getTerm().simpleNormalisedName().equalsIgnoreCase(col)) {
          f.setIndex(idx2);
          // we have automapped the term, so increment automapped counter and exit
          automapped++;
          break;
        }
        idx2++;
      }
    }

    return automapped;
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

  /**
   * Normalizes an incoming column name so that it can later be compared against a ConceptTerm's simpleNormalizedName.
   * This method converts the incoming string to lower case, and will take the substring up to, but no including the
   * first ":".
   * 
   * @param col column name
   * @return the normalized column name, or null if the incoming name was null or empty
   */
  String normalizeColumnName(String col) {
    if (!Strings.isNullOrEmpty(col)) {
      col = NORM_TERM.matcher(col.toLowerCase()).replaceAll("");
      if (col.contains(":")) {
        col = StringUtils.substringAfter(col, ":");
      }
      return col;
    }
    return null;
  }

  private void readSource() {
    if (mapping.getSource() == null) {
      columns = new ArrayList<String>();
    } else {
      peek = sourceManager.peek(mapping.getSource(), 5);
      // If user wants to import a source without a header lines, the columns are going to be numbered with the first
      // non-null value as an example. Otherwise, read the file/database normally.
      if (mapping.getSource().isFileSource() && ((FileSource) mapping.getSource()).getIgnoreHeaderLines() == 0) {
        columns = mapping.getColumns(peek);
      } else {
        columns = sourceManager.columns(mapping.getSource());
      }
    }
  }

  @Override
  public String save() throws IOException {
    ActionLogger actionLogger = new ActionLogger(this.log, this);
    try {
      File tmpFile = uploadToTmp();
      File dataFileElements = null;
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
          dataFileElements = excelToCsvConverter.convertExcelToCsv(resource, tmpFile, actionLogger);

          Extension extension = extensionManager.get(Constants.DWC_ROWTYPE_OCCURRENCE);
          if (extension != null) {
            mapping = new ExtensionMapping();
            mapping.setExtension(extension);
          }
          if (mapping != null || mapping.getExtension() != null) {
            if (mapping.getSource() == null) {
              Source source = sourceManager.add(this.resource, dataFileElements, fileFileName);
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

            readSource();

            // prepare all other fields
            fields = new ArrayList<PropertyMapping>(mapping.getExtension().getProperties().size());
            for (ExtensionProperty p : mapping.getExtension().getProperties()) {
              // ignore core id term
              if (p.equals(coreid)) {
                continue;
              }
              // uses a vocabulary?
              /*
               * if (p.getVocabulary() != null) {
               * vocabTerms.put(p.getVocabulary().getUriString(),
               * vocabManager.getI18nVocab(p.getVocabulary().getUriString(), getLocaleLanguage(), true));
               * }
               */
              // mapped already?
              PropertyMapping f = mapping.getField(p.getQualname());
              if (f == null) {
                // no, create bare mapping field
                f = new PropertyMapping();
              }
              f.setTerm(p);
              fields.add(f);
            }

            // finally do automapping if no fields are found
            if (mapping.getFields().isEmpty()) {
              int automapped = automap();
              if (automapped > 0) {
                addActionMessage(getText("manage.mapping.automaped", new String[] {String.valueOf(automapped)}));
              }
            }
            this.resource.addMapping(mapping);


            saveResource();
          }


          if (resourceManager.publish(resource, this)) {
            addActionMessage(getText("sibsp.application.portal.overview.publishing.resource.version",
              new String[] {Integer.toString(resource.getEmlVersion())}));
          }
          tmpFile.delete();
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
      if (error.getMessage().isEmpty()) {
        addFieldError("file", getText("sibsp.application.error.invalidfiletype"));
      } else {
        addFieldError("file", error.getMessage());
      }
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
