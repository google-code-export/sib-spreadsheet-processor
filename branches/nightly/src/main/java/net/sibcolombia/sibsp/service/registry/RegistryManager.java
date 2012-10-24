package net.sibcolombia.sibsp.service.registry;

import java.util.List;

import com.google.inject.ImplementedBy;
import net.sibcolombia.sibsp.model.Extension;
import net.sibcolombia.sibsp.model.Vocabulary;
import net.sibcolombia.sibsp.service.RegistryException;
import net.sibcolombia.sibsp.service.registry.implementation.RegistryManagerImplementation;

@ImplementedBy(RegistryManagerImplementation.class)
public interface RegistryManager {

  /**
   * Gets list of extensions from the Registry.
   * 
   * @return list of extensions, or an empty list if none were retrieved from valid response
   * @throws RegistryException if the list of extensions couldn't be populated
   */
  List<Extension> getExtensions() throws RegistryException;

  /**
   * Retrieves a list of Vocabulary from the Registry, but only the basic metadata, i.e. each without the list concepts.
   * 
   * @return list of thesauri, or an empty list if none were retrieved from valid response
   * @throws RegistryException if the list of thesauri couldn't be populated
   */
  List<Vocabulary> getVocabularies() throws RegistryException;

}
