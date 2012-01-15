/*
 *  Copyright (C) 2011 - 2012 Interactive Media Management
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
package dk.i2m.converge.plugins.decoders.newsml12;

import dk.i2m.converge.core.content.catalogue.Catalogue;
import dk.i2m.converge.core.logging.LogSeverity;
import dk.i2m.converge.core.newswire.NewswireItem;
import dk.i2m.converge.core.newswire.NewswireService;
import dk.i2m.converge.core.newswire.NewswireServiceProperty;
import dk.i2m.converge.core.plugin.NewswireDecoder;
import dk.i2m.converge.core.plugin.PluginContext;
import dk.i2m.converge.core.search.SearchEngineIndexingException;
import dk.i2m.converge.core.utils.StringUtils;
import dk.i2m.converge.plugins.decoders.newsml12.binding.NIType;
import dk.i2m.converge.plugins.decoders.newsml12.binding.NewsComponentType;
import dk.i2m.converge.plugins.decoders.newsml12.binding.NewsComponentType.ContentItem;
import dk.i2m.converge.plugins.decoders.newsml12.binding.NewsComponentType.ContentItem.DataContent;
import dk.i2m.converge.plugins.decoders.newsml12.binding.NewsComponentType.NewsLines.DateLine;
import dk.i2m.converge.plugins.decoders.newsml12.binding.NewsComponentType.NewsLines.HeadLine;
import dk.i2m.converge.plugins.decoders.newsml12.binding.NewsML;
import dk.i2m.converge.plugins.decoders.newsml12.transformer.NamespaceFilter;
import dk.i2m.converge.plugins.decoders.newsml12.transformer.NitfToHtmlTransformer;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXSource;
import org.w3c.dom.Node;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * Decoder for the NewsML 1.2 format.
 *
 * @author Allan Lykke Christensen
 */
@dk.i2m.converge.core.annotations.NewswireDecoder
public class NewsMLDecoder implements NewswireDecoder {

    private static final Logger LOG = Logger.getLogger(NewsMLDecoder.class.
            getName());

    private ResourceBundle bundle = ResourceBundle.getBundle(
            "dk.i2m.converge.plugins.decoders.newsml12.Messages");

    private Map<String, String> availableProperties = null;

    private Map<String, String> properties = new HashMap<String, String>();

    private PluginContext pluginCtx;

    private NewswireService newswireService;

    private Map<String, String> renditionMapping = new HashMap<String, String>();

    private String location = "";

    private String processedLocation = "";

    private Catalogue catalogue = null;

    private boolean moveProcessed = false;

    private boolean deleteProcessed = false;

    private NitfToHtmlTransformer nitfToHtmlTransformer = null;

    /**
     * Creates a new instance of {@link NewsMLDecoder}.
     */
    public NewsMLDecoder() {
    }

    //<editor-fold defaultstate="collapsed" desc="Default plug-in methods">
    @Override
    public String getName() {
        return bundle.getString("PLUGIN_NAME");
    }

    @Override
    public String getAbout() {
        return bundle.getString("PLUGIN_ABOUT");
    }

    @Override
    public String getDescription() {
        return bundle.getString("PLUGIN_DESCRIPTION");
    }

    @Override
    public String getVendor() {
        return bundle.getString("PLUGIN_VENDOR");
    }

    @Override
    public Date getDate() {
        try {
            SimpleDateFormat format =
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return format.parse(bundle.getString("PLUGIN_BUILD_TIME"));
        } catch (Exception ex) {
            return Calendar.getInstance().getTime();
        }
    }

    @Override
    public ResourceBundle getBundle() {
        return bundle;
    }

    @Override
    public Map<String, String> getAvailableProperties() {
        if (availableProperties == null) {
            availableProperties = new LinkedHashMap<String, String>();
            for (Property p : Property.values()) {
                availableProperties.put(bundle.getString(p.name()), p.name());
            }
        }
        return this.availableProperties;
    }
//</editor-fold>

    @Override
    public void decode(PluginContext ctx, NewswireService newswire) {
        this.pluginCtx = ctx;
        this.newswireService = newswire;
        this.properties = newswire.getPropertiesMap();

        readRenditionMappings();
        readAttachmentCatalogue();
        readLocations();
        setupTransformers();

        if (!validateProperties()) {
            return;
        }

        processNewswireFiles();
    }

    private void setupTransformers() {
        this.nitfToHtmlTransformer = new NitfToHtmlTransformer();
    }

    /**
     * Processes all the pending newswire files.
     */
    private void processNewswireFiles() {
        // Read NewsML files from the newswire location
        File[] xmlFilesToProcess = getFilesToProcess();
        int pending = xmlFilesToProcess.length;
        log(LogSeverity.INFO, "{0} NewsML files ready for processing", pending);
        for (File file : xmlFilesToProcess) {
            log(LogSeverity.INFO, "Processing {1}. {0} items to go",
                    new Object[]{(pending--), file.getName()});
            processNewswireFile(file);
        }
    }

    /**
     * Process a single newswire file.
     *
     * @param file Newswire file to process
     */
    private void processNewswireFile(File file) {

        List<NewswireItem> existing =
                pluginCtx.findNewswireItemsByExternalId(file.getName());
        if (!existing.isEmpty()) {
            log(LogSeverity.INFO, "{0} has already been processed. Skipping",
                    new Object[]{file.getName()});
            return;
        }

        NewswireItem item = new NewswireItem();
        item.setExternalId(file.getName());
        item.setNewswireService(this.newswireService);

        long start = Calendar.getInstance().getTimeInMillis();
        NewsML newsMl = null;
        try {
            newsMl = unmarshal(file);

            List<NIType> newsItem = newsMl.getNewsItem();
            NIType next = newsItem.iterator().next();
            NewsComponentType newsComponent = next.getNewsComponent();

            List<ContentItem> contentItems = newsComponent.getContentItem();
            ContentItem contentItem = contentItems.iterator().next();

            String contentFormat = contentItem.getFormat().getFormalName();
            DataContent dc = contentItem.getDataContent();


            for (Object obj : newsComponent.getNewsLines().
                    getHeadLineAndSubHeadLineOrByLine()) {
                if (obj instanceof HeadLine) {
                    HeadLine headLine = (HeadLine) obj;
                    StringBuilder title = new StringBuilder();
                    for (Object hl : headLine.getContent()) {
                        title.append(hl);
                    }
                    item.setTitle(title.toString());
                }
                if (obj instanceof DateLine) {
                    DateLine dateLine = (DateLine) obj;
                    for (Object hl : dateLine.getContent()) {
                        item.addSummary((String) hl);
                    }
                    item.addSummary(" ");
                }
            }

            for (Object obj : dc.getContent()) {
                if (obj instanceof Node) {
                    try {
                        Node node = (Node) obj;
                        String dataContent = extractXml(node, false, true);
                        if (contentFormat.startsWith("NITF")) {
                            String html = nitfToHtmlTransformer.transform(
                                    dataContent);
                            item.setContent(html);
                        } else {
                            item.setContent(dataContent);
                        }

                        String story = item.getContent();
                        String stippedStory = StringUtils.stripHtml(story);
                        String summary = org.apache.commons.lang.StringUtils.
                                abbreviate(stippedStory, 600);
                        item.addSummary(summary);

                    } catch (TransformerException ex) {
                        log(LogSeverity.SEVERE,
                                "Could not transform NITF to HTML",
                                ex);
                    }
                }
            }

            NewswireItem nwi = pluginCtx.createNewswireItem(item);
            try {
                pluginCtx.index(nwi);
            } catch (SearchEngineIndexingException seie) {
                log(LogSeverity.SEVERE,
                        "Could not index news wire item #{0}. {1}",
                        new Object[]{nwi.getId(), seie.getMessage()});
            }

            if (moveProcessed) {
                File newLocation = new File(this.processedLocation,
                        file.getName());
                log(LogSeverity.INFO, "Moving {0} to {1}", new Object[]{file.
                            getAbsolutePath(), newLocation.getAbsolutePath()});
                file.renameTo(newLocation);
            } else if (deleteProcessed) {
                if (file.delete()) {
                    log(LogSeverity.INFO, "{0} deleted", new Object[]{file.
                                getAbsolutePath()});
                } else {
                    log(LogSeverity.WARNING, "{0} could not be deleted",
                            new Object[]{file.getAbsolutePath()});
                }
            }

        } catch (NewsMLUnmarshalException ex) {
            log(LogSeverity.SEVERE, "{0} could not be unmarshalled. {1}",
                    new Object[]{file.getName(), ex.getMessage()});
        } finally {
            long end = Calendar.getInstance().getTimeInMillis();
            long duration = end - start;
            log(LogSeverity.INFO, "It took {0}ms to process {1}", new Object[]{
                        duration, file.getName()});
        }
    }

    private NewsML unmarshal(File file) throws NewsMLUnmarshalException {
        try {
            Unmarshaller unmarshaller;
            try {
                JAXBContext jc = JAXBContext.newInstance(
                        "dk.i2m.converge.plugins.decoders.newsml12.binding",
                        getClass().getClassLoader());
                unmarshaller = jc.createUnmarshaller();
            } catch (JAXBException ex) {
                throw new NewsMLUnmarshalException(
                        "Could not instantiate JAXBContext", ex);
            }

            //Create an XMLReader to use with our filter
            XMLReader reader = XMLReaderFactory.createXMLReader();

            //Create the filter (to add namespace) and set the xmlReader as its parent.
            NamespaceFilter inFilter = new NamespaceFilter(
                    "http://iptc.org/std/NewsML/2003-10-10/", true);
            inFilter.setParent(reader);

            //Prepare the input, in this case a java.io.File (output)
            InputSource is = new InputSource(new FileInputStream(file));

            //Create a SAXSource specifying the filter
            SAXSource source = new SAXSource(inFilter, is);

            return (NewsML) unmarshaller.unmarshal(source);
        } catch (JAXBException ex) {
            throw new NewsMLUnmarshalException(
                    "Could not unmarshal source file into NewsML", ex);
        } catch (FileNotFoundException ex) {
            throw new NewsMLUnmarshalException(
                    "Could not find the file to be unmarshalled", ex);
        } catch (SAXException ex) {
            throw new NewsMLUnmarshalException(
                    "Could not create XML reader for sanitising the XML namespace",
                    ex);
        }
    }

    private String extractXml(Node node, boolean addNamespaceDeclarations,
            boolean addXmlDeclaration) {
        DOMImplementationLS lsImpl =
                (DOMImplementationLS) node.getOwnerDocument().getImplementation().
                getFeature("LS", "3.0");
        LSSerializer lsSerializer = lsImpl.createLSSerializer();
        lsSerializer.getDomConfig().setParameter("xml-declaration",
                addXmlDeclaration);
        lsSerializer.getDomConfig().setParameter("namespace-declarations",
                addNamespaceDeclarations);
        return lsSerializer.writeToString(node);
    }

    private boolean isCatalogueAvailable() {
        if (catalogue == null) {
            return false;
        } else {
            return true;
        }
    }

    private void readAttachmentCatalogue() {

        if (this.properties.containsKey(Property.ATTACHMENT_CATALOGUE.name())) {

            try {
                Long catalogueId =
                        Long.valueOf(this.properties.get(Property.ATTACHMENT_CATALOGUE.
                        name()));
                this.catalogue = pluginCtx.findCatalogue(catalogueId);
                if (this.catalogue == null) {
                    log(LogSeverity.WARNING, "Catalogue with ID {0} not found",
                            this.properties.get(Property.ATTACHMENT_CATALOGUE.
                            name()));
                }
            } catch (NumberFormatException ex) {
                log(LogSeverity.WARNING, "Invalid catalogue specified: {0}",
                        this.properties.get(Property.ATTACHMENT_CATALOGUE.name()));
            }
        }
    }

    /**
     * Read the {@link NewsMLDecoder.Property#RENDITION_MAPPING} properties into
     * the map ({@link NewsMLDecoder#renditionMapping}) of renditionMappings.
     */
    private void readRenditionMappings() {
        final String SEPARATOR = ";";
        for (NewswireServiceProperty property : this.newswireService.
                getProperties()) {
            if (property.getKey().equals(Property.RENDITION_MAPPING.name())) {
                String[] mapping = property.getValue().split(SEPARATOR);
                if (mapping != null && mapping.length == 2) {
                    renditionMapping.put(mapping[0], mapping[1]);
                }
            }
        }
    }

    /**
     * Read the location of the NewsML files to be processed into the plug-in.
     * After executing this method {@link NewsMLDecoder#moveProcessed},
     * {@link NewsMLDecoder#deleteProcessed} and
     * {@link NewsMLDecoder#processedLocation} are all set.
     */
    private void readLocations() {
        this.location = this.properties.get(Property.LOCATION.name());

        if (this.properties.containsKey(Property.PROCESSED_LOCATION.name())) {
            this.moveProcessed = true;
            this.processedLocation =
                    this.properties.get(Property.PROCESSED_LOCATION.name());
        }

        if (this.properties.containsKey(Property.DELETE_AFTER_PROCESS.name())) {
            this.deleteProcessed = true;
        }
    }

    private boolean validateProperties() {

        if (!this.properties.containsKey(Property.LOCATION.name())) {
            log(LogSeverity.SEVERE,
                    "Newswire location is missing. Newswire processing stopped.");
            return false;
        }

        if (!this.moveProcessed && !this.deleteProcessed) {
            log(LogSeverity.SEVERE,
                    "Processed newswires must either be moved or deleted.");
            return false;
        }

        return true;
    }

    private File[] getFilesToProcess() {
        File newswireDirectory = new File(location);
        FilenameFilter xmlFiles = new FileExtensionFilter("xml");
        return newswireDirectory.listFiles(xmlFiles);
    }

    private void log(LogSeverity severity, String msg) {
        log(severity, msg, new Object[]{});
    }

    private void log(LogSeverity severity, String msg, Object param) {
        log(severity, msg, new Object[]{param});
    }

    private void log(LogSeverity severity, String msg, Object[] params) {
        this.pluginCtx.log(severity, msg, params, this.newswireService,
                this.newswireService.getId());
    }

    /**
     * Enumeration of properties available for the {@link NewsMLDecoder}.
     */
    public enum Property {

        /**
         * Location where the NewsML files for processing can be found.
         */
        LOCATION,
        /**
         * Determines if processed files should be deleted.
         */
        DELETE_AFTER_PROCESS,
        /**
         * Location to move processed NewsML files. Note, this property is only
         * applicable if {@link NewsMLDecoder.Property#DELETE_AFTER_PROCESS} is
         * unset or {@code false}.
         */
        PROCESSED_LOCATION,
        /**
         * Unique ID of the catalogue to store attachments accompanying the
         * NewsML files.
         */
        ATTACHMENT_CATALOGUE,
        /**
         * Mapping of renditions to content items (attachments).
         */
        RENDITION_MAPPING,
        /**
         * Date format used in the NewsML file.
         */
        DATE_FORMAT
    }
}
