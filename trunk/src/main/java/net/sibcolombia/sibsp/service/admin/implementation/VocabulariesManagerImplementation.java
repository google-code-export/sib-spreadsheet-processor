package net.sibcolombia.sibsp.service.admin.implementation;

import org.gbif.utils.HttpUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import com.google.inject.Inject;
import com.thoughtworks.xstream.XStream;
import net.sibcolombia.sibsp.action.BaseAction;
import net.sibcolombia.sibsp.configuration.ApplicationConfig;
import net.sibcolombia.sibsp.configuration.ConfigWarnings;
import net.sibcolombia.sibsp.configuration.Constants;
import net.sibcolombia.sibsp.configuration.DataDir;
import net.sibcolombia.sibsp.model.Vocabulary;
import net.sibcolombia.sibsp.model.factory.VocabularyFactory;
import net.sibcolombia.sibsp.service.BaseManager;
import net.sibcolombia.sibsp.service.InvalidConfigException;
import net.sibcolombia.sibsp.service.RegistryException;
import net.sibcolombia.sibsp.service.admin.ExtensionManager;
import net.sibcolombia.sibsp.service.admin.VocabulariesManager;
import net.sibcolombia.sibsp.service.registry.RegistryManager;
import net.sibcolombia.sibsp.struts2.SimpleTextProvider;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xml.sax.SAXException;


public class VocabulariesManagerImplementation extends BaseManager implements VocabulariesManager {

  public class UpdateResult {

    // key=uri
    public Set<String> updated = new HashSet<String>();
    // key=uri
    public Set<String> unchanged = new HashSet<String>();
    // key=uri, value=error text
    public Map<String, String> errors = new HashMap<String, String>();
  }

  private final Map<URI, Vocabulary> vocabularies = new HashMap<URI, Vocabulary>();
  // Vocabulary identifier, to Vocabulary resolvable URI
  private final Map<String, URI> id2uri = new HashMap<String, URI>();
  // these vocabularies are always updates on startup of the SiBSP
  private final String[] defaultVocabularies = {Constants.VOCAB_URI_LANGUAGE, Constants.VOCAB_URI_COUNTRY,
    Constants.VOCAB_URI_DATASET_TYPE, Constants.VOCAB_URI_RANKS, Constants.VOCAB_URI_ROLES,
    Constants.VOCAB_URI_PRESERVATION_METHOD, Constants.VOCAB_URI_DATASET_SUBTYPES};
  private static final String VOCAB_FILE_SUFFIX = ".vocab";
  protected static final String CONFIG_FOLDER = ".vocabularies";
  public static final String PERSISTENCE_FILE = "vocabularies.xml";
  private final HttpUtil downloadUtil;
  private final VocabularyFactory vocabularyFactory;
  private final ConfigWarnings warnings;
  private final ExtensionManager extensionManager;
  private final RegistryManager registryManager;
  private final XStream xstream = new XStream();

  // create instance of BaseAction - allows class to retrieve i18n terms via getText()
  private final BaseAction baseAction;

  @Inject
  public VocabulariesManagerImplementation(ApplicationConfig config, DataDir dataDir, SimpleTextProvider textProvider,
    ExtensionManager extensionManager, ConfigWarnings warnings, VocabularyFactory vocabularyFactory,
    RegistryManager registryManager, DefaultHttpClient client) {
    super(config, dataDir);
    this.vocabularyFactory = vocabularyFactory;
    this.extensionManager = extensionManager;
    this.registryManager = registryManager;
    this.warnings = warnings;
    this.downloadUtil = new HttpUtil(client);
    baseAction = new BaseAction(textProvider, config);
  }

  private boolean addToCache(Vocabulary v, URI uriObject) {
    if (uriObject == null) {
      log.error("Cannot add vocabulary " + v.getUriString() + " to cache without a valid URL");
      return false;
    }
    id2uri.put(v.getUriString().toLowerCase(), uriObject);
    // keep vocab in local lookup
    if (vocabularies.containsKey(uriObject)) {
      log.warn("Vocabulary URI " + v.getUriString() + " exists already - overwriting with new vocabulary from "
        + uriObject);
    }
    vocabularies.put(uriObject, v);
    return true;
  }

  @Override
  public Vocabulary get(URI uriObject) {
    if (!vocabularies.containsKey(uriObject)) {
      try {
        install(uriObject);
      } catch (InvalidConfigException e) {
        log.error(e);
      } catch (IOException e) {
        log.error(e);
      }
    }
    return vocabularies.get(uriObject);
  }

  @Override
  public Map<String, String> getI18nVocab(String uri, String lang, boolean sortAlphabetically) {
    // TODO Auto-generated method stub
    return null;
  }

  private File getVocabFile(URI uriObject) {
    String filename = uriObject.toString().replaceAll("[/.:]+", "_") + VOCAB_FILE_SUFFIX;
    return dataDir.configFile(CONFIG_FOLDER + "/" + filename);
  }

