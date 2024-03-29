package net.sibcolombia.sibsp.service.admin;

import java.net.URL;
import java.util.List;

import com.google.inject.ImplementedBy;
import net.sibcolombia.sibsp.model.Extension;
import net.sibcolombia.sibsp.service.InvalidConfigException;
import net.sibcolombia.sibsp.service.admin.implementation.ExtensionManagerImplementation;

/**
 * This interface details ALL methods associated with the DwC extensions.
 */
@ImplementedBy(ExtensionManagerImplementation.class)
public interface ExtensionManager {

  /**
   * Get a locally installed extension by its rowType.
   * 
   * @return extension for that rowtype or null if not installed
   */
  Extension get(String rowType);

  /**
   * Downloads an extension to the local cache and installs it for mapping. If the file is already locally existing
   * overwrite the older copy.
   * 
   * @param url the url that returns the xml based extension definition
   */
  Extension install(URL url) throws InvalidConfigException;

  void installCoreTypes();

  /**
   * List all installed extensions.
   * 
   * @return list of installed IPT extensions
   */
  List<Extension> list();

  /**
   * List all available extensions available for the given core.
   * 
   * @param coreRowType extension
   */
  List<Extension> list(String coreRowType);

  /**
   * List only the available core extensions.
   */
  List<Extension> listCore();

  /**
   * Load all installed extensions from the data dir.
   * 
   * @return number of extensions that have been loaded successfully
   */
  int load();

  /**
   * List all available extensions matching a registered keyword.
   * 
   * @param keyword to filter by, e.g. dwc:Taxon for all taxonomic extensions
   */
  List<Extension> search(String keyword);

}