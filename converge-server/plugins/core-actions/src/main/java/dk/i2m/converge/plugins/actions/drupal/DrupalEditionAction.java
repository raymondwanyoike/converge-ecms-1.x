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
import dk.i2m.converge.core.content.NewsItem;
import dk.i2m.converge.core.content.NewsItemEditionState;
import dk.i2m.converge.core.content.NewsItemPlacement;
import dk.i2m.converge.core.metadata.Concept;
import dk.i2m.converge.core.metadata.Subject;
import dk.i2m.converge.core.plugin.EditionAction;
import dk.i2m.converge.core.plugin.PluginContext;
import dk.i2m.converge.core.workflow.Edition;
import dk.i2m.converge.core.workflow.Outlet;
import dk.i2m.converge.core.workflow.OutletEditionAction;
import dk.i2m.converge.plugins.actions.drupal.client.*;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

/**
 * Plug-in {@link EditionAction} for uploading {@link NewsItem}s to a Drupal instance.
 *
 * @author Raymond Wanyoike
 */
@OutletAction
public class DrupalEditionAction implements EditionAction {

    private static final Logger LOG = Logger.getLogger("DrupalEditionAction");

    private static final String STATUS = "status";

    private static final String SUBMITTED = "SUBMITTED";

    private static final String UPLOADED = "UPLOADED";

    private static final String FAILED = "FAILED";

    private static final String UPLOADING = "UPLOADING";
    
    private static final String NID = "nid";

    private static final String URI = "uri";

    private ResourceBundle bundle = ResourceBundle.getBundle("dk.i2m.converge.plugins.actions.drupal.Messages");

    private Map<String, String> availableProperties;

    private Map<Long, Long> pressMappings = new HashMap<Long, Long>();
    
    private Map<Long, Long> groupMappings = new HashMap<Long, Long>();

    private enum Property {

        CONNECTION_TIMEOUT,
        CONTENT_TYPE,
        PRESS_MAPPING,
        PRESS_MAPPING_FIELD,
        GROUP_MAPPING,
        GROUP_MAPPING_FIELD,
        ENDPOINT,
        LANGUAGE,
        PASSWORD,
        HOSTNAME,
        SOCKET_TIMEOUT,
        USERNAME
    }

