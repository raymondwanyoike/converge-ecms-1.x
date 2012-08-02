/*
 * Copyright (C) 2012 Interactive Media Management
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.i2m.converge.plugins.actions.drupal;

import dk.i2m.converge.core.annotations.OutletAction;
import dk.i2m.converge.core.content.*;
import dk.i2m.converge.core.content.catalogue.MediaItem;
import dk.i2m.converge.core.content.catalogue.MediaItemRendition;
import dk.i2m.converge.core.content.catalogue.RenditionNotFoundException;
import dk.i2m.converge.core.plugin.EditionAction;
import dk.i2m.converge.core.plugin.PluginContext;
import dk.i2m.converge.core.workflow.Edition;
import dk.i2m.converge.core.workflow.OutletEditionAction;
import dk.i2m.converge.core.workflow.Section;
import dk.i2m.converge.plugins.actions.drupal.client.*;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.ArrayUtils;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

/**
 * Plug-in {@link EditionAction} for uploading {@link NewsItem}s to a Drupal instance.
 *
 * @author Raymond Wanyoike
 */
@OutletAction
public class DrupalEditionAction implements EditionAction {

    private enum Property {

        CONNECTION_TIMEOUT,
        EXCLUDE_MEDIA_TYPES,
        IMAGE_RENDITION,
        NODE_TYPE,
        NODE_LANGUAGE,
        PASSWORD,
        PROMOTE_TO_FRONT_PAGE,
        PUBLISH_DELAY,
        PUBLISH_IMMEDIATELY,
        SECTION_MAPPING,
        SERVICE_ENDPOINT,
        SOCKET_TIMEOUT,
        UNDISCLOSED_AUTHOR,
        URL,
        USERNAME
    }

    private static final Logger LOG = Logger.getLogger("DrupalEditionAction");

    private static final String FAILED = "FAILED";

    private static final String SUBMITTED = "SUBMITTED";

    private static final String UPLOADED = "UPLOADED";

    private static final String UPLOADING = "UPLOADING";

    private static final String NID = "nid";

    private static final String STATUS = "status";

    private static final String URI = "uri";

    private ResourceBundle bundle = ResourceBundle.getBundle(
            "dk.i2m.converge.plugins.actions.drupal.Messages");

    private Map<String, String> availableProperties;

    private String promoteToFrontPage;

    private String publishDelay;

    private String publishImmediately;

    private String renditionName;

    private String undisclosedAuthor;

    private String[] excludeMediaTypes;

    private Map<Long, Long> sectionMapping;

