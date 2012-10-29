package net.sibcolombia.sibsp.service.admin;

import java.net.URI;
import java.util.List;
import java.util.Map;

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
   * Retrieve vocabulary by its unique global URI identifier from installed vocabularies.
   * 
   * @param uriString unique URI string identifying the vocabulary as given in the vocabulary definition
   * @return the installed vocabulary or null if not found
   */
  Vocabulary get(String uriString);

  /**
   * Returns the parsed vocabulary located at the given URI. If downloaded already it will return the cached copy or
   * otherwise download it from the URI.
   * 
   * @param uriObject the resolvable URI that locates the xml vocabulary definition
   * @return the installed vocabulary or null if not found
   */
  Vocabulary get(URI uriObject);

  /**
   * Returns a regular map than can be used to populate html select drop downs with
   * keys=vocabulary concept identifiers and values=preferred term for the given language.
   * Defaults to english if no term for the requested language exists.
   * 
   * @param uri the identifier for the vocabulary
   * @param lang a 2 character iso language code, e.g. DE
   * @param sortAlphabetically if true sort map values alphabetically, otherwise use native ordering
   * @return return vocabulary map for given language sorted alphabetically, or an empty map if no vocabulary concepts
   *         could be populated
   */
  Map<String, String> getI18nVocab(String uriString, String lang, boolean sortAlphabetically);

  /**
   * Lists all locally known vocabularies.
   * 
   * @return all locally known vocabularies
   */
  List<Vocabulary> list();

  /**
   * Load all known vocabularies from the data dir. It also ensures that all default vocabularies
   * get installed and loaded also.
   * 
   * @return number of vocabularies that have been loaded successfully
   */
  int load();

  /**
   * Downloads the latest version for the locally known vocabuarlies by looking up the latest registry entry
   * for their URI. Updates all related concepts & terms.
   * 
   * @return UpdateResult with information relating to sucesses and failures of the update
   */
  UpdateResult updateAll();

}
