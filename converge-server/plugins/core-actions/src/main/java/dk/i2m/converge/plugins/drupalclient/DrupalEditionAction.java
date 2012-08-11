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
package dk.i2m.converge.plugins.drupalclient;

import dk.i2m.converge.core.annotations.OutletAction;
import dk.i2m.converge.core.content.NewsItem;
import dk.i2m.converge.core.content.NewsItemActor;
import dk.i2m.converge.core.content.NewsItemEditionState;
import dk.i2m.converge.core.content.NewsItemMediaAttachment;
import dk.i2m.converge.core.content.NewsItemPlacement;
import dk.i2m.converge.core.content.catalogue.MediaItem;
import dk.i2m.converge.core.content.catalogue.MediaItemRendition;
import dk.i2m.converge.core.content.catalogue.RenditionNotFoundException;
import dk.i2m.converge.core.plugin.EditionAction;
import dk.i2m.converge.core.plugin.PluginContext;
import dk.i2m.converge.core.workflow.Edition;
import dk.i2m.converge.core.workflow.OutletEditionAction;
import dk.i2m.converge.core.workflow.Section;
import dk.i2m.drupal.DefaultDrupalClient;
import dk.i2m.drupal.core.DrupalClient;
import dk.i2m.drupal.fields.Image;
import dk.i2m.drupal.fields.wrappers.BasicWrapper;
import dk.i2m.drupal.fields.wrappers.ImageWrapper;
import dk.i2m.drupal.fields.wrappers.ListWrapper;
import dk.i2m.drupal.fields.wrappers.TextWrapper;
import dk.i2m.drupal.resources.FileResource;
import dk.i2m.drupal.resources.NodeResource;
import dk.i2m.drupal.resources.UserResource;
import dk.i2m.drupal.response.FileResponce;
import dk.i2m.drupal.response.NodeResponse;
import dk.i2m.drupal.util.HttpMessageBuilder;
import java.io.File;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Raymond Wanyoike <rwa at i2m.dk>
 */
@OutletAction
public class DrupalEditionAction implements EditionAction {

    private enum Property {

        CONNECTION_TIMEOUT,
        FRONTPAGE_PLACEMENT,
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

    private static final Logger LOG = Logger.
            getLogger(DrupalEditionAction.class.getName());

    private static final String UPLOADING = "UPLOADING";

    private static final String UPLOADED = "UPLOADED";

    private static final String FAILED = "FAILED";

    private static final String DATE = "date";

    private static final String NID_LABEL = "nid";

    private static final String URI_LABEL = "uri";

    private static final String STATUS_LABEL = "status";

    private ResourceBundle bundle = ResourceBundle.getBundle(
            "dk.i2m.converge.plugins.drupalclient.Messages");

    private Map<String, String> availableProperties;

    private String publishDelay;

    private String publishImmediately;

    private String renditionName;

    private String undisclosedAuthor;

    private String frontpagePlacement;

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
        String endpoint = properties.get(Property.SERVICE_ENDPOINT.name());
        String socketTimeout = properties.get(Property.SOCKET_TIMEOUT.name());
        String hostname = properties.get(Property.URL.name());
        String username = properties.get(Property.USERNAME.name());

        publishDelay = properties.get(Property.PUBLISH_DELAY.name());
        publishImmediately = properties.get(Property.PUBLISH_IMMEDIATELY.name());
        renditionName = properties.get(Property.IMAGE_RENDITION.name());
        undisclosedAuthor = properties.get(Property.UNDISCLOSED_AUTHOR.name());
        frontpagePlacement = properties.get(Property.FRONTPAGE_PLACEMENT.name());
        sectionMapping = new HashMap<Long, Long>();

        if (hostname == null) {
            throw new IllegalArgumentException("'hostname' cannot be null");
        }

        if (endpoint == null) {
            throw new IllegalArgumentException("'endpoint' cannot be null");
        }

        if (username == null) {
            throw new IllegalArgumentException("'username' cannot be null");
        }

        if (password == null) {
            throw new IllegalArgumentException("'password' cannot be null");
        }

        if (nodeType == null) {
            throw new IllegalArgumentException("'nodeType' cannot be null");
        }

        if (mappings == null) {
            throw new IllegalArgumentException("'mappings' cannot be null");
        }

        if (publishImmediately == null && publishDelay == null) {
            throw new IllegalArgumentException(
                    "'publishImmediately' or 'publishDelay' cannot be null");
        }

        if (publishImmediately == null && publishDelay != null) {
            if (!isInteger(publishDelay)) {
                throw new IllegalArgumentException(
                        "'publishDelay' must be an integer");
            } else if (Integer.parseInt(publishDelay) <= 0) {
                throw new IllegalArgumentException(
                        "'publishDelay' cannot be <= 0");
            }
        }

