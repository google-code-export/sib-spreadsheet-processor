/***************************************************************************
 * Copyright 2010 Global Biodiversity Information Facility Secretariat
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ***************************************************************************/

package net.sibcolombia.sibsp.model;

import org.gbif.file.CSVReader;
import org.gbif.ipt.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.Iterator;

import javax.annotation.Nullable;

import com.google.common.base.Objects;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import static com.google.common.base.Objects.equal;

public abstract class Source implements Comparable<Source>, Serializable {

  public static class FileSource extends Source {

    private static final long serialVersionUID = -6165227796710382655L;

    private static final Logger LOG = Logger.getLogger(FileSource.class);

    private String fieldsTerminatedBy = "\t";
    private String fieldsEnclosedBy;
    private int ignoreHeaderLines = 1;
    private File file;
    private long fileSize;
    private int rows;
    protected Date lastModified;

    private String escape(String x) {
      if (x == null) {
        return null;
      }
      return x.replaceAll("\\t", "\\\\t").replaceAll("\\n", "\\\\n").replaceAll("\\r", "\\\\r")
        .replaceAll("\\f", "\\\\f");
    }

    public Character getFieldQuoteChar() {
      if (fieldsEnclosedBy == null || fieldsEnclosedBy.length() == 0) {
        return null;
      }
      return fieldsEnclosedBy.charAt(0);
    }

    public String getFieldsEnclosedBy() {
      return fieldsEnclosedBy;
    }

    public String getFieldsEnclosedByEscaped() {
      return escape(fieldsEnclosedBy);
    }

    public String getFieldsTerminatedBy() {
      return fieldsTerminatedBy;
    }

    public String getFieldsTerminatedByEscaped() {
      return escape(fieldsTerminatedBy);
    }

    public File getFile() {
      return file;
    }

    public long getFileSize() {
      return fileSize;
    }

    public String getFileSizeFormatted() {
      return FileUtils.formatSize(fileSize, 1);
    }

    public int getIgnoreHeaderLines() {
      return ignoreHeaderLines;
    }

    public Date getLastModified() {
      return lastModified;
    }

    public CSVReader getReader() throws IOException {
      return CSVReader.build(file, encoding, fieldsTerminatedBy, getFieldQuoteChar(), ignoreHeaderLines);
    }

    public int getRows() {
      return rows;
    }

    public Iterator<String[]> iterator() {
      try {
        CSVReader reader = getReader();
        return reader.iterator();
      } catch (IOException e) {
        LOG.warn("Exception caught", e);
      }
      return null;
    }

    public void setFieldsEnclosedBy(String fieldsEnclosedBy) {
      this.fieldsEnclosedBy = fieldsEnclosedBy;
    }

    public void setFieldsEnclosedByEscaped(String fieldsEnclosedBy) {
      this.fieldsEnclosedBy = unescape(fieldsEnclosedBy);
    }

    public void setFieldsTerminatedBy(String fieldsTerminatedBy) {
      this.fieldsTerminatedBy = fieldsTerminatedBy;
    }

    public void setFieldsTerminatedByEscaped(String fieldsTerminatedBy) {
      this.fieldsTerminatedBy = unescape(fieldsTerminatedBy);
    }

    public void setFile(File file) {
      this.file = file;
    }

    public void setFileSize(long fileSize) {
      this.fileSize = fileSize;
    }

    public void setIgnoreHeaderLines(Integer ignoreHeaderLines) {
      this.ignoreHeaderLines = ignoreHeaderLines == null ? 0 : ignoreHeaderLines;
    }

    public void setLastModified(Date lastModified) {
      this.lastModified = lastModified;
    }

    public void setRows(int rows) {
      this.rows = rows;
    }

    private String unescape(String x) {
      if (x == null) {
        return null;
      }
      return x.replaceAll("\\\\t", String.valueOf('\t')).replaceAll("\\\\n", String.valueOf('\n'))
        .replaceAll("\\\\r", String.valueOf('\r')).replaceAll("\\\\f", String.valueOf('\f'));
    }

  }

  private static final long serialVersionUID = 119920000112L;

  protected Resource resource;
  protected String name;
  protected String encoding = "UTF-8";
  protected String dateFormat = "YYYY-MM-DD";
  protected int columns;
  protected boolean readable = false;

  /**
   * This method normalises a file name by removing certain reserved characters and converting all file name characters
   * to lowercase.
   * The reserved characters are:
   * <ul>
   * <li>All whitespace characters</li>
   * <li>All slash character</li>
   * <li>All backslash character</li>
   * <li>All question mark character</li>
   * <li>All percent character</li>
   * <li>All asterik character</li>
   * <li>All colon character</li>
   * <li>All pipe character</li>
   * <li>All less than character</li>
   * <li>All greater than character</li>
   * <li>All quote character</li>
   * </ul>
   * 
   * @param name to normalise, may be null
   * @return normalised name
   */
  public static String normaliseName(@Nullable String name) {
    if (name == null) {
      return null;
    }
    return StringUtils.substringBeforeLast(name, ".").replaceAll("[\\s.:/\\\\*?%|><\"]+", "").toLowerCase();
  }

  @Override
  public int compareTo(Source o) {
    if (this == o) {
      return 0;
    }
    if (this.name == null) {
      return -1;
    }
    return name.compareTo(o.name);
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }

    if (!Source.class.isInstance(other)) {
      return false;
    }
    Source o = (Source) other;
    // return equal(resource, o.resource) && equal(name, o.name);
    return equal(name, o.name);
  }

  public int getColumns() {
    return columns;
  }

  public String getDateFormat() {
    return dateFormat;
  }

  public String getEncoding() {
    return encoding;
  }

  public String getName() {
    return name;
  }

  public Resource getResource() {
    return resource;
  }

  @Override
  public int hashCode() {
    // return Objects.hashCode(resource, name);
    return Objects.hashCode(name);
  }

  public boolean isFileSource() {
    return FileSource.class.isInstance(this);
  }

  public boolean isReadable() {
    return readable;
  }

  public void setColumns(int columns) {
    this.columns = columns;
  }

  public void setDateFormat(String dateFormat) {
    this.dateFormat = dateFormat;
  }

  public void setEncoding(String encoding) {
    this.encoding = encoding;
  }

  public void setName(String name) {
    this.name = normaliseName(name);
  }

  public void setNameNoNormalise(String name) {
    this.name = name;
  }

  public void setReadable(boolean readable) {
    this.readable = readable;
  }

  public void setResource(Resource resource) {
    this.resource = resource;
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName() + "[" + name + ";" + resource + "]";
  }

}