  /**
   * Downloads vocabulary into local file for subsequent SiBSP startups and adds the vocabulary to the internal cache.
   * Downloads use a conditional GET, i.e. only download the vocabulary files if the content has been changed since the
   * last download.
   * lastModified dates are taken from the filesystem.
   */
  private Vocabulary install(URI uriObject) throws IOException, InvalidConfigException {
    Vocabulary v = null;
    if (uriObject != null) {
      // the file to download to. It may exist already in which case we do a conditional download
      File vocabFile = getVocabFile(uriObject);
      FileUtils.forceMkdir(vocabFile.getParentFile());
      if (downloadUtil.downloadIfChanged(uriObject.toURL(), vocabFile)) {
        // parse vocabulary file
        try {
          v = loadFromFile(vocabFile);
          addToCache(v, uriObject);
          save();
        } catch (InvalidConfigException e) {
          warnings.addStartupError("Cannot install vocabulary " + uriObject, e);
        }

      } else {
        log.info("Vocabulary " + uriObject + " hasn't been modified since last download");
      }
    }
    return v;
  }

  @Override
  public List<Vocabulary> list() {
    return new ArrayList<Vocabulary>(vocabularies.values());
  }

  @Override
  public int load() {
    File vocabularies = dataDir.configFile(CONFIG_FOLDER + "/" + PERSISTENCE_FILE);
    // for IPTs version 2.0.3 or earlier: must transition vocabularies.xml to store vocabulary address as URI vs URL
    if (vocabularies.exists()) {
      transitionVocabulariesBetweenVersions();
    }

    // now iterate over all vocab files and load them
    File dir = dataDir.configFile(CONFIG_FOLDER);
    int counter = 0;
    if (dir.isDirectory()) {
      List<File> files = new ArrayList<File>();
      FilenameFilter ff = new SuffixFileFilter(VOCAB_FILE_SUFFIX, IOCase.INSENSITIVE);
      files.addAll(Arrays.asList(dir.listFiles(ff)));
      for (File ef : files) {
        try {
          Vocabulary v = loadFromFile(ef);
          if (v != null && addToCache(v, id2uri.get(v.getUriString().toLowerCase()))) {
            counter++;
          }
        } catch (InvalidConfigException e) {
          warnings.addStartupError("Cant load local vocabulary definition " + ef.getAbsolutePath(), e);
        }
      }
    }

    // we could be starting up for the very first time. Try to load mandatory/default vocabs with URIs from registry
    Map<String, URI> registeredVocabs = null;
    for (String vuriString : defaultVocabularies) {
      if (!id2uri.containsKey(vuriString.toLowerCase())) {
        // lazy load list of all registered vocabularies
        registeredVocabs = registeredVocabs();
        // provided that at least some vocabularies were loaded, proceed loading default vocabularies
        if (!registeredVocabs.isEmpty()) {
          try {
            URI vuriObject = registeredVocabs.get(vuriString);
            if (vuriObject == null) {
              log.warn("Default vocabulary " + vuriString + " unknown to GBIF registry");
            } else {
              log.info("Installing vocabulary: " + vuriObject);
              install(vuriObject);
              // increment counter, since these haven't been loaded yet
              counter++;
            }
          } catch (Exception e) {
            warnings.addStartupError(baseAction.getTextWithDynamicArgs("admin.extensions.vocabulary.couldnt.load",
              new String[] {vuriString, config.getRegistryUrl()}));
          }
        }
      }
    }

    return counter;
  }

  private Vocabulary loadFromFile(File vocabFile) throws InvalidConfigException {
    Vocabulary v = null;
    // finally read in the new file and create the vocabulary object
    InputStream fileIn = null;
    String fileName = (vocabFile.exists()) ? vocabFile.getName() : "";
    try {
      fileIn = new FileInputStream(vocabFile);
      v = vocabularyFactory.build(fileIn);
      // read filesystem date
      Date modified = new Date(vocabFile.lastModified());
      v.setLastUpdate(modified);
      log.info("Successfully loaded Vocabulary: " + v.getTitle());
    } catch (FileNotFoundException e) {
      warnings.addStartupError("Cant find local vocabulary file: " + fileName, e);
    } catch (IOException e) {
      warnings.addStartupError("Cant access local vocabulary file: " + fileName, e);
    } catch (SAXException e) {
      warnings.addStartupError("Cant parse local vocabulary file: " + fileName, e);
    } catch (ParserConfigurationException e) {
      warnings.addStartupError("Cant create sax parser", e);
    } finally {
      if (fileIn != null) {
        try {
          fileIn.close();
        } catch (IOException e) {
        }
      }
    }
    return v;
  }

  /**
   * Retrieves a Map of registered vocabularies. The key is equal to the vocabulary URI String. The value is equal to
   * the
   * vocabulary URI object.
   * 
   * @return Map of registered vocabularies
   */
  private Map<String, URI> registeredVocabs() {
    Map<String, URI> registeredVocabs = new HashMap<String, URI>();
    try {
      for (Vocabulary vocabulary : registryManager.getVocabularies()) {
        if (vocabulary != null) {
          registeredVocabs.put(vocabulary.getUriString(), vocabulary.getUriResolvable());
        }
      }
    } catch (RegistryException e) {
      // log as specific error message as possible about why the Registry error occurred
      String msg = RegistryException.logRegistryException(e.getType(), baseAction);
      // add startup error message about Registry error
      warnings.addStartupError(msg);
      log.error(msg);

      // add startup error message that explains the consequence of the Registry error
      msg = baseAction.getText("admin.extensions.vocabularies.couldnt.load", new String[] {config.getRegistryUrl()});
      warnings.addStartupError(msg);
      log.error(msg);
    }
    return registeredVocabs;
  }

