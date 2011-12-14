/*
 * Copyright (C) 2011 Interactive Media Management
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package dk.i2m.converge.plugins.decoders.newsml;

import dk.i2m.converge.core.content.ContentTag;
import dk.i2m.converge.core.content.catalogue.Catalogue;
import dk.i2m.converge.core.content.catalogue.Rendition;
import dk.i2m.converge.core.newswire.NewswireItem;
import dk.i2m.converge.core.newswire.NewswireItemAttachment;
import dk.i2m.converge.core.newswire.NewswireService;
import dk.i2m.converge.core.newswire.NewswireServiceProperty;
import dk.i2m.converge.core.plugin.ArchiveException;
import dk.i2m.converge.core.plugin.NewswireDecoder;
import dk.i2m.converge.core.plugin.PluginContext;
import dk.i2m.converge.core.search.SearchEngineIndexingException;
import dk.i2m.converge.core.utils.FileUtils;
import dk.i2m.converge.core.utils.StringUtils;
import dk.i2m.converge.nar.newsml.v1_0.*;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Decoder for newswires formatted using NewsML.
 *
 * @author Allan Lykke Christensen
 */
@dk.i2m.converge.core.annotations.NewswireDecoder
public class NewsMLDecoder implements NewswireDecoder {

    private static final Logger LOG = Logger.getLogger(NewsMLDecoder.class.getName());

    private ResourceBundle bundle = ResourceBundle.getBundle("dk.i2m.converge.plugins.decoders.newsml.Messages");

    private Map<String, String> properties = new HashMap<String, String>();

    private Map<String, String> availableProperties = null;

    public enum Property {

        PROPERTY_NEWSWIRE_LOCATION, PROPERTY_ATTACHMENT_CATALOGUE,
        PROPERTY_NEWSWIRE_PROCESSED_LOCATION,
        PROPERTY_NEWSWIRE_DELETE_AFTER_PROCESS, PROPERTY_RENDITION_MAPPING
    }

    private final static DateFormat NEWSML_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd'T'HHmmssZZZZ");

    private PluginContext pluginCtx;

    private NewswireService newswireService;

    //private List<NewswireItem> newswireItems = null;
    private Map<String, String> renditionMapping = new HashMap<String, String>();

    @Override
    public Map<String, String> getAvailableProperties() {
        if (availableProperties == null) {
            availableProperties = new LinkedHashMap<String, String>();
            for (Property p : Property.values()) {
                availableProperties.put(bundle.getString(p.name()), p.name());
            }
        }
        return availableProperties;
    }

