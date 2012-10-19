package net.sibcolombia.sibsp.service.admin;

import com.google.inject.ImplementedBy;
import net.sibcolombia.sibsp.service.admin.implementation.VocabulariesManagerImplementation;
import net.sibcolombia.sibsp.service.admin.implementation.VocabulariesManagerImplementation.UpdateResult;

/**
 * This interface details ALL methods associated with the vocabularies within the IPT.
 */
@ImplementedBy(VocabulariesManagerImplementation.class)
public interface VocabulariesManager {

  /**
   * Downloads the latest version for the locally known vocabuarlies by looking up the latest registry entry
   * for their URI. Updates all related concepts & terms.
   * 
   * @return UpdateResult with information relating to sucesses and failures of the update
   */
  UpdateResult updateAll();

}
