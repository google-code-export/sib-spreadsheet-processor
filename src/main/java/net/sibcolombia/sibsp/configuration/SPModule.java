package net.sibcolombia.sibsp.configuration;

import org.gbif.ipt.utils.InputStreamUtils;
import org.gbif.utils.HttpUtil;
import org.gbif.utils.PreemptiveAuthenticationInterceptor;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletContext;
import javax.xml.parsers.SAXParserFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import net.sibcolombia.sibsp.model.factory.ExtensionFactory;
import net.sibcolombia.sibsp.model.factory.VocabularyFactory;
import net.sibcolombia.sibsp.struts2.SimpleTextProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;


/**
 * Guice connections for production and test app
 * 
 * @author Valentina Grajales
 */
public class SPModule extends AbstractModule {

  private static final Logger LOG = Logger.getLogger(SPModule.class);

  @Override
  protected void configure() {
    // singletons
    bind(ApplicationConfig.class).in(Scopes.SINGLETON);
    bind(InputStreamUtils.class).in(Scopes.SINGLETON);
    bind(SimpleTextProvider.class).in(Scopes.SINGLETON);
    bind(ExtensionFactory.class).in(Scopes.SINGLETON);
    bind(VocabularyFactory.class).in(Scopes.SINGLETON);
  }

  @Provides
  @Singleton
  @Inject
  public DataDir provideDataDir(ServletContext ctx) {
    File dataDirSettingFile = new File(ctx.getRealPath("/") + "/WEB-INF/datadir.location");
    LOG.info("provide servlet context data dir location file at " + dataDirSettingFile.getAbsolutePath());
    DataDir dataDirectory = DataDir.buildFromLocationFile(dataDirSettingFile);
    try {
      if (dataDirectory != null && dataDirectory.isConfigured()) {
        dataDirectory.clearTmp();
      }
    } catch (IOException e) {
      LOG.warn("Couldnt clear temporary data dir folder", e);
    }
    return dataDirectory;
  }

  @Provides
  @Singleton
  @Inject
  public DefaultHttpClient provideHttpClient() {
    // This client instance, available from httputils 0.3-SNAPSHOT onward, supports both http and https protocols
    DefaultHttpClient client = HttpUtil.newMultithreadedClientHttps();

    // registry currently requires Preemptive authentication
    // Add as the very first interceptor in the protocol chain
    client.addRequestInterceptor(new PreemptiveAuthenticationInterceptor(), 0);

    return client;
  }

  @Provides
  @Inject
  public HttpUtil provideHttpUtil() {
    // Retrieve the same client instance as configured in this module
    DefaultHttpClient client = provideHttpClient();
    // Return a singleton instance of HttpUtil
    return new HttpUtil(client);
  }

  @Provides
  @Inject
  @Singleton
  public SAXParserFactory provideNsAwareSaxParserFactory() {
    SAXParserFactory saxf = null;
    try {
      saxf = SAXParserFactory.newInstance();
      saxf.setValidating(false);
      saxf.setNamespaceAware(true);
    } catch (Exception e) {
      LOG.error("Cant create namespace aware SAX Parser Factory: " + e.getMessage(), e);
    }
    return saxf;
  }
}
