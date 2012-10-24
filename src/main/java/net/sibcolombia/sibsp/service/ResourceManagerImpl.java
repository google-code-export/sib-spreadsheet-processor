package net.sibcolombia.sibsp.service;

import org.gbif.ipt.utils.ActionLogger;
import org.gbif.metadata.eml.Address;
import org.gbif.metadata.eml.Agent;
import org.gbif.metadata.eml.BBox;
import org.gbif.metadata.eml.BibliographicCitationSet;
import org.gbif.metadata.eml.Citation;
import org.gbif.metadata.eml.Eml;
import org.gbif.metadata.eml.EmlFactory;
import org.gbif.metadata.eml.EmlWriter;
import org.gbif.metadata.eml.GeospatialCoverage;
import org.gbif.metadata.eml.JGTICuratorialUnit;
import org.gbif.metadata.eml.KeywordSet;
import org.gbif.metadata.eml.PhysicalData;
import org.gbif.metadata.eml.Project;
import org.gbif.metadata.eml.StudyAreaDescription;
import org.gbif.metadata.eml.TaxonKeyword;
import org.gbif.metadata.eml.TaxonomicCoverage;
import org.gbif.metadata.eml.TemporalCoverage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.inject.Inject;
import com.thoughtworks.xstream.XStream;
import freemarker.template.TemplateException;
import net.sibcolombia.sibsp.action.BaseAction;
import net.sibcolombia.sibsp.configuration.ApplicationConfig;
import net.sibcolombia.sibsp.configuration.Constants;
import net.sibcolombia.sibsp.configuration.DataDir;
import net.sibcolombia.sibsp.interfaces.ResourceManager;
import net.sibcolombia.sibsp.model.Extension;
import net.sibcolombia.sibsp.model.ExtensionMapping;
import net.sibcolombia.sibsp.model.Resource;
import net.sibcolombia.sibsp.model.Resource.CoreRowType;
import net.sibcolombia.sibsp.model.Source;
import net.sibcolombia.sibsp.model.Source.FileSource;
import net.sibcolombia.sibsp.service.InvalidConfigException.TYPE;
import net.sibcolombia.sibsp.service.admin.ExtensionManager;
import net.sibcolombia.sibsp.service.admin.VocabulariesManager;
import net.sibcolombia.sibsp.service.registry.RegistryManager;
import net.sibcolombia.sibsp.struts2.SimpleTextProvider;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.xml.sax.SAXException;


public class ResourceManagerImpl extends BaseManager implements ResourceManager {

  private Resource resource;
  // key=shortname in lower case, value=resource
  private final Map<String, Resource> resources = new HashMap<String, Resource>();
  public static final String PERSISTENCE_FILE = "resource.xml";
  private final XStream xstream = new XStream();
  public static final String RESOURCE_IDENTIFIER_LINK_PART = "/resource.do?id=";
  private final ExtensionManager extensionManager;

  // create instance of BaseAction - allows class to retrieve i18n terms via getText()
  private final BaseAction baseAction;
  private final RegistryManager registryManager;
  private final VocabulariesManager vocabularyManager;
  private final SimpleTextProvider textProvider;

  @Inject
  public ResourceManagerImpl(ApplicationConfig config, DataDir dataDir, SimpleTextProvider simpleTextProvider,
    RegistryManager registryManager, ExtensionManager extensionManager, VocabulariesManager vocabularyManager) {
    super(config, dataDir);
    this.extensionManager = extensionManager;
    this.registryManager = registryManager;
    this.vocabularyManager = vocabularyManager;
    this.textProvider = simpleTextProvider;
    baseAction = new BaseAction(simpleTextProvider, config);
  }

  private void addResource(Resource res) {
    resources.put(res.getShortname().toLowerCase(), res);
  }

  @Override
  public void create(File sourceFile, String fileName, String onlyFileName, String onlyFileExtension,
    BaseAction createEmlAction) throws InvalidFormatException, IOException {
    ActionLogger actionLogger = new ActionLogger(this.log, createEmlAction);
    if (isEmlOnly(onlyFileName)) {
      // Process template with metadata only workbook
      this.resource = createFromMetadataOnlySpreadsheet(sourceFile, fileName, actionLogger);
      saveEml(fileName);
    } else if (isBasicOcurrenceOnly(onlyFileName)) {
      // Process template with metadata and basic data of ocurrence file

    } else {
      // Process template with metadata and taxonomy file
    }
  }

  /**
   * Process template file to generate an EML XML file
   * 
   * @param sourceFile
   * @param actionLogger
   * @throws IOException
   * @throws InvalidFormatException
   */
  private Resource createFromMetadataOnlySpreadsheet(File sourceFile, String fileName, ActionLogger actionLogger)
    throws InvalidFormatException, IOException, NullPointerException {
    Resource resource = new Resource();
    Eml eml = new Eml();
    Workbook template = WorkbookFactory.create(sourceFile);

    readBasicMetaData(eml, template, resource);
    readGeographicCoverage(eml, template);
    readTaxonomicCoverage(eml, template);
    readTemporalCoverage(eml, template);
    readKeywords(eml, template);
    readAssociatedParties(eml, template);
    readProjectData(eml, template);
    readSamplingMethods(eml, template);
    readCitations(eml, template);
    readCollectionData(eml, template);
    readExternallinks(eml, template);
    readAdditionalMetadata(eml, template);

    // Set resource details
    resource.setFileName(fileName);
    resource.setEml(eml);

    return resource;
  }

  public URL getResourceLink(String shortname) {
    URL url = null;
    try {
      url = new URL(config.getRootURL() + RESOURCE_IDENTIFIER_LINK_PART + shortname);
    } catch (MalformedURLException e) {
      log.error(e);
    }
    return url;
  }

  /**
   * The resource's coreType could be null. This could happen because before 2.0.3 it was not saved to resource.xml.
   * During upgrades to 2.0.3, a bug in MetadataAction would (wrongly) automatically set the coreType:
   * Checklist resources became Occurrence, and vice versa. This method will try to infer the coreType by matching
   * the coreRowType against the taxon and occurrence rowTypes.
   * 
   * @param resource Resource
   * @return resource with coreType set if it could be inferred, or unchanged if it couldn't be inferred.
   */
  Resource inferCoreType(Resource resource) {
    if (resource != null && resource.getCoreRowType() != null) {
      if (Constants.DWC_ROWTYPE_OCCURRENCE.equalsIgnoreCase(resource.getCoreRowType())) {
        resource.setCoreType(CoreRowType.OCCURRENCE.toString().toLowerCase());
      } else if (Constants.DWC_ROWTYPE_TAXON.equalsIgnoreCase(resource.getCoreRowType())) {
        resource.setCoreType(CoreRowType.CHECKLIST.toString().toLowerCase());
      }
    } else {
      // don't do anything - no taxon or occurrence mapping has been done yet
    }
    return resource;
  }

  private boolean isBasicOcurrenceOnly(String onlyFileName) {
    if (onlyFileName.equalsIgnoreCase("DwC_min_elements_template_version_1.0")) {
      return true;
    } else {
      return false;
    }
  }

  private boolean isEmlOnly(String onlyFileName) {
    if (onlyFileName.equalsIgnoreCase("GMP_template_version_1.0")) {
      return true;
    } else {
      return false;
    }
  }

  @Override
  public List<Resource> list() {
    return new ArrayList<Resource>(resources.values());
  }

  @Override
  public int load() {
    File resourcesDir = dataDir.dataFile(DataDir.RESOURCES_DIR);
    resources.clear();
    int counter = 0;
    if (resourcesDir != null) {
      File[] resources = resourcesDir.listFiles();
      if (resources != null) {
        for (File resourceDir : resources) {
          if (resourceDir.isDirectory()) {
            try {
              addResource(loadFromDir(resourceDir));
              counter++;
            } catch (InvalidConfigException e) {
              log.error("Cant load resource " + resourceDir.getName(), e);
            }
          }
        }
        log.info("Loaded " + counter + " resources into memory alltogether.");
      } else {
        log.info("Data directory does not hold a resources directory: " + dataDir.dataFile(""));
      }
    } else {
      log.info("Data directory does not hold a resources directory: " + dataDir.dataFile(""));
    }
    return counter;
  }

  private Eml loadEml(Resource resource) {
    File emlFile = dataDir.resourceEmlFile(resource.getShortname(), null);
    Eml eml = null;
    try {
      InputStream in = new FileInputStream(emlFile);
      eml = EmlFactory.build(in);
    } catch (FileNotFoundException e) {
      eml = new Eml();
    } catch (IOException e) {
      log.error(e);
    } catch (SAXException e) {
      log.error("Invalid EML document", e);
      eml = new Eml();
    } catch (Exception e) {
      eml = new Eml();
    }
    resource.setEml(eml);
    syncEmlWithResource(resource);
    return eml;
  }

  /**
   * Calls loadFromDir(File, ActionLogger), inserting a new instance of ActionLogger.
   * 
   * @param resourceDir resource directory
   * @return loaded Resource
   */
  private Resource loadFromDir(File resourceDir) {
    return loadFromDir(resourceDir, new ActionLogger(log, new BaseAction(textProvider, config)));
  }

  /**
   * Reads a complete resource configuration (resource config & eml) from the resource config folder
   * and returns the Resource instance for the internal in memory cache.
   */
  private Resource loadFromDir(File resourceDir, ActionLogger alog) throws InvalidConfigException {
    if (resourceDir.exists()) {
      // load full configuration from resource.xml and eml.xml files
      String shortname = resourceDir.getName();
      try {
        File cfgFile = dataDir.resourceFile(shortname, PERSISTENCE_FILE);
        InputStream input = new FileInputStream(cfgFile);
        Resource resource = (Resource) xstream.fromXML(input);
        // non existing users end up being a NULL in the set, so remove them
        // shouldnt really happen - but people can even manually cause a mess
        resource.getManagers().remove(null);

        // non existent Extension end up being NULL
        // for ex, a user is trying to import a resource from one IPT to another without all required exts installed.
        for (ExtensionMapping ext : resource.getMappings()) {
          Extension x = ext.getExtension();
          if (x == null) {
            alog.warn("manage.resource.create.extension.null");
            throw new InvalidConfigException(TYPE.INVALID_EXTENSION, "Resource references non-existent extension");
          } else if (extensionManager.get(x.getRowType()) == null) {
            alog.warn("manage.resource.create.rowType.null", new String[] {x.getRowType()});
            throw new InvalidConfigException(TYPE.INVALID_EXTENSION, "Resource references non-installed extension");
          }
        }

        // shortname persists as folder name, so xstream doesnt handle this:
        resource.setShortname(shortname);

        // infer coreType if null
        if (resource.getCoreType() == null) {
          inferCoreType(resource);
        }

        // standardize subtype if not null
        if (resource.getSubtype() != null) {
          standardizeSubtype(resource);
        }

        // add proper source file pointer
        for (Source src : resource.getSources()) {
          src.setResource(resource);
          if (src instanceof FileSource) {
            ((FileSource) src).setFile(dataDir.sourceFile(resource, src));
          }
        }
        // load eml
        loadEml(resource);
        log.debug("Read resource configuration for " + shortname);
        return resource;
      } catch (FileNotFoundException e) {
        log.error("Cannot read resource configuration for " + shortname, e);
        throw new InvalidConfigException(TYPE.RESOURCE_CONFIG, "Cannot read resource configuration for " + shortname
          + ": " + e.getMessage());
      }
    }
    return null;
  }