        if (frontpagePlacement != null && !isInteger(frontpagePlacement)) {
            throw new IllegalArgumentException(
                    "'frontpagePlacement' must be an integer");
        }

        if (connectionTimeout == null) {
            connectionTimeout = "30000"; // 30 seconds
        } else if (!isInteger(connectionTimeout)) {
            throw new IllegalArgumentException(
                    "'connectionTimeout' must be an integer");
        }

        if (socketTimeout == null) {
            socketTimeout = "30000"; // 30 seconds
        } else if (!isInteger(socketTimeout)) {
            throw new IllegalArgumentException(
                    "'socketTimeout' must be an integer");
        }

        LOG.log(Level.INFO, "Starting action... Edition #{0}", edition.getId());

        setSectionMapping(mappings);

        DrupalClient dc =
                new DefaultDrupalClient(URI.create(hostname), endpoint, Integer.
                parseInt(connectionTimeout), Integer.parseInt(socketTimeout));
        UserResource ur = new UserResource(dc, username, password);
        FileResource fr = new FileResource(dc);
        NodeResource nr = new NodeResource(dc);

        int errors = 0;

        try {
            dc.setup();
            ur.login();

            LOG.log(Level.INFO, "Found {0} NewsItem(s)", edition.
                    getNumberOfPlacements());

            for (NewsItemPlacement nip : edition.getPlacements()) {
                HttpMessageBuilder fb = new HttpMessageBuilder();

                if (nodeLanguage != null) {
                    fb = new HttpMessageBuilder(nodeLanguage);
                }

                NewsItem newsItem = nip.getNewsItem();

                fb
                        .add(new BasicWrapper("title", getTitle(newsItem)))
                        .add(new BasicWrapper("type", nodeType))
                        .add(new TextWrapper("body", newsItem.getBrief(),
                        newsItem.getStory(), "full_html"))
                        .add(
                        new TextWrapper("field_author", getAuthor(newsItem)))
                        .add(new TextWrapper("field_newsitem", newsItem.getId().
                        toString()))
                        .add(new TextWrapper("field_edition", edition.getId().
                        toString()));

                if (getPromoted(nip) != null) {
                    fb.add(new BasicWrapper("promote", getPromoted(nip)));
                }

                if (getPublishOn() != null) {
                    fb.add(new BasicWrapper("publish_on", getPublishOn()));
                }

                if (getSection(nip) != null) {
                    fb.add(new ListWrapper("field_section", getSection(nip)));
                }

                if (nip.getStart() != null) {
                    fb.add(new TextWrapper("field_placement_start", nip.
                            getStart().toString()));
                }

                if (nip.getPosition() != null) {
                    fb.add(new TextWrapper("field_placement_position", nip.
                            getPosition().toString()));
                }

                List<MediaItem> mediaItems = getMediaItems(newsItem);

                try {
                    ImageWrapper imageWrapper = new ImageWrapper("field_image");

                    for (MediaItem mediaItem : mediaItems) {
                        MediaItemRendition mir = mediaItem.getOriginal();

                        try {
                            mir = mediaItem.findRendition(renditionName);
                        } catch (RenditionNotFoundException ex) {
                            LOG.log(Level.SEVERE,
                                    "Rendition ''{0}'' missing for MediaItem #{1} - NewsItem #{2}",
                                    new Object[]{renditionName, mediaItem.
                                        getId(), newsItem.getId()});
                        }

                        File file = new File(mir.getFileLocation());
                        String title = truncateString(mediaItem.getTitle(), 20);

                        FileResponce fileResponce = fr.createRaw(file, title);

                        String fid = fileResponce.getId().toString();
                        String alt = truncateString(mediaItem.getTitle(), 512);
                        String description = truncateString(mediaItem.
                                getDescription(), 1024);

                        imageWrapper.add(new Image(fid, alt, description));
                    }

                    if (!mediaItems.isEmpty()) {
                        fb.add(imageWrapper);
                    }
                } catch (Exception ex) {
                    LOG.log(Level.SEVERE,
                            "> Uploading NewsItem #{0} image(s) failed",
                            newsItem.getId());
                    LOG.log(Level.SEVERE, null, ex);

                    errors++;

                    if (errors > 4) {
                        break;
                    } else {
                        continue;
                    }
                }

                NewsItemEditionState status = ctx.
                        addNewsItemEditionState(edition.getId(), newsItem.
                        getId(), STATUS_LABEL, UPLOADING.toString());
                NewsItemEditionState nid = ctx.addNewsItemEditionState(edition.
                        getId(), newsItem.getId(), NID_LABEL, null);
                NewsItemEditionState uri = ctx.addNewsItemEditionState(edition.
                        getId(), newsItem.getId(), URI_LABEL, null);
                NewsItemEditionState submitted = ctx.
                        addNewsItemEditionState(edition.getId(), newsItem.
                        getId(), DATE, null);

                LOG.log(Level.INFO, "> Uploading NewsItem #{0} & {1} image(s)",
                        new Object[]{newsItem.getId(), mediaItems.size()});

                try {
                    NodeResponse nodeResponse = nr.create(fb.
                            toUrlEncodedFormEntity());

                    nid.setValue(nodeResponse.getId().toString());
                    uri.setValue(nodeResponse.getUri().toString());
                    submitted.setValue(new Date().toString());
                    status.setValue(UPLOADED.toString());
                } catch (Exception ex) {
                    status.setValue(FAILED.toString());
                    LOG.log(Level.SEVERE, null, ex);

                    ctx.updateNewsItemEditionState(status);
                    ctx.updateNewsItemEditionState(nid);
                    ctx.updateNewsItemEditionState(uri);
                    ctx.updateNewsItemEditionState(submitted);

                    errors++;

                    if (errors > 4) {
                        break;
                    } else {
                        continue;
                    }
                }

                ctx.updateNewsItemEditionState(status);
                ctx.updateNewsItemEditionState(nid);
                ctx.updateNewsItemEditionState(uri);
                ctx.updateNewsItemEditionState(submitted);
            }

            LOG.log(Level.INFO, "Encountered {0} error(s)", errors);

            ur.logout();
            dc.shutdown();
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, null, ex);
        }

