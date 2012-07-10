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

        FRONT_PAGE_PAGE,
        PUBLISH_IMMEDIATELY,
        PUBLISH_DELAY,
        CONNECTION_TIMEOUT,
        UNDISCLOSED_AUTHOR_LABEL,
        CONTENT_TYPE,
        SECTION_MAPPING,
        ENDPOINT,
        LANGUAGE,
        PASSWORD,
        HOSTNAME,
        SOCKET_TIMEOUT,
        USERNAME,
        RENDITION_NAME,
        EXCLUDE_MEDIA_CONTENT_TYPES
    }

    private static final Logger LOG = Logger.getLogger("DrupalEditionAction");

    private static final String STATUS = "status";

    private static final String SUBMITTED = "SUBMITTED";

    private static final String UPLOADED = "UPLOADED";

    private static final String FAILED = "FAILED";

    private static final String UPLOADING = "UPLOADING";

    private static final String NID = "nid";

    private static final String URI = "uri";

    private ResourceBundle bundle = ResourceBundle.getBundle(
            "dk.i2m.converge.plugins.actions.drupal.Messages");

    private Map<String, String> availableProperties;

    private String undisclosedAuthor;

    private String frontPagePage;

    private String publishImmediately;

    private String publishDelay;
    
    private String renditionName;
    
    private String[] excludeMediaContentTypes;
    
    private Map<Long, Long> sectionMapping = new HashMap<Long, Long>();

    @Override
    public void execute(PluginContext ctx, Edition edition, OutletEditionAction action) {
        Map<String, String> properties = action.getPropertiesAsMap();

        String hostname = properties.get(Property.HOSTNAME.name());
        String endPoint = properties.get(Property.ENDPOINT.name());
        String username = properties.get(Property.USERNAME.name());
        String password = properties.get(Property.PASSWORD.name());
        String contentType = properties.get(Property.CONTENT_TYPE.name());
        String language = properties.get(Property.LANGUAGE.name());
        String socketTimeout = properties.get(Property.SOCKET_TIMEOUT.name());
        String connectionTimeout = properties.get(Property.CONNECTION_TIMEOUT.name());
        String sectionMapping = properties.get(Property.SECTION_MAPPING.name());
        String excludeMediaContentTypes = properties.get(Property.EXCLUDE_MEDIA_CONTENT_TYPES.name());
        
        undisclosedAuthor = properties.get(Property.UNDISCLOSED_AUTHOR_LABEL.name());
        frontPagePage = properties.get(Property.FRONT_PAGE_PAGE.name());
        publishImmediately = properties.get(Property.PUBLISH_IMMEDIATELY.name());
        publishDelay = properties.get(Property.PUBLISH_DELAY.name());
        renditionName = properties.get(Property.RENDITION_NAME.name());
        
        if (hostname == null) {
            throw new NullPointerException("Host name is not set");
        }
        
        if (endPoint == null) {
            throw new NullPointerException("Service endpoint is not set");
        }
        
        if (username == null) {
            throw new NullPointerException("Username is not set");
        }
        
        if (password == null) {
            throw new NullPointerException("Password is not set");
        }
        
        if (contentType == null) {
            throw new NullPointerException("Content type is not set");
        }
        
        if (language == null) {
            // Fallback to the default
            language = "und";
        }
        
        if (socketTimeout == null) {
            // Fallback to a default
            socketTimeout = "30000";
        }
        
        if (connectionTimeout == null) {
            // Fallback to a default
            connectionTimeout = "30000";
        }
        
        if (sectionMapping == null) {
            throw new NullPointerException("Section mapping is not set");
        }

        setSectionMapping(sectionMapping);
        setExcludeMediaContentTypes(excludeMediaContentTypes);

        DrupalClient client = new DrupalClient(hostname, endPoint);
        client.setSocketTimeout(Integer.parseInt(socketTimeout));
        client.setConnectionTimeout(Integer.parseInt(connectionTimeout));
        client.setup();

        try {
            UserResource ur = new UserResource(client);
            NodeResource nr = new NodeResource(client);
            FileResource fr = new FileResource(client);

            ur.setUsername(username);
            ur.setPassword(password);
            ur.connect();

            for (NewsItemPlacement nip : edition.getPlacements()) {
                NewsItem newsItem = nip.getNewsItem();

                Map<String, Object> body = new HashMap<String, Object>();
                Map<String, Object> actor = new HashMap<String, Object>();
                Map<String, Object> section = mapSection(nip);
                Map<String, Object> image = new HashMap<String, Object>();

                body.put("0", new TextField(newsItem.getBrief(),
                        cleanString(newsItem.getStory()), "html"));
                actor.put("0", new TextField(null, getActor(newsItem), null));

                DrupalMessage nodeMessage = new DrupalMessage();
                nodeMessage.getFields().put("status", getStatus());
                nodeMessage.getFields().put("publish_on", getPublishOn());
                nodeMessage.getFields().put("sticky", getPromote(nip));
                nodeMessage.getFields().put("title", getTitle(newsItem));
                nodeMessage.getFields().put("type", contentType);
                nodeMessage.getFields().put("language", language);
                nodeMessage.getFields().put("body", new FieldModule(body));
                nodeMessage.getFields().put("actor", new FieldModule(actor));
                nodeMessage.getFields().put("section", new FieldModule(section));

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
                    ArrayList<FileCreateMessage> fcms = fr.create(renditions);

                    for (int i = 0; i < fcms.size(); i++) {
                        image.put(String.valueOf(i), new ImageField(fcms.get(i).
                                getFid(), 1, NID, contentType));
                    }

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

            ur.disconnect();
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }

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

        for (int i = 0; i < values.length; i++) {
            String[] value = values[i].split(":");

            Long convergeId =Long.valueOf(value[0].trim());
            Long drupalId = Long.valueOf(value[1].trim());
            sectionMapping.put(convergeId, drupalId);
        }
    }
    
    private void setExcludeMediaContentTypes(String mapping) {
        excludeMediaContentTypes = mapping.split(";");
    }

    private Map<String, Object> mapSection(NewsItemPlacement placement) {
        Map<String, Object> map = new HashMap<String, Object>();
        Section section = placement.getSection();

        if (sectionMapping.containsKey(section.getId())) {
            map.put("0", sectionMapping.get(section.getId()));
        }

        return map;
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
            value = value.trim().substring(0, length);
        }

        return value;
    }

    /**
     * Get safe HTML from untrusted input HTML.
     * 
     * @param content input untrusted HTML
     * @return safe HTML
     */
    private String cleanString(String content) {
        return Jsoup.clean(content, Whitelist.relaxed());
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
     * Get <b>Actor</b> text value.
     * 
     * @param newsItem NewsItem
     * @return actor, or actors (separated by ', ')
     */
    private String getActor(NewsItem newsItem) {
        if (newsItem.isUndisclosedAuthor()) {
            // Return the UNDISCLOSED_AUTHOR_LABEL property value
            return undisclosedAuthor;
        } else {
            if (newsItem.getByLine().trim().isEmpty()) {
                StringBuilder by = new StringBuilder();

                // Iterate over all the actors
                for (NewsItemActor actor : newsItem.getActors()) {
                    boolean firstActor = true;

                    // TODO: Document
                    if (actor.getRole().equals(newsItem.getOutlet().getWorkflow().
                            getStartState().getActorRole())) {
                        if (!firstActor) {
                            by.append(", ");
                        } else {
                            firstActor = false;
                        }

                        by.append(actor.getUser().getFullName());
                    }
                }
                return by.toString();
            } else {
                // Return the "by-line" of the NewsItem
                return newsItem.getByLine();
            }
        }
    }
    
        
    /**
     * Get <b>Published</b> checkbox value.
     * 
     * @return 1 = checked, 0 otherwise
     */
    private String getStatus() {
        if (publishImmediately != null) {
            // 1 == true
            if (publishImmediately.equals("1")) {
                // Default 
                return "1";
            }
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
            // 1 == true
            if (publishImmediately.equals("1")) {
                return "";
            }
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
        if (frontPagePage != null) {
            // Get the NewsItemPlacement's' start page
            String start = String.valueOf(placement.getStart());

            // Check if the start page matches the FRONT_PAGE_MAPPING
            if (frontPagePage.equalsIgnoreCase(start)) {
                return "1";
            }
        }

        // Default
        return "0";
    }
    
    private List<MediaItemRendition> getMedia(NewsItem newsItem) {
        List<MediaItemRendition> renditions = new ArrayList<MediaItemRendition>();

        for (NewsItemMediaAttachment attachment : newsItem.getMediaAttachments()) {
            MediaItem item = attachment.getMediaItem();

            // Verify that the item exist and renditions are attached
            if (item == null || !item.isRenditionsAttached()) {
                continue;
            }

            try {
                MediaItemRendition rendition = item.findRendition(renditionName);

                // Check if the file should be excluded
                if (ArrayUtils.contains(excludeMediaContentTypes, rendition.getContentType())) {
                    continue;
                }

                String filename = newsItem.getId() + "-" + rendition.getId()
                        + "." + rendition.getExtension();

                renditions.add(rendition);
            } catch (RenditionNotFoundException ex) {
                LOG.log(Level.WARNING,
                        "Rendition ({0}) missing for Media Item #{1}. Ignoring Media Item.",
                        new Object[]{renditionName, item.getId()});
            }
        }

        return renditions;
    }
}