  private void readAdditionalMetadata(Eml eml, Workbook template) throws InvalidFormatException {
    Sheet sheet = template.getSheet("Metadatos Adicionales");
    DateFormat dateFormatA = new SimpleDateFormat("MM/dd/yyyy");
    DateFormat dateFormatB = new SimpleDateFormat("yyyy-MM-dd");
    eml.setHierarchyLevel(readCellValue(sheet.getRow(5).getCell(1)));
    eml.setLogoUrl(readCellValue(sheet.getRow(7).getCell(1)));
    try {
      if (readCellValue(sheet.getRow(5).getCell(4)).matches("\\d{4}-\\d{2}-\\d{2}")) {
        eml.setPubDate(dateFormatB.parse(readCellValue(sheet.getRow(5).getCell(4))));
      } else if (readCellValue(sheet.getRow(5).getCell(4)).matches("\\d{2}/\\d{2}/\\d{4}")) {
        eml.setPubDate(dateFormatA.parse(readCellValue(sheet.getRow(5).getCell(4))));
      } else {
        throw new InvalidFormatException("Error al procesar fecha inicial y final en cobertura temporal: ");
      }
    } catch (ParseException e) {
      throw new InvalidFormatException("Error al procesar fecha inicial y final en cobertura temporal: " + e);
    }
    eml.setPurpose(readCellValue(sheet.getRow(9).getCell(1)));
    switch (readCellValue(sheet.getRow(11).getCell(1))) {
      case "Ningúna licencia seleccionada":
        eml.setIntellectualRights(readCellValue(sheet.getRow(12).getCell(1)));
        break;
      case "Creative Commons CCZero":
        eml.setIntellectualRights(baseAction.getText("eml.intellectualRights.license.cczero.text"));
        break;
      case "Open Data Commons Public Domain Dedication and Licence (PDDL)":
        eml.setIntellectualRights(baseAction.getText("eml.intellectualRights.license.pddl.text"));
        break;
      case "Open Data Commons Attribution License":
        eml.setIntellectualRights(baseAction.getText("eml.intellectualRights.license.odcby.text"));
        break;
      case "Open Data Commons Open Database License (ODbL)":
        eml.setIntellectualRights(baseAction.getText("eml.intellectualRights.license.odbl.text"));
        break;
      default:
        throw new InvalidFormatException("El tipo de licencia elegida es inválida.");
    }
    eml.setAdditionalInfo(readCellValue(sheet.getRow(14).getCell(1)));
    List<String> alternateIdentifiers = new ArrayList<String>();
    Iterator<Row> rowIterator = sheet.rowIterator();
    Row row;
    while (rowIterator.hasNext()) {
      row = rowIterator.next();
      if (readCellValue(sheet.getRow(row.getRowNum()).getCell(1, Row.CREATE_NULL_AS_BLANK)).equalsIgnoreCase(
        "Identificador Alterno:")) {
        if (!readCellValue(sheet.getRow(row.getRowNum() + 1).getCell(1)).isEmpty()) {
          alternateIdentifiers.add(readCellValue(sheet.getRow(row.getRowNum() + 1).getCell(1)));
        }
      }
    }
    eml.setAlternateIdentifiers(alternateIdentifiers);
  }

