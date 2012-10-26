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
import dk.i2m.drupal.field.wrapper.BasicWrapper;
import dk.i2m.drupal.field.wrapper.ImageWrapper;
import dk.i2m.drupal.field.wrapper.ListWrapper;
import dk.i2m.drupal.field.wrapper.TextWrapper;
import dk.i2m.drupal.message.FileMessage;
import dk.i2m.drupal.message.NodeMessage;
import dk.i2m.drupal.resource.FileResource;
import dk.i2m.drupal.resource.UserResource;
import dk.i2m.drupal.util.HttpMessageBuilder;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.client.HttpResponseException;

/**
 *
 * @author Raymond Wanyoike <rwa at i2m.dk>
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
        IGNORED_MAPPING,
        SERVICE_ENDPOINT,
        SOCKET_TIMEOUT,
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

    private Map<Long, Long> sectionMapping;

    private List<String> ignoredMapping;

    private DrupalClient dc;

    private UserResource ur;

    private FileResource fr;

    private NodeResourceFix nr;

    private NewsItemResource nir;

    private SimpleDateFormat sdf;

    private String publishDelay;

    private String publishImmediately;

    private String renditionName;

    private String nodeLanguage;

    private String nodeType;

    private String mappings;

    private String ignoredMappings;

    private String date;

    private Long nodeId;

    @Override
    public void execute(PluginContext ctx, Edition edition,
            OutletEditionAction action) {
        LOG.log(Level.INFO, "Starting action... Edition #{0}", edition.getId());

        try {
            setupPlugin(action);
            int errors = 0;
            date = sdf.format(edition.getPublicationDate().getTime());
            String publishOn = getPublishOn(edition);
            boolean update = false;

            dc.setup();
            ur.login();

            LOG.log(Level.INFO, "Found {0} NewsItem(s)", edition.
                    getNumberOfPlacements());

            for (NewsItemPlacement nip : edition.getPlacements()) {
                NewsItem newsItem = nip.getNewsItem();

                if (!newsItem.isEndState()) {
                    continue;
                } else if (getSection(nip) != null) {
                    if (ignoredMapping.contains(nip.getSection().getId().
                            toString())) {
                        continue;
                    }
                }

                try {
                    update = newsItemExists(nir, newsItem);
                } catch (Exception ex) {
                    LOG.log(Level.SEVERE, "Retrieving NewsItem #{0} failed",
                            newsItem.getId());
                    LOG.log(Level.SEVERE, null, ex);

                    if (errors++ > 4) {
                        break;
                    } else {
                        continue;
                    }
                }

                HttpMessageBuilder fb = (nodeLanguage != null
                        ? new HttpMessageBuilder(nodeLanguage)
                        : new HttpMessageBuilder());
                fb = prepareHttpMessage(edition, nip, fb);

                if (!update && publishOn != null) {
                    fb.add(new BasicWrapper("publish_on", publishOn));
                }

                List<MediaItem> mediaItems = getMediaItems(newsItem);

                if (update) {
                    LOG.log(Level.INFO,
                            "Updating Node #{0} with NewsItem #{1} & {2} image(s)",
                            new Object[]{nodeId, newsItem.getId(), mediaItems.
                                size()});

                    try {
                        NodeMessage response = nr.update(nodeId, fb.
                                toUrlEncodedFormEntity());

                        if (!mediaItems.isEmpty()) {
                            deleteNodeFiles();

                            try {
                                Map<File, Map<String, String>> files =
                                        processMediaItems(mediaItems);
                                nr.attachFilesFix(response.getId(), files,
                                        "field_image", false);
                            } catch (Exception ex) {
                                LOG.log(Level.SEVERE,
                                        "Uploading NewsItem #{0} image(s) failed",
                                        newsItem.getId());
                                LOG.log(Level.SEVERE, null, ex);
                            }
                        }
                    } catch (Exception ex) {
                        LOG.log(Level.SEVERE, null, ex);

                        if (errors++ > 4) {
                            break;
                        } else {
                            continue;
                        }
                    }
                } else {
                    LOG.log(Level.INFO,
                            "Uploading NewsItem #{0} & {1} image(s)",
                            new Object[]{newsItem.getId(), mediaItems.size()});

                    NewsItemEditionState status = ctx.
                            addNewsItemEditionState(edition.getId(), newsItem.
                            getId(), STATUS_LABEL, UPLOADING.toString());
                    NewsItemEditionState nid = ctx.
                            addNewsItemEditionState(edition.
                            getId(), newsItem.getId(), NID_LABEL, null);
                    NewsItemEditionState uri = ctx.
                            addNewsItemEditionState(edition.
                            getId(), newsItem.getId(), URI_LABEL, null);
                    NewsItemEditionState submitted = ctx.
                            addNewsItemEditionState(edition.getId(), newsItem.
                            getId(), DATE, null);

                    try {
                        NodeMessage response = nr.create(fb.
                                toUrlEncodedFormEntity());

                        if (!mediaItems.isEmpty()) {
                            try {
                                Map<File, Map<String, String>> files =
                                        processMediaItems(mediaItems);
                                nr.attachFilesFix(response.getId(), files,
                                        "field_image", false);
                            } catch (Exception ex) {
                                LOG.log(Level.SEVERE,
                                        "Uploading NewsItem #{0} image(s) failed",
                                        newsItem.getId());
                                LOG.log(Level.SEVERE, null, ex);
                            }
                        }

                        nid.setValue(response.getId().toString());
                        uri.setValue(response.getUri().toString());
                        submitted.setValue(new Date().toString());
                        status.setValue(UPLOADED.toString());
                    } catch (Exception ex) {
                        status.setValue(FAILED.toString());
                        LOG.log(Level.SEVERE, null, ex);

                        ctx.updateNewsItemEditionState(status);
                        ctx.updateNewsItemEditionState(nid);
                        ctx.updateNewsItemEditionState(uri);
                        ctx.updateNewsItemEditionState(submitted);

                        if (errors++ > 4) {
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
        LOG.log(Level.INFO, "Starting action... Edition #{0}", edition.getId());

        try {
            setupPlugin(action);
            NewsItem newsItem = placement.getNewsItem();

            if (!newsItem.isEndState()) {
                return;
            } else if (getSection(placement) != null) {
                if (ignoredMapping.contains(placement.getSection().getId().
                        toString())) {
                    return;
                }
            }

            date = sdf.format(edition.getPublicationDate().getTime());

            dc.setup();
            ur.login();

            boolean update = newsItemExists(nir, newsItem);
            HttpMessageBuilder fb = (nodeLanguage != null
                    ? new HttpMessageBuilder(nodeLanguage)
                    : new HttpMessageBuilder());
            fb = prepareHttpMessage(edition, placement, fb);
            String publishOn = getPublishOn(edition);

            if (!update && publishOn != null) {
                fb.add(new BasicWrapper("publish_on", publishOn));
            }

            List<MediaItem> mediaItems = getMediaItems(newsItem);

            if (update) {
                LOG.log(Level.INFO,
                        "Updating Node #{0} with NewsItem #{1} & {2} image(s)",
                        new Object[]{nodeId, newsItem.getId(), mediaItems.size()});
            } else {
                LOG.log(Level.INFO, "Uploading NewsItem #{0} & {1} image(s)",
                        new Object[]{newsItem.getId(), mediaItems.size()});
            }

            try {
                NodeMessage response = (update ? nr.update(nodeId, fb.
                        toUrlEncodedFormEntity()) : nr.create(fb.
                        toUrlEncodedFormEntity()));

                if (!mediaItems.isEmpty()) {
                    if (update) {
                        deleteNodeFiles();
                    }

                    try {
                        Map<File, Map<String, String>> files =
                                processMediaItems(mediaItems);
                        nr.attachFilesFix(response.getId(), files,
                                "field_image", false);
                    } catch (Exception ex) {
                        LOG.log(Level.SEVERE,
                                "Uploading NewsItem #{0} image(s) failed",
                                newsItem.getId());
                        LOG.log(Level.SEVERE, null, ex);
                    }
                }
            } catch (Exception ex) {
                LOG.log(Level.SEVERE, null, ex);
            }

            ur.logout();
            dc.shutdown();
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, null, ex);
        }

        LOG.log(Level.INFO, "Finishing action... Edition #{0}", edition.getId());
    }

    private Map<File, Map<String, String>> processMediaItems(
            List<MediaItem> mediaItems) throws Exception {
        Map<File, Map<String, String>> files =
                new LinkedHashMap<File, Map<String, String>>();

        for (MediaItem mediaItem : mediaItems) {
            MediaItemRendition mir = null;

            try {
                mir = mediaItem.findRendition(renditionName);
            } catch (RenditionNotFoundException ex) {
                LOG.log(Level.INFO,
                        "Rendition ''{0}'' missing for MediaItem #{1}",
                        new Object[]{renditionName, mediaItem.getId()});
                continue;
            }

            File file = new File(mir.getFileLocation());
            String name = truncateString(mediaItem.getTitle(), 20);
            String alt = truncateString(mediaItem.getTitle(), 512);
            String title = truncateString(mediaItem.getDescription(), 1024);

            Map<String, String> values = new HashMap<String, String>();
            values.put("name", name);
            values.put("alt", alt);
            values.put("title", title);

            files.put(file, values);
        }

        return files;
    }

    @Override
    public boolean isSupportEditionExecute() {
        return true;
    }

    @Override
    public boolean isSupportPlacementExecute() {
        return true;
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
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").
                    parse(bundle.getString("PLUGIN_BUILD_TIME"));
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

    /**
     * Set the section mapping.
     *
     * @param mapping mapping to set.
     */
    private void setSectionMapping(String mapping) {
        String[] values = mapping.split(";");

        for (int i = 0; i < values.length; i++) {
            String[] value = values[i].split(":");
            Long convergeId = Long.valueOf(value[0].trim());
            Long drupalId = Long.valueOf(value[1].trim());
            sectionMapping.put(convergeId, drupalId);
        }

        LOG.log(Level.INFO, "Found {0} Section mapping(s)", sectionMapping.
                size());
    }

    /**
     * Set the ignored mapping.
     *
     * @param mapping mapping to set.
     */
    private void setIgnoredMapping(String mapping) {
        if (mapping != null) {
            ignoredMapping = Arrays.asList(mapping.split(";"));
        }

        LOG.log(Level.INFO, "Found {0} Ignored mapping(s)", ignoredMapping.
                size());
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
    private String getPublishOn(Edition edition) {
        if (publishImmediately != null) {
            return null;
        }

        Calendar calendar = (Calendar) edition.getPublicationDate().clone();
        calendar.add(Calendar.HOUR_OF_DAY, Integer.valueOf(publishDelay));

        return sdf.format(calendar.getTime());
    }

    /**
     * Get Author text field.
     *
     * @param newsItem {@link NewsItem}
     * @return
     */
    private String getAuthor(NewsItem newsItem) {
        if (newsItem.isUndisclosedAuthor()) {
            return "N/A";
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
     * Return {@link Section} Drupal mapping.
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

    /**
     * Prepare HTTP message to send.
     *
     * @param edition {@link Edition}
     * @param nip {@link NewsItemPlacement}
     * @param fb {@link HttpMessageBuilder} to build
     * @return prepared {@link HttpMessageBuilder}
     */
    private HttpMessageBuilder prepareHttpMessage(Edition edition,
            NewsItemPlacement nip, HttpMessageBuilder fb) {
        NewsItem newsItem = nip.getNewsItem();

        fb.add(new BasicWrapper("type", nodeType));
        fb.add(new BasicWrapper("date", date));
        fb.add(new BasicWrapper("title", getTitle(newsItem)));
        fb.add(new TextWrapper("body", newsItem.getBrief(), newsItem.getStory(),
                "full_html"));
        fb.add(new TextWrapper("field_author", getAuthor(newsItem)));
        fb.add(new TextWrapper("field_newsitem", newsItem.getId().toString()));
        fb.add(new TextWrapper("field_edition", edition.getId().toString()));

        if (getSection(nip) != null) {
            fb.add(new ListWrapper("field_section", getSection(nip)));
        }

        if (nip.getStart() != null) {
            fb.add(new TextWrapper("field_placement_start", nip.getStart().
                    toString()));
        }

        if (nip.getPosition() != null) {
            fb.add(new TextWrapper("field_placement_position",
                    nip.getPosition().toString()));
        }

        return fb;
    }

    /**
     * Returns true if a news item exists.
     *
     * @param nir {@link NewsItemResource} to use
     * @param newsItem {@link NewsItem} to check
     * @return true if exists
     * @throws HttpResponseException
     * @throws IOException
     */
    private boolean newsItemExists(NewsItemResource nir, NewsItem newsItem)
            throws HttpResponseException, IOException {
        try {
            NodeMessage nodeMessage = nir.retrieve(newsItem.getId());

            if (nodeMessage == null) {
                return false;
            } else {
                nodeId = nodeMessage.getId();
                return true;
            }
        } catch (HttpResponseException ex) {
            if (ex.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                return false;
            } else {
                throw ex;
            }
        }
    }

    /**
     * Delete all of a node's files.
     *
     * @throws HttpResponseException
     * @throws IOException
     */
    private void deleteNodeFiles() throws HttpResponseException, IOException {
        List<FileMessage> fileMessages = nr.loadFiles(nodeId);

        for (FileMessage fileMessage : fileMessages) {
            fr.delete(fileMessage.getId());
        }
    }

    /**
     * Setup the plugin.
     *
     * @param action {@link OutletEditionAction}
     */
    private void setupPlugin(OutletEditionAction action) {
        Map<String, String> properties = action.getPropertiesAsMap();

        mappings = properties.get(Property.SECTION_MAPPING.name());
        ignoredMappings = properties.get(Property.IGNORED_MAPPING.name());
        nodeType = properties.get(Property.NODE_TYPE.name());
        nodeLanguage = properties.get(Property.NODE_LANGUAGE.name());
        publishDelay = properties.get(Property.PUBLISH_DELAY.name());
        publishImmediately = properties.get(Property.PUBLISH_IMMEDIATELY.name());
        renditionName = properties.get(Property.IMAGE_RENDITION.name());

        sectionMapping = new HashMap<Long, Long>();
        ignoredMapping = new ArrayList<String>();

        String hostname = properties.get(Property.URL.name());
        String endpoint = properties.get(Property.SERVICE_ENDPOINT.name());
        String username = properties.get(Property.USERNAME.name());
        String password = properties.get(Property.PASSWORD.name());
        String connectionTimeout = properties.get(Property.CONNECTION_TIMEOUT.
                name());
        String socketTimeout = properties.get(Property.SOCKET_TIMEOUT.name());

        if (hostname == null) {
            throw new IllegalArgumentException("'hostname' cannot be null");
        } else if (endpoint == null) {
            throw new IllegalArgumentException("'endpoint' cannot be null");
        } else if (username == null) {
            throw new IllegalArgumentException("'username' cannot be null");
        } else if (password == null) {
            throw new IllegalArgumentException("'password' cannot be null");
        }

        if (nodeType == null) {
            throw new IllegalArgumentException("'nodeType' cannot be null");
        } else if (mappings == null) {
            throw new IllegalArgumentException("'mappings' cannot be null");
        }

        if (publishImmediately == null && publishDelay == null) {
            throw new IllegalArgumentException(
                    "'publishImmediately' or 'publishDelay' cannot be null");
        } else if (publishImmediately == null && publishDelay != null) {
            if (!isInteger(publishDelay)) {
                throw new IllegalArgumentException(
                        "'publishDelay' must be an integer");
            } else if (Integer.parseInt(publishDelay) <= 0) {
                throw new IllegalArgumentException(
                        "'publishDelay' cannot be <= 0");
            }
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

        setSectionMapping(mappings);
        setIgnoredMapping(ignoredMappings);

        dc = new DefaultDrupalClient(URI.create(hostname), endpoint, Integer.
                parseInt(connectionTimeout), Integer.parseInt(socketTimeout));
        ur = new UserResource(dc, username, password);
        fr = new FileResource(dc);
        nr = new NodeResourceFix(dc);
        nir = new NewsItemResource(dc);
        sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }
}
