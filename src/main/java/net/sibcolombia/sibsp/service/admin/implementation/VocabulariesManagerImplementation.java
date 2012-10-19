package net.sibcolombia.sibsp.service.admin.implementation;

import org.gbif.utils.HttpUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import com.google.inject.Inject;
import com.thoughtworks.xstream.XStream;
import net.sibcolombia.sibsp.configuration.ApplicationConfig;
import net.sibcolombia.sibsp.configuration.ConfigWarnings;
import net.sibcolombia.sibsp.configuration.DataDir;
import net.sibcolombia.sibsp.model.Vocabulary;
import net.sibcolombia.sibsp.model.factory.VocabularyFactory;
import net.sibcolombia.sibsp.service.BaseManager;
import net.sibcolombia.sibsp.service.InvalidConfigException;
import net.sibcolombia.sibsp.service.admin.VocabulariesManager;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
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
  private static final String VOCAB_FILE_SUFFIX = ".vocab";
  protected static final String CONFIG_FOLDER = ".vocabularies";
  public static final String PERSISTENCE_FILE = "vocabularies.xml";
  private HttpUtil downloadUtil;
  private VocabularyFactory vocabularyFactory;
  private ConfigWarnings warnings;
  private final XStream xstream = new XStream();

  @Inject
  public VocabulariesManagerImplementation(ApplicationConfig config, DataDir dataDir) {
    super(config, dataDir);
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
      // URI -> URL, used in download
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