  private void readAssociatedParties(Eml eml, Workbook template) throws InvalidFormatException {
    Sheet sheet = template.getSheet("Partes Asociadas");
    Iterator<Row> rowIterator = sheet.rowIterator();
    Agent agent;
    Address address;
    Row row;
    while (rowIterator.hasNext()) {
      row = rowIterator.next();
      if (readCellValue(sheet.getRow(row.getRowNum()).getCell(1, Row.CREATE_NULL_AS_BLANK)).equalsIgnoreCase("Nombre:")) {
        agent = new Agent();
        agent.setFirstName(readCellValue(sheet.getRow(row.getRowNum() + 1).getCell(1, Row.CREATE_NULL_AS_BLANK)));
        agent.setLastName(readCellValue(sheet.getRow(row.getRowNum() + 1).getCell(4, Row.CREATE_NULL_AS_BLANK)));
        agent.setPosition(readCellValue(sheet.getRow(row.getRowNum() + 3).getCell(1, Row.CREATE_NULL_AS_BLANK)));
        agent.setOrganisation(readCellValue(sheet.getRow(row.getRowNum() + 3).getCell(4, Row.CREATE_NULL_AS_BLANK)));
        agent.setPhone(readCellValue(sheet.getRow(row.getRowNum() + 9).getCell(4, Row.CREATE_NULL_AS_BLANK)));
        agent.setEmail(readCellValue(sheet.getRow(row.getRowNum() + 11).getCell(1, Row.CREATE_NULL_AS_BLANK)));
        agent.setHomepage(readCellValue(sheet.getRow(row.getRowNum() + 11).getCell(4, Row.CREATE_NULL_AS_BLANK)));
        agent.setRole(readCellValue(sheet.getRow(row.getRowNum() + 13).getCell(1, Row.CREATE_NULL_AS_BLANK)));
        address = new Address();
        address.setAddress(readCellValue(sheet.getRow(row.getRowNum() + 5).getCell(1, Row.CREATE_NULL_AS_BLANK)));
        address.setCity(readCellValue(sheet.getRow(row.getRowNum() + 5).getCell(4, Row.CREATE_NULL_AS_BLANK)));
        address.setProvince(readCellValue(sheet.getRow(row.getRowNum() + 7).getCell(1, Row.CREATE_NULL_AS_BLANK)));
        address.setCountry(readCellValue(sheet.getRow(row.getRowNum() + 7).getCell(4, Row.CREATE_NULL_AS_BLANK)));
        address.setPostalCode(readCellValue(sheet.getRow(row.getRowNum() + 9).getCell(1, Row.CREATE_NULL_AS_BLANK)));
        agent.setAddress(address);
        eml.addAssociatedParty(agent);
      }
    }
  }

  private void readBasicMetaData(Eml eml, Workbook template, Resource resource) throws InvalidFormatException {
    Sheet sheet = template.getSheet("Metadatos Básicos");

    // Title
    eml.setTitle(readCellValue(sheet.getRow(5).getCell(1)));
    // Description
    eml.setDescription(readCellValue(sheet.getRow(7).getCell(1)));
    // Metadata language
    eml.setMetadataLanguage(readCellValue(sheet.getRow(9).getCell(1)));
    // Language resource
    eml.setLanguage(readCellValue(sheet.getRow(9).getCell(4)));

    // Resource creator
    Agent agent = new Agent();
    agent.setFirstName(readCellValue(sheet.getRow(33).getCell(1)));
    agent.setLastName(readCellValue(sheet.getRow(33).getCell(4)));
    agent.setPosition(readCellValue(sheet.getRow(35).getCell(1)));
    agent.setOrganisation(readCellValue(sheet.getRow(35).getCell(4)));
    agent.setPhone(readCellValue(sheet.getRow(41).getCell(4)));
    agent.setEmail(readCellValue(sheet.getRow(43).getCell(1)));
    agent.setHomepage(readCellValue(sheet.getRow(43).getCell(4)));
    Address address = new Address();
    address.setAddress(readCellValue(sheet.getRow(37).getCell(1)));
    address.setCity(readCellValue(sheet.getRow(37).getCell(4)));
    address.setProvince(readCellValue(sheet.getRow(39).getCell(1)));
    address.setCountry(readCellValue(sheet.getRow(39).getCell(4)));
    address.setPostalCode(String.valueOf(Math.round(sheet.getRow(41).getCell(1).getNumericCellValue())));
    agent.setAddress(address);
    eml.setResourceCreator(agent);

    // Contact
    agent = new Agent();
    agent.setFirstName(readCellValue(sheet.getRow(17).getCell(1)));
    agent.setLastName(readCellValue(sheet.getRow(17).getCell(4)));
    agent.setPosition(readCellValue(sheet.getRow(19).getCell(1)));
    agent.setOrganisation(readCellValue(sheet.getRow(19).getCell(4)));
    agent.setPhone(readCellValue(sheet.getRow(25).getCell(4)));
    agent.setEmail(readCellValue(sheet.getRow(27).getCell(1)));
    agent.setHomepage(readCellValue(sheet.getRow(27).getCell(4)));
    address = new Address();
    address.setAddress(readCellValue(sheet.getRow(21).getCell(1)));
    address.setCity(readCellValue(sheet.getRow(21).getCell(4)));
    address.setProvince(readCellValue(sheet.getRow(23).getCell(1)));
    address.setCountry(readCellValue(sheet.getRow(23).getCell(4)));
    address.setPostalCode(String.valueOf(Math.round(sheet.getRow(25).getCell(1).getNumericCellValue())));
    agent.setAddress(address);
    eml.setContact(agent);

    // Metadata provider
    agent = new Agent();
    agent.setFirstName(readCellValue(sheet.getRow(49).getCell(1)));
    agent.setLastName(readCellValue(sheet.getRow(49).getCell(4)));
    agent.setPosition(readCellValue(sheet.getRow(51).getCell(1)));
    agent.setOrganisation(readCellValue(sheet.getRow(51).getCell(4)));
    agent.setPhone(readCellValue(sheet.getRow(57).getCell(4)));
    agent.setEmail(readCellValue(sheet.getRow(59).getCell(1)));
    agent.setHomepage(readCellValue(sheet.getRow(59).getCell(4)));
    address = new Address();
    address.setAddress(readCellValue(sheet.getRow(53).getCell(1)));
    address.setCity(readCellValue(sheet.getRow(53).getCell(4)));
    address.setProvince(readCellValue(sheet.getRow(55).getCell(1)));
    address.setCountry(readCellValue(sheet.getRow(55).getCell(4)));
    address.setPostalCode(String.valueOf(Math.round(sheet.getRow(57).getCell(1).getNumericCellValue())));
    agent.setAddress(address);
    eml.setMetadataProvider(agent);
    // ////////////////////////////////////////////

    // ///////////////////////////////////////////
    // Core Type
    resource.setCoreType(readCellValue(sheet.getRow(11).getCell(1)));
    // SubType
    resource.setSubtype(readCellValue(sheet.getRow(11).getCell(4)));
    // //////////////////////////////////////////

  }

