package org.gbif.ipt.task;

import org.gbif.ipt.utils.CoordinateUtils;
import org.gbif.metadata.eml.Agent;
import org.gbif.metadata.eml.BBox;
import org.gbif.metadata.eml.Citation;
import org.gbif.metadata.eml.Eml;
import org.gbif.metadata.eml.GeospatialCoverage;
import org.gbif.metadata.eml.JGTICuratorialUnit;
import org.gbif.metadata.eml.JGTICuratorialUnitType;
import org.gbif.metadata.eml.KeywordSet;
import org.gbif.metadata.eml.PhysicalData;
import org.gbif.metadata.eml.TaxonKeyword;
import org.gbif.metadata.eml.TaxonomicCoverage;
import org.gbif.metadata.eml.TemporalCoverage;
import org.gbif.metadata.eml.TemporalCoverageType;

import java.awt.Color;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.lowagie.text.Anchor;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.List;
import com.lowagie.text.ListItem;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import net.sibcolombia.sibsp.configuration.ApplicationConfig;
import net.sibcolombia.sibsp.configuration.Constants;
import net.sibcolombia.sibsp.configuration.DataDir;
import net.sibcolombia.sibsp.model.Resource;
import net.sibcolombia.sibsp.model.Vocabulary;
import net.sibcolombia.sibsp.model.VocabularyConcept;
import net.sibcolombia.sibsp.service.admin.VocabulariesManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.log4j.Logger;

import static com.google.common.base.Objects.equal;


/**
 * Populates a RTF document with a resources metadata, mainly derived from its EML.
 */
@Singleton
public class Eml2Rtf {

  private final Font font = FontFactory.getFont(FontFactory.TIMES_ROMAN, 12, Font.NORMAL, Color.BLACK);
  private final Font fontToComplete = FontFactory.getFont(FontFactory.TIMES_ROMAN, 12, Font.NORMAL, Color.RED);
  private final Font fontTitle = FontFactory.getFont(FontFactory.TIMES_BOLD, 12, Font.BOLD, Color.BLACK);
  private final Font fontHeader = FontFactory.getFont(FontFactory.TIMES_BOLD, 14, Font.BOLD, Color.BLACK);
  private final Font fontLink = FontFactory.getFont(FontFactory.TIMES_ROMAN, 12, Font.UNDERLINE, Color.BLUE);
  private static final String DEFAULT_LANGUAGE = Locale.ENGLISH.getLanguage();
  private ResourceBundle resourceBundle;

  // logging
  protected static Logger log = Logger.getLogger(Eml2Rtf.class);

  @Inject
  private VocabulariesManager vocabManager;
  @Inject
  private ApplicationConfig appConfig;

  /**
   * Add abstract section. This corresponds to resource's description.
   * 
   * @param doc Document
   * @param eml EML
   * @throws DocumentException if problem occurs during add
   */
  private void addAbstract(Document doc, Eml eml) throws DocumentException {
    if (exists(eml.getDescription())) {
      Paragraph p = new Paragraph();
      p.setAlignment(Element.ALIGN_JUSTIFIED);
      p.setFont(font);
      p.add(new Phrase(getText("rtf.abstract"), fontTitle));
      p.add(Chunk.NEWLINE);
      p.add(Chunk.NEWLINE);
      p.add(eml.getDescription().replace("\r\n", "\n"));
      p.add(Chunk.NEWLINE);
      doc.add(p);
      p.clear();
    }
  }

