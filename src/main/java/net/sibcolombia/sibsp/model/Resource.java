package net.sibcolombia.sibsp.model;

import org.gbif.metadata.eml.Eml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.google.common.base.Strings;


public class Resource {

  public enum CoreRowType {
    OCCURRENCE, CHECKLIST, METADATA, OTHER
  }

  private Eml eml;
  private String fileName;
  private String coreType;
  private String subtype;
  private UUID uniqueID;
  private final int emlVersion = 0;
  private final Set<User> managers = new HashSet<User>();
  // mapping configs
  private final Set<Source> sources = new HashSet<Source>();
  // registry data - only exists when status=REGISTERED
  private UUID key;

  private final List<ExtensionMapping> mappings = new ArrayList<ExtensionMapping>();
  private Date created;


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

  public Set<User> getManagers() {
    return managers;
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

  public void setCoreType(String coreType) {
    this.coreType = Strings.isNullOrEmpty(coreType) ? null : coreType;
  }

  public void setCreated(Date created) {
    this.created = created;
  }

  public void setEml(Eml eml) {
    this.eml = eml;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
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