  private String readCellValue(Cell cell) throws InvalidFormatException {
    switch (cell.getCellType()) {
      case Cell.CELL_TYPE_STRING:
        return cell.getStringCellValue();
      case Cell.CELL_TYPE_NUMERIC:
        return Double.toString(cell.getNumericCellValue());
      case Cell.CELL_TYPE_BOOLEAN:
        return Boolean.toString(cell.getBooleanCellValue());
      case Cell.CELL_TYPE_FORMULA:
        return cell.getCellFormula();
      case Cell.CELL_TYPE_ERROR:
        throw new InvalidFormatException("Error en el formato de archivo");
      case Cell.CELL_TYPE_BLANK:
        return "";
      default:
        throw new InvalidFormatException("Error en el formato de archivo");
    }
  }

  private void readCitations(Eml eml, Workbook template) throws InvalidFormatException {
    Sheet sheet = template.getSheet("Referencias");
    Citation citation = new Citation();
    citation.setIdentifier(readCellValue(sheet.getRow(5).getCell(1)));
    citation.setCitation(readCellValue(sheet.getRow(7).getCell(1)));
    eml.setCitation(citation);
    BibliographicCitationSet val = new BibliographicCitationSet();
    Iterator<Row> rowIterator = sheet.rowIterator();
    Row row;
    while (rowIterator.hasNext()) {
      row = rowIterator.next();
      if (readCellValue(sheet.getRow(row.getRowNum()).getCell(1, Row.CREATE_NULL_AS_BLANK)).equalsIgnoreCase(
        "Identificación de la Referencia:")) {
        if (!readCellValue(sheet.getRow(row.getRowNum() + 1).getCell(1)).isEmpty()
          || !readCellValue(sheet.getRow(row.getRowNum() + 3).getCell(1)).isEmpty()) {
          val.add(readCellValue(sheet.getRow(row.getRowNum() + 3).getCell(1)),
            readCellValue(sheet.getRow(row.getRowNum() + 1).getCell(1)));
        }
      }
    }
    eml.setBibliographicCitationSet(val);
  }

  private void readCollectionData(Eml eml, Workbook template) throws InvalidFormatException {
    Sheet sheet = template.getSheet("Datos de la Colección");
    eml.setCollectionName(readCellValue(sheet.getRow(5).getCell(1)));
    eml.setCollectionId(readCellValue(sheet.getRow(5).getCell(4)));
    eml.setParentCollectionId(readCellValue(sheet.getRow(7).getCell(1)));
    eml.setSpecimenPreservationMethod(readCellValue(sheet.getRow(7).getCell(4)));
    Iterator<Row> rowIterator = sheet.rowIterator();
    Row row;
    List<JGTICuratorialUnit> jgtiCuratorialUnits = new ArrayList<JGTICuratorialUnit>();
    JGTICuratorialUnit jgtiCuratorialUnit;
    while (rowIterator.hasNext()) {
      row = rowIterator.next();
      if (readCellValue(sheet.getRow(row.getRowNum()).getCell(1, Row.CREATE_NULL_AS_BLANK)).equalsIgnoreCase(
        "Tipo de Método:")) {
        switch (readCellValue(sheet.getRow(row.getRowNum() + 1).getCell(1))) {
          case "Rango de conteo":
            jgtiCuratorialUnit = new JGTICuratorialUnit();
            jgtiCuratorialUnit.setRangeStart((int) sheet.getRow(row.getRowNum() + 5).getCell(2).getNumericCellValue());
            jgtiCuratorialUnit.setRangeEnd((int) sheet.getRow(row.getRowNum() + 5).getCell(4).getNumericCellValue());
            jgtiCuratorialUnit.setUnitType(readCellValue(sheet.getRow(row.getRowNum() + 5).getCell(6)));
            jgtiCuratorialUnits.add(jgtiCuratorialUnit);
            break;
          case "Conteo con incertidumbre":
            jgtiCuratorialUnit = new JGTICuratorialUnit();
            jgtiCuratorialUnit.setRangeMean((int) sheet.getRow(row.getRowNum() + 8).getCell(2).getNumericCellValue());
            jgtiCuratorialUnit.setUncertaintyMeasure((int) sheet.getRow(row.getRowNum() + 8).getCell(4)
              .getNumericCellValue());
            jgtiCuratorialUnit.setUnitType(readCellValue(sheet.getRow(row.getRowNum() + 8).getCell(6)));
            jgtiCuratorialUnits.add(jgtiCuratorialUnit);
            break;
        }
      }
    }
    eml.setJgtiCuratorialUnits(jgtiCuratorialUnits);
  }