  /**
   * Add authors section.
   * 
   * @param doc Document
   * @param eml EML
   * @throws DocumentException if problem occurs during add
   */
  private void addAuthors(Document doc, Eml eml) throws DocumentException {
    // Creating set of authors with different names. (first names + last names).
    HashSet<Agent> tempAgents = new LinkedHashSet<Agent>();
    if (exists(eml.getResourceCreator()) && exists(eml.getResourceCreator().getLastName())) {
      tempAgents.add(eml.getResourceCreator());
    }
    if (exists(eml.getMetadataProvider()) && exists(eml.getMetadataProvider().getLastName())) {
      tempAgents.add(eml.getMetadataProvider());
    }
    tempAgents.addAll(eml.getAssociatedParties());

    // comparing and removing those repeated agents with same name and same address.
    Collection<Integer> toRemove = new ArrayList<Integer>();
    int counter = 0;
    for (Iterator<Agent> i = tempAgents.iterator(); i.hasNext(); counter++) {
      if (toRemove.contains(counter)) {
        i.next();
        i.remove();
      } else {
        Agent agentA = i.next();
        // when second iterator should be start
        boolean flag = false;
        int countTemp = 0;
        for (Iterator<Agent> j = tempAgents.iterator(); j.hasNext(); countTemp++) {
          Agent agentB = j.next();
          if (flag) {
            if (equal(agentA.getLastName(), agentB.getLastName())
              && equal(agentA.getFirstName(), agentB.getFirstName()) && equal(agentA.getAddress(), agentB.getAddress())) {
              toRemove.add(countTemp);
            }
          } else if (agentA.equals(agentB)) {
            flag = true;
          }
        }
      }
    }

    Agent[] agentsArray = new Agent[tempAgents.size()];
    tempAgents.toArray(agentsArray);
    // Adding authors
    Paragraph p = new Paragraph();
    p.setFont(font);
    p.setAlignment(Element.ALIGN_CENTER);
    java.util.List<Agent> affiliations = new ArrayList<Agent>();
    int superScriptCounter = 1;
    for (int c = 0; c < agentsArray.length; c++) {
      if (exists(agentsArray[c].getLastName())) {
        if (c != 0) {
          p.add(", ");
        }
        // First Name and Last Name
        if (exists(agentsArray[c].getFirstName())) {
          p.add(agentsArray[c].getFirstName() + " ");
        }
        p.add(agentsArray[c].getLastName());
        // Looking for addresses and organisations of other authors
        // (superscripts should not be repeated).
        boolean isRepeated = false;
        // look into the affiliations array to find any previous repeated agent info.
        for (int index = 0; index < affiliations.size(); index++) {
          if (equal(agentsArray[c].getAddress(), affiliations.get(index).getAddress())
            && equal(agentsArray[c].getOrganisation(), affiliations.get(index).getOrganisation())) {
            p.add(createSuperScript(String.valueOf(index + 1)));
            isRepeated = true;
            break;
          }
        }
        // if the agent is not repeated.
        if (!isRepeated) {
          p.add(createSuperScript(String.valueOf(superScriptCounter)));
          affiliations.add(agentsArray[c]);
          superScriptCounter++;
        }
      }
    }
    doc.add(p);
    p.clear();
    doc.add(Chunk.NEWLINE);
    tempAgents.clear();
    // <AFFILIATIONS>
    p = new Paragraph();
    p.setFont(font);
    p.setAlignment(Element.ALIGN_JUSTIFIED);
    for (int c = 0; c < affiliations.size(); c++) {
      if (c != 0) {
        p.add("; ");
      }
      p.add((c + 1) + " ");
      if (exists(affiliations.get(c).getOrganisation())) {
        p.add(affiliations.get(c).getOrganisation() + ", ");
      }
      if (exists(affiliations.get(c).getAddress().getAddress())) {
        p.add(affiliations.get(c).getAddress().getAddress() + ", ");
      }
      if (exists(affiliations.get(c).getAddress().getPostalCode())) {
        p.add(affiliations.get(c).getAddress().getPostalCode() + ", ");
      }
      if (exists(affiliations.get(c).getAddress().getCity())) {
        p.add(affiliations.get(c).getAddress().getCity());
      }
      if (exists(affiliations.get(c).getAddress().getCountry())) {
        // log.info("Country: " + Constants.VOCAB_URI_COUNTRY);
        // log.info("All vocabulary manager country: " + vocabManager.get(Constants.VOCAB_URI_COUNTRY).toString());
        String country =
          vocabManager.get(Constants.VOCAB_URI_COUNTRY).findConcept(affiliations.get(c).getAddress().getCountry())
            .getPreferredTerm(DEFAULT_LANGUAGE).getTitle();
        p.add(", " + WordUtils.capitalizeFully(country));
      }
    }
    doc.add(p);
    p.clear();
    doc.add(Chunk.NEWLINE);
    // <Corresponding Authors>
    p = new Paragraph();
    p.setAlignment(Element.ALIGN_JUSTIFIED);
    p.add(new Phrase(getText("rtf.authors") + ": ", fontTitle));
    p.setFont(font);
    boolean isFirst = true;
    if (exists(eml.getResourceCreator())) {
      if (exists(eml.getResourceCreator().getFirstName())) {
        p.add(eml.getResourceCreator().getFirstName() + " ");
      }
      p.add(eml.getResourceCreator().getLastName());
      if (exists(eml.getResourceCreator().getEmail())) {
        p.add(" (" + eml.getResourceCreator().getEmail() + ")");
      }
      isFirst = false;
    }
    if (exists(eml.getMetadataProvider())) {
      boolean sameAsCreator = false;
      if (!isFirst) {
        sameAsCreator =
          equal(eml.getMetadataProvider().getAddress(), eml.getResourceCreator().getAddress())
            && equal(eml.getMetadataProvider().getEmail(), eml.getResourceCreator().getEmail());
      }
      if (!sameAsCreator) {
        p.add(", ");
        if (exists(eml.getMetadataProvider().getFirstName())) {
          p.add(eml.getMetadataProvider().getFirstName() + " ");
        }
        p.add(eml.getMetadataProvider().getLastName());
        if (exists(eml.getMetadataProvider().getEmail())) {
          p.add(" (" + eml.getMetadataProvider().getEmail() + ")");
        }
      }
    }
    p.add(Chunk.NEWLINE);
    doc.add(p);
    p.clear();
  }

  /**
   * Add Citation statement:
   * "Citation: Combination of authors, year of data paper publication (in parentheses), Title, Journal Name, Volume,
   * Issue number (in parentheses), and DOI of the data paper.
   * </p>
   * This section is intended to be manually entered by the author, following publication of the data paper.
   * 
   * @param doc Document
   * @throws DocumentException if problem occurs during add
   */
  private void addCitations(Document doc) throws DocumentException {
    Paragraph p = new Paragraph();
    p.setAlignment(Element.ALIGN_JUSTIFIED);
    p.setFont(fontToComplete);
    p.add(new Phrase(getText("rtf.citations") + ": ", fontTitle));
    p.add(getText("rtf.citations.description"));
    p.add(Chunk.NEWLINE);
    doc.add(p);
    p.clear();
  }

  /**
   * Add Dataset Description section. The "Dataset description" describes the DwC-A being published by the IPT.
   * If there is no data uploaded/published through the IPT, and no "External links", then that means
   * only the metadata is being published and no "Dataset" and "Dataset description" sections will appear at all.
   * 
   * @param doc Document
   * @param resource Resource
   * @throws DocumentException if problem occurs during add
   */
  private void addDatasetDescriptions(Document doc, Resource resource) throws DocumentException {
    Paragraph p = new Paragraph();
    p.setAlignment(Element.ALIGN_JUSTIFIED);
    p.setFont(font);
    Eml eml = resource.getEml();
    // If there is data uploaded/published through the IPT
    if (resource.hasMappedData()) {
      p.add(new Phrase(getText("rtf.datasets"), fontTitle));
      p.add(Chunk.NEWLINE);
      p.add(Chunk.NEWLINE);
      p.add(new Phrase(getText("rtf.datasets.description"), fontTitle));
      p.add(Chunk.NEWLINE);
      p.add(new Phrase(getText("rtf.datasets.object") + ": ", fontTitle));
      p.add(getText("rtf.datasets.dwca") + " " + eml.getTitle());
      p.add(Chunk.NEWLINE);
      p.add(new Phrase(getText("rtf.datasets.character") + ": ", fontTitle));
      p.add("UTF-8");
      p.add(Chunk.NEWLINE);
      p.add(new Phrase(getText("rtf.datasets.format") + ": ", fontTitle));
      p.add(getText("rtf.datasets.dwca.format"));
      p.add(Chunk.NEWLINE);
      p.add(new Phrase(getText("rtf.datasets.format.version") + ": ", fontTitle));
      p.add("1.0");
      p.add(Chunk.NEWLINE);
      p.add(new Phrase(getText("rtf.datasets.distribution") + ": ", fontTitle));
      String dwcaLink = appConfig.getRootURL() + "/archive.do?r=" + resource.getUniqueID().toString();
      Anchor distributionLink = new Anchor(dwcaLink, fontLink);
      distributionLink.setReference(dwcaLink);
      p.add(distributionLink);
      p.add(Chunk.NEWLINE);
      if (exists(eml.getPubDate())) {
        p.add(new Phrase(getText("rtf.publication") + ": ", fontTitle));
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
        p.add(f.format(eml.getPubDate()));
        p.add(Chunk.NEWLINE);
      }

      VocabularyConcept vocabConcept = vocabManager.get(Constants.VOCAB_URI_LANGUAGE).findConcept(eml.getLanguage());
      p.add(new Phrase(getText("rtf.language") + ": ", fontTitle));
      if (exists(vocabConcept)) {
        p.add(vocabConcept.getPreferredTerm(DEFAULT_LANGUAGE).getTitle());
      } else {
        p.add(getText("rtf.unknown"));
      }
      p.add(Chunk.NEWLINE);
      if (exists(eml.getIntellectualRights())) {
        p.add(new Phrase(getText("rtf.license") + ": ", fontTitle));
        p.add(eml.getIntellectualRights().replace("\r\n", "\n"));
        p.add(Chunk.NEWLINE);
      }

      doc.add(p);
    } else {
      // If no data is uploaded/published through the IPT but there are one or more "External links"
      if (!eml.getPhysicalData().isEmpty()) {
        p.add(new Phrase(getText("rtf.datasets"), fontTitle));
        p.add(Chunk.NEWLINE);
        p.add(Chunk.NEWLINE);
        p.add(new Phrase(getText("rtf.datasets.description"), fontTitle));
        p.add(Chunk.NEWLINE);
        p.add(getText("rtf.datasets.noPublished"));
        p.add(Chunk.NEWLINE);
        VocabularyConcept vocabConcept = vocabManager.get(Constants.VOCAB_URI_LANGUAGE).findConcept(eml.getLanguage());
        p.add(new Phrase(getText("rtf.language") + ": ", fontTitle));
        if (exists(vocabConcept)) {
          p.add(vocabConcept.getPreferredTerm(DEFAULT_LANGUAGE).getTitle());
        } else {
          p.add(getText("rtf.unknown"));
        }
        p.add(Chunk.NEWLINE);
        if (exists(eml.getIntellectualRights())) {
          p.add(new Phrase(getText("rtf.license") + ": ", fontTitle));
          p.add(eml.getIntellectualRights());
          p.add(Chunk.NEWLINE);
        }
        doc.add(p);
      }
    }
    // Add external datasets
    addExternalLinks(doc, eml);
    p.clear();
  }

  /**
   * Add dates statement:
   * "Received {date}; Revised {date}; Accepted {date}; Published {date}"
   * </p>
   * These are to be manually inserted by the Publisher of the Data Paper to indicate the dates of the original
   * manuscript submission, revised manuscript submission, acceptance of manuscript and publishing of the manuscript as
   * a Data Paper in the journal.
   * 
   * @param doc Document
   */
  private void addDates(Document doc) throws DocumentException {
    Paragraph p = new Paragraph();
    Phrase phrase = new Phrase("{" + getText("rtf.date") + "}", fontToComplete);
    p.setFont(font);
    p.add(getText("rtf.received") + " ");
    p.add(phrase);
    p.add("; " + getText("rtf.revised") + " ");
    p.add(phrase);
    p.add("; " + getText("rtf.accepted") + " ");
    p.add(phrase);
    p.add("; " + getText("rtf.published") + " ");
    p.add(phrase);
    p.add(Chunk.NEWLINE);
    doc.add(p);
    p.clear();
  }

