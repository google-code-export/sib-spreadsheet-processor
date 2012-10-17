package net.sibcolombia.sibsp.model;

import org.gbif.metadata.eml.Eml;

import com.google.common.base.Strings;
import org.apache.log4j.Logger;


public class Resource {

  private static Logger log = Logger.getLogger(Resource.class);
  private Eml eml;
  private String fileName;
  private String coreType;
  private String subtype;


  public String getCoreType() {
    return coreType;
  }


  public Eml getEml() {
    return eml;
  }

  public String getFileName() {
    return fileName;
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
