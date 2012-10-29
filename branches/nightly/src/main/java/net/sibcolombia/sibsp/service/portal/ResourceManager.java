package net.sibcolombia.sibsp.service.portal;

import org.gbif.ipt.utils.ActionLogger;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import com.google.inject.ImplementedBy;
import net.sibcolombia.sibsp.action.BaseAction;
import net.sibcolombia.sibsp.model.Resource;
import net.sibcolombia.sibsp.service.DeletionNotAllowedException;
import net.sibcolombia.sibsp.service.InvalidConfigException;
import net.sibcolombia.sibsp.service.PublicationException;
import net.sibcolombia.sibsp.service.portal.implementation.ResourceManagerImplementation;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

/**
 * This interface details ALL methods associated with the main resource entity.
 * The manager keeps a map of the basic metadata and authorisation information in memory, but further details like the
 * full EML or mapping configuration is stored in files and loaded into manager sessions when needed.
 */
@ImplementedBy(ResourceManagerImplementation.class)
public interface ResourceManager {

  /**
   * Create a new Resource.
   * 
   * @param shortname Resource's shortName
   * @return Resource newly created, or null if it couldn't be created successfully
   */
  Resource create(UUID uniqueID);

  /**
   * Deletes a Resource.
   * 
   * @param resource Resource
   * @throws IOException if deletion could not be completed
   * @throws DeletionNotAllowedException if deletion was not allowed to be completed
   */
  void delete(Resource resource) throws IOException;

  /**
   * Process template file to generate an EML XML file
   * 
   * @param sourceFile
   * @param actionLogger
   * @throws IOException
   * @throws InvalidFormatException
   */
  public Resource processMetadataSpreadsheetPart(File sourceFile, String fileName, ActionLogger actionLogger)
    throws InvalidFormatException, IOException, NullPointerException;

  /**
   * Publishes a new version of a resource including generating a darwin core archive and issuing a new EML version.
   * 
   * @param resource Resource
   * @param action the action to use for logging messages to
   * @return true if a new asynchronous DwC-A generation job has been issued which requires some mapped data
   * @throws PublicationException if resource was already registered
   */
  boolean publish(Resource resource, BaseAction action) throws PublicationException;

  /**
   * Issues a new EML version for the given resource.
   * 
   * @param resource Resource
   * @param action the action to use for logging messages
   * @throws PublicationException if resource was already registered
   */
  void publishMetadata(Resource resource, BaseAction action) throws PublicationException;

  void save(Resource resource);

  /**
   * Save the eml file of a resource only. Complementary method to @See save(Resource).
   * 
   * @param resource Resource
   */
  void saveEml(Resource resource) throws InvalidConfigException;

}
