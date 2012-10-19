package net.sibcolombia.sibsp.interfaces;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.google.inject.ImplementedBy;
import net.sibcolombia.sibsp.action.BaseAction;
import net.sibcolombia.sibsp.model.Resource;
import net.sibcolombia.sibsp.service.ResourceManagerImpl;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

/**
 * This interface details ALL methods associated with the main resource entity.
 * The manager keeps a map of the basic metadata and authorisation information in memory, but further details like the
 * full EML or mapping configuration is stored in files and loaded into manager sessions when needed.
 */
@ImplementedBy(ResourceManagerImpl.class)
public interface ResourceManager {

  void create(File tmpFile, String fileName, String onlyFileName, String onlyFileExtension, BaseAction createEmlAction)
    throws InvalidFormatException, IOException;

  /**
   * list all resources SiB Spreadsheet processor.
   * 
   * @return list of resources, or an empty list if none were found
   */
  List<Resource> list();

}
