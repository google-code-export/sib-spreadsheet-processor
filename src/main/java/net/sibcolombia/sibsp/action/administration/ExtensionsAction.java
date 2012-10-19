package net.sibcolombia.sibsp.action.administration;

import java.util.List;
import java.util.Map.Entry;

import com.google.inject.Inject;
import net.sibcolombia.sibsp.action.POSTAction;
import net.sibcolombia.sibsp.configuration.ApplicationConfig;
import net.sibcolombia.sibsp.model.Extension;
import net.sibcolombia.sibsp.service.admin.ExtensionManager;
import net.sibcolombia.sibsp.service.admin.VocabulariesManager;
import net.sibcolombia.sibsp.service.admin.implementation.VocabulariesManagerImplementation.UpdateResult;
import net.sibcolombia.sibsp.struts2.SimpleTextProvider;
import org.apache.log4j.Logger;


public class ExtensionsAction extends POSTAction {

  private static final long serialVersionUID = -6153307714807450134L;

  // logging
  private static final Logger log = Logger.getLogger(ExtensionsAction.class);

  private ExtensionManager extensionManager;

  private final Boolean updateVocabs = false;
  private VocabulariesManager vocabularyManager;
  private List<Extension> extensions;

  @Inject
  public ExtensionsAction(SimpleTextProvider textProvider, ApplicationConfig cfg) {
    super(textProvider, cfg);
  }

  public String list() {
    if (updateVocabs) {
      UpdateResult result = vocabularyManager.updateAll();
      addActionMessage(getText("admin.extensions.vocabularies.updated",
        new String[] {String.valueOf(result.updated.size())}));
      addActionMessage(getText("admin.extensions.vocabularies.unchanged",
        new String[] {String.valueOf(result.unchanged.size())}));
      if (!result.errors.isEmpty()) {
        addActionWarning(getText("admin.extensions.vocabularies.errors",
          new String[] {String.valueOf(result.errors.size())}));
        for (Entry<String, String> err : result.errors.entrySet()) {
          addActionError(getText("admin.extensions.error.updating", new String[] {err.getKey(), err.getValue()}));
        }
      }
    }

    // retrieve all extensions
    extensions = extensionManager.list();
    // load any new extensions
    // loadRegisteredExtensions();

    // newExtensions = new ArrayList<Extension>(registered.getExtensions());
    // remove already installed ones
    // for (Extension e : extensions) {
    // newExtensions.remove(e);
    // }

    // find latest update data of all vocabularies
    // List<Vocabulary> vocabs = vocabManager.list();
    // numVocabs = vocabs.size();
    // for (Vocabulary v : vocabs) {
    // if (vocabsLastUpdated == null || vocabsLastUpdated.before(v.getLastUpdate())) {
    // Locale locale = getLocale();
    // vocabsLastUpdated = v.getLastUpdate();
    // dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, locale).format(vocabsLastUpdated);
    // }
    // }
    return SUCCESS;
  }

}
