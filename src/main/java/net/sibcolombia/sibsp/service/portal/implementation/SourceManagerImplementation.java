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

package net.sibcolombia.sibsp.service.portal.implementation;

import org.gbif.dwc.text.ArchiveFactory;
import org.gbif.dwc.text.ArchiveFile;
import org.gbif.dwc.text.UnsupportedArchiveException;
import org.gbif.file.CSVReader;
import org.gbif.utils.file.ClosableIterator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.inject.Inject;
import net.sibcolombia.sibsp.configuration.ApplicationConfig;
import net.sibcolombia.sibsp.configuration.DataDir;
import net.sibcolombia.sibsp.model.Resource;
import net.sibcolombia.sibsp.model.Source;
import net.sibcolombia.sibsp.model.Source.FileSource;
import net.sibcolombia.sibsp.service.AlreadyExistingException;
import net.sibcolombia.sibsp.service.BaseManager;
import net.sibcolombia.sibsp.service.ImportException;
import net.sibcolombia.sibsp.service.SourceException;
import net.sibcolombia.sibsp.service.portal.SourceManager;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

public class SourceManagerImplementation extends BaseManager implements SourceManager {

  private class FileColumnIterator implements ClosableIterator<Object> {

    private final CSVReader reader;
    private final int column;

    public FileColumnIterator(FileSource source, int column) throws IOException {
      reader = source.getReader();
      this.column = column;
    }

    @Override
    public void close() {
      if (reader != null) {
        reader.close();
      }
    }

    @Override
    public boolean hasNext() {
      return reader.hasNext();
    }

    @Override
    public Object next() {
      String[] row = reader.next();
      if (row == null || row.length < column) {
        return null;
      }
      return row[column];
    }

    @Override
    public void remove() {
      // unsupported
    }
  }

  @Inject
  public SourceManagerImplementation(ApplicationConfig config, DataDir dataDir) {
    super(config, dataDir);
  }

  public static void copyArchiveFileProperties(ArchiveFile from, FileSource to) {
    to.setEncoding(from.getEncoding());
    to.setFieldsEnclosedBy(from.getFieldsEnclosedBy() == null ? null : from.getFieldsEnclosedBy().toString());
    to.setFieldsTerminatedBy(from.getFieldsTerminatedBy());
    to.setIgnoreHeaderLines(from.getIgnoreHeaderLines());
    to.setDateFormat(from.getDateFormat());
  }

  @Override
  public FileSource add(Resource resource, File file, @Nullable String sourceName) throws ImportException {
    return addOneFile(resource, file, sourceName);
  }

  private FileSource addOneFile(Resource resource, File file, @Nullable String sourceName) throws ImportException {
    FileSource src = new FileSource();
    if (sourceName == null) {
      sourceName = file.getName();
    }
    src.setName(sourceName);
    src.setResource(resource);
    log.debug("ADDING SOURCE " + sourceName + " FROM " + file.getAbsolutePath());

    try {
      // copy file
      File ddFile = dataDir.sourceFile(resource, src);
      try {
        FileUtils.copyFile(file, ddFile);
      } catch (IOException e1) {
        throw new ImportException(e1);
      }
      src.setFile(ddFile);
      src.setLastModified(new Date());

      // add to resource, allow overwriting existing ones
      // if the file is uploaded not for the first time
      resource.addSource(src, true);
    } catch (AlreadyExistingException e) {
      throw new ImportException(e);
    }
    // analyze file
    // analyze(src);
    return src;
  }

  @Override
  public String analyze(Source source) {
    String problem = null;
    if (source instanceof FileSource) {
      FileSource fs = (FileSource) source;
      try {
        CSVReader reader = fs.getReader();
        fs.setFileSize(fs.getFile().length());
        // careful - the reader.header can be null. In this case set number of columns to 0
        fs.setColumns((reader.header == null) ? 0 : reader.header.length);
        while (reader.hasNext()) {
          reader.next();
        }
        fs.setRows(reader.getReadRows());
        fs.setReadable(true);

        File logFile = dataDir.sourceLogFile(source.getResource().getUniqueID().toString(), source.getName());
        FileUtils.deleteQuietly(logFile);
        BufferedWriter logWriter = null;
        try {
          logWriter = new BufferedWriter(new FileWriter(logFile));
          logWriter.write("Log for source name:" + source.getName() + " from resource: "
            + source.getResource().getUniqueID().toString() + "\n");
          if (!reader.getEmptyLines().isEmpty()) {
            List<Integer> emptyLines = new ArrayList<Integer>(reader.getEmptyLines());
            Collections.sort(emptyLines);
            for (Integer i : emptyLines) {
              logWriter.write("Line: " + i + " [EMPTY LINE]\n");
            }
          } else {
            logWriter.write("No rows were skipped in this source");
          }
          logWriter.flush();
        } catch (IOException e) {
          log.warn("Cant write source log file " + logFile.getAbsolutePath(), e);
        } finally {
          if (logWriter != null) {
            logWriter.flush();
            IOUtils.closeQuietly(logWriter);
          }
        }
      } catch (IOException e) {
        problem = e.getMessage();
        log.warn("Cant read source file " + fs.getFile().getAbsolutePath(), e);
        fs.setReadable(false);
        fs.setRows(-1);
      }


    }
    return problem;
  }