  private void readExternallinks(Eml eml, Workbook template) throws InvalidFormatException {
    Sheet sheet = template.getSheet("Enlaces externos");
    eml.setDistributionUrl(readCellValue(sheet.getRow(5).getCell(1)));
    Iterator<Row> rowIterator = sheet.rowIterator();
    Row row;
    List<PhysicalData> physicalDatas = new ArrayList<PhysicalData>();
    PhysicalData physicalData = null;
    while (rowIterator.hasNext()) {
      row = rowIterator.next();
      if (readCellValue(sheet.getRow(row.getRowNum()).getCell(1, Row.CREATE_NULL_AS_BLANK)).equalsIgnoreCase("Nombre:")) {
        if (!readCellValue(sheet.getRow(row.getRowNum() + 1).getCell(1)).isEmpty()
          || !readCellValue(sheet.getRow(row.getRowNum() + 1).getCell(4)).isEmpty()
          || !readCellValue(sheet.getRow(row.getRowNum() + 3).getCell(1)).isEmpty()
          || !readCellValue(sheet.getRow(row.getRowNum() + 5).getCell(1)).isEmpty()
          || !readCellValue(sheet.getRow(row.getRowNum() + 5).getCell(4)).isEmpty()) {
          physicalData = new PhysicalData();
          physicalData.setName(readCellValue(sheet.getRow(row.getRowNum() + 1).getCell(1)));
          physicalData.setCharset(readCellValue(sheet.getRow(row.getRowNum() + 1).getCell(4)));
          physicalData.setDistributionUrl(readCellValue(sheet.getRow(row.getRowNum() + 3).getCell(1)));
          physicalData.setFormat(readCellValue(sheet.getRow(row.getRowNum() + 5).getCell(1)));
          physicalData.setFormatVersion(readCellValue(sheet.getRow(row.getRowNum() + 5).getCell(4)));
          physicalDatas.add(physicalData);
        }
      }
    }
    eml.setPhysicalData(physicalDatas);
  }

  private void readGeographicCoverage(Eml eml, Workbook template) {
    Sheet sheet = template.getSheet("Cobertura Geográfica");
    List<GeospatialCoverage> geospatialCoverageList = new ArrayList<GeospatialCoverage>();
    GeospatialCoverage geospatialCoverage = null;
    BBox boundingCoordinates = null;
    geospatialCoverage = new GeospatialCoverage();
    boundingCoordinates = new BBox();
    geospatialCoverage.setDescription(sheet.getRow(9).getCell(1).getStringCellValue());
    geospatialCoverageList.add(geospatialCoverage);
    boundingCoordinates.setOrderedBounds(sheet.getRow(7).getCell(1).getNumericCellValue(), sheet.getRow(5).getCell(1)
      .getNumericCellValue(), sheet.getRow(7).getCell(4).getNumericCellValue(), sheet.getRow(5).getCell(4)
      .getNumericCellValue());
    geospatialCoverage.setBoundingCoordinates(boundingCoordinates);
    eml.setGeospatialCoverages(geospatialCoverageList);
  }

  private void readKeywords(Eml eml, Workbook template) throws InvalidFormatException {
    Sheet sheet = template.getSheet("Palabras Clave");
    List<KeywordSet> keywordsSet = new ArrayList<KeywordSet>();
    KeywordSet keywordSet = null;
    Iterator<Row> rowIterator = sheet.rowIterator();
    Row row;
    while (rowIterator.hasNext()) {
      row = rowIterator.next();
      if (readCellValue(sheet.getRow(row.getRowNum()).getCell(1, Row.CREATE_NULL_AS_BLANK)).equalsIgnoreCase(
        "Tesauro *REQUERIDO:")) {
        row = rowIterator.next();
        if (!readCellValue(sheet.getRow(row.getRowNum()).getCell(1)).isEmpty()
          && !readCellValue(sheet.getRow(row.getRowNum() + 2).getCell(1)).isEmpty()) {
          keywordSet = new KeywordSet();
          keywordSet.setKeywordThesaurus(readCellValue(sheet.getRow(row.getRowNum()).getCell(1)));
          keywordSet.setKeywordsString(readCellValue(sheet.getRow(row.getRowNum() + 2).getCell(1)));
          keywordsSet.add(keywordSet);
        }
      }
    }
    eml.setKeywords(keywordsSet);
  }

  private void readProjectData(Eml eml, Workbook template) throws InvalidFormatException {
    Sheet sheet = template.getSheet("Datos del Proyecto");
    Project project = new Project();
    project.setTitle(readCellValue(sheet.getRow(5).getCell(1)));
    project.setFunding(readCellValue(sheet.getRow(11).getCell(1)));
    project.setDesignDescription(readCellValue(sheet.getRow(15).getCell(1)));
    Agent personnel = new Agent();
    personnel.setFirstName(readCellValue(sheet.getRow(7).getCell(1)));
    personnel.setLastName(readCellValue(sheet.getRow(7).getCell(4)));
    personnel.setRole(readCellValue(sheet.getRow(9).getCell(1)));
    project.setPersonnel(personnel);
    StudyAreaDescription studyAreaDescription = new StudyAreaDescription();
    studyAreaDescription.setDescriptorValue(readCellValue(sheet.getRow(13).getCell(1)));
    project.setStudyAreaDescription(studyAreaDescription);
    eml.setProject(project);
  }

