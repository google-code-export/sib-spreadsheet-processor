package org.gbif.ipt.task;

import org.gbif.ipt.utils.ActionLogger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.sibcolombia.sibsp.action.BaseAction;
import net.sibcolombia.sibsp.action.portal.CreateResourceAction;
import net.sibcolombia.sibsp.configuration.ApplicationConfig;
import net.sibcolombia.sibsp.configuration.DataDir;
import net.sibcolombia.sibsp.model.Resource;
import net.sibcolombia.sibsp.service.BaseManager;
import net.sibcolombia.sibsp.struts2.SimpleTextProvider;
import org.apache.log4j.Logger;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

@Singleton
public class Xls2Csv extends BaseManager {

  // logging
  private static final Logger log = Logger.getLogger(CreateResourceAction.class);

  private final Map<String, ArrayList<String>> columnMapping = initializeColumnMapping();

  // create instance of BaseAction - allows class to retrieve i18n terms via getText()
  private final BaseAction baseAction;

  @Inject
  public Xls2Csv(ApplicationConfig config, DataDir dataDir, SimpleTextProvider simpleTextProvider) {
    super(config, dataDir);
    baseAction = new BaseAction(simpleTextProvider, config);
  }

  public File convertExcelToCsv(Resource resource, File sourceFile, ActionLogger actionLogger) throws IOException,
    InvalidFormatException {
    Workbook template = WorkbookFactory.create(sourceFile);
    File file = dataDir.sourceExcelFile(resource, "data");
    FileWriter csvFile = new FileWriter(file);
    CSVWriter writer = new CSVWriter(csvFile, '\t', CSVWriter.NO_QUOTE_CHARACTER);

    // Map of required columns
    Map<Integer, String> columnLocationRequiredElements = new HashMap<Integer, String>();

    // Read first row columns names
    Sheet sheet = template.getSheet("Elementos mínimos");
    int totalColumns = 49;
    int columnsWithRequiredElements = 0;
    Iterator<Row> rowIterator = sheet.rowIterator();
    String[] entries = new String[totalColumns];
    Row row = sheet.getRow(0);
    for (int counter = 0; counter < totalColumns; counter++) {
      log.info("Fila: " + row.getRowNum() + " Columna: " + counter);
      if (!readCellValue(row.getCell(counter, Row.CREATE_NULL_AS_BLANK)).isEmpty()) {
        if (columnMapping.containsKey(readCellValue(row.getCell(counter, Row.CREATE_NULL_AS_BLANK)))) {
          entries[counter] = columnMapping.get(readCellValue(row.getCell(counter, Row.CREATE_NULL_AS_BLANK))).get(0);
          if (columnMapping.get(readCellValue(row.getCell(counter, Row.CREATE_NULL_AS_BLANK))).get(1)
            .equalsIgnoreCase("required")) {
            // This column of template couldn't be empty, so map it
            columnLocationRequiredElements.put(counter,
              columnMapping.get(readCellValue(row.getCell(counter, Row.CREATE_NULL_AS_BLANK))).get(0));
            columnsWithRequiredElements++;
          }
        }
      } else {
        entries[counter] = "";
      }
    }
    if (columnsWithRequiredElements != Integer.valueOf(columnMapping.get("Total required elements").get(0))) {
      throw new InvalidFormatException(baseAction.getText("sibsp.application.portal.error.with.template.elements.data"));
    }
    writer.writeNext(entries);

    while (rowIterator.hasNext()) {
      row = rowIterator.next();
      if (row.getRowNum() != 0) {
        for (int counter = 0; counter < totalColumns; counter++) {
          log.info("Fila: " + row.getRowNum() + " Columna: " + counter);
          if (!readCellValue(row.getCell(counter, Row.CREATE_NULL_AS_BLANK)).isEmpty()) {
            entries[counter] = readCellValue(row.getCell(counter, Row.CREATE_NULL_AS_BLANK));
          } else {
            if (columnLocationRequiredElements.containsKey(counter)) {
              throw new InvalidFormatException(baseAction.getTextWithDynamicArgs(
                "sibsp.application.portal.error.element.notempty", columnLocationRequiredElements.get(counter)));
            } else {
              entries[counter] = "";
            }
          }
        }
        writer.writeNext(entries);
      }
    }

    writer.close();
    return file;
  }

  /**
   * Initialize Map of colums to read from template
   * 
   * @return
   */
  private Map<String, ArrayList<String>> initializeColumnMapping() {
    Map<String, ArrayList<String>> columnMapping = new HashMap<String, ArrayList<String>>();
    int totalRequiredElements = 0;
    try {
      CSVReader reader =
        new CSVReader(new FileReader(dataDir.getDataDir() + "/" + DataDir.CONFIG_DIR + "/columnmapping.csv"), ',', '"');
      String[] nextLine;
      ArrayList<String> list;
      while ((nextLine = reader.readNext()) != null) {
        list = new ArrayList<String>();
        list.add(nextLine[2]);
        list.add(nextLine[3]);
        columnMapping.put(nextLine[0], list);
        if (nextLine[3].equalsIgnoreCase("required")) {
          totalRequiredElements++;
        }
      }
      list = new ArrayList<String>();
      list.add(String.valueOf(totalRequiredElements));
      columnMapping.put("Total required elements", list);
      reader.close();
    } catch (FileNotFoundException exception) {
      exception.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return columnMapping;
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
}