  private List<String> columns(FileSource source) {
    if (source != null) {
      try {
        CSVReader reader = source.getReader();
        if (source.getIgnoreHeaderLines() > 0) {
          return Arrays.asList(reader.header);
        } else {
          List<String> columns = new ArrayList<String>();
          // careful - the reader.header can be null. In this case set number of columns to 0
          int numColumns = (reader.header == null) ? 0 : reader.header.length;
          for (int x = 1; x <= numColumns; x++) {
            columns.add("Column #" + x);
          }
          return columns;
        }
      } catch (IOException e) {
        log.warn("Cant read source " + source.getName(), e);
      }
    }

    return new ArrayList<String>();
  }

  @Override
  /*
   * (non-Javadoc)
   * @see org.gbif.ipt.service.manage.SourceManager#columns(org.gbif.ipt.model.Source)
   */
  public List<String> columns(Source source) {
    return columns((FileSource) source);
  }

  /*
   * (non-Javadoc)
   * @see org.gbif.ipt.service.manage.MappingConfigManager#delete(org.gbif.ipt.model.Source.FileSource)
   */
  @Override
  public boolean delete(Resource resource, Source source) {
    boolean deleted = false;
    if (source != null) {
      resource.deleteSource(source);
      if (source instanceof FileSource) {
        // also delete source data file
        FileSource fs = (FileSource) source;
        fs.getFile().delete();
      }
      deleted = true;
    }
    return deleted;
  }

  @Override
  public int importArchive(Resource resource, File file, boolean overwriteEml) throws ImportException {
    // anaylze using the dwca reader
    try {
      ArchiveFactory.openArchive(file);
      return 0;
    } catch (UnsupportedArchiveException e) {
      throw new ImportException(e);
    } catch (IOException e) {
      throw new ImportException(e);
    }
  }

  /*
   * (non-Javadoc)
   * @see org.gbif.ipt.service.manage.SourceManager#inspectColumn(org.gbif.ipt.model.Source, int, int)
   */
  @Override
  public Set<String> inspectColumn(Source source, int column, int maxValues, int maxRows) throws SourceException {
    Set<String> values = new HashSet<String>();
    ClosableIterator<Object> iter = null;
    try {
      iter = iterSourceColumn(source, column, maxRows);
      // get distinct values
      while (iter.hasNext() && (maxValues < 1 || values.size() < maxValues)) {
        Object obj = iter.next();
        if (obj != null) {
          String val = obj.toString();
          values.add(val);
        }
      }
    } catch (Exception e) {
      log.error(e);
      throw new SourceException("Error reading source " + source.getName() + ": " + e.getMessage());
    } finally {
      if (iter != null) {
        iter.close();
      }
    }
    return values;
  }

  /**
   * @param limit limit for the recordset passed into the sql. If negative or zero no limit will be used
   */
  private ClosableIterator<Object> iterSourceColumn(Source source, int column, int limit) throws Exception {
    FileSource src = (FileSource) source;
    return new FileColumnIterator(src, column);
  }

  private List<String[]> peek(FileSource source, int rows) {
    List<String[]> preview = new ArrayList<String[]>();
    if (source != null) {
      try {
        CSVReader reader = source.getReader();
        while (rows > 0 && reader.hasNext()) {
          rows--;
          preview.add(reader.next());
        }
      } catch (IOException e) {
        log.warn("Cant read source " + source.getName(), e);
      }
    }

    return preview;
  }

  /*
   * (non-Javadoc)
   * @see org.gbif.ipt.service.manage.SourceManager#peek(org.gbif.ipt.model.Source)
   */
  @Override
  public List<String[]> peek(Source source, int rows) {
    return peek((FileSource) source, rows);
  }

  @Override
  public ClosableIterator<String[]> rowIterator(Source source) throws SourceException {
    if (source == null) {
      return null;
    }
    try {
      return ((FileSource) source).getReader().iterator();
    } catch (Exception e) {
      log.error(e);
      throw new SourceException("Cant build iterator for source " + source.getName() + " :" + e.getMessage());
    }
  }
}