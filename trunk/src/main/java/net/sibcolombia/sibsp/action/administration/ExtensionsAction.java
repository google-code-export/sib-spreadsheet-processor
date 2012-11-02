package net.sibcolombia.sibsp.action.administration;

import java.net.URL;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;

import com.google.inject.Inject;
import net.sibcolombia.sibsp.action.POSTAction;
import net.sibcolombia.sibsp.configuration.ApplicationConfig;
import net.sibcolombia.sibsp.configuration.ConfigWarnings;
import net.sibcolombia.sibsp.model.Extension;
import net.sibcolombia.sibsp.model.Vocabulary;
import net.sibcolombia.sibsp.service.RegistryException;
import net.sibcolombia.sibsp.service.admin.ExtensionManager;
import net.sibcolombia.sibsp.service.admin.VocabulariesManager;
import net.sibcolombia.sibsp.service.admin.implementation.ExtensionManagerImplementation;
import net.sibcolombia.sibsp.service.admin.implementation.VocabulariesManagerImplementation.UpdateResult;
import net.sibcolombia.sibsp.struts2.SimpleTextProvider;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;


public class ExtensionsAction extends POSTAction {

  private static final long serialVersionUID = -6153307714807450134L;

  // logging
  private static final Logger log = Logger.getLogger(ExtensionsAction.class);

  private final ExtensionManager extensionManager;

  private Boolean updateVocabs = false;
  private final VocabulariesManager vocabularyManager;
  private List<Extension> extensions;
  private Extension extension;
  private final ExtensionManagerImplementation.RegisteredExtensions registered;
  private final ConfigWarnings warnings;
  private List<Extension> newExtensions;
  private int numVocabularies = 0;
  private Date vocabulariesLastUpdated;
  private String dateFormat;
  private String url;

  @Inject
  public ExtensionsAction(SimpleTextProvider textProvider, ApplicationConfig config, ExtensionManager extensionManager,
    VocabulariesManager vocabularyManager, ExtensionManagerImplementation.RegisteredExtensions registered,
    ConfigWarnings warnings) {
    super(textProvider, config);
    this.extensionManager = extensionManager;
    this.vocabularyManager = vocabularyManager;
    this.registered = registered;
    this.warnings = warnings;
  }

  public String getDateFormat() {
    return dateFormat;
  }

  public Extension getExtension() {
    return extension;
  }

  public List<Extension> getExtensions() {
    return extensions;
  }

  public List<Extension> getNewExtensions() {
    return newExtensions;
  }

  public int getNumVocabularies() {
    return numVocabularies;
  }

  public String list() {
    if (updateVocabs) {
      UpdateResult result = vocabularyManager.updateAll();
      addActionMessage(getText("admin.config.extensions.vocabularies.updated",
        new String[] {String.valueOf(result.updated.size())}));
      addActionMessage(getText("admin.config.extensions.vocabularies.unchanged",
        new String[] {String.valueOf(result.unchanged.size())}));
      if (!result.errors.isEmpty()) {
        addActionWarning(getText("admin.config.extensions.vocabularies.errors",
          new String[] {String.valueOf(result.errors.size())}));
        for (Entry<String, String> err : result.errors.entrySet()) {
          addActionError(getText("admin.config.extensions.error.updating", new String[] {err.getKey(), err.getValue()}));
        }
      }
    }


    // retrieve all extensions
    extensions = extensionManager.list();
    // load any new extensions
    loadRegisteredExtensions();

    newExtensions = new ArrayList<Extension>(registered.getExtensions());
    // remove already installed ones
    for (Extension e : extensions) {
      newExtensions.remove(e);
    }

    // find latest update data of all vocabularies
    List<Vocabulary> vocabularies = vocabularyManager.list();
    numVocabularies = vocabularies.size();
    for (Vocabulary vocabulary : vocabularies) {
      if (vocabulariesLastUpdated == null || vocabulariesLastUpdated.before(vocabulary.getLastUpdate())) {
        Locale locale = getLocale();
        vocabulariesLastUpdated = vocabulary.getLastUpdate();
        dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, locale).format(vocabulariesLastUpdated);
      }
    }
    return SUCCESS;
  }

  /**
   * Reload all the list of registered extensions. Used in case the session just started.
   */
  private void loadRegisteredExtensions() {
    try {
      registered.load();
    } catch (RegistryException e) {
      // log as specific error message as possible about why the Registry error occurred
      String msg = RegistryException.logRegistryException(e.getType(), this);
      // add startup error message about Registry error
      warnings.addStartupError(msg);
      log.error(msg);

      // add startup error message that explains the consequence of the Registry error
      msg = getText("admin.config.extensions.couldnt.load", new String[] {config.getRegistryUrl()});
      warnings.addStartupError(msg);
      log.error(msg);
    }
  }

  @Override
  public void prepare() {
    super.prepare();
    // in case session just started
    if (!registered.isLoaded()) {
      // load all registered extensions from registry
      loadRegisteredExtensions();
    }
    // ensure mandatory vocabs are always loaded
    vocabularyManager.load();

    if (id != null) {
      extension = extensionManager.get(id);
      if (extension == null) {
        // set notFound flag to true so POSTAction will return a NOT_FOUND 404 result name
        notFound = true;
      }
    }
    // the only warnings that can still hang around after startup, have to do with vocabs and extensions not loading
    // if both have loaded, ensure the warnings are cleared
    if (registered.isLoaded() && !vocabularyManager.list().isEmpty()) {
      warnings.clearStartupErrors();
      this.getActionWarnings().clear();
    }
  }

  @Override
  public String save() {
    try {
      extensionManager.install(new URL(url));
      addActionMessage(getText("admin.extension.install.success", new String[] {url}));
    } catch (Exception e) {
      log.debug(e);
      addActionWarning(getText("admin.extension.install.error", new String[] {url}), e);
    }
    return SUCCESS;
  }

  public void setDateFormat(String dateFormat) {
    this.dateFormat = dateFormat;
  }

  public void setUpdateVocabs(String x) {
    if (StringUtils.trimToNull(x) != null) {
      this.updateVocabs = true;
    }
  }

  public void setUrl(String url) {
    this.url = url;
  }

}