  /**
   * Add external links section.
   * 
   * @param doc Document
   * @param eml EML
   * @throws DocumentException if problem occurs during add
   */
  private void addExternalLinks(Document doc, Eml eml) throws DocumentException {
    if (!eml.getPhysicalData().isEmpty()) {
      Paragraph p = new Paragraph();
      p.setAlignment(Element.ALIGN_JUSTIFIED);
      p.setFont(font);

      p.add(new Phrase(getText("rtf.datasets.external"), fontTitle));
      p.add(Chunk.NEWLINE);
      p.add(Chunk.NEWLINE);
      for (PhysicalData data : eml.getPhysicalData()) {
        p.add(new Phrase(getText("rtf.datasets.description"), fontTitle));
        p.add(Chunk.NEWLINE);
        if (exists(data.getName())) {
          p.add(new Phrase(getText("rtf.datasets.object") + ": ", fontTitle));
          p.add(data.getName());
          p.add(Chunk.NEWLINE);
        }
        if (exists(data.getCharset())) {
          p.add(new Phrase(getText("rtf.datasets.character") + ": ", fontTitle));
          p.add(data.getCharset());
          p.add(Chunk.NEWLINE);
        }
        if (exists(data.getFormat())) {
          p.add(new Phrase(getText("rtf.datasets.format") + ": ", fontTitle));
          p.add(data.getFormat());
          p.add(Chunk.NEWLINE);
        }
        if (exists(data.getFormatVersion())) {
          p.add(new Phrase(getText("rtf.datasets.format.version") + ": ", fontTitle));
          p.add(data.getFormatVersion());
          p.add(Chunk.NEWLINE);
        }
        if (exists(data.getDistributionUrl())) {
          p.add(new Phrase(getText("rtf.datasets.distribution") + ": ", fontTitle));
          Anchor distributionLink = new Anchor(data.getDistributionUrl(), fontLink);
          distributionLink.setReference(data.getDistributionUrl());
          p.add(distributionLink);
          p.add(Chunk.NEWLINE);
        }
        p.add(Chunk.NEWLINE);
      }
      doc.add(p);
      p.clear();
    }
  }

  /**
   * Add general description section.
   * 
   * @throws DocumentException if problem occurs during add
   */
  private void addGeneralDescription(Document doc, Eml eml) throws DocumentException {
    if (exists(eml.getPurpose()) || exists(eml.getAdditionalInfo())) {
      Paragraph p = new Paragraph();
      p.setAlignment(Element.ALIGN_JUSTIFIED);
      p.setFont(font);

      p.add(new Phrase(getText("rtf.generalDescription"), fontTitle));
      p.add(Chunk.NEWLINE);
      p.add(Chunk.NEWLINE);
      if (exists(eml.getPurpose())) {
        p.add(new Phrase(getText("rtf.purpose") + ": ", fontTitle));
        p.add(eml.getPurpose().replace("\r\n", "\n"));
        p.add(Chunk.NEWLINE);
      }
      if (exists(eml.getAdditionalInfo())) {
        p.add(new Phrase(getText("rtf.additionalinformation") + ": ", fontTitle));
        p.add(eml.getAdditionalInfo().replace("\r\n", "\n"));
        p.add(Chunk.NEWLINE);
      }
      doc.add(p);
      p.clear();
    }
  }

  /**
   * Add keywords section.
   * 
   * @param doc Document
   * @param keys keywords Strings
   * @throws DocumentException if problem occurs during add
   */
  private void addKeywords(Document doc, String keys) throws DocumentException {
    if (keys != null && !(keys.length() == 0)) {
      Paragraph p = new Paragraph();
      p.setAlignment(Element.ALIGN_JUSTIFIED);
      p.setFont(font);
      p.add(new Phrase(getText("rtf.keywords") + ": ", fontTitle));
      p.add(keys);
      p.add(Chunk.NEWLINE);
      doc.add(p);
      p.clear();
    }
  }