  public synchronized void save() {
    // persist uri2url
    log.debug("Saving id2uri vocabulary map with " + id2uri.size() + " entries ...");
    Writer userWriter;
    try {
      userWriter =
        org.gbif.ipt.utils.FileUtils.startNewUtf8File(dataDir.configFile(CONFIG_FOLDER + "/" + PERSISTENCE_FILE));
      xstream.toXML(id2uri, userWriter);
    } catch (IOException e) {
      log.error("Cant write id2uri mapping", e);
    }
  }

  /**
   * First, the vocabularies.xml file, that stores information about the list of installed vocabularies, previous to
   * 2.0.4, the IPT stored the resolvable vocabulary address in a URL object. Now, it stores it as a URI. To avoid
   * startup errors updating existing IPT installations, this method should be called to help transition
   * the vocabularies.xml to use this new format.
   * </p>
   * Second, any vocabularies that are deprecated and will never be referenced again from any extension or used again
   * in the IPT, must be removed. Otherwise, the IPT will try to download the vocabulary which no loner exists.
   */
  private void transitionVocabulariesBetweenVersions() {
    // first load persistent id2uri map
    InputStream in = null;
    try {
      in = new FileInputStream(dataDir.configFile(CONFIG_FOLDER + "/" + PERSISTENCE_FILE));
      // as of 2.0.4, need to transition id2url from Map<String, URL -> Map<String, URI>
      Map<String, Object> tempId2uri = (Map<String, Object>) xstream.fromXML(in);
      for (String id : tempId2uri.keySet()) {
        try {
          String st = tempId2uri.get(id).toString();
          URI uri = new URI(st);
          id2uri.put(id, uri);
        } catch (URISyntaxException e1) {
          // log error, clear vocabs dir, try download them all again
          log.error("URL could not be converted to URI - check vocabularies.xml");
        }
      }
    } catch (IOException e) {
      log.warn("Cannot load the id2uri mapping from datadir (This is normal when first setting up a new datadir)");
    } finally {
      if (in != null) {
        try {
          in.close();
        } catch (IOException e) {
          log.error("InputStream on vocabularies.xml could not be closed");
        }
      }
    }
    // remove the old vocabularies.xml file - it will soon get rewritten..
    FileUtils.deleteQuietly(dataDir.configFile(CONFIG_FOLDER + "/" + PERSISTENCE_FILE));

    // before rewriting, take the chance to remove any deprecated vocabularies that no longer should be persisted/loaded
    if (id2uri.containsKey(Constants.DEPRECATED_VOCAB_URI_RESOURCE_TYPE)) {
      id2uri.remove(Constants.DEPRECATED_VOCAB_URI_RESOURCE_TYPE);
    }
    // ensure the actual deprecated vocab file is also removed
    File dep1 = dataDir.configFile(CONFIG_FOLDER + "/" + Constants.DEPRECATED_VOCAB_URL_RESOLVABLE_RESOURCE_TYPE);
    if (dep1.exists()) {
      FileUtils.deleteQuietly(dep1);
    }
    // rewrite vocabularies.xml
    save();
  }

  @Override
  public UpdateResult updateAll() {
    UpdateResult result = new UpdateResult();
    // list all known vocab URIs in debug log
    log.info("Updating all installed vocabularies");
    log.debug("Known vocabulary locations for URIs: " + StringUtils.join(id2uri.keySet(), ", "));
    for (Vocabulary vocabulary : vocabularies.values()) {
      if (vocabulary.getUriString() == null) {
        log.warn("Vocabulary without identifier, skipped!");
        continue;
      }
      log.debug("Updating vocabulary " + vocabulary.getUriString());
      URI uriObject = id2uri.get(vocabulary.getUriString().toLowerCase());
      if (uriObject == null) {
        String msg =
          "Dont know the vocabulary URL to retrieve update from for vocabulary Identifier " + vocabulary.getUriString();
        result.errors.put(vocabulary.getUriString(), msg);
        log.error(msg);
        continue;
      }
      File vocabularyFile = getVocabFile(uriObject);
      Date modified = new Date(vocabularyFile.lastModified());
      try {
        install(uriObject);
        Date modified2 = new Date(vocabularyFile.lastModified());
        if (modified.equals(modified2)) {
          // no update
          result.unchanged.add(vocabulary.getUriString());
        } else {
          result.updated.add(vocabulary.getUriString());
        }
      } catch (Exception e) {
        result.errors.put(vocabulary.getUriString(), e.getMessage());
        log.error(e);
      }
    }
    return result;
  }

}
