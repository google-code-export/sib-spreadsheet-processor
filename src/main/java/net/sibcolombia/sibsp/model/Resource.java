package net.sibcolombia.sibsp.model;

import org.gbif.metadata.eml.Eml;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Strings;


public class Resource {

  private Eml eml;
  private String fileName;
  private String coreType;
  private String subtype;
  private String shortname; // unique

  private final List<ExtensionMapping> mappings = new ArrayList<ExtensionMapping>();


  public String getCoreType() {
    return coreType;
  }


  public Eml getEml() {
    return eml;
  }

  public String getFileName() {
    return fileName;
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

  public String getShortname() {
    return shortname;
  }

  public String getSubtype() {
    return subtype;
  }

  public void setCoreType(String coreType) {
    this.coreType = Strings.isNullOrEmpty(coreType) ? null : coreType;
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

  @Override
  public String toString() {
    return "Resource " + fileName;
  }

}