  /**
   * Add metadata description section.
   * 
   * @param doc Document
   * @param eml EML
   * @throws DocumentException if problem occurs during add
   */
  private void addMetadataDescriptions(Document doc, Eml eml) throws DocumentException {
    Paragraph p = new Paragraph();
    p.setAlignment(Element.ALIGN_JUSTIFIED);
    p.setFont(font);
    if (exists(eml.getMetadataLanguage())) {
      Vocabulary vocab = vocabManager.get(Constants.VOCAB_URI_LANGUAGE);
      VocabularyConcept vocabConcept = vocab.findConcept(eml.getMetadataLanguage());
      if (exists(vocabConcept)) {
        p.add(new Phrase(getText("rtf.metadata.language") + ": ", fontTitle));
        p.add(vocabConcept.getPreferredTerm(DEFAULT_LANGUAGE).getTitle());
        p.add(Chunk.NEWLINE);
      }
    }
    if (exists(eml.getDateStamp())) {
      p.add(new Phrase(getText("rtf.metadata.creation") + ": ", fontTitle));
      SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
      p.add(f.format(eml.getDateStamp()));
      p.add(Chunk.NEWLINE);
    }
    if (exists(eml.getHierarchyLevel())) {
      p.add(new Phrase(getText("rtf.metadata.level") + ": ", fontTitle));
      p.add(WordUtils.capitalizeFully(eml.getHierarchyLevel()));
      p.add(Chunk.NEWLINE);
    }
    if (exists(eml.getMetadataLocale())) {
      VocabularyConcept vocabConcept =
        vocabManager.get(Constants.VOCAB_URI_LANGUAGE).findConcept(eml.getMetadataLocale().getLanguage());
      if (exists(vocabConcept)) {
        p.add(new Phrase(getText("rtf.metadata.locale") + ": ", fontTitle));
        p.add(vocabConcept.getPreferredTerm(DEFAULT_LANGUAGE).getTitle());
        p.add(Chunk.NEWLINE);
      }
    }
    doc.add(p);
    p.clear();
  }

  /**
   * Add methods section.
   * 
   * @param doc Document
   * @param eml EML
   * @throws DocumentException if problem occurs during add
   */
  private void addMethods(Document doc, Eml eml) throws DocumentException {
    if (exists(eml.getMethodSteps()) && !eml.getMethodSteps().isEmpty() || exists(eml.getStudyExtent())
      || exists(eml.getStudyExtent()) || exists(eml.getStudyExtent())) {
      Paragraph p = new Paragraph();
      p.setAlignment(Element.ALIGN_JUSTIFIED);
      p.setFont(font);
      p.add(new Phrase(getText("rtf.methods"), fontTitle));
      p.add(Chunk.NEWLINE);
      p.add(Chunk.NEWLINE);
      if (eml.getMethodSteps().size() == 1) {
        p.add(new Phrase(getText("rtf.methods.description") + ": ", fontTitle));
        p.add(eml.getMethodSteps().get(0).replace("\r\n", "\n"));
        p.add(Chunk.NEWLINE);
      } else if (eml.getMethodSteps().size() > 1) {
        p.add(new Phrase(getText("rtf.methods.description") + ": ", fontTitle));
        p.add(Chunk.NEWLINE);
        List list = new List(List.UNORDERED, 0);
        list.setIndentationLeft(20);
        for (String method : eml.getMethodSteps()) {
          list.add(new ListItem(method.replace("\r\n", "\n"), font));
        }
        p.add(list);
      }
      if (exists(eml.getStudyExtent())) {
        p.add(new Phrase(getText("rtf.methods.studyExtent") + ": ", fontTitle));
        p.add(eml.getStudyExtent().replace("\r\n", "\n"));
        p.add(Chunk.NEWLINE);
      }
      if (exists(eml.getStudyExtent())) {
        p.add(new Phrase(getText("rtf.methods.sampling") + ": ", fontTitle));
        p.add(eml.getSampleDescription().replace("\r\n", "\n"));
        p.add(Chunk.NEWLINE);
      }
      if (exists(eml.getQualityControl())) {
        p.add(new Phrase(getText("rtf.methods.quality") + ": ", fontTitle));
        p.add(eml.getQualityControl().replace("\r\n", "\n"));
        p.add(Chunk.NEWLINE);
      }
      doc.add(p);
      p.clear();
    }
  }

  /**
   * Add natural collections section.
   * 
   * @param doc Document
   * @param eml EML
   * @throws DocumentException if problem occurs during add
   */
  private void addNaturalCollections(Document doc, Eml eml) throws DocumentException {
    if (exists(eml.getParentCollectionId()) || exists(eml.getCollectionName()) || exists(eml.getCollectionId())
      || !eml.getTemporalCoverages().isEmpty() || exists(eml.getSpecimenPreservationMethod())
      || !eml.getJgtiCuratorialUnits().isEmpty()) {
      Paragraph p = new Paragraph();
      p.setAlignment(Element.ALIGN_JUSTIFIED);
      p.setFont(font);
      if (exists(eml.getParentCollectionId())) {
        p.add(new Phrase(getText("rtf.collections.description"), fontTitle));
        p.add(Chunk.NEWLINE);
        p.add(Chunk.NEWLINE);
        p.add(new Phrase(getText("rtf.collections.parent") + ": ", fontTitle));
        p.add(eml.getParentCollectionId());
        p.add(Chunk.NEWLINE);
      }
      if (exists(eml.getCollectionName())) {
        p.add(new Phrase(getText("rtf.collections.name") + ": ", fontTitle));
        p.add(eml.getCollectionName());
        p.add(Chunk.NEWLINE);
      }
      if (exists(eml.getCollectionId())) {
        p.add(new Phrase(getText("rtf.collections.identifier") + ": ", fontTitle));
        p.add(eml.getCollectionId());
        p.add(Chunk.NEWLINE);
      }
      for (TemporalCoverage coverage : eml.getTemporalCoverages()) {
        if (coverage.getType() == TemporalCoverageType.FORMATION_PERIOD) {
          p.add(new Phrase(getText("rtf.collections.formatPeriod") + ": ", fontTitle));
          p.add(coverage.getFormationPeriod());
          p.add(Chunk.NEWLINE);
        }
      }
      for (TemporalCoverage coverage : eml.getTemporalCoverages()) {
        if (coverage.getType() == TemporalCoverageType.LIVING_TIME_PERIOD) {
          p.add(new Phrase(getText("rtf.collections.livingPeriod") + ": ", fontTitle));
          p.add(coverage.getLivingTimePeriod());
          p.add(Chunk.NEWLINE);
        }
      }
      if (exists(eml.getSpecimenPreservationMethod())) {
        p.add(new Phrase(getText("rtf.collections.specimen") + ": ", fontTitle));
        VocabularyConcept vocabConcept =
          vocabManager.get(Constants.VOCAB_URI_PRESERVATION_METHOD).findConcept(eml.getSpecimenPreservationMethod());
        p.add(vocabConcept.getPreferredTerm(DEFAULT_LANGUAGE).getTitle());
        p.add(Chunk.NEWLINE);
      }
      for (JGTICuratorialUnit unit : eml.getJgtiCuratorialUnits()) {
        p.add(new Phrase(getText("rtf.collections.curatorial") + ": ", fontTitle));
        if (unit.getType() == JGTICuratorialUnitType.COUNT_RANGE) {
          p.add("Between " + unit.getRangeStart() + " and " + unit.getRangeEnd());
        }
        if (unit.getType() == JGTICuratorialUnitType.COUNT_WITH_UNCERTAINTY) {
          p.add(unit.getRangeMean() + " " + getText("rtf.collections.curatorial.text") + " "
            + unit.getUncertaintyMeasure());
        }
        p.add(" (" + unit.getUnitType() + ")");
        p.add(Chunk.NEWLINE);
      }
      if (!p.isEmpty()) {
        doc.add(p);
      }
      p.clear();
    }
  }

