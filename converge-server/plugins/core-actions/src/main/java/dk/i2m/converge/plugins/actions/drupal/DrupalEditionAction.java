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
import dk.i2m.converge.plugins.actions.drupal.client.modules.FieldModule;
import dk.i2m.converge.plugins.actions.drupal.client.resources.NodeResource;
import dk.i2m.converge.plugins.actions.drupal.client.resources.UserResource;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Plug-in {@link EditionAction} for uploading
 */
@OutletAction
public class DrupalEditionAction implements EditionAction {

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
        String socketTimeout = properties.get(Property.SOCKET_TIMEOUT.name());
        String connectionTimeout = properties.get(Property.CONNECTION_TIMEOUT.
                name());
        String drupalMappingField =
                properties.get(Property.DRUPAL_MAPPING_FIELD.name());

        for (OutletEditionActionProperty p : action.getProperties()) {
            if (Property.DRUPAL_MAPPING.name().equals(p.getKey())) {
                String[] mapValues = p.getValue().split(";");
                if (mapValues.length != 2) {
                    continue;
                } else {
                    mappings.put(Long.valueOf(mapValues[0].trim()),
                            Long.valueOf(mapValues[1].trim()));
                }
            }
        }

        ctx.log(LogSeverity.WARNING,
                "Executing Drupal Client action for Outlet #{0}",
                new Object[]{action.getOutlet().getId()}, action, action.getId());

        DrupalConnector dc = new DrupalConnector(server, endPoint,
                Integer.parseInt(connectionTimeout), Integer.parseInt(
                socketTimeout));
        UserResource ur = new UserResource(dc, username, password, true);
        NodeResource nr = new NodeResource(dc, ur);

        for (NewsItemPlacement nip : edition.getPlacements()) {
            int x = 0;

            NewsItem newsItem = nip.getNewsItem();
            DrupalMessage nodeMessage = new DrupalMessage();
            Map<String, Object> body = new HashMap<String, Object>();
            Map<String, Object> categories = new HashMap<String, Object>();

            nodeMessage.getFields().put("title", newsItem.getTitle());
            nodeMessage.getFields().put("type", type);
            nodeMessage.getFields().put("language", language);

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
            nodeMessage.getFields().put("category", new FieldModule(
                    categories));

            ctx.log(LogSeverity.INFO,
                    "Creating new Drupal node for News Item #{0}",
                    new Object[]{newsItem.getId()}, action, action.getId());
            nr.createNode(nodeMessage);
        }

        ur.disconnect();
        dc.shutdown();

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
