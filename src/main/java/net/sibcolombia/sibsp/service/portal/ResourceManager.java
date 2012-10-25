package net.sibcolombia.sibsp.service.portal;

import java.io.File;
import java.io.IOException;
import java.util.List;

import net.sibcolombia.sibsp.service.portal.implementation.ResourceManagerImpl;


import com.google.inject.ImplementedBy;
import net.sibcolombia.sibsp.action.BaseAction;
import net.sibcolombia.sibsp.model.Resource;
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

  /**
   * Load all configured resources from the datadir into memory.
   * We do not keep the EML or mapping configuration in memory for all resources, but we
   * maintain a map of the basic metadata and authorisation information in this manager.
   * 
   * @return number of configured resource loaded into memory
   */
  int load();

}