  /**
   * Add paragraph to Document.
   * 
   * @param doc Document
   * @param text text of paragraph
   * @param font Font to be used
   * @param spacing number of spaces before the paragraph
   * @param alignType alignment of the paragraph
   * @throws DocumentException if problem occurs during add
   */
  private void addPara(Document doc, String text, Font font, int spacing, int alignType) throws DocumentException {
    Paragraph p = new Paragraph(text, font);
    if (spacing != 0) {
      p.setSpacingBefore(spacing);
    }
    if (alignType != 0) {
      p.setAlignment(alignType);
    }
    doc.add(p);
    p.clear();
  }

  /**
   * Add project data section.
   * 
   * @param doc Document
   * @param eml EML
   * @throws DocumentException if problem occurs during add
   */
  private void addProjectData(Document doc, Eml eml) throws DocumentException {
    if (exists(eml.getProject().getTitle()) || exists(eml.getProject().getPersonnel().getFirstName())
      || exists(eml.getProject().getFunding())
      || exists(eml.getProject().getStudyAreaDescription().getDescriptorValue())
      || exists(eml.getProject().getDesignDescription())) {
      Paragraph p = new Paragraph();
      p.setAlignment(Element.ALIGN_JUSTIFIED);
      p.setFont(font);
      p.add(new Phrase(getText("rtf.project.details"), fontTitle));
      p.add(Chunk.NEWLINE);
      p.add(Chunk.NEWLINE);
      if (exists(eml.getProject().getTitle())) {
        p.add(new Phrase(getText("rtf.project.title") + ": ", fontTitle));
        p.add(eml.getProject().getTitle());
        p.add(Chunk.NEWLINE);
      }
      p.add(new Phrase(getText("rtf.project.personnel") + ": ", fontTitle));
      if (exists(eml.getProject().getPersonnel().getFirstName())) {
        p.add(eml.getProject().getPersonnel().getFirstName() + " " + eml.getProject().getPersonnel().getLastName());
        p.add(Chunk.NEWLINE);
      }
      if (exists(eml.getProject().getFunding())) {
        p.add(new Phrase(getText("rtf.project.funding") + ": ", fontTitle));
        p.add(eml.getProject().getFunding().replace("\r\n", "\n"));
        p.add(Chunk.NEWLINE);
      }
      if (exists(eml.getProject().getStudyAreaDescription().getDescriptorValue())) {
        p.add(new Phrase(getText("rtf.project.area") + ": ", fontTitle));
        p.add(eml.getProject().getStudyAreaDescription().getDescriptorValue().replace("\r\n", "\n"));
        p.add(Chunk.NEWLINE);
      }
      if (exists(eml.getProject().getDesignDescription())) {
        p.add(new Phrase(getText("rtf.project.design") + ": ", fontTitle));
        p.add(eml.getProject().getDesignDescription().replace("\r\n", "\n"));
        p.add(Chunk.NEWLINE);
      }
      doc.add(p);
      p.clear();
    }
  }

  /**
   * Add references section.
   * 
   * @param doc Document
   * @param eml EML
   * @throws DocumentException if problem occurs during add
   */
  private void addReferences(Document doc, Eml eml) throws DocumentException {
    if (exists(eml.getBibliographicCitationSet())
      && !eml.getBibliographicCitationSet().getBibliographicCitations().isEmpty()) {
      Paragraph p = new Paragraph();
      p.setAlignment(Element.ALIGN_JUSTIFIED);
      p.setFont(font);
      p.add(new Phrase(getText("rtf.references"), fontTitle));
      p.add(Chunk.NEWLINE);
      for (Citation citation : eml.getBibliographicCitationSet().getBibliographicCitations()) {
        p.add(citation.getCitation().replace("\r\n", "\n"));
        p.add(Chunk.NEWLINE);
      }
      doc.add(p);
      p.clear();
    }
  }