    @Override
    public void decode(PluginContext ctx, NewswireService newswire) {
        this.pluginCtx = ctx;
        this.newswireService = newswire;
        this.properties = newswire.getPropertiesMap();

        // Read rendition mapping properties
        for (NewswireServiceProperty property : newswire.getProperties()) {
            if (property.getKey().equals(Property.PROPERTY_RENDITION_MAPPING.name())) {
                String[] mapping = property.getValue().split(";");
                if (mapping != null && mapping.length == 2) {
                    renditionMapping.put(mapping[0], mapping[1]);
                }
            }
        }

        readNewswires();
    }

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
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return format.parse(bundle.getString("PLUGIN_BUILD_TIME"));
        } catch (Exception ex) {
            return Calendar.getInstance().getTime();
        }
    }

    private void readNewswires() {
        try {
            JAXBContext jc = JAXBContext.newInstance("dk.i2m.converge.nar.newsml.v1_0", getClass().getClassLoader());

            Unmarshaller u = jc.createUnmarshaller();

            // Property Validation
            if (!this.properties.containsKey(Property.PROPERTY_NEWSWIRE_LOCATION.name())) {
                LOG.log(Level.SEVERE, "Newswire location is missing. Newswire processing stopped.");
                return;
            }

            boolean useCatalogue = false;
            Catalogue catalogue = null;

            // Property Validation
            if (this.properties.containsKey(Property.PROPERTY_ATTACHMENT_CATALOGUE.name())) {

                try {
                    Long catalogueId = Long.valueOf(this.properties.get(Property.PROPERTY_ATTACHMENT_CATALOGUE.name()));
                    catalogue = pluginCtx.findCatalogue(catalogueId);
                    if (catalogue == null) {
                        LOG.log(Level.WARNING, "Catalogue with ID {0} not found", this.properties.get(Property.PROPERTY_ATTACHMENT_CATALOGUE.name()));
                        useCatalogue = false;
                    } else {
                        useCatalogue = true;
                    }
                } catch (NumberFormatException ex) {
                    LOG.log(Level.WARNING, "Invalid catalogue specified: {0}", this.properties.get(Property.PROPERTY_ATTACHMENT_CATALOGUE.name()));
                }
            }

            String location = this.properties.get(Property.PROPERTY_NEWSWIRE_LOCATION.name());

            String processedLocation = "";
            boolean moveProcessed = false;
            if (this.properties.containsKey(Property.PROPERTY_NEWSWIRE_PROCESSED_LOCATION.name())) {
                moveProcessed = true;
                processedLocation = this.properties.get(Property.PROPERTY_NEWSWIRE_PROCESSED_LOCATION.name());
            }

            boolean deleteProcessed = false;
            if (this.properties.containsKey(
                    Property.PROPERTY_NEWSWIRE_DELETE_AFTER_PROCESS.name())) {
                deleteProcessed = true;
            }

            if (!moveProcessed && !deleteProcessed) {
                LOG.log(Level.SEVERE,
                        "Processed newswires must either be moved or deleted.");
                return;
            }

            TransformerFactory tFactory = TransformerFactory.newInstance();
            Transformer transformer = null;
            try {
                transformer = tFactory.newTransformer();
            } catch (TransformerConfigurationException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }

            // Read NewsML files from the newswire location
            File newswireDirectory = new File(location);
            File processedDirectory = new File(processedLocation);

            FilenameFilter xmlFiles = new FileExtensionFilter("xml");


            File[] xmlFilesToProcess = newswireDirectory.listFiles(xmlFiles);
            int pending = xmlFilesToProcess.length;
            LOG.log(Level.INFO, "Starting to process {0} files", pending);

            for (File file : newswireDirectory.listFiles(xmlFiles)) {
                LOG.log(Level.INFO, "{0} items to go", (pending--));
                boolean fileMissing = false;
                List<NewswireItem> results = pluginCtx.findNewswireItemsByExternalId(file.getName());

                if (results.isEmpty()) {
                    LOG.log(Level.INFO, "Processing {0}", file.getName());
                    NewswireItem newswireItem = new NewswireItem();
                    newswireItem.setExternalId(file.getName());
                    newswireItem.setNewswireService(newswireService);

                    NewsML newsMl = (NewsML) u.unmarshal(file);
                    Calendar itemCal = Calendar.getInstance();
                    try {
                        Date itemDate = NEWSML_DATE_FORMAT.parse(newsMl.getNewsEnvelope().getDateAndTime().getValue());
                        itemCal.setTime(itemDate);
                        newswireItem.setDate(itemCal);
                    } catch (ParseException ex) {
                        LOG.log(Level.SEVERE, null, ex);
                        newswireItem.setDate(itemCal);
                    }

                    for (NewsItem item : newsMl.getNewsItem()) {

                        List<TopicSet> topicSets = item.getNewsComponent().getTopicSet();

                        for (TopicSet topicSet : topicSets) {
                            for (Topic topic : topicSet.getTopic()) {
                                for (Description description : topic.getDescription()) {
                                    ContentTag topicTag = pluginCtx.findOrCreateContentTag(description.getValue());

                                    if (!newswireItem.getTags().contains(topicTag)) {
                                        newswireItem.getTags().add(topicTag);
                                    }
                                }
                            }
                        }

                        for (NewsComponent c : item.getNewsComponent().getNewsComponent()) {
                            if (c.getRole().getFormalName().equalsIgnoreCase("Main Text")) {

                                StringBuffer byLine = new StringBuffer();
                                for (CreditLine cl : c.getNewsLines().getCreditLine()) {
                                    for (Object clObj : cl.getContent()) {
                                        byLine.append(clObj);
                                    }
                                }
                                newswireItem.setAuthor(byLine.toString());

                                List<Object> objs = c.getNewsLines().getHeadLineAndSubHeadLine();
                                StringBuilder title = new StringBuilder();

                                for (Object o : objs) {
                                    if (o instanceof HeadLine) {
                                        HeadLine headline = (HeadLine) o;

                                        for (Object line : headline.getContent()) {
                                            title.append(line).append(" ");
                                        }
                                    }
                                }
                                newswireItem.setTitle(title.toString().trim());

                                List<NewsLine> newsLines = c.getNewsLines().getNewsLine();
                                StringBuilder summary = new StringBuilder();
                                for (NewsLine nl : newsLines) {
                                    for (NewsLineText line : nl.getNewsLineText()) {
                                        for (Object objLine : line.getContent()) {
                                            summary.append(objLine).append(" ");
                                        }
                                    }
                                }

                                newswireItem.setSummary(summary.toString().trim());

                                for (ContentItem ci : c.getContentItem()) {
                                    StringWriter sw = new StringWriter();
                                    StreamResult result = new StreamResult(sw);

                                    try {
                                        if (ci.getFormat().getFormalName().equalsIgnoreCase("XHTML")) {
                                            for (DataContent dc : ci.getDataContent()) {

                                                for (Object obj : dc.getAny()) {
                                                    Element element =
                                                            (Element) obj;
                                                    NodeList bodies =
                                                            element.getElementsByTagName(
                                                            "body");
                                                    if (bodies.getLength()
                                                            > 0) {
                                                        Node body =
                                                                bodies.item(0);
                                                        DOMSource src =
                                                                new DOMSource(
                                                                body);
                                                        transformer.transform(
                                                                src,
                                                                result);
                                                        String storyBody =
                                                                sw.toString();
                                                        newswireItem.addContent(storyBody.replaceAll(
                                                                "<\\?xml version=\"1.0\" encoding=\"UTF-8\"\\?>",
                                                                ""));
                                                    }
                                                }
                                            }
                                        }
                                    } catch (Exception ex) {
                                        LOG.log(Level.WARNING, ex.getMessage(), ex);
                                    }
                                }
                            } else if (c.getRole().getFormalName().
                                    equalsIgnoreCase("Main Picture") || c.getRole().getFormalName().
                                    equalsIgnoreCase("Main Graphic")) {


                                for (NewsComponent picComponent : c.getNewsComponent()) {
                                    String componentRole = picComponent.getRole().getFormalName();
                                    if (componentRole.equalsIgnoreCase("Picture Caption")) {

                                        StringBuilder byLine = new StringBuilder();
                                        for (CreditLine cl : c.getNewsLines().getCreditLine()) {
                                            for (Object clObj : cl.getContent()) {
                                                byLine.append(clObj);
                                            }
                                        }
                                        newswireItem.setAuthor(byLine.toString());


                                        List<Object> objs = c.getNewsLines().getHeadLineAndSubHeadLine();
                                        StringBuilder title = new StringBuilder();

                                        for (Object o : objs) {
                                            if (o instanceof HeadLine) {
                                                HeadLine headline = (HeadLine) o;

                                                for (Object line : headline.getContent()) {
                                                    title.append(line).append(" ");
                                                }
                                            }
                                        }
                                        newswireItem.setTitle(title.toString().trim());


//                                            List<NewsLine> newsLines = c.getNewsLines().getNewsLine();
//                                            StringBuilder summary = new StringBuilder();
//
//                                            for (NewsLine nl : newsLines) {
//                                                for (NewsLineText line : nl.getNewsLineText()) {
//                                                    for (Object objLine : line.getContent()) {
//                                                        summary.append(objLine).append(" ");
//                                                    }
//                                                }
//                                            }
//                                            for (ByLine bl : c.getNewsLines().getByLine()) {
//                                                for (Object objLine : bl.getContent()) {
//                                                    summary.append(objLine).append(" ");
//                                                }
//                                            }
//
//                                            newswireItem.setSummary(summary.toString().trim());

                                        for (ContentItem ci :
                                                picComponent.getContentItem()) {
                                            StringWriter sw =
                                                    new StringWriter();
                                            StreamResult result =
                                                    new StreamResult(sw);

                                            try {
                                                if (ci.getFormat().
                                                        getFormalName().
                                                        equalsIgnoreCase(
                                                        "XHTML")) {
                                                    for (DataContent dc :
                                                            ci.getDataContent()) {

                                                        for (Object obj :
                                                                dc.getAny()) {
                                                            Element element =
                                                                    (Element) obj;
                                                            NodeList bodies =
                                                                    element.getElementsByTagName(
                                                                    "body");
                                                            if (bodies.getLength()
                                                                    > 0) {
                                                                Node body =
                                                                        bodies.item(
                                                                        0);
                                                                DOMSource src =
                                                                        new DOMSource(
                                                                        body);
                                                                transformer.transform(
                                                                        src,
                                                                        result);
                                                                String storyBody =
                                                                        StringUtils.stripHtml(
                                                                        sw.toString());
                                                                newswireItem.addSummary(
                                                                        storyBody);
                                                            }
                                                        }
                                                    }
                                                }
                                            } catch (Exception ex) {
                                                LOG.log(Level.WARNING, ex.getMessage(), ex);
                                            }
                                        }
                                    } else if (componentRole.equalsIgnoreCase("Image Wrapper")) {
                                        String imgDir = this.properties.get(Property.PROPERTY_NEWSWIRE_LOCATION.name());
                                        for (ContentItem picContentItem : picComponent.getContentItem()) {
                                            if (picContentItem.getHref() != null) {
                                                for (dk.i2m.converge.nar.newsml.v1_0.Property p : picContentItem.getCharacteristics().getProperty()) {
                                                    if (p.getFormalName().equalsIgnoreCase("PicType")) {
                                                        NewswireItemAttachment attachment = new NewswireItemAttachment();
                                                        attachment.setNewswireItem(newswireItem);

                                                        File imgFile = new File(imgDir, picContentItem.getHref());

                                                        if (useCatalogue) {
                                                            attachment.setCatalogue(catalogue);
                                                            try {
                                                                attachment.setCataloguePath(pluginCtx.archive(imgFile, catalogue.getId(), imgFile.getName()));
                                                            } catch (ArchiveException ex) {
                                                                fileMissing = true;
                                                            }
                                                        } else {
                                                            try {
                                                                attachment.setData(FileUtils.getBytes(imgFile));
                                                            } catch (IOException ex) {
                                                                LOG.log(Level.SEVERE, null, ex);
                                                            }
                                                        }
                                                        attachment.setContentType("image/jpeg");
                                                        attachment.setDescription(p.getValue());
                                                        attachment.setFilename(picContentItem.getHref());
                                                        attachment.setSize(imgFile.length());

                                                        if (renditionMapping.containsKey(p.getValue())) {
                                                            String renditionId = renditionMapping.get(p.getValue());
                                                            Rendition rendition = pluginCtx.findRenditionByName(renditionId);
                                                            attachment.setRendition(rendition);
                                                        }

                                                        newswireItem.getAttachments().add(attachment);

                                                        if (p.getValue().equalsIgnoreCase("Thumbnail")) {
                                                            newswireItem.setThumbnailUrl(attachment.getCatalogueUrl());
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (fileMissing) {
                        LOG.log(Level.INFO, "Newswire file missing. Skipping newswire item");
                    } else {
                        NewswireItem nwi = pluginCtx.createNewswireItem(newswireItem);
                        try {
                            pluginCtx.index(nwi);
                        } catch (SearchEngineIndexingException seie) {
                            LOG.log(Level.WARNING, seie.getMessage());
                            LOG.log(Level.FINEST, "", seie);
                        }

                        if (moveProcessed) {
                            File newLocation = new File(processedDirectory, file.getName());
                            LOG.log(Level.INFO, "Moving {0} to {1}", new Object[]{file.getAbsolutePath(), newLocation.getAbsolutePath()});
                            file.renameTo(newLocation);
                        } else if (deleteProcessed) {
                            if (file.delete()) {
                                LOG.log(Level.INFO, "{0} deleted", new Object[]{file.getAbsolutePath()});
                            } else {
                                LOG.log(Level.WARNING, "{0} could not be deleted", new Object[]{file.getAbsolutePath()});
                            }
                        }
                    }
                }
            }
        } catch (JAXBException ex) {
            LOG.log(Level.WARNING, ex.getMessage(), ex);
        }
    }

    @Override
    public ResourceBundle getBundle() {
        return bundle;
    }

    class FileExtensionFilter implements FilenameFilter {

        private String extension;

        public FileExtensionFilter(String extension) {
            this.extension = extension;
        }

        @Override
        public boolean accept(File directory, String filename) {
            boolean fileOK = true;

            if (extension != null) {
                fileOK &= filename.toLowerCase().endsWith('.' + extension);
            }
            return fileOK;
        }
    }
}
