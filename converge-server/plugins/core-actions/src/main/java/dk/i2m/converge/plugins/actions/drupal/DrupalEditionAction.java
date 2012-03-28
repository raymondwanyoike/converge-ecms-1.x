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
import dk.i2m.converge.core.logging.LogSeverity;
import dk.i2m.converge.core.metadata.Concept;
import dk.i2m.converge.core.metadata.Subject;
import dk.i2m.converge.core.plugin.EditionAction;
import dk.i2m.converge.core.plugin.PluginContext;
import dk.i2m.converge.core.workflow.Edition;
import dk.i2m.converge.core.workflow.OutletEditionAction;
import dk.i2m.converge.core.workflow.OutletEditionActionProperty;
import dk.i2m.converge.plugins.actions.drupal.client.DrupalConnector;
import dk.i2m.converge.plugins.actions.drupal.client.fields.TextField;
import dk.i2m.converge.plugins.actions.drupal.client.messages.DrupalMessage;
import dk.i2m.converge.plugins.actions.drupal.client.messages.NodeCreateMessage;
import dk.i2m.converge.plugins.actions.drupal.client.modules.FieldModule;
import dk.i2m.converge.plugins.actions.drupal.client.resources.NodeResource;
import dk.i2m.converge.plugins.actions.drupal.client.resources.UserResource;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.client.HttpResponseException;

/**
 * Plug-in {@link EditionAction} for uploading {@link NewsItem}s to a Drupal instance.
 *
 * @author Raymond Wanyoike
 */
@OutletAction
public class DrupalEditionAction implements EditionAction {

    private static final String NID = "nid";

    private static final String URI = "uri";

    private static final String STATUS = "status";

    private static final String SUBMITTED = "submitted";

    private static final Integer UPLOADING = 0;

    private static final Integer UPLOADED = 1;

    private static final Integer FAILED = -1;

    private enum Property {

        CONNECTION_TIMEOUT,
        CONTENT_TYPE,
        DRUPAL_MAPPING,
        DRUPAL_MAPPING_FIELD,
        ENDPOINT,
        LANGUAGE,
        PASSWORD,
        SERVER,
        SOCKET_TIMEOUT,
        USERNAME
    }

    private ResourceBundle bundle = ResourceBundle.getBundle(
            "dk.i2m.converge.plugins.actions.drupal.Messages");

    private Map<String, String> availableProperties;

    private Map<Long, Long> mappings = new HashMap<Long, Long>();

    /** {@inheritDoc} */
    @Override
    public void execute(PluginContext ctx, Edition edition,
            OutletEditionAction action) {
        Map<String, String> properties = action.getPropertiesAsMap();

        String server = properties.get(Property.SERVER.name());
        String endPoint = properties.get(Property.ENDPOINT.name());
        String username = properties.get(Property.USERNAME.name());
        String password = properties.get(Property.PASSWORD.name());
        String type = properties.get(Property.CONTENT_TYPE.name());
        String language = properties.get(Property.LANGUAGE.name());
        String mapping = properties.get(Property.DRUPAL_MAPPING.name());
        String socketTimeout = properties.get(Property.SOCKET_TIMEOUT.name());
        String connectionTimeout = properties.get(Property.CONNECTION_TIMEOUT.
                name());
        String mappingField = properties.get(
                Property.DRUPAL_MAPPING_FIELD.name());

        String[] mapValues = mapping.split(";");

        for (int i = 0; i < mapValues.length; i++) {
            String[] mapValue = mapValues[i].split(":");
            mappings.put(Long.valueOf(mapValue[0].trim()),
                    Long.valueOf(mapValue[1].trim()));
        }

        ctx.log(LogSeverity.INFO,
                "Executing Drupal Client action for Outlet #{0}",
                new Object[]{action.getOutlet().getId()}, action, action.getId());

        DrupalConnector connector = new DrupalConnector(server, endPoint,
                Integer.parseInt(connectionTimeout), Integer.parseInt(
                socketTimeout));
        UserResource ur = new UserResource(connector, username, password, true);
        NodeResource nr = new NodeResource(connector);

        for (NewsItemPlacement nip : edition.getPlacements()) {
            int x = 0;

            NewsItem newsItem = nip.getNewsItem();
            DrupalMessage nodeMessage = new DrupalMessage();
            Map<String, Object> body = new HashMap<String, Object>();
            Map<String, Object> categories = new HashMap<String, Object>();

            nodeMessage.getFields().put("type", type);
            nodeMessage.getFields().put("language", language);
            nodeMessage.getFields().put("title", newsItem.getTitle());

            body.put("0", new TextField(newsItem.getBrief(), newsItem.getStory(),
                    "html"));

            for (Concept c : newsItem.getConcepts()) {
                if (c instanceof Subject) {
                    if (mappings.containsKey(c.getId())) {
                        categories.put(String.valueOf(x),
                                mappings.get(c.getId()));
                        x++;
                    }
                }
            }

            nodeMessage.getFields().put("body", new FieldModule(body));
            nodeMessage.getFields().put(mappingField, new FieldModule(
                    categories));

            ctx.log(LogSeverity.INFO,
                    "Creating new Drupal node for News Item #{0}",
                    new Object[]{newsItem.getId()}, action, action.getId());

            NewsItemEditionState status = ctx.addNewsItemEditionState(edition.
                    getId(), newsItem.getId(), STATUS, UPLOADING.toString());
            NewsItemEditionState nid =
                    ctx.addNewsItemEditionState(edition.getId(),
                    newsItem.getId(), NID, null);
            NewsItemEditionState uri =
                    ctx.addNewsItemEditionState(edition.getId(),
                    newsItem.getId(), URI, null);
            NewsItemEditionState submitted =
                    ctx.addNewsItemEditionState(edition.getId(),
                    newsItem.getId(), SUBMITTED, null);

            try {
                NodeCreateMessage createdNode = nr.createNode(nodeMessage);

                nid.setValue(createdNode.getNid().toString());
                uri.setValue(createdNode.getUri().toString());
                submitted.setValue(new Date().toString());
                status.setValue(UPLOADED.toString());
            } catch (HttpResponseException ex) {
                status.setValue(FAILED.toString());

                Logger.getLogger(DrupalEditionAction.class.getName()).
                        log(Level.SEVERE, null, ex);

                ctx.log(LogSeverity.SEVERE,
                        "HttpResponseException for News Item #{0}, with the message: {1} - {2} ",
                        new Object[]{newsItem.getId(), ex.getStatusCode(), ex.
                            getMessage()}, action, action.getId());
            }

            ctx.updateNewsItemEditionState(status);
            ctx.updateNewsItemEditionState(nid);
            ctx.updateNewsItemEditionState(uri);
            ctx.updateNewsItemEditionState(submitted);
        }

        ur.disconnect();
        connector.shutdown();

        ctx.log(LogSeverity.WARNING,
                "Finished Drupal Client action for Outlet #{0}",
                new Object[]{action.getOutlet().getId()}, action, action.getId());
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
            final String pattern = "yyyy-MM-dd HH:mm:ss";
            SimpleDateFormat format = new SimpleDateFormat(pattern);
            return format.parse(bundle.getString("PLUGIN_BUILD_TIME"));
        } catch (Exception e) {
            return Calendar.getInstance().getTime();
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
}