  /**
   * Add Spatial coverage section.
   * 
   * @param doc Document
   * @param eml EML
   * @throws DocumentException if problem occurs during add
   */
  private void addSpatialCoverage(Document doc, Eml eml) throws DocumentException {
    if (exists(eml.getGeospatialCoverages()) && !eml.getGeospatialCoverages().isEmpty()) {
      Paragraph p = new Paragraph();
      p.setAlignment(Element.ALIGN_JUSTIFIED);
      p.setFont(font);
      boolean firstCoverage = true;
      for (GeospatialCoverage coverage : eml.getGeospatialCoverages()) {
        if (firstCoverage) {
          firstCoverage = false;
        } else {
          p.add(Chunk.NEWLINE);
        }
        if (exists(coverage.getDescription())) {
          p.add(new Phrase(getText("rtf.spatialCoverage"), fontTitle));
          p.add(Chunk.NEWLINE);
          p.add(Chunk.NEWLINE);
          p.add(new Phrase(getText("rtf.spatialCoverage.general") + ": ", fontTitle));
          p.add(coverage.getDescription().replace("\r\n", "\n"));
          p.add(Chunk.NEWLINE);
        }
        p.add(new Phrase(getText("rtf.spatialCoverage.coordinates") + ": ", fontTitle));
        BBox coordinates = coverage.getBoundingCoordinates();
        p.add(CoordinateUtils.decToDms(coordinates.getMin().getLatitude(), CoordinateUtils.LATITUDE));
        p.add(" " + getText("rtf.spatialCoverage.and") + " ");
        p.add(CoordinateUtils.decToDms(coordinates.getMax().getLatitude(), CoordinateUtils.LATITUDE));
        p.add(" " + getText("rtf.spatialCoverage.latitude") + "; ");
        p.add(CoordinateUtils.decToDms(coordinates.getMin().getLongitude(), CoordinateUtils.LONGITUDE));
        p.add(" " + getText("rtf.spatialCoverage.and") + " ");
        p.add(CoordinateUtils.decToDms(coordinates.getMax().getLongitude(), CoordinateUtils.LONGITUDE));
        p.add(" " + getText("rtf.spatialCoverage.longitude") + " ");
        p.add(Chunk.NEWLINE);
      }
      doc.add(p);
      p.clear();
    }
  }

  /**
   * Add taxonomic coverages, writing in this order: description, ranks, then common names.
   * 
   * @param doc Document
   * @param eml EML object
   * @throws DocumentException if an error occurred adding to the Document
   */
  private void addTaxonomicCoverages(Document doc, Eml eml) throws DocumentException {
    // proceed, provided there is at least 1 Taxonomic Coverage to iterate over
    if (exists(eml.getTaxonomicCoverages()) && !eml.getTaxonomicCoverages().isEmpty()) {

      // begin new paragraph
      Paragraph p = new Paragraph();
      p.setAlignment(Element.ALIGN_JUSTIFIED);
      p.setFont(font);
      boolean firstTaxon = true;
      for (TaxonomicCoverage taxcoverage : eml.getTaxonomicCoverages()) {
        if (!firstTaxon) {
          p.add(Chunk.NEWLINE);
        }
        firstTaxon = false;
        p.add(new Phrase(getText("rtf.taxcoverage"), fontTitle));
        p.add(Chunk.NEWLINE);
        p.add(Chunk.NEWLINE);
        if (exists(taxcoverage.getDescription())) {
          p.add(new Phrase(getText("rtf.taxcoverage.description") + ": ", fontTitle));
          p.add(taxcoverage.getDescription().replace("\r\n", "\n"));
          p.add(Chunk.NEWLINE);
        }
        Map<String, String> ranks =
          vocabManager.getI18nVocab(Constants.VOCAB_URI_RANKS, Locale.getDefault().getLanguage(), false);
        boolean firstRank = true;
        for (String rank : ranks.keySet()) {
          boolean wroteRank = false;
          for (TaxonKeyword keyword : taxcoverage.getTaxonKeywords()) {
            if (exists(keyword.getRank()) && keyword.getRank().equals(rank)) {
              if (!wroteRank) {
                if (firstRank) {
                  p.add(new Phrase(getText("rtf.taxcoverage.rank"), fontTitle));
                }
                p.add(Chunk.NEWLINE);
                p.add(StringUtils.capitalize(rank) + ": ");
                p.add(keyword.getScientificName());
                wroteRank = true;
                firstRank = false;
              } else {
                p.add(", " + keyword.getScientificName());
              }
            }
          }
        }
        p.add(Chunk.NEWLINE);
        boolean isFirst = true;
        for (TaxonKeyword keyword : taxcoverage.getTaxonKeywords()) {
          if (exists(keyword.getCommonName())) {
            if (isFirst) {
              p.add(new Phrase(getText("rtf.taxcoverage.common") + ": ", fontTitle));
            } else {
              p.add(", ");
            }
            isFirst = false;
            p.add(keyword.getCommonName());
          }
        }
      }
      p.add(Chunk.NEWLINE);
      doc.add(p);
      p.clear();
    }
  }