    @Override
    public void execute(PluginContext ctx, Edition edition,
            OutletEditionAction action) {
        Map<String, String> properties = action.getPropertiesAsMap();

        String connectionTimeout = properties.get(Property.CONNECTION_TIMEOUT.
                name());
        String excludeContentTypes =
                properties.get(Property.EXCLUDE_MEDIA_TYPES.name());
        String mappings = properties.get(Property.SECTION_MAPPING.name());
        String nodeType = properties.get(Property.NODE_TYPE.name());
        String nodeLanguage = properties.get(Property.NODE_LANGUAGE.name());
        String password = properties.get(Property.PASSWORD.name());
        String serviceEndpoint =
                properties.get(Property.SERVICE_ENDPOINT.name());
        String socketTimeout = properties.get(Property.SOCKET_TIMEOUT.name());
        String url = properties.get(Property.URL.name());
        String username = properties.get(Property.USERNAME.name());

        promoteToFrontPage = properties.get(
                Property.PROMOTE_TO_FRONT_PAGE.name());
        publishDelay = properties.get(Property.PUBLISH_DELAY.name());
        publishImmediately = properties.get(Property.PUBLISH_IMMEDIATELY.name());
        renditionName = properties.get(Property.IMAGE_RENDITION.name());
        undisclosedAuthor = properties.get(Property.UNDISCLOSED_AUTHOR.name());
        sectionMapping = new HashMap<Long, Long>();

        if (url == null) {
            throw new NullPointerException("'Url' is null");
        }

        if (serviceEndpoint == null) {
            throw new NullPointerException("'Service Endpoint' is null");
        }

        if (username == null) {
            throw new NullPointerException("'Username' is null");
        }

        if (password == null) {
            throw new NullPointerException("'Password' is null");
        }

        if (nodeType == null) {
            throw new NullPointerException("'Node Type' is null");
        }

        if (mappings == null) {
            throw new NullPointerException("'Section Mapping' is null");
        }

        if (publishImmediately == null && publishDelay == null) {
            throw new NullPointerException(
                    "'Publish Immediately' or 'Publish Delay' is null");
        }

        if (publishImmediately == null && publishDelay != null) {
            if (Integer.parseInt(publishDelay) <= 0) {
                throw new IllegalArgumentException(
                        "'Publish Delay' cannot be <= 0");
            }
        }

        if (connectionTimeout == null) {
            connectionTimeout = "30000"; // 30 seconds
        }

        if (nodeLanguage == null) {
            nodeLanguage = "und";
        }

        if (socketTimeout == null) {
            socketTimeout = "30000"; // 30 seconds
        }

        DrupalClient client = new DrupalClient(url, serviceEndpoint);
        client.setSocketTimeout(Integer.parseInt(socketTimeout));
        client.setConnectionTimeout(Integer.parseInt(connectionTimeout));

        LOG.log(Level.INFO, "Starting action... Edition #{0}", edition.getId());

        client.setup();

        UserResource ur = new UserResource(client);
        NodeResource nr = new NodeResource(client);
        FileResource fr = new FileResource(client);

        ur.setUsername(username);
        ur.setPassword(password);

        setSectionMapping(mappings);
        setExcludeMediaTypes(excludeContentTypes);

        try {
            LOG.log(Level.INFO, "Trying to login");

            ur.connect();

            LOG.log(Level.INFO, "Found {0} News Item(s)", edition.
                    getNumberOfPlacements());

            for (NewsItemPlacement nip : edition.getPlacements()) {
                NewsItem newsItem = nip.getNewsItem();

                DrupalMessage nodeMessage = new DrupalMessage();
                nodeMessage.getFields().put("actor", getActor(newsItem));
                nodeMessage.getFields().put("converge_id", getConvergeId(newsItem));
                nodeMessage.getFields().put("body", getBody(newsItem));
                nodeMessage.getFields().put("language", nodeLanguage);
                nodeMessage.getFields().put("publish_on", getPublishOn());
                nodeMessage.getFields().put("section", getSection(nip));
                nodeMessage.getFields().put("status", getStatus());
                nodeMessage.getFields().put("promote", getPromote(nip));
                nodeMessage.getFields().put("title", getTitle(newsItem));
                nodeMessage.getFields().put("type", nodeType);

                NewsItemEditionState status =
                        ctx.addNewsItemEditionState(edition.getId(), newsItem.
                        getId(), STATUS, UPLOADING.toString());
                NewsItemEditionState nid = ctx.addNewsItemEditionState(edition.
                        getId(), newsItem.getId(), NID, null);
                NewsItemEditionState uri = ctx.addNewsItemEditionState(edition.
                        getId(), newsItem.getId(), URI, null);
                NewsItemEditionState submitted =
                        ctx.addNewsItemEditionState(edition.getId(), newsItem.
                        getId(), SUBMITTED, null);

                try {
                    List<MediaItemRendition> renditions = getMedia(newsItem);

                    LOG.log(Level.INFO,
                            "> Uploading News Item #{0} & {1} image(s)",
                            new Object[]{newsItem.getId(), renditions.size()});

                    ArrayList<FileCreateMessage> msgs = fr.create(renditions);
                    nodeMessage.getFields().put("image", getImage(msgs));

                    NodeCreateMessage ncm = nr.create(nodeMessage);

                    nid.setValue(ncm.getNid().toString());
                    uri.setValue(ncm.getUri().toString());
                    submitted.setValue(new Date().toString());
                    status.setValue(UPLOADED.toString());
                } catch (IOException ex) {
                    status.setValue(FAILED.toString());
                    LOG.log(Level.SEVERE, null, ex);
                }

                ctx.updateNewsItemEditionState(status);
                ctx.updateNewsItemEditionState(nid);
                ctx.updateNewsItemEditionState(uri);
                ctx.updateNewsItemEditionState(submitted);
            }

            LOG.log(Level.INFO, "Trying to logout");

            ur.disconnect();
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }

        LOG.log(Level.INFO, "Finishing action... Edition #{0}", edition.getId());

        client.close();
    }

    @Override
    public void executePlacement(PluginContext ctx, NewsItemPlacement placement,
            Edition edition, OutletEditionAction action) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isSupportEditionExecute() {
        return true;
    }