  private void readSamplingMethods(Eml eml, Workbook template) throws InvalidFormatException {
    Sheet sheet = template.getSheet("Métodos de Muestreo");
    eml.setStudyExtent(readCellValue(sheet.getRow(5).getCell(1)));
    eml.setSampleDescription(readCellValue(sheet.getRow(7).getCell(1)));
    eml.setQualityControl(readCellValue(sheet.getRow(9).getCell(1)));
    List<String> methodSteps = new ArrayList<String>();
    Iterator<Row> rowIterator = sheet.rowIterator();
    Row row;
    while (rowIterator.hasNext()) {
      row = rowIterator.next();
      if (readCellValue(sheet.getRow(row.getRowNum()).getCell(1, Row.CREATE_NULL_AS_BLANK)).equalsIgnoreCase(
        "Descripción del Paso Metodológico:")) {
        if (!readCellValue(sheet.getRow(row.getRowNum() + 1).getCell(1)).isEmpty()) {
          methodSteps.add(readCellValue(sheet.getRow(row.getRowNum() + 1).getCell(1)));
        }
      }
    }
    eml.setMethodSteps(methodSteps);
  }

  private void readTaxonomicCoverage(Eml eml, Workbook template) throws InvalidFormatException {
    Sheet sheet = template.getSheet("Cobertura Taxonómica");
    List<TaxonomicCoverage> taxonomicCoverages = new ArrayList<TaxonomicCoverage>();
    TaxonomicCoverage taxonomicCoverage = null;
    List<TaxonKeyword> keywords = null;
    TaxonKeyword taxonKeyword = null;
    Iterator<Row> rowIterator = sheet.rowIterator();
    Row row;
    while (rowIterator.hasNext()) {
      row = rowIterator.next();
      if (readCellValue(sheet.getRow(row.getRowNum()).getCell(1, Row.CREATE_NULL_AS_BLANK)).equalsIgnoreCase(
        "Descripción:")) {
        if (!readCellValue(sheet.getRow(row.getRowNum() + 1).getCell(1)).isEmpty()) {
          taxonomicCoverage = new TaxonomicCoverage();
          taxonomicCoverage.setDescription(readCellValue(sheet.getRow(row.getRowNum() + 1).getCell(1)));
          keywords = new ArrayList<TaxonKeyword>();
          row = rowIterator.next();
          row = rowIterator.next();
          while (readCellValue(sheet.getRow(row.getRowNum()).getCell(1, Row.CREATE_NULL_AS_BLANK)).equalsIgnoreCase(
            "Nombre científico *REQUERIDO:")) {
            row = rowIterator.next();
            if (!readCellValue(sheet.getRow(row.getRowNum()).getCell(1)).isEmpty()) {
              taxonKeyword = new TaxonKeyword();
              taxonKeyword.setScientificName(readCellValue(sheet.getRow(row.getRowNum()).getCell(1)));
              taxonKeyword.setCommonName(readCellValue(sheet.getRow(row.getRowNum()).getCell(4)));
              taxonKeyword.setRank(readCellValue(sheet.getRow(row.getRowNum()).getCell(7)));
              keywords.add(taxonKeyword);
            }
            row = rowIterator.next();
          }
          taxonomicCoverage.setTaxonKeywords(keywords);
          taxonomicCoverages.add(taxonomicCoverage);
        }
      }
    }
    eml.setTaxonomicCoverages(taxonomicCoverages);
  }