        LOG.log(Level.INFO, "Finishing action... Edition #{0}", edition.getId());
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
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return sdf.parse(bundle.getString("PLUGIN_BUILD_TIME"));
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
            value = value.trim().substring(0, length - 3) + "...";
        }

        return value;
    }

    /**
     * Get Title text value.
     * 
     * @param newsItem {@link NewsItem}
     * @return
     */
    private String getTitle(NewsItem newsItem) {
        // Must be < 255 in length
        return truncateString(newsItem.getTitle(), 255);
    }

    /**
     * Get Publish on text value.
     * 
     * @return "YYYY-MM-DD HH:MM:SS" or ""
     */
    private String getPublishOn() {
        if (publishImmediately != null) {
            return null;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR_OF_DAY, Integer.valueOf(publishDelay));

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(calendar.getTime());
    }

    /**
     * Get Promoted to front page checkbox value.
     * 
     * @param placement {@link NewsItemPlacement}
     * @return
     */
    private String getPromoted(NewsItemPlacement placement) {
        if (frontpagePlacement == null) {
            return null;
        }

        if (placement.getStart() == Integer.parseInt(frontpagePlacement)) {
            return "1";
        } else {
            return null;
        }
    }

    /**
     * Get Author text field.
     * 
     * @param newsItem {@link NewsItem}
     * @return
     */
    private String getAuthor(NewsItem newsItem) {
        if (newsItem.isUndisclosedAuthor()) {
            return undisclosedAuthor;
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

                return sb.toString();
            } else {
                // Return the "by-line" of the NewsItem
                return newsItem.getByLine();
            }
        }
    }

    /**
     * Get Section taxonomy field.
     * 
     * @param nip {@link NewsItemPlacement}
     * @return 
     */
    private String getSection(NewsItemPlacement nip) {
        Section section = nip.getSection();

        if (section != null) {
            if (sectionMapping.containsKey(section.getId())) {
                return sectionMapping.get(section.getId()).toString();
            }
        } else {
            // Use fallback section
            if (sectionMapping.containsKey(Long.parseLong("0"))) {
                return sectionMapping.get(Long.parseLong("0")).toString();
            }
        }

        LOG.log(Level.WARNING, "Section mapping failed for NewsItem #{0}",
                nip.getNewsItem().getId());

        return null;
    }

    /**
     * Get {@link ImageField}s for {@link NewsItem}.
     * 
     * @param newsItem NewsItem
     * @return 
     */
    private List<MediaItem> getMediaItems(NewsItem newsItem) {
        List<MediaItem> mediaItems = new ArrayList<MediaItem>();

        for (NewsItemMediaAttachment nima : newsItem.getMediaAttachments()) {
            MediaItem mediaItem = nima.getMediaItem();

            // Verify that the item exist and any renditions are attached
            if (mediaItem == null || !mediaItem.isRenditionsAttached()) {
                continue;
            }

            mediaItems.add(mediaItem);
        }

        return mediaItems;
    }

    private boolean isInteger(String input) {
        try {
            Integer.parseInt(input);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