    @Override
    public boolean isSupportPlacementExecute() {
        return false;
    }

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
    public String getName() {
        return bundle.getString("PLUGIN_NAME");
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
            String pattern = "yyyy-MM-dd HH:mm:ss";
            SimpleDateFormat format = new SimpleDateFormat(pattern);
            return format.parse(bundle.getString("PLUGIN_BUILD_TIME"));
        } catch (Exception e) {
            return new Date();
        }
    }

    @Override
    public ResourceBundle getBundle() {
        return bundle;
    }

    @Override
    public String getAbout() {
        return bundle.getString("PLUGIN_ABOUT");
    }

    private void setSectionMapping(String mapping) {
        String[] values = mapping.split(";");

        LOG.log(Level.INFO, "Found {0} Section mapping(s)", values.length);

        for (int i = 0; i < values.length; i++) {
            String[] value = values[i].split(":");

            Long convergeId = Long.valueOf(value[0].trim());
            Long drupalId = Long.valueOf(value[1].trim());
            sectionMapping.put(convergeId, drupalId);
        }
    }

    private void setExcludeMediaTypes(String mapping) {
        if (mapping == null) {
            return;
        }

        String[] values = mapping.split(";");
        excludeMediaTypes = values;

        LOG.log(Level.INFO, "Found {0} excluded media type(s)", values.length);
    }

    /**
     * Truncate text.
     * 
     * @param value text
     * @param length length to truncate
     * @return truncated text
     */
    private String truncateString(String value, int length) {
        if (value != null && value.length() > length) {
            // TODO: Log this
            value = value.trim().substring(0, length);
        }

        return value;
    }

    /**
     * Get safe HTML from untrusted input HTML.
     * 
     * @param html input untrusted HTML
     * @return safe HTML
     */
    private String cleanHTML(String html) {
        // return Jsoup.clean(html, Whitelist.relaxed());
        return html;
    }

    /**
     * Get <b>Title</b> text value.
     * 
     * @param newsItem NewsItem
     * @return (< 254 in length) title
     */
    private String getTitle(NewsItem newsItem) {
        return truncateString(newsItem.getTitle(), 254);
    }

    /**
     * Get <b>Published</b> checkbox value.
     * 
     * @return 1 = checked, 0 otherwise
     */
    private String getStatus() {
        if (publishImmediately != null) {
            return "1";
        }

        // Set to unpublished, see getPublishOn()
        return "0";
    }

    /**
     * Get <b>Publish on</b> text value.
     * 
     * @return "YYYY-MM-DD HH:MM:SS", or ""
     */
    private String getPublishOn() {
        if (publishImmediately != null) {
            return "";
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // Get the post delay property
        Integer amount = Integer.valueOf(publishDelay);
        Calendar calendar = Calendar.getInstance();
        // Add post delay to current time
        calendar.add(Calendar.HOUR_OF_DAY, amount);

        // Return future time
        return sdf.format(calendar.getTime());
    }

    /**
     * Get <b>Promoted to front page</b> checkbox value.
     * 
     * @param placement placement of the {@link NewsItem}
     * @return 1 = checked, 0 otherwise
     */
    private String getPromote(NewsItemPlacement placement) {
        if (promoteToFrontPage != null) {
            // Get the NewsItemPlacement's' start page
            String start = String.valueOf(placement.getStart());

            // Check if the start page matches the FRONT_PAGE_MAPPING
            if (promoteToFrontPage.equalsIgnoreCase(start)) {
                return "1";
            }
        }

        // Default
        return "0";
    }

    /**
     * Get <b>Image</b> image field.
     * 
     * @param msgs ?
     * @return image image field
     */
    private FieldModule getImage(ArrayList<FileCreateMessage> msgs) {
        Map<String, Object> map = new HashMap<String, Object>();

        for (int i = 0; i < msgs.size(); i++) {
            ImageField imageField = new ImageField(msgs.get(i).
                    getFid(), 1, "", "");
            map.put(String.valueOf(i), imageField);
        }

        return new FieldModule(map);
    }

    /**
     * Get <b>Body</b> text field.
     * 
     * @param newsItem NewsItem
     * @return body text field
     */
    private FieldModule getBody(NewsItem newsItem) {
        TextField textField = new TextField(newsItem.getBrief(),
                cleanHTML(newsItem.getStory()), "html");

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("0", textField);

        return new FieldModule(map);
    }

    /**
     * Get <b>Actor</b> text field.
     * 
     * @param newsItem NewsItem
     * @return actor text field
     */
    private FieldModule getActor(NewsItem newsItem) {
        TextField textField;

        if (newsItem.isUndisclosedAuthor()) {
            textField = new TextField(null, undisclosedAuthor, null);
        } else {
            if (newsItem.getByLine().trim().isEmpty()) {
                StringBuilder by = new StringBuilder();

                // Iterate over all the actors
                for (NewsItemActor actor : newsItem.getActors()) {
                    boolean firstActor = true;

                    // TODO: Document
                    if (actor.getRole().equals(newsItem.getOutlet().
                            getWorkflow().
                            getStartState().getActorRole())) {
                        if (!firstActor) {
                            by.append(", ");
                        } else {
                            firstActor = false;
                        }

                        by.append(actor.getUser().getFullName());
                    }
                }

                textField = new TextField(null, by.toString(), null);
            } else {
                // Return the "by-line" of the NewsItem
                textField = new TextField(null, newsItem.getByLine(), null);
            }
        }

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("0", textField);

        return new FieldModule(map);
    }

    /**
     * Get <b>Converge ID</b> text field.
     * 
     * @param newsItem NewsItem
     * @return converge_id text field
     */
    private FieldModule getConvergeId(NewsItem newsItem) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("0", new TextField(null, newsItem.getId().toString(), null));

        return new FieldModule(map);
    }
    
    /**
     * Get <b>Section</b> taxonomy field.
     * 
     * @param newsItem NewsItemPlacement
     * @return section taxonomy field
     */
    private FieldModule getSection(NewsItemPlacement placement) {
        Map<String, Object> map = new HashMap<String, Object>();

        Section section = placement.getSection();

        if (section != null) {
            if (sectionMapping.containsKey(section.getId())) {
                map.put("0", sectionMapping.get(section.getId()));
                return new FieldModule(map);
            }
        }

        if (sectionMapping.containsKey(Long.parseLong("0"))) {
            map.put("0", sectionMapping.get(Long.parseLong("0")));
            return new FieldModule(map);
        }

        NewsItem newsItem = placement.getNewsItem();

        LOG.log(Level.WARNING, "Section mapping failed for News Item #{0}",
                newsItem.getId());

        return new FieldModule(map);
    }

    private List<MediaItemRendition> getMedia(NewsItem newsItem) {
        List<MediaItemRendition> renditions =
                new ArrayList<MediaItemRendition>();

        for (NewsItemMediaAttachment attachment : newsItem.getMediaAttachments()) {
            MediaItem item = attachment.getMediaItem();

            // Verify that the item exist and renditions are attached
            if (item == null || !item.isRenditionsAttached()) {
                continue;
            }

            MediaItemRendition rendition;

            try {
                if (renditionName != null) {
                    rendition = item.findRendition(renditionName);
                } else {
                    rendition = item.getOriginal();
                }
            } catch (RenditionNotFoundException ex) {
                LOG.log(Level.WARNING,
                        "Rendition ({0}) missing for Media Item #{1}",
                        new Object[]{renditionName, item.getId()});

                rendition = item.getOriginal();
            }

            // Check if the file should be excluded
            if (ArrayUtils.contains(excludeMediaTypes, rendition.
                    getContentType())) {
                continue;
            }

            String filename = newsItem.getId() + "-" + rendition.getId() + "."
                    + rendition.getExtension();
            renditions.add(rendition);
        }

        return renditions;
    }

    /**
     * Get {@link ImageField}s for {@link NewsItem}.
     * 
     * @param newsItem NewsItem
     * @return 
     */
    private List<ImageField> getImageFields(NewsItem newsItem) {
        List<ImageField> imageFields = new ArrayList<ImageField>();

        for (NewsItemMediaAttachment nima : newsItem.getMediaAttachments()) {
            MediaItem mediaItem = nima.getMediaItem();

            // Verify that the item exist and any renditions are attached
            if (mediaItem == null || !mediaItem.isRenditionsAttached()) {
                continue;
            }

            MediaItemRendition mir;

            try {
                if (renditionName != null) {
                    mir = mediaItem.findRendition(renditionName);
                } else {
                    mir = mediaItem.getOriginal();
                }
            } catch (RenditionNotFoundException ex) {
                mir = mediaItem.getOriginal();

                LOG.log(Level.SEVERE,
                        "Rendition ({0}) missing for Media Item #{1} - News Item #{2}",
                        new Object[]{renditionName, mediaItem.getId(), newsItem.
                            getId()});
            }

            // Verify that the item is an image
            if (!mir.isImage()) {
                continue;
            }

            String title = mediaItem.getTitle();
            String description = mediaItem.getDescription();
            File file = new File(mir.getFileLocation());

            ImageField imageField = new ImageField(null, description, title,
                    file);

            imageFields.add(imageField);
        }

        return imageFields;
    }
}
