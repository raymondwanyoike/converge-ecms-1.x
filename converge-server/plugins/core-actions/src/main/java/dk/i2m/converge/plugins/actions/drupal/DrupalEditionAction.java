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
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Plug-in {@link EditionAction} for uploading {@link NewsItem}s to a Drupal instance.
 *
 * @author Raymond Wanyoike
 */
@OutletAction
public class DrupalEditionAction implements EditionAction {

    private enum Property {

        CONNECTION_TIMEOUT,
        IMAGE_RENDITION,
        NODE_LANGUAGE,
        NODE_TYPE,
        PASSWORD,
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

    private String publishDelay;

    private String publishImmediately;

    private String renditionName;

    private String undisclosedAuthor;

    private Map<Long, Long> sectionMapping;

    @Override
    public void execute(PluginContext ctx, Edition edition,
            OutletEditionAction action) {
        Map<String, String> properties = action.getPropertiesAsMap();

        String connectionTimeout = properties.get(Property.CONNECTION_TIMEOUT.
                name());
        String mappings = properties.get(Property.SECTION_MAPPING.name());
        String nodeType = properties.get(Property.NODE_TYPE.name());
        String nodeLanguage = properties.get(Property.NODE_LANGUAGE.name());
        String password = properties.get(Property.PASSWORD.name());
        String serviceEndpoint =
                properties.get(Property.SERVICE_ENDPOINT.name());
        String socketTimeout = properties.get(Property.SOCKET_TIMEOUT.name());
        String url = properties.get(Property.URL.name());
        String username = properties.get(Property.USERNAME.name());

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
        ur.setUsername(username);
        ur.setPassword(password);

        NodeResource nr = new NodeResource(client);
        FileResource fr = new FileResource(client);

        setSectionMapping(mappings);

        try {
            LOG.log(Level.INFO, "Trying to login");

            ur.connect();

            LOG.log(Level.INFO, "Found {0} News Item(s)", edition.
                    getNumberOfPlacements());

            for (NewsItemPlacement nip : edition.getPlacements()) {
                NewsItem newsItem = nip.getNewsItem();

                DrupalMessage nodeMessage = new DrupalMessage();
                nodeMessage.getFields().put("actor", getActor(newsItem));
                nodeMessage.getFields().put("body", getBody(newsItem));
                nodeMessage.getFields().put("converge_id", getConvergeId(
                        newsItem));
                nodeMessage.getFields().put("language", nodeLanguage);
                nodeMessage.getFields().put("promote", getPromoted(nip));
                nodeMessage.getFields().put("publish_on", getPublishOn());
                nodeMessage.getFields().put("section", getSection(nip));
                nodeMessage.getFields().put("status", getStatus());
                nodeMessage.getFields().put("title", getTitle(newsItem));
                nodeMessage.getFields().put("type", nodeType);

                List<ImageField> imageFields = getImageFields(newsItem);

                try {
                    for (ImageField imageField : imageFields) {
                        FileCreateMessage fcm = fr.create(imageField);
                        imageField.setFid(fcm.getFid());
                    }
                } catch (IOException ex) {
                    LOG.log(Level.SEVERE,
                            "> Uploading News Item #{0} image(s) failed",
                            newsItem.getId());

                    continue;
                }

                nodeMessage.getFields().put("image", getImage(imageFields));

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
                    LOG.log(Level.INFO,
                            "> Uploading News Item #{0} & {1} image(s)",
                            new Object[]{newsItem.getId(), imageFields.size()});

                    NodeCreateMessage ncm = nr.create(nodeMessage);

                    if (ncm == null) {
                        // TODO: Big error
                        throw new IOException("Null NodeCreateMessage");
                    }

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
     * Get Title text value.
     * 
     * @param newsItem {@link NewsItem}
     * @return
     */
    private String getTitle(NewsItem newsItem) {
        // < 254 in length
        return truncateString(newsItem.getTitle(), 254);
    }

    /**
     * Get Published checkbox value.
     * 
     * @return
     */
    private String getStatus() {
        if (publishImmediately != null) {
            return "1";
        } else {
            // see getPublishOn()
            return "0";
        }
    }

    /**
     * Get Publish on text value.
     * 
     * @return "YYYY-MM-DD HH:MM:SS" or ""
     */
    private String getPublishOn() {
        if (publishImmediately != null) {
            return "";
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Integer amount = Integer.valueOf(publishDelay);
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR_OF_DAY, amount);

        return sdf.format(calendar.getTime());
    }

    /**
     * Get Promoted to front page checkbox value.
     * 
     * @param placement {@link NewsItemPlacement}
     * @return
     */
    private String getPromoted(NewsItemPlacement placement) {
        if (placement.getStart() == 1) {
            return "1";
        } else {
            return "0";
        }
    }

    /**
     * Get <b>Image</b> image field.
     * 
     * @param imageFields {@link ImageField}
     * @return
     */
    private FieldModule getImage(List<ImageField> imageFields) {
        Map<String, Object> map = new HashMap<String, Object>();

        for (int i = 0; i < imageFields.size(); i++) {
            ImageField imageField = imageFields.get(i);

            // Null the file and contentType variables
            imageField.setContentType(null);
            imageField.setFile(null); // Causes a NPE in JSON if not null

            map.put(String.valueOf(i), imageFields.get(i));
        }

        return new FieldModule(map);
    }

    /**
     * Get Body text field.
     * 
     * @param newsItem {@link NewsItem}
     * @return
     */
    private FieldModule getBody(NewsItem newsItem) {
        TextField textField = new TextField(newsItem.getBrief(),
                cleanHTML(newsItem.getStory()), "html");

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("0", textField);

        return new FieldModule(map);
    }

    /**
     * Get Actor text field.
     * 
     * @param newsItem {@link NewsItem}
     * @return
     */
    private FieldModule getActor(NewsItem newsItem) {
        TextField textField;

        if (newsItem.isUndisclosedAuthor()) {
            textField = new TextField(null, undisclosedAuthor, null);
        } else {
            if (newsItem.getByLine().trim().isEmpty()) {
                StringBuilder sb = new StringBuilder();

                for (NewsItemActor actor : newsItem.getActors()) {
                    boolean firstActor = true;

                    // TODO: Document this
                    if (actor.getRole().equals(newsItem.getOutlet().
                            getWorkflow().getStartState().getActorRole())) {
                        if (!firstActor) {
                            sb.append(", ");
                        } else {
                            firstActor = false;
                        }

                        sb.append(actor.getUser().getFullName());
                    }
                }

                textField = new TextField(null, sb.toString(), null);
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
     * Get Converge ID text field.
     * 
     * @param newsItem {@link NewsItem}
     * @return
     */
    private FieldModule getConvergeId(NewsItem newsItem) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("0", new TextField(null, newsItem.getId().toString(), null));

        return new FieldModule(map);
    }

    /**
     * Get Section taxonomy field.
     * 
     * @param nip {@link NewsItemPlacement}
     * @return 
     */
    private FieldModule getSection(NewsItemPlacement nip) {
        Map<String, Object> map = new HashMap<String, Object>();
        Section section = nip.getSection();

        if (section != null) {
            if (sectionMapping.containsKey(section.getId())) {
                map.put("0", sectionMapping.get(section.getId()));
                return new FieldModule(map);
            }
        } else {
            // Use fallback section
            if (sectionMapping.containsKey(Long.parseLong("0"))) {
                map.put("0", sectionMapping.get(Long.parseLong("0")));
                return new FieldModule(map);
            }
        }

        LOG.log(Level.WARNING, "Section mapping failed for News Item #{0}",
                nip.getNewsItem().getId());

        return new FieldModule(map);
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
            String contentType = mir.getContentType();
            File file = new File(mir.getFileLocation());

            ImageField imageField = new ImageField(null, truncateString(
                    description, 511), title, contentType, file);

            imageFields.add(imageField);
        }

        return imageFields;
    }
}
