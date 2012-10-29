package net.sibcolombia.sibsp.model;

import org.gbif.dwc.terms.ConceptTerm;
import org.gbif.dwc.terms.TermFactory;
import org.gbif.metadata.eml.Eml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.google.common.base.Strings;
import net.sibcolombia.sibsp.service.AlreadyExistingException;
import org.apache.log4j.Logger;


public class Resource {

  public enum CoreRowType {
    OCCURRENCE, CHECKLIST, METADATA, OTHER
  }

  private static Logger log = Logger.getLogger(Resource.class);

  private Eml eml;
  private String fileName;
  private String coreType;
  private String subtype;
  private UUID uniqueID;
  private int emlVersion = 0;
  private int recordsPublished = 0;
  private final Set<User> managers = new HashSet<User>();
  // mapping configs
  private final Set<Source> sources = new HashSet<Source>();
  // registry data - only exists when status=REGISTERED
  private UUID key;

  private final List<ExtensionMapping> mappings = new ArrayList<ExtensionMapping>();
  private Date created;

  private static final TermFactory FACT = new TermFactory();
  private Date lastPublished;


  /**
   * Adds a new extension mapping to the resource. For non core extensions a core extension must exist already.
   * It returns the list index for this mapping according to getMappings(rowType)
   * 
   * @return list index corresponding to getMappings(rowType) or null if the mapping couldnt be added
   * @throws IllegalArgumentException if no core mapping exists when adding a non core mapping
   */
  public Integer addMapping(ExtensionMapping mapping) throws IllegalArgumentException {
    if (mapping != null && mapping.getExtension() != null) {
      if (!mapping.isCore() && !hasCore()) {
        throw new IllegalArgumentException("Cannot add extension mapping before a core mapping exists");
      }
      Integer index = getMappings(mapping.getExtension().getRowType()).size();
      this.mappings.add(mapping);
      return index;
    }
    return null;
  }

  public void addSource(Source src, boolean allowOverwrite) throws AlreadyExistingException {
    // make sure we talk about the same resource
    src.setResource(this);
    if (!allowOverwrite && sources.contains(src)) {
      throw new AlreadyExistingException();
    }
    if (allowOverwrite && sources.contains(src)) {
      // If source file is going to be overwritten, it should be actually re-add it.
      sources.remove(src);
      // Changing the Source in the ExtensionMapping object from the mapping list.
      for (ExtensionMapping ext : this.getMappings()) {
        if (ext.getSource().equals(src)) {
          ext.setSource(src);
        }
      }
    }
    sources.add(src);
  }

  /**
   * Delete a Resource's mapping. If the mapping gets successfully deleted, and the mapping is a core type mapping,
   * and there are no additional core type mappings, all other mappings are also cleared.
   * 
   * @param mapping ExtensionMapping
   * @return if deletion was successful or not
   */
  public boolean deleteMapping(ExtensionMapping mapping) {
    boolean result = false;
    if (mapping != null) {
      result = mappings.remove(mapping);
      // if last core gets deleted, delete all other mappings too!
      if (result && mapping.isCore() && getCoreMappings().isEmpty()) {
        mappings.clear();
      }
    }
    return result;
  }

  public boolean deleteSource(Source src) {
    boolean result = false;
    if (src != null) {
      result = sources.remove(src);
      // also remove existing mappings
      List<ExtensionMapping> ems = new ArrayList<ExtensionMapping>(mappings);
      for (ExtensionMapping em : ems) {
        if (em.getSource().equals(src)) {
          deleteMapping(em);
          log.debug("Cascading source delete to mapping " + em.getExtension().getTitle());
        }
      }
    }
    return result;
  }

  public List<ExtensionMapping> getCoreMappings() {
    List<ExtensionMapping> cores = new ArrayList<ExtensionMapping>();
    for (ExtensionMapping m : mappings) {
      if (m.isCore()) {
        cores.add(m);
      }
    }
    return cores;
  }

