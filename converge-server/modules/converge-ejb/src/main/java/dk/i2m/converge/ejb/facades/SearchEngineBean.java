/*
 *  Copyright (C) 2010 Interactive Media Management
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.i2m.converge.ejb.facades;

import com.google.common.base.Splitter;
import dk.i2m.converge.core.search.SearchEngineIndexingException;
import dk.i2m.converge.domain.search.IndexField;
import dk.i2m.converge.domain.search.SearchResult;
import dk.i2m.converge.core.content.catalogue.MediaItem;
import dk.i2m.converge.core.content.NewsItem;
import dk.i2m.converge.core.metadata.Concept;
import dk.i2m.converge.core.metadata.GeoArea;
import dk.i2m.converge.core.metadata.Organisation;
import dk.i2m.converge.core.metadata.Person;
import dk.i2m.converge.core.metadata.PointOfInterest;
import dk.i2m.converge.core.metadata.Subject;
import dk.i2m.converge.domain.search.SearchFacet;
import dk.i2m.converge.domain.search.SearchResults;
import dk.i2m.converge.core.security.UserRole;
import dk.i2m.converge.core.ConfigurationKey;
import dk.i2m.converge.core.content.NewsItemActor;
import dk.i2m.converge.core.content.NewsItemPlacement;
import dk.i2m.converge.core.content.catalogue.MediaItemRendition;
import dk.i2m.converge.core.search.IndexQueueEntry;
import dk.i2m.converge.core.search.QueueEntryOperation;
import dk.i2m.converge.core.search.QueueEntryType;
import dk.i2m.converge.core.utils.BeanComparator;
import dk.i2m.converge.ejb.services.ConfigurationServiceLocal;
import dk.i2m.converge.ejb.services.DaoServiceLocal;
import dk.i2m.converge.ejb.services.DataNotFoundException;
import dk.i2m.converge.ejb.services.QueryBuilder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import net.sf.json.JSONObject;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.lang.StringUtils;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;
import org.apache.poi.hssf.usermodel.HSSFHeader;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.usermodel.HeaderFooter;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Footer;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.BinaryRequestWriter;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;

/**
 * Stateless session bean implementing a search engine service.
 *
 * @author Allan Lykke Christensen
 */
@Stateless
public class SearchEngineBean implements SearchEngineLocal {

    private static final Logger LOG = Logger.getLogger(SearchEngineBean.class.getName());

    private static final String OPEN_CALAIS_URL = "http://api.opencalais.com/tag/rs/enrich";

    @EJB private ConfigurationServiceLocal cfgService;

    @EJB private UserFacadeLocal userFacade;

    @EJB private DaoServiceLocal daoService;

    @EJB private NewsItemFacadeLocal newsItemFacade;

    @EJB private CatalogueFacadeLocal catalogueFacade;

    @EJB private MetaDataFacadeLocal metaDataFacade;