    @Override
    public void execute(PluginContext ctx, Edition edition, OutletEditionAction action) {
        Map<String, String> properties = action.getPropertiesAsMap();

        String hostname = properties.get(Property.HOSTNAME.name());
        String endPoint = properties.get(Property.ENDPOINT.name());
        String username = properties.get(Property.USERNAME.name());
        String password = properties.get(Property.PASSWORD.name());
        String type = properties.get(Property.CONTENT_TYPE.name());
        String language = properties.get(Property.LANGUAGE.name());
        String socketTimeout = properties.get(Property.SOCKET_TIMEOUT.name());
        String connectionTimeout = properties.get(Property.CONNECTION_TIMEOUT.name());
        
        String pressMapping = properties.get(Property.PRESS_MAPPING.name());
        String pressMappingField = properties.get(Property.PRESS_MAPPING_FIELD.name());
        String groupMapping = properties.get(Property.GROUP_MAPPING.name());
        String groupMappingField = properties.get(Property.GROUP_MAPPING_FIELD.name());
        
        setMapping(1, pressMapping);
        setMapping(0, groupMapping);
        
        Map<String, Object> press = mapPress(edition.getOutlet());
        
        DrupalClient client = new DrupalClient(hostname, endPoint);
        client.setSocketTimeout(Integer.parseInt(socketTimeout));
        client.setConnectionTimeout(Integer.parseInt(connectionTimeout));
        client.setup();
        
        try {
            UserResource ur = new UserResource(client);
            ur.setUsername(username);
            ur.setPassword(password);
            ur.connect();
            
            NodeResource nr = new NodeResource(client);

            for (NewsItemPlacement nip : edition.getPlacements()) {
                NewsItem newsItem = nip.getNewsItem();

                DrupalMessage nodeMessage = new DrupalMessage();
                nodeMessage.getFields().put("title", truncateString(newsItem.getTitle(), 254));
                nodeMessage.getFields().put("type", type);
                nodeMessage.getFields().put("language", language);
                nodeMessage.getFields().put("date", new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(newsItem.getUpdated().getTime()));

                Map<String, Object> body = new HashMap<String, Object>();
                body.put("0", new TextField(newsItem.getBrief(), cleanString(newsItem.getStory()), "html"));
            
                Map<String, Object> group = mapGroup(newsItem);
                
                nodeMessage.getFields().put("body", new FieldModule(body));
                nodeMessage.getFields().put(pressMappingField, new FieldModule(press));
                nodeMessage.getFields().put(groupMappingField, new FieldModule(group));
         
                NewsItemEditionState status = ctx.addNewsItemEditionState(edition.getId(), newsItem.getId(), STATUS, UPLOADING.toString());
                NewsItemEditionState nid = ctx.addNewsItemEditionState(edition.getId(), newsItem.getId(), NID, null);
                NewsItemEditionState uri = ctx.addNewsItemEditionState(edition.getId(), newsItem.getId(), URI, null);
                NewsItemEditionState submitted = ctx.addNewsItemEditionState(edition.getId(), newsItem.getId(), SUBMITTED, null);
            
                try {
                    NodeCreateMessage ncm = nr.create(nodeMessage);
                    
                    nid.setValue(ncm.getId().toString());
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
    
//    /** {@inheritDoc} */
//    @Override
//    public void execute(PluginContext ctx, Edition edition,
//            OutletEditionAction action) {
//        // Get the action properties as a Map
//        Map<String, String> properties = action.getPropertiesAsMap();
//
//        // Assign the action properties
//        String server = properties.get(Property.SERVER.name());
//        String endPoint = properties.get(Property.ENDPOINT.name());
//        String username = properties.get(Property.USERNAME.name());
//        String password = properties.get(Property.PASSWORD.name());
//        String type = properties.get(Property.CONTENT_TYPE.name());
//        String language = properties.get(Property.LANGUAGE.name());
//        String socketTimeout = properties.get(Property.SOCKET_TIMEOUT.name());
//        String connectionTimeout = properties.get(Property.CONNECTION_TIMEOUT.
//                name());
//        // The convergeId:drupalId mapping
//        String mapping = properties.get(Property.DRUPAL_MAPPING.name());
//        // The field that is being mapped (e.g. categories)
//        String mappingField = properties.get(
//                Property.DRUPAL_MAPPING_FIELD.name());
//
//        // Split the mapping into mappings with each occurrence of ";"
//        String[] mapValues = mapping.split(";");
//
//        /// Loop through the mappings adding them to a Map<Long,Long>
//        for (int i = 0; i < mapValues.length; i++) {
//            String[] mapValue = mapValues[i].split(":");
//            groupMappings.put(Long.valueOf(mapValue[0].trim()),
//                    Long.valueOf(mapValue[1].trim()));
//        }
//
//        LOG.log(Level.INFO, "Executing Drupal Client action for Outlet #{0}",
//                action.getOutlet().getId());
//        LOG.log(Level.INFO, "Discovered {0} mappings", groupMappings.size());
//        ctx.log(LogSeverity.INFO,
//                "Executing Drupal Client action for Outlet #{0}",
//                new Object[]{action.getOutlet().getId()}, action, action.getId());
//
//        if (socketTimeout == null) {
//            // TODO: Set a default
//            // socketTimeout = 0;
//        }
//
//        if (connectionTimeout == null) {
//            // TODO: Set a default
//            // connectionTimeout = 0;
//        }
//
//        DrupalConnector connector = new DrupalConnector(server, endPoint,
//                Integer.parseInt(connectionTimeout), Integer.parseInt(
//                socketTimeout));
//        UserResource user =
//                new UserResource(connector, username, password, true);
//        NodeResource node = new NodeResource(connector);
//
//        // Loop through the NewsItems in the Edition acting on each
//        for (NewsItemPlacement nip : edition.getPlacements()) {
//            NewsItem newsItem = nip.getNewsItem();
//
//            Map<String, Object> body = new HashMap<String, Object>();
//            // Add the NewsItem content, brief (as summary) and story
//            System.out.println(newsItem);
//            body.put("0", new TextField(newsItem.getBrief(), clean(newsItem.
//                    getStory()), "filtered_html"));
//
//            // TODO: This part of the code (categories and concepts) is hard coded
//            Map<String, Object> categories = new HashMap<String, Object>();
//
//            // Counter for the nth Concept that has been processed
//            int x = 0;
//
//            // Loop through the Concepts building our request
//            for (Concept c : newsItem.getConcepts()) {
//                if (c instanceof Subject) {
//
//                    // Check if the mappings contain the Concept
//                    if (groupMappings.containsKey(c.getId())) {
//                        categories.put(String.valueOf(x),
//                                groupMappings.get(c.getId()));
//                    } else {
//                        // Fallback to the parent Concept, and keep at it until 
//                        // a Concept is found in the Concept parent hierarchy or
//                        // nothing is found.
//
//                        LOG.log(Level.WARNING,
//                                "News Item #{0}- Mapping not found for concept: {1} {2}. Trying parent",
//                                new Object[]{newsItem.getId(), c.getId(), c.
//                                    getName()});
//
//                        Concept child = c;
//
//                        if (!child.getBroader().isEmpty()) {
//                            while (!child.getBroader().isEmpty()) {
//                                Concept parent = child.getBroader().get(0);
//
//                                if (groupMappings.containsKey(parent.getId())) {
//                                    LOG.log(Level.INFO,
//                                            "News Item #{0}- Mapping ({1}) found for parent. Concept: {2} {3}",
//                                            new Object[]{newsItem.getId(), c.
//                                                getId(), parent.getId(), parent.
//                                                getName()});
//
//                                    categories.put(String.valueOf(x), groupMappings.
//                                            get(parent.getId()));
//                                    break;
//                                }
//
//                                LOG.log(Level.WARNING,
//                                        "News Item #{0}- Mapping ({1}) not found for parent. Trying next parent",
//                                        new Object[]{newsItem.getId(), c.getId()});
//
//                                child = parent;
//                            }
//                        } else {
//                            LOG.log(Level.SEVERE,
//                                    "News Item #{0}- Mapping ({1}) has no parent.",
//                                    new Object[]{newsItem.getId(), c.getId()});
//                        }
//
//                        x++;
//                    }
//                }
//            }
//
//            DrupalMessage nodeMessage = new DrupalMessage();
//            nodeMessage.getFields().put("title", newsItem.getTitle());
//            nodeMessage.getFields().put("type", type);
//            nodeMessage.getFields().put("language", language);
//            nodeMessage.getFields().put("date", new SimpleDateFormat(
//                    "yyyy-MM-dd hh:mm:ss").format(
//                    newsItem.getUpdated().getTime()));
//            nodeMessage.getFields().put("body", new FieldModule(body));
//            nodeMessage.getFields().put(mappingField,
//                    new FieldModule(categories));
//
//            Map<String, Object> press = new HashMap<String, Object>();
//            press.put("0", edition.getOutlet().getTitle());
//
//            nodeMessage.getFields().put("press", new FieldModule(press));
//
//            ctx.log(LogSeverity.INFO,
//                    "Creating new Drupal node for News Item #{0}",
//                    new Object[]{newsItem.getId()}, action, action.getId());
//
//            // Record the progress in the db
//            NewsItemEditionState status = ctx.addNewsItemEditionState(edition.
//                    getId(), newsItem.getId(), STATUS, UPLOADING.toString());
//            NewsItemEditionState nid =
//                    ctx.addNewsItemEditionState(edition.getId(),
//                    newsItem.getId(), NID, null);
//            NewsItemEditionState uri =
//                    ctx.addNewsItemEditionState(edition.getId(),
//                    newsItem.getId(), URI, null);
//            NewsItemEditionState submitted =
//                    ctx.addNewsItemEditionState(edition.getId(),
//                    newsItem.getId(), SUBMITTED, null);
//
//            try {
//                // Create the Drupal node
//                NodeCreateMessage createdNode = node.create(nodeMessage);
//
//                nid.setValue(createdNode.getNid().toString());
//                uri.setValue(createdNode.getUri().toString());
//                submitted.setValue(new Date().toString());
//                status.setValue(UPLOADED.toString());
//            } catch (HttpResponseException ex) {
//                status.setValue(FAILED.toString());
//
//                LOG.log(Level.SEVERE, "{0} - {1}",
//                        new Object[]{ex.getStatusCode(), ex.getMessage()});
//                ctx.log(LogSeverity.SEVERE,
//                        "HttpResponseException for News Item #{0}, with the message: {1} - {2} ",
//                        new Object[]{newsItem.getId(), ex.getStatusCode(), ex.
//                            getMessage()}, action, action.getId());
//            } catch (Exception ex) {
//                status.setValue(FAILED.toString());
//
//                LOG.log(Level.SEVERE, null, ex);
//            }
//
//            // Update the progress in the db
//            ctx.updateNewsItemEditionState(status);
//            ctx.updateNewsItemEditionState(nid);
//            ctx.updateNewsItemEditionState(uri);
//            ctx.updateNewsItemEditionState(submitted);
//        }
//
//        // Clean up
//        user.disconnect();
//        connector.shutdown();
//
//        ctx.log(LogSeverity.INFO,
//                "Finished Drupal Client action for Outlet #{0}",
//                new Object[]{action.getOutlet().getId()}, action, action.getId());
//    }

    @Override
    public void executePlacement(PluginContext ctx, NewsItemPlacement placement, Edition edition, OutletEditionAction action) {
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
    
    private void setMapping(int map, String mapping) {
        String[] values = mapping.split(";");

        for (int i = 0; i < values.length; i++) {
            String[] value = values[i].split(":");
            
            if (map == 1) {
                pressMappings.put(Long.valueOf(value[0].trim()), Long.valueOf(value[1].trim()));
            } else {
                groupMappings.put(Long.valueOf(value[0].trim()), Long.valueOf(value[1].trim()));
            }
        }
    }
    
    private Map<String, Object> mapPress(Outlet outlet) {
        Map<String, Object> press = new HashMap<String, Object>();
        
        if (pressMappings.containsKey(outlet.getId())) {
            press.put("0", pressMappings.get(outlet.getId()));
        } 
        
        return press;
    }
    
    private Map<String, Object> mapGroup(NewsItem newsItem) {
        Map<String, Object> categories = new HashMap<String, Object>();

        // Counter for the nth processed Concept
        int x = 0; 

        for (Concept c : newsItem.getConcepts()) {
            if (c instanceof Subject) {
                // Check if the mappings contain the Concept
                if (groupMappings.containsKey(c.getId())) {
                    categories.put(String.valueOf(x), groupMappings.get(c.getId()));
                } else {
                    // Fallback to the parent Concept, and keep at it until 
                    // a Concept is found in the Concept hierarchy or until
                    // nothing is found.
                    Concept child = c;

                    if (!child.getBroader().isEmpty()) {
                        while (!child.getBroader().isEmpty()) {
                            Concept parent = child.getBroader().get(0);

                            if (groupMappings.containsKey(parent.getId())) {
                                // Found a Concept, assign it and break
                                categories.put(String.valueOf(x), groupMappings.get(parent.getId()));
                                break;
                            }

                            // Nothing found here, try the parent
                            child = parent;
                        }
                    } else {
                        // At the top of the tree with nothing found
                    }

                    // Next!
                    x++;
                }
            }
        }
        
        return categories;
    }

    private static String truncateString(String value, int length) {
        if (value != null && value.length() > length) {
            value = value.substring(0, length);
        }

        return value;
    }

    private static String cleanString(String content) {
        return Jsoup.clean(content, Whitelist.relaxed());
    }
}
