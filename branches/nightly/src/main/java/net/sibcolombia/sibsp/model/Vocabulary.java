package net.sibcolombia.sibsp.model;

import java.io.Serializable;
import java.net.URI;
import java.util.Date;

import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.builder.CompareToBuilder;


public class Vocabulary implements Comparable, Serializable {

  private static final long serialVersionUID = -5207915916507494914L;

  @SerializedName("identifier")
  private String uriString; // identifier
  @SerializedName("url")
  private URI uriResolvable; // resolvable URI to its definition
  private String title;

  private Date lastUpdate;

  @Override
  public int compareTo(Object object) {
    Vocabulary myClass = (Vocabulary) object;
    return new CompareToBuilder().append(this.uriString, myClass.uriString).toComparison();
  }

  public String getTitle() {
    return title;
  }

  /**
   * Identifier for Vocabulary. E.g. http://dublincore.org/documents/dcmi-type-vocabulary/
   * 
   * @return identifier for Vocabulary
   */
  public String getUriString() {
    return uriString;
  }

  public void setLastUpdate(Date lastUpdate) {
    this.lastUpdate = lastUpdate;
  }

}
