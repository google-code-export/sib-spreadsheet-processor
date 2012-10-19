package net.sibcolombia.sibsp.service.admin;

import java.net.URI;

import com.google.inject.ImplementedBy;
import net.sibcolombia.sibsp.model.Vocabulary;
import net.sibcolombia.sibsp.service.admin.implementation.VocabulariesManagerImplementation;
import net.sibcolombia.sibsp.service.admin.implementation.VocabulariesManagerImplementation.UpdateResult;

/**
 * This interface details ALL methods associated with the vocabularies within the IPT.
 */
@ImplementedBy(VocabulariesManagerImplementation.class)
public interface VocabulariesManager {

  /**
   * Returns the parsed vocabulary located at the given URI. If downloaded already it will return the cached copy or
   * otherwise download it from the URI.
   * 
   * @param uriObject the resolvable URI that locates the xml vocabulary definition
   * @return the installed vocabulary or null if not found
   */
  Vocabulary get(URI uriObject);

  /**
   * Downloads the latest version for the locally known vocabuarlies by looking up the latest registry entry
   * for their URI. Updates all related concepts & terms.
   * 
   * @return UpdateResult with information relating to sucesses and failures of the update
   */
  UpdateResult updateAll();

}