  /**
   * Add temporal coverages section.
   * 
   * @param doc Document
   * @param eml EML
   * @throws DocumentException if problem occurs during add
   */
  private void addTemporalCoverages(Document doc, Eml eml) throws DocumentException {
    if (exists(eml.getTemporalCoverages()) && !eml.getTemporalCoverages().isEmpty()) {
      Paragraph p = new Paragraph();
      p.setAlignment(Element.ALIGN_JUSTIFIED);
      p.setFont(font);
      DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.LONG);
      SimpleDateFormat timeFormat = new SimpleDateFormat("SSS");
      SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy");
      boolean firstCoverage = true;
      for (TemporalCoverage coverage : eml.getTemporalCoverages()) {
        if (coverage.getType().equals(TemporalCoverageType.SINGLE_DATE)) {
          if (firstCoverage) {
            firstCoverage = false;
          } else {
            p.add(Chunk.NEWLINE);
          }
          p.add(new Phrase(getText("rtf.tempcoverage") + ": ", fontTitle));
          if (timeFormat.format(coverage.getStartDate()).equals("001")) {
            p.add(yearFormat.format(coverage.getStartDate()));
          } else {
            p.add(dateFormat.format(coverage.getStartDate()));
          }
          p.add(Chunk.NEWLINE);
        } else if (coverage.getType() == TemporalCoverageType.DATE_RANGE) {
          if (firstCoverage) {
            firstCoverage = false;
          } else {
            p.add(Chunk.NEWLINE);
          }
          p.add(new Phrase(getText("rtf.tempcoverage") + ": ", fontTitle));
          if (timeFormat.format(coverage.getStartDate()).equals("001")) {
            p.add(yearFormat.format(coverage.getStartDate()));
          } else {
            p.add(dateFormat.format(coverage.getStartDate()));
          }
          p.add(" - ");
          if (timeFormat.format(coverage.getEndDate()).equals("001")) {
            p.add(yearFormat.format(coverage.getEndDate()));
          } else {
            p.add(dateFormat.format(coverage.getEndDate()));
          }
          p.add(Chunk.NEWLINE);
        }
      }
      doc.add(p);
      p.clear();
    }
  }

  /**
   * Converts text to superscript text.
   * 
   * @param text text to be superscripted
   * @return superscripted text
   */
  private Chunk createSuperScript(String text) {
    return new Chunk(text).setTextRise(5f);
  }

  /**
   * Checks whether a given object is null. If the object is an instance of String, it checks if the String is an empty
   * String. Only if the object is not null, and not an empty String, will the method return true.
   * 
   * @param obj Object
   * @return whether true if the object is not null, or not an empty string
   */
  private boolean exists(Object obj) {
    if (obj == null) {
      return false;
    }
    if (obj instanceof String) {
      if (StringUtils.isEmpty((String) obj)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Get text for resource bundle property key.
   * 
   * @param key key
   * @return text corresponding to key in property file
   */
  public String getText(String key) {
    return resourceBundle.getString(key);
  }

  public void setAppConfig(ApplicationConfig appConfig) {
    this.appConfig = appConfig;
  }

  public void setVocabManager(VocabulariesManager vocabManager) {
    this.vocabManager = vocabManager;
  }

  /**
   * Construct RTF document, mainly out of information extracted from Resource's EML object. Currently, the decision
   * has been made to always do this in English.
   * 
   * @param doc Document
   * @param resource Resource
   * @throws DocumentException if problem occurs during add
   */
  public void writeEmlIntoRtf(Document doc, Resource resource) throws DocumentException {
    // initialising english resourceBundle.
    resourceBundle = ResourceBundle.getBundle("ApplicationResources", Locale.ENGLISH);
    // this.action = action;
    Eml eml = resource.getEml();
    // configure page
    doc.setMargins(72, 72, 72, 72);
    System.out.println(DataDir.CONFIG_DIR);
    // write metadata
    doc.addAuthor("");
    doc.addCreationDate();
    doc.addTitle(eml.getTitle());
    // add the keywords to the document
    String keys = "";
    for (KeywordSet kw : eml.getKeywords()) {
      if (keys.length() == 0) {
        keys = kw.getKeywordsString(", ");
      } else {
        keys += ", " + kw.getKeywordsString(", ");
      }
    }
    doc.addKeywords(keys);
    // write proper doc
    doc.open();
    // title
    addPara(doc, eml.getTitle(), fontHeader, 0, Element.ALIGN_CENTER);
    doc.add(Chunk.NEWLINE);
    // Authors, affiliations and corresponging authors
    addAuthors(doc, eml);
    // Other various sections..
    addDates(doc);
    addCitations(doc);
    addAbstract(doc, eml);
    addKeywords(doc, keys);
    addGeneralDescription(doc, eml);
    addProjectData(doc, eml);
    addTaxonomicCoverages(doc, eml);
    addSpatialCoverage(doc, eml);
    addTemporalCoverages(doc, eml);
    addNaturalCollections(doc, eml);
    addMethods(doc, eml);
    addDatasetDescriptions(doc, resource);
    addMetadataDescriptions(doc, eml);
    addReferences(doc, eml);
    doc.close();
  }
}