  private void readTemporalCoverage(Eml eml, Workbook template) throws InvalidFormatException {
    Sheet sheet = template.getSheet("Cobertura Temporal");
    List<TemporalCoverage> temporalCoverages = new ArrayList<TemporalCoverage>();
    TemporalCoverage temporalCoverage = null;
    DateFormat dateFormatA = new SimpleDateFormat("MM/dd/yyyy");
    DateFormat dateFormatB = new SimpleDateFormat("yyyy-MM-dd");
    Iterator<Row> rowIterator = sheet.rowIterator();
    Row row;
    while (rowIterator.hasNext()) {
      row = rowIterator.next();
      if (readCellValue(sheet.getRow(row.getRowNum()).getCell(1, Row.CREATE_NULL_AS_BLANK)).equalsIgnoreCase(
        "Tipo de cobertura temporal:")) {
        switch (readCellValue(sheet.getRow(row.getRowNum() + 1).getCell(1))) {
          case "Fecha Simple":
            try {
              temporalCoverage = new TemporalCoverage();
              if (readCellValue(sheet.getRow(row.getRowNum() + 5).getCell(2)).matches("\\d{4}-\\d{2}-\\d{2}")) {
                temporalCoverage.setStartDate(dateFormatB.parse(readCellValue(sheet.getRow(row.getRowNum() + 14)
                  .getCell(2))));
              } else if (readCellValue(sheet.getRow(row.getRowNum() + 5).getCell(2)).matches("\\d{2}/\\d{2}/\\d{4}")) {
                temporalCoverage.setStartDate(dateFormatA.parse(readCellValue(sheet.getRow(row.getRowNum() + 14)
                  .getCell(2))));
              } else {
                throw new InvalidFormatException("Error al procesar fecha inicial y final en cobertura temporal: ");
              }
              temporalCoverages.add(temporalCoverage);
            } catch (ParseException e) {
              throw new InvalidFormatException("Error al procesar fecha inicial y final en cobertura temporal: " + e);
            }
            break;
          case "Período de Tiempo de Vida":
            temporalCoverage = new TemporalCoverage();
            temporalCoverage.setLivingTimePeriod(readCellValue(sheet.getRow(row.getRowNum() + 8).getCell(2)));
            temporalCoverages.add(temporalCoverage);
            break;
          case "Período de Formación":
            temporalCoverage = new TemporalCoverage();
            temporalCoverage.setFormationPeriod(readCellValue(sheet.getRow(row.getRowNum() + 11).getCell(2)));
            temporalCoverages.add(temporalCoverage);
            break;
          case "Rango de Fechas":
            try {
              temporalCoverage = new TemporalCoverage();
              if (readCellValue(sheet.getRow(row.getRowNum() + 14).getCell(2)).matches("\\d{4}-\\d{2}-\\d{2}")) {
                temporalCoverage.setStartDate(dateFormatB.parse(readCellValue(sheet.getRow(row.getRowNum() + 14)
                  .getCell(2))));
              } else if (readCellValue(sheet.getRow(row.getRowNum() + 14).getCell(2)).matches("\\d{2}/\\d{2}/\\d{4}")) {
                temporalCoverage.setStartDate(dateFormatA.parse(readCellValue(sheet.getRow(row.getRowNum() + 14)
                  .getCell(2))));
              } else {
                throw new InvalidFormatException("Error al procesar fecha inicial y final en cobertura temporal: ");
              }
              if (readCellValue(sheet.getRow(row.getRowNum() + 14).getCell(5)).matches("\\d{4}-\\d{2}-\\d{2}")) {
                temporalCoverage.setEndDate(dateFormatB.parse(readCellValue(sheet.getRow(row.getRowNum() + 14).getCell(
                  5))));
              } else if (readCellValue(sheet.getRow(row.getRowNum() + 14).getCell(5)).matches("\\d{2}/\\d{2}/\\d{4}")) {
                temporalCoverage.setEndDate(dateFormatA.parse(readCellValue(sheet.getRow(row.getRowNum() + 14).getCell(
                  5))));
              } else {
                throw new InvalidFormatException("Error al procesar fecha inicial y final en cobertura temporal: ");
              }
              temporalCoverages.add(temporalCoverage);
            } catch (ParseException e) {
              throw new InvalidFormatException("Error al procesar fecha inicial y final en cobertura temporal: " + e);
            }
            break;
          default:
            break;
        }
      }
    }
    eml.setTemporalCoverages(temporalCoverages);
  }

  private void saveEml(String fileName) {
    // save into data dir
    File emlFile = dataDir.resourceEmlFile(fileName, null);

    try {
      EmlWriter.writeEmlFile(emlFile, resource.getEml());
      log.debug("Updated EML file for " + resource);
    } catch (IOException e) {
      log.error(e);
      // throw new InvalidConfigException(TYPE.CONFIG_WRITE, "IO exception when writing eml for " + resource);
    } catch (TemplateException e) {
      log.error("EML template exception", e);
      // throw new InvalidConfigException(TYPE.EML, "EML template exception when writing eml for " + resource + ": " +
// e.getMessage());
    }
  }

  /**
   * The resource's subType might not have been set using a standardized term from the dataset_subtype vocabulary.
   * All versions before 2.0.4 didn't use the vocabulary, so this method is particularly important during upgrades
   * to 2.0.4 and later. Basically, if the subType isn't recognized as belonging to the vocabulary, it is reset as
   * null. That would mean the user would then have to reselect the subtype from the Basic Metadata page.
   * 
   * @param resource Resource
   * @return resource with subtype set using term from dataset_subtype vocabulary (assuming it has been set).
   */
  Resource standardizeSubtype(Resource resource) {
    if (resource != null && resource.getSubtype() != null) {
      // the vocabulary key names are identifiers and standard across Locales
      // it's this key we want to persist as the subtype
      Map<String, String> subtypes =
        vocabularyManager.getI18nVocab(Constants.VOCAB_URI_DATASET_SUBTYPES, Locale.ENGLISH.getLanguage(), false);
      boolean usesVocab = false;
      for (Map.Entry<String, String> entry : subtypes.entrySet()) {
        // remember to do comparison regardless of case, since the subtype is stored in lowercase
        if (resource.getSubtype().equalsIgnoreCase(entry.getKey())) {
          usesVocab = true;
        }
      }
      // if the subtype doesn't use a standardized term from the vocab, it's reset to null
      if (!usesVocab) {
        resource.setSubtype(null);
      }
    }
    return resource;
  }

  private void syncEmlWithResource(Resource resource) {
    resource.getEml().setEmlVersion(resource.getEmlVersion());
    // we need some GUID. If we have use the registry key, if not use the resource URL
    if (resource.getKey() != null) {
      resource.getEml().setGuid(resource.getKey().toString());
    } else {
      resource.getEml().setGuid(getResourceLink(resource.getShortname()).toString());
    }
  }

}