  /**
   * Get the rowType of the core mapping. This method first iterates through a list of the core mappings if there
   * are any. Then, since they will all be of the same core rowType, the first mapping is read for its rowType and
   * this String is returned.
   * 
   * @return core rowType
   */
  public String getCoreRowType() {
    List<ExtensionMapping> cores = getCoreMappings();
    if (!cores.isEmpty()) {
      return cores.get(0).getExtension().getRowType();
    }
    return null;
  }

  public String getCoreType() {
    return coreType;
  }

  public ConceptTerm getCoreTypeTerm() {
    List<ExtensionMapping> cores = getCoreMappings();
    if (!cores.isEmpty()) {
      return FACT.findTerm(cores.get(0).getExtension().getRowType());
    }
    return null;
  }

  public Date getCreated() {
    return created;
  }

  public Eml getEml() {
    return eml;
  }

  public int getEmlVersion() {
    return emlVersion;
  }

  public String getFileName() {
    return fileName;
  }

  public UUID getKey() {
    return key;
  }

  public Date getLastPublished() {
    return this.lastPublished;
  }

  public Set<User> getManagers() {
    return managers;
  }

  public List<Extension> getMappedExtensions() {
    Set<Extension> exts = new HashSet<Extension>();
    for (ExtensionMapping em : mappings) {
      if (em.getExtension() != null) {
        exts.add(em.getExtension());
      } else {
        log.error("Encountered an ExtensionMapping referencing NULL Extension for resource: " + uniqueID.toString());
      }
    }
    return new ArrayList<Extension>(exts);
  }

  public List<ExtensionMapping> getMappings() {
    return mappings;
  }

  /**
   * Get the list of mappings for the requested extension rowtype.
   * The order of mappings in the list is guaranteed to be stable and the same as the underlying original mappings
   * list.
   * 
   * @param rowType identifying the extension
   * @return the list of mappings for the requested extension rowtype
   */
  public List<ExtensionMapping> getMappings(String rowType) {
    List<ExtensionMapping> maps = new ArrayList<ExtensionMapping>();
    if (rowType != null) {
      for (ExtensionMapping m : mappings) {
        if (rowType.equals(m.getExtension().getRowType())) {
          maps.add(m);
        }
      }
    }
    return maps;
  }

  public int getRecordsPublished() {
    return this.recordsPublished;
  }

  public List<Source> getSources() {
    List<Source> srcs = new ArrayList<Source>(sources);
    Collections.sort(srcs);
    return srcs;
  }

  public String getSubtype() {
    return subtype;
  }

  public UUID getUniqueID() {
    return uniqueID;
  }

  /**
   * @return true if this resource is mapped to at least one core extension
   */
  public boolean hasCore() {
    return getCoreTypeTerm() != null;
  }

  public boolean hasMappedData() {
    for (ExtensionMapping cm : getCoreMappings()) {
      // test each core mapping if there is at least one field mapped
      if (!cm.getFields().isEmpty()) {
        return true;
      }
    }
    return false;
  }

  public void setCoreType(String coreType) {
    this.coreType = Strings.isNullOrEmpty(coreType) ? null : coreType;
  }

  public void setCreated(Date created) {
    this.created = created;
  }

  public void setEml(Eml eml) {
    this.eml = eml;
  }

  public void setEmlVersion(int emlVersion) {
    this.emlVersion = emlVersion;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public void setLastPublished(Date lastPublished) {
    this.lastPublished = lastPublished;
  }

  public void setRecordsPublished(int recordsPublished) {
    this.recordsPublished = recordsPublished;
  }

  /**
   * Sets the resource subtype. If it is null or an empty string, it is set to null. Otherwise, it is simply set
   * in lowercase.
   * 
   * @param subtype subtype String
   */
  public void setSubtype(String subtype) {
    this.subtype = (Strings.isNullOrEmpty(subtype)) ? null : subtype.toLowerCase();
  }

  public void setUniqueID(UUID uniqueID) {
    this.uniqueID = uniqueID;
  }

  @Override
  public String toString() {
    return "Resource " + fileName;
  }

}