    private DateFormat solrDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    @Override
    public IndexQueueEntry addToIndexQueue(QueueEntryType type, Long id, QueueEntryOperation operation) {
        IndexQueueEntry entry = new IndexQueueEntry(type, id, operation);
        Map<String, Object> params = QueryBuilder.with("entryId", entry.getId()).and("type", entry.getType()).and("operation", entry.getOperation()).parameters();
        List<IndexQueueEntry> entries = daoService.findWithNamedQuery(IndexQueueEntry.FIND_BY_TYPE_ID_AND_OPERATION, params);

        if (entries.isEmpty()) {
            return daoService.create(entry);
        } else {
            return entries.iterator().next();
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public List<IndexQueueEntry> getIndexQueue() {
        List<IndexQueueEntry> queue = daoService.findAll(IndexQueueEntry.class);
        Collections.sort(queue, new BeanComparator("added", false));
        return queue;
    }

    @Override
    public void removeFromQueue(Long id) {
        daoService.delete(IndexQueueEntry.class, id);
    }

    @Override
    public void processIndexingQueue() {
        SolrServer solrServer = getSolrServer();
        List<IndexQueueEntry> items = getIndexQueue();
        for (IndexQueueEntry entry : items) {
            if (entry.getOperation().equals(QueueEntryOperation.REMOVE)) {
                try {
                    solrServer.deleteById(String.valueOf(entry.getEntryId()));
                    removeFromQueue(entry.getId());
                } catch (Exception ex) {
                    LOG.log(Level.WARNING, entry.getType().name() + " #{0} could not be removed from index", entry.getEntryId());
                    LOG.log(Level.WARNING, ex.getMessage(), ex);
                }
            } else {
                switch (entry.getType()) {
                    case NEWS_ITEM:
                        try {
                            NewsItem newsItem = newsItemFacade.findNewsItemById(entry.getEntryId());
                            index(newsItem, solrServer);
                            removeFromQueue(entry.getId());
                        } catch (DataNotFoundException ex) {
                            LOG.log(Level.WARNING, "NewsItem #{0} does not exist in the database. Skipping indexing.", entry.getEntryId());
                            removeFromQueue(entry.getId());
                        } catch (SearchEngineIndexingException ex) {
                            LOG.log(Level.WARNING, "NewsItem #{0} could not be indexed", entry.getEntryId());
                            LOG.log(Level.WARNING, ex.getMessage(), ex);
                        }
                        break;
                    case MEDIA_ITEM:
                        try {
                            MediaItem mediaItem = catalogueFacade.findMediaItemById(entry.getEntryId());
                            index(mediaItem, solrServer);
                            removeFromQueue(entry.getId());
                        } catch (DataNotFoundException ex) {
                            LOG.log(Level.WARNING, "MediaItem #{0} does not exist in the database. Skipping indexing.", entry.getEntryId());
                            removeFromQueue(entry.getId());
                        } catch (SearchEngineIndexingException ex) {
                            LOG.log(Level.WARNING, "MediaItem #{0} could not be indexed", entry.getEntryId());
                            LOG.log(Level.WARNING, ex.getMessage(), ex);
                        }
                        break;
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public SearchResults search(String query, int start, int rows, String... filterQueries) {
        return search(query, start, rows, "score", false, filterQueries);
    }

    @Override
    public SearchResults search(String query, int start, int rows, String sortField, boolean sortOrder, String... filterQueries) {
        return search(query, start, rows, "score", false, null, null, filterQueries);
    }

    /**
     * Queries the search engine.
     *
     * @param query
     *          Query string
     * @param start
     *          First record to retrieve
     * @param rows
     *          Number of rows to retrieve
     * @param sortField
     *          Field to sort by
     * @param sortOrder
     *          Ascending ({@code true}) or descending ({@code false})
     * @param dateFrom
     *          Search results must not be older than this date
     * @param dateTo
     *          Search results must not be newer than this date
     * @param filterQueries 
     *          Filter queries to include in the search
     * @return {@link SearchResults} matching the {@code query}
     */
    @Override
    public SearchResults search(String query, int start, int rows, String sortField, boolean sortOrder, Date dateFrom, Date dateTo, String... filterQueries) {
        long startTime = System.currentTimeMillis();
        SearchResults searchResults = new SearchResults();
        try {
            SolrQuery solrQuery = new SolrQuery();
            solrQuery.setStart(start);
            solrQuery.setRows(rows);

            StringBuilder queryString = new StringBuilder(query);

            // Check if the query has date restrictions
            if (dateFrom != null || dateTo != null) {

                // Construct date query
                if (!query.isEmpty()) {
                    queryString.append(" AND date:");
                }

                if (dateFrom == null) {
                    queryString.append("[* TO ");
                } else {
                    queryString.append("[");
                    queryString.append(solrDateFormat.format(dateFrom));
                    queryString.append(" TO ");
                }

                if (dateTo == null) {
                    queryString.append("*]");
                } else {
                    queryString.append(solrDateFormat.format(dateTo));
                    queryString.append("]");
                }
            }

            solrQuery.setQuery(queryString.toString());

            solrQuery.setFacet(true);

            if (sortOrder) {
                solrQuery.setSortField(sortField, SolrQuery.ORDER.asc);
            } else {
                solrQuery.setSortField(sortField, SolrQuery.ORDER.desc);
            }

            solrQuery.addFacetField(IndexField.TYPE.getName());
            solrQuery.addFacetField(IndexField.OUTLET.getName());
            solrQuery.addFacetField(IndexField.SECTION.getName());
            solrQuery.addFacetField(IndexField.SUBJECT.getName());
            solrQuery.addFacetField(IndexField.ORGANISATION.getName());
            solrQuery.addFacetField(IndexField.PERSON.getName());
            solrQuery.addFacetField(IndexField.LOCATION.getName());
            solrQuery.addFacetField(IndexField.POINT_OF_INTEREST.getName());

            for (UserRole userRole : userFacade.getUserRoles()) {
                solrQuery.addFacetField(userRole.getName());
            }

            solrQuery.addFilterQuery(filterQueries);
            solrQuery.setFacetMinCount(1);
            solrQuery.setIncludeScore(true);
            solrQuery.setHighlight(true).setHighlightSnippets(1); //set other params as needed
            solrQuery.setParam("hl.fl", "title,story,caption");
            solrQuery.setParam("hl.fragsize", "500");
            solrQuery.setParam("hl.simple.pre", "<span class=\"searchHighlight\">");
            solrQuery.setParam("hl.simple.post", "</span>");
            solrQuery.setParam("facet.date", "date");
            solrQuery.setParam("facet.date.start", "NOW/YEAR-10YEAR");
            solrQuery.setParam("facet.date.end", "NOW");
            solrQuery.setParam("facet.date.gap", "+1MONTH");

            SolrServer srv = getSolrServer();

            // POST is used to support UTF-8
            QueryResponse qr = srv.query(solrQuery, METHOD.POST);
            SolrDocumentList sdl = qr.getResults();
            searchResults.setNumberOfResults(sdl.getNumFound());

            for (SolrDocument d : sdl) {

                // Copy all fields to map for easy access
                HashMap<String, Object> values = new HashMap<String, Object>();

                for (Iterator<Map.Entry<String, Object>> i = d.iterator(); i.hasNext();) {
                    Map.Entry<String, Object> e2 = i.next();
                    values.put(e2.getKey(), e2.getValue());
                }

                String type = (String) values.get("type");

                SearchResult hit = null;
                if ("Story".equalsIgnoreCase(type)) {
                    hit = generateStoryHit(qr, values);
                } else if ("Media".equalsIgnoreCase(type)) {
                    hit = generateMediaHit(qr, values);
                }

                if (hit != null) {
                    hit.setScore((Float) d.getFieldValue("score"));
                    searchResults.getHits().add(hit);
                }
            }

            List<FacetField> facets = qr.getFacetFields();

            for (FacetField facet : facets) {
                List<FacetField.Count> facetEntries = facet.getValues();
                if (facetEntries != null) {
                    for (FacetField.Count fcount : facetEntries) {
                        if (!searchResults.getFacets().containsKey(facet.getName())) {
                            searchResults.getFacets().put(facet.getName(), new ArrayList<SearchFacet>());
                        }

                        SearchFacet sf = new SearchFacet(fcount.getName(), fcount.getAsFilterQuery(), fcount.getCount());

                        // Check if the filter query is already active
                        for (String fq : filterQueries) {
                            if (fq.equals(fcount.getAsFilterQuery())) {
                                sf.setSelected(true);
                            }
                        }

                        // Ensure that the facet is not already there
                        if (!searchResults.getFacets().get(facet.getName()).contains(sf)) {
                            searchResults.getFacets().get(facet.getName()).add(sf);
                        }
                    }
                }
            }
        } catch (SolrServerException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        long endTime = System.currentTimeMillis();

        searchResults.setSearchTime(endTime - startTime);
        searchResults.setStart(start);
        searchResults.setResultsPerPage(rows);

        return searchResults;
    }

    @Override
    public byte[] generateReport(SearchResults results) {

        SimpleDateFormat format = new SimpleDateFormat("d MMMM yyyy");
        HSSFWorkbook wb = new HSSFWorkbook();

        String sheetName = WorkbookUtil.createSafeSheetName("Results");
        int overviewSheetRow = 0;

        Font headerFont = wb.createFont();
        headerFont.setFontHeightInPoints((short) 14);
        headerFont.setBoldweight(Font.BOLDWEIGHT_BOLD);

        Font userFont = wb.createFont();
        userFont.setFontHeightInPoints((short) 12);
        userFont.setBoldweight(Font.BOLDWEIGHT_BOLD);

        Font storyFont = wb.createFont();
        storyFont.setFontHeightInPoints((short) 12);
        storyFont.setBoldweight(Font.BOLDWEIGHT_NORMAL);

//        CellStyle headerStyle = createHeaderStyle(wb, headerFont);
//        CellStyle headerVerticalStyle = createHeaderVerticalStyle(wb, headerFont);
//        CellStyle userStyle = createUserStyle(wb, userFont);
//        CellStyle storyCenterStyle = createStoryCenterStyle(wb);
//        CellStyle storyStyle = createStoryStyle(wb, storyFont);
//        CellStyle percentStyle = createPercentStyle(wb, userFont);

        HSSFSheet overviewSheet = wb.createSheet(sheetName);
        HSSFHeader sheetHeader = overviewSheet.getHeader();

        sheetHeader.setLeft("CONVERGE Story Report");
        sheetHeader.setRight("");

        overviewSheet.createFreezePane(0, 1, 0, 1);

        Row row = overviewSheet.createRow(0);
        row.createCell(0).setCellValue("ID");
        row.createCell(1).setCellValue("Title");
        row.createCell(2).setCellValue("Outlet");

        for (int i = 0; i <= 2; i++) {
//                row.getCell(i).setCellStyle(headerStyle);
        }

        overviewSheetRow++;
        for (SearchResult result : results.getHits()) {
            row = overviewSheet.createRow(overviewSheetRow);
            row.createCell(0).setCellValue(result.getId());
            row.createCell(1).setCellValue(result.getTitle());
            row.createCell(2).setCellValue(result.getNote());

            overviewSheetRow++;
        }

        // Auto-size
        for (int i = 0; i <= 2; i++) {
            overviewSheet.autoSizeColumn(i);
        }

        wb.setRepeatingRowsAndColumns(0, 0, 0, 0, 0);
        //wb.setPrintArea(0, 0, 8, 0, overviewSheetRow);
        overviewSheet.setFitToPage(true);
        overviewSheet.setAutobreaks(true);
//        overviewSheet.getPrintSetup().setFitWidth((short)1);
//        overviewSheet.getPrintSetup().setFitHeight((short)500);

        Footer footer = overviewSheet.getFooter();
        footer.setLeft("Page " + HeaderFooter.page() + " of " + HeaderFooter.numPages());
        footer.setRight("Generated on " + HeaderFooter.date() + " at " + HeaderFooter.time());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            wb.write(baos);
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }

        return baos.toByteArray();
    }

    private void index(NewsItem ni, SolrServer solrServer) throws SearchEngineIndexingException {

        SolrInputDocument solrDoc = new SolrInputDocument();
        solrDoc.addField(IndexField.ID.getName(), ni.getId(), 1.0f);
        solrDoc.addField(IndexField.TITLE.getName(), ni.getTitle(), 1.0f);
        solrDoc.addField(IndexField.TYPE.getName(), "Story");
        solrDoc.addField(IndexField.BYLINE.getName(), ni.getByLine());
        solrDoc.addField(IndexField.BRIEF.getName(), ni.getBrief());
        solrDoc.addField(IndexField.STORY.getName(), dk.i2m.converge.core.utils.StringUtils.stripHtml(ni.getStory()));
        try {
            solrDoc.addField(IndexField.LANG.getName(), ni.getLanguage().getCode());
        } catch (NullPointerException ex) {
        }
        solrDoc.addField(IndexField.LANGUAGE.getName(), ni.getLanguage().getName());
        solrDoc.addField(IndexField.WORD_COUNT.getName(), ni.getWordCount());

        for (NewsItemPlacement placement : ni.getPlacements()) {
            if (placement.getEdition() != null) {
                if (placement.getEdition().getPublicationDate() != null) {
                    solrDoc.addField(IndexField.DATE.getName(), placement.getEdition().getPublicationDate().getTime());
                }
                solrDoc.addField(IndexField.EDITION_NUMBER.getName(), placement.getEdition().getNumber());
                solrDoc.addField(IndexField.EDITION_VOLUME.getName(), placement.getEdition().getVolume());
            }
            if (placement.getSection() != null) {
                solrDoc.addField(IndexField.SECTION.getName(), placement.getSection().getFullName());
            }
            if (placement.getOutlet() != null) {
                solrDoc.addField(IndexField.OUTLET.getName(), placement.getOutlet().getTitle());
            }
            solrDoc.addField(IndexField.PLACEMENT.getName(), placement.toString());
        }


        //                for (WorkflowStateTransition wst : ni.getHistory()) {
        //                    doc.add(new Field(IndexField.ACTOR_UID.getName(), wst.getUser().getUsername(), Field.Store.YES, Field.Index.ANALYZED));
        //                    doc.add(new Field(IndexField.ACTOR_NAME.getName(), wst.getUser().getFullName(), Field.Store.YES, Field.Index.ANALYZED));
        //                }


        for (NewsItemActor actor : ni.getActors()) {
            solrDoc.addField(IndexField.ACTOR.getName(), actor.getUser().getFullName());
            // Dynamic fields for the actors role
            solrDoc.addField(actor.getRole().getName(), actor.getUser().getFullName());
        }

        for (Concept concept : ni.getConcepts()) {
            if (concept instanceof Subject) {
                solrDoc.addField(IndexField.SUBJECT.getName(), concept.getFullTitle());
            }
            if (concept instanceof Person) {
                solrDoc.addField(IndexField.PERSON.getName(), concept.getFullTitle());
            }

            if (concept instanceof Organisation) {
                solrDoc.addField(IndexField.ORGANISATION.getName(), concept.getFullTitle());
            }

            if (concept instanceof GeoArea) {
                solrDoc.addField(IndexField.LOCATION.getName(), concept.getFullTitle());
            }

            if (concept instanceof PointOfInterest) {
                solrDoc.addField(IndexField.POINT_OF_INTEREST.getName(), concept.getFullTitle());
            }

            solrDoc.addField(IndexField.CONCEPT.getName(), concept.getFullTitle());
        }
        try {
            solrServer.add(solrDoc);
        } catch (SolrServerException ex) {
            throw new SearchEngineIndexingException(ex);
        } catch (IOException ex) {
            throw new SearchEngineIndexingException(ex);
        }
    }

    public void index(MediaItem mi, SolrServer solrServer) throws SearchEngineIndexingException {
        LOG.log(Level.FINE, "Adding MediaItem #{0} to index", mi.getId());

        if (mi.isOriginalAvailable()) {

            MediaItemRendition mir = mi.getOriginal();

            SolrInputDocument solrDoc = new SolrInputDocument();
            solrDoc.addField(IndexField.ID.getName(), mi.getId(), 1.0f);
            solrDoc.addField(IndexField.TYPE.getName(), "Media");

            String mediaFormat;
            String contentType = mi.getOriginal().getContentType();
            String story = "";

            if (mir.isAudio()) {
                mediaFormat = "Audio";
            } else if (mir.isVideo()) {
                mediaFormat = "Video";
            } else if (mir.isImage()) {
                mediaFormat = "Image";
            } else if (mir.isDocument()) {
                mediaFormat = "Document";

                if (contentType.equals("application/pdf")) {
                    // Extract text in PDF
                    try {
                        URL originalFile = new URL(mir.getAbsoluteFilename());
                        PDDocument doc = null;
                        try {
                            // Read PDF
                            PDFParser parser = new PDFParser(originalFile.openStream());
                            parser.parse();
                            COSDocument cosDoc = parser.getDocument();
                            PDDocument pdDoc = new PDDocument(cosDoc);

                            PDFTextStripper stripper = new PDFTextStripper();
                            story = stripper.getText(pdDoc);

                        } catch (IOException ex) {
                            LOG.log(Level.SEVERE, null, ex);
                        } finally {
                            if (doc != null) {
                                try {
                                    doc.close();
                                } catch (IOException ex) {
                                    LOG.log(Level.SEVERE, null, ex);
                                }
                            }
                        }


                    } catch (MalformedURLException ex) {
                    }
                } else if (contentType.equals("application/msword") || contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
                    try {
                        URL originalFile = new URL(mir.getAbsoluteFilename());
                        HWPFDocument doc = new HWPFDocument(originalFile.openStream());
                        WordExtractor extractor = new WordExtractor(doc);
                        story = extractor.getText();
                    } catch (IOException ex) {
                        LOG.log(Level.SEVERE, null, ex);
                    }
                }
            } else {
                mediaFormat = "Unknown";
            }

            boolean enrich = cfgService.getBoolean(ConfigurationKey.SEARCH_ENGINE_OPEN_CALAIS_ENABLED);

            if (enrich) {
                enrich(mi, story);
            }

            solrDoc.addField(IndexField.MEDIA_FORMAT.getName(), mediaFormat);
            solrDoc.addField(IndexField.TITLE.getName(), mi.getTitle(), 1.0f);
            solrDoc.addField(IndexField.BYLINE.getName(), mi.getByLine());
            solrDoc.addField(IndexField.STORY.getName(), dk.i2m.converge.core.utils.StringUtils.stripHtml(mi.getDescription()) + " " + story);
            solrDoc.addField(IndexField.CAPTION.getName(), dk.i2m.converge.core.utils.StringUtils.stripHtml(mi.getDescription()));
            solrDoc.addField(IndexField.CONTENT_TYPE.getName(), mi.getOriginal().getContentType());
            solrDoc.addField(IndexField.REPOSITORY.getName(), mi.getCatalogue().getName());

            if (mi.getMediaDate() != null) {
                solrDoc.addField(IndexField.DATE.getName(), mi.getMediaDate().getTime());
            }

            if (mi.isPreviewAvailable()) {
                solrDoc.addField(IndexField.THUMB_URL.getName(), mi.getPreview().getAbsoluteFilename());
                solrDoc.addField(IndexField.DIRECT_URL.getName(), mi.getPreview().getFileLocation());
            }

            solrDoc.addField(IndexField.ACTOR.getName(), mi.getOwner().getFullName());

            for (Concept concept : mi.getConcepts()) {
                if (concept instanceof Subject) {
                    solrDoc.addField(IndexField.SUBJECT.getName(), concept.getFullTitle());
                }
                if (concept instanceof Person) {
                    solrDoc.addField(IndexField.PERSON.getName(), concept.getFullTitle());
                }

                if (concept instanceof Organisation) {
                    solrDoc.addField(IndexField.ORGANISATION.getName(), concept.getFullTitle());
                }

                if (concept instanceof GeoArea) {
                    solrDoc.addField(IndexField.LOCATION.getName(), concept.getFullTitle());
                }

                if (concept instanceof PointOfInterest) {
                    solrDoc.addField(IndexField.POINT_OF_INTEREST.getName(), concept.getFullTitle());
                }

                solrDoc.addField(IndexField.CONCEPT.getName(), concept.getFullTitle());
            }

            try {
                solrServer.add(solrDoc);
            } catch (SolrServerException ex) {
                throw new SearchEngineIndexingException(ex);
            } catch (IOException ex) {
                throw new SearchEngineIndexingException(ex);
            }
        } else {
            LOG.log(Level.FINE, "Ignoring MediaItem #{0}. Missing original {1} rendition", new Object[]{mi.getId(), mi.getCatalogue().getOriginalRendition().getName()});
        }
    }

    private List<Concept> enrich(String story) {
        List<Concept> concepts = new ArrayList<Concept>();

        PostMethod method = new PostMethod(OPEN_CALAIS_URL);
        method.setRequestHeader("x-calais-licenseID", cfgService.getString(ConfigurationKey.OPEN_CALAIS_API_KEY));
        method.setRequestHeader("Content-Type", "text/raw; charset=UTF-8");
        method.setRequestHeader("Accept", "application/json");
        method.setRequestHeader("enableMetadataType", "SocialTags");
        method.setRequestEntity(new StringRequestEntity(story));

        try {
            HttpClient client = new HttpClient();
            int returnCode = client.executeMethod(method);
            if (returnCode == HttpStatus.SC_NOT_IMPLEMENTED) {
                System.err.println("The Post method is not implemented by this URI");
                // still consume the response body
                method.getResponseBodyAsString();
            } else if (returnCode == HttpStatus.SC_OK) {

                JSONObject response = JSONObject.fromObject(method.getResponseBodyAsString());

                for (Object key : response.keySet()) {
                    String sKey = (String) key;

                    if (sKey.startsWith("http://d.opencalais.com/")) {
                        JSONObject entity = response.getJSONObject(sKey);

                        if (((String) entity.get("_typeGroup")).equalsIgnoreCase("entities")) {
                            String conceptType = (String) entity.get("_type");
                            String conceptName = (String) entity.get("name");
                            Concept match = null;
                            try {
                                match = metaDataFacade.findConceptByName(conceptName);
                            } catch (DataNotFoundException dnfe) {
                            }

                            if (entity.containsKey("_type")) {
                                if (conceptType.equalsIgnoreCase("company") || conceptType.equalsIgnoreCase("organization")) {

                                    if (match == null || (!(match instanceof Organisation))) {
                                        match = new Organisation(conceptName, "");
                                        match = metaDataFacade.create(match);
                                    }

                                    if (match instanceof Organisation) {
                                        concepts.add(match);
                                    }
                                } else if (conceptType.equalsIgnoreCase("person")) {
                                    if (match == null || (!(match instanceof Person))) {
                                        match = new Person(conceptName, "");
                                        match = metaDataFacade.create(match);
                                    }

                                    if (match instanceof Person) {
                                        concepts.add(match);
                                    }
                                } else if (conceptType.equalsIgnoreCase("city") || conceptType.equalsIgnoreCase("country") || conceptType.equalsIgnoreCase("continent") || conceptType.equalsIgnoreCase("ProvinceOrState") || conceptType.equalsIgnoreCase("region")) {
                                    if (match == null || (!(match instanceof GeoArea))) {
                                        match = new GeoArea(conceptName, "");
                                        match = metaDataFacade.create(match);
                                    }

                                    if (match instanceof GeoArea) {
                                        concepts.add(match);
                                    }
                                } else if (conceptType.equalsIgnoreCase("facility")) {
                                    if (match == null || (!(match instanceof PointOfInterest))) {
                                        match = new PointOfInterest(conceptName, "");
                                        match = metaDataFacade.create(match);
                                    }

                                    if (match instanceof PointOfInterest) {
                                        concepts.add(match);
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                LOG.log(Level.WARNING, "Invalid response received from OpenCalais [{0}] {1}", new Object[]{returnCode, method.getResponseBodyAsString()});
            }

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "", e);
        } finally {
            method.releaseConnection();
        }
        return concepts;
    }

    private void enrich(MediaItem mi, String story) {

        StringBuilder enrich = new StringBuilder();
        enrich.append(story).append(" ").append(mi.getDescription()).append(" ").append(mi.getTitle());

        int chunkSize = 100000;
        Iterable<String> chunks = Splitter.fixedLength(chunkSize).split(enrich.toString());

        List<Concept> concepts = new ArrayList<Concept>();
        for (Iterator<String> i = chunks.iterator(); i.hasNext();) {
            LOG.log(Level.INFO, "Sending chunk");
            String chunk = i.next();
            concepts.addAll(enrich(chunk));
            LOG.log(Level.INFO, "Concepts total " + concepts.size());
        }

        mi.getConcepts().addAll(concepts);
    }

    @Override
    public void optimizeIndex() throws SearchEngineIndexingException {
        try {
            getSolrServer().optimize();
        } catch (SolrServerException ex) {
            throw new SearchEngineIndexingException(ex);
        } catch (IOException ex) {
            throw new SearchEngineIndexingException(ex);
        }
    }

    /**
     * Generates a {link SearchResult} for a media item.
     *
     * @param qr
     *          QueryResponse from Solr
     * @param values
     *          Fields available
     * @return {@link SearchResult}
     */
    private SearchResult generateMediaHit(QueryResponse qr, HashMap<String, Object> values) {
        String id = (String) values.get(IndexField.ID.getName());

        StringBuilder caption = new StringBuilder();
        StringBuilder title = new StringBuilder();
        StringBuilder note = new StringBuilder();

        Map<String, List<String>> highlighting = qr.getHighlighting().get(id);

        boolean highlightingExist = highlighting != null;

        if (highlightingExist && highlighting.get(IndexField.STORY.getName()) != null) {
            for (String hl : highlighting.get(IndexField.STORY.getName())) {
                caption.append(hl);
            }
        } else {
            caption.append(StringUtils.abbreviate((String) values.get(IndexField.STORY.getName()), 500));
        }

        if (highlightingExist && highlighting.get(IndexField.TITLE.getName()) != null) {
            for (String hl : qr.getHighlighting().get(id).get(IndexField.TITLE.getName())) {
                title.append(hl);
            }
        } else {
            title.append((String) values.get(IndexField.TITLE.getName()));
        }

        String format = (String) values.get(IndexField.MEDIA_FORMAT.getName());

        note.append((String) values.get(IndexField.TYPE.getName()));
        note.append(" - ");
        note.append(format);
        note.append(" - ");
        note.append((String) values.get(IndexField.REPOSITORY.getName()));

        SearchResult hit = new SearchResult();
        hit.setId(Long.valueOf(id));
        hit.setTitle(title.toString());
        hit.setDescription(caption.toString());
        hit.setNote(note.toString());
        hit.setLink("{0}/MediaItemArchive.xhtml?id=" + values.get(IndexField.ID.getName()));
        hit.setType((String) values.get(IndexField.TYPE.getName()));
        hit.setFormat(format);

        if (values.containsKey(IndexField.THUMB_URL.getName())) {
            hit.setPreview(true);
            hit.setPreviewLink((String) values.get(IndexField.THUMB_URL.getName()));
            hit.setDirectLink((String) values.get(IndexField.DIRECT_URL.getName()));

        } else {
            hit.setPreview(false);
        }

        if (values.containsKey(IndexField.DATE.getName())) {
            if (values.get(IndexField.DATE.getName()) instanceof List) {
                hit.setDates((List<Date>) values.get(IndexField.DATE.getName()));
            } else {
                hit.addDate((Date) values.get(IndexField.DATE.getName()));
            }
        }

        return hit;
    }

    /**
     * Generates a {link SearchResult} for a story.
     *
     * @param qr
     *          QueryResponse from Solr
     * @param values
     *          Fields available
     * @return {@link SearchResult}
     */
    private SearchResult generateStoryHit(QueryResponse qr, HashMap<String, Object> values) {
        String id = (String) values.get(IndexField.ID.getName());

        StringBuilder story = new StringBuilder();
        StringBuilder title = new StringBuilder();
        StringBuilder note = new StringBuilder();

        Map<String, List<String>> highlighting = qr.getHighlighting().get(id);

        boolean highlightingExist = highlighting != null;

        if (highlightingExist && highlighting.get(IndexField.STORY.getName()) != null) {
            for (String hl : highlighting.get(IndexField.STORY.getName())) {
                story.append(hl);
            }
        } else {
            story.append(StringUtils.abbreviate((String) values.get(IndexField.STORY.getName()), 500));
        }

        if (highlightingExist && highlighting.get(IndexField.TITLE.getName()) != null) {
            for (String hl : qr.getHighlighting().get(id).get(IndexField.TITLE.getName())) {
                title.append(hl);
            }
        } else {
            title.append((String) values.get(IndexField.TITLE.getName()));
        }

        note.append((String) values.get(IndexField.TYPE.getName()));
        note.append(" - Words: ");

        if (values.containsKey(IndexField.WORD_COUNT.getName())) {
            note.append(String.valueOf(values.get(IndexField.WORD_COUNT.getName())));
        } else {
            note.append("Unknown");
        }


        note.append("<br/>");

        if (values.containsKey(IndexField.PLACEMENT.getName())) {
            if (values.get(IndexField.PLACEMENT.getName()) instanceof String) {
                note.append(values.get(IndexField.PLACEMENT.getName()));
            } else if (values.get(IndexField.PLACEMENT.getName()) instanceof List) {
                List<String> placements = (List<String>) values.get(IndexField.PLACEMENT.getName());
                for (String placement : placements) {
                    note.append(placement);
                    note.append("<br/>");
                }
            } else {
                LOG.warning("Unexpected value returned from search engine");
            }
        }

        SearchResult hit = new SearchResult();
        hit.setId(Long.valueOf(id));
        hit.setTitle(title.toString());
        hit.setDescription(story.toString());
        hit.setNote(note.toString());
        hit.setLink("{0}/NewsItemArchive.xhtml?id=" + id);
        hit.setType((String) values.get(IndexField.TYPE.getName()));

        if (values.containsKey(IndexField.DATE.getName())) {
            if (values.get(IndexField.DATE.getName()) instanceof Date) {
                hit.addDate((Date) values.get(IndexField.DATE.getName()));
            } else if (values.get(IndexField.DATE.getName()) instanceof List) {
                hit.setDates((List<Date>) values.get(IndexField.DATE.getName()));
            } else {
                LOG.warning("Unexpected value returned from search engine");
            }
        }
        return hit;
    }

    /**
     * Gets the instance of the Apache Solr server used for indexing.
     *
     * @return Instance of the Apache Solr server
     * @throws IllegalStateException
     *          If the search engine is not properly configured
     */
    private SolrServer getSolrServer() {
        try {
            String url = cfgService.getString(ConfigurationKey.SEARCH_ENGINE_URL);
            Integer socketTimeout = cfgService.getInteger(ConfigurationKey.SEARCH_ENGINE_SOCKET_TIMEOUT);
            Integer connectionTimeout = cfgService.getInteger(ConfigurationKey.SEARCH_ENGINE_CONNECTION_TIMEOUT);
            Integer maxTotalConnectionsPerHost = cfgService.getInteger(ConfigurationKey.SEARCH_ENGINE_MAX_TOTAL_CONNECTIONS_PER_HOST);
            Integer maxTotalConnections = cfgService.getInteger(ConfigurationKey.SEARCH_ENGINE_MAX_TOTAL_CONNECTIONS);
            Integer maxRetries = cfgService.getInteger(ConfigurationKey.SEARCH_ENGINE_MAX_RETRIES);
            Boolean followRedirects = cfgService.getBoolean(ConfigurationKey.SEARCH_ENGINE_FOLLOW_REDIRECTS);
            Boolean allowCompression = cfgService.getBoolean(ConfigurationKey.SEARCH_ENGINE_ALLOW_COMPRESSION);

            CommonsHttpSolrServer solrServer = new CommonsHttpSolrServer(url);
            solrServer.setRequestWriter(new BinaryRequestWriter());
            solrServer.setSoTimeout(socketTimeout);
            solrServer.setConnectionTimeout(connectionTimeout);
            solrServer.setDefaultMaxConnectionsPerHost(maxTotalConnectionsPerHost);
            solrServer.setMaxTotalConnections(maxTotalConnections);
            solrServer.setFollowRedirects(followRedirects);
            solrServer.setAllowCompression(allowCompression);
            solrServer.setMaxRetries(maxRetries);

            return solrServer;
        } catch (MalformedURLException ex) {
            LOG.log(Level.SEVERE, "Invalid search engine configuration. {0}", ex.getMessage());
            LOG.log(Level.FINE, "", ex);
            throw new IllegalStateException("Invalid search engine configuration", ex);
        }
    }
}
