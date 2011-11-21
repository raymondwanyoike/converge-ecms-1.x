/*
 * Copyright (C) 2010 - 2011 Interactive Media Management
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package dk.i2m.converge.plugins.joomla;

import dk.i2m.converge.core.content.NewsItem;
import dk.i2m.converge.core.content.NewsItemPlacement;
import dk.i2m.converge.core.plugin.EditionAction;
import dk.i2m.converge.core.plugin.PluginContext;
import dk.i2m.converge.core.workflow.Edition;
import dk.i2m.converge.core.workflow.OutletEditionAction;
import dk.i2m.converge.core.workflow.OutletEditionActionProperty;
import dk.i2m.converge.plugins.joomla.client.JoomlaConnection;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {Plug-in {link EditionAction} for uploading
 *
 * @author Allan Lykke Christensen
 */
@dk.i2m.converge.core.annotations.OutletAction
public class JoomlaEditionAction extends JoomlaPlugin implements EditionAction {

    private static final Logger LOG = Logger.getLogger(JoomlaEditionAction.class.getName());

    private ResourceBundle bundle = ResourceBundle.getBundle("dk.i2m.converge.plugins.joomla.JoomlaEditionAction");

    @Override
    public void execute(PluginContext ctx, Edition edition,
            OutletEditionAction action) {
        this.properties = action.getPropertiesAsMap();
        this.categoryMapping = new HashMap<String, String>();
        this.categoryImageMapping = new HashMap<String, String>();

        for (OutletEditionActionProperty prop : action.getProperties()) {

            // Fetch Category Mappings
            if (prop.getKey().equalsIgnoreCase(PROPERTY_CATEGORY_MAPPING)) {
                String[] catMap = prop.getValue().split(";");
                if (catMap.length == 2) {
                    categoryMapping.put(catMap[0], catMap[1]);
                } else if (catMap.length == 3) {
                    categoryMapping.put(catMap[0], catMap[1]);
                    try {
                        categoryPublish.put(catMap[0],
                                Integer.valueOf(catMap[2].trim()));
                    } catch (NumberFormatException ex) {
                        LOG.log(Level.INFO,
                                "Invalid category publish delay: {0}",
                                catMap[2]);
                    }
                } else if (catMap.length == 4) {
                    categoryMapping.put(catMap[0], catMap[1]);
                    try {
                        categoryPublish.put(catMap[0],
                                Integer.valueOf(catMap[2].trim()));
                    } catch (NumberFormatException ex) {
                        LOG.log(Level.INFO,
                                "Invalid category publish delay: {0}",
                                catMap[2]);
                    }
                    try {
                        categoryExpire.put(catMap[0],
                                Integer.valueOf(catMap[3].trim()));
                    } catch (NumberFormatException ex) {
                        LOG.log(Level.INFO,
                                "Invalid category expire delay: {0}",
                                catMap[3]);
                    }
                } else {
                    LOG.log(Level.INFO, "Invalid category mapping: {0}", prop.getValue());
                }
            } else if (prop.getKey().equalsIgnoreCase(
                    PROPERTY_CATEGORY_IMAGE_RESIZE)) {

                String[] imgCat = prop.getValue().split(";");
                if (imgCat.length == 4) {
                    this.categoryImageMapping.put(imgCat[0], prop.getValue());
                } else {
                    LOG.log(Level.INFO, "Invalid image category settings: {0}",
                            prop.getValue());
                }
            }
        }

        if (!properties.containsKey(JoomlaEditionAction.PROPERTY_URL)) {
            LOG.log(Level.WARNING,
                    "{0} property missing from action properties",
                    JoomlaEditionAction.PROPERTY_URL);
            return;
        }

        String method = "";
        if (!properties.containsKey(JoomlaEditionAction.PROPERTY_METHOD)) {
            LOG.log(Level.WARNING, "{0} property missing from action properties", JoomlaEditionAction.PROPERTY_METHOD);
            return;
        } else {
            method = properties.get(JoomlaEditionAction.PROPERTY_METHOD);
        }

        if (!properties.containsKey(JoomlaEditionAction.PROPERTY_USERNAME)) {
            LOG.log(Level.WARNING, "{0} property missing from action properties", JoomlaEditionAction.PROPERTY_USERNAME);
            return;
        }

        if (!properties.containsKey(JoomlaEditionAction.PROPERTY_PASSWORD)) {
            LOG.log(Level.WARNING, "{0} property missing from action properties", JoomlaEditionAction.PROPERTY_PASSWORD);
            return;
        }

        int timeout = DEFAULT_TIMEOUT;
        if (properties.containsKey(PROPERTY_XMLRPC_TIMEOUT)) {
            try {
                timeout = Integer.valueOf(properties.get(PROPERTY_XMLRPC_TIMEOUT));
            } catch (NumberFormatException ex) {
                LOG.log(Level.WARNING, "Invalid value contained in property ({0}): {1}", new Object[]{PROPERTY_XMLRPC_TIMEOUT, properties.get(
                            PROPERTY_XMLRPC_TIMEOUT)});
            }
        }

        int replyTimeout = DEFAULT_REPLY_TIMEOUT;
        if (properties.containsKey(PROPERTY_XMLRPC_REPLY_TIMEOUT)) {
            try {
                replyTimeout = Integer.valueOf(properties.get(
                        PROPERTY_XMLRPC_REPLY_TIMEOUT));
            } catch (NumberFormatException ex) {
                LOG.log(Level.WARNING, "Invalid value contained in property ({0}): {1}", new Object[]{PROPERTY_XMLRPC_REPLY_TIMEOUT, properties.get(
                            PROPERTY_XMLRPC_REPLY_TIMEOUT)});
            }
        }

        JoomlaConnection connection = new JoomlaConnection();
        connection.setUrl(properties.get(JoomlaEditionAction.PROPERTY_URL));
        connection.setUsername(properties.get(JoomlaEditionAction.PROPERTY_USERNAME));
        connection.setPassword(properties.get(JoomlaEditionAction.PROPERTY_PASSWORD));
        connection.setTimeout(timeout);
        connection.setReplyTimeout(replyTimeout);
//        connection.setTimeZone(user.getTimeZone());

        if (!connection.isConnectionValid()) {
            LOG.log(Level.WARNING, "Connection invalid");
            return;
        }

        if (method.equalsIgnoreCase(JoomlaEditionAction.XMLRPC_METHOD_NEW_ARTICLE)) {
            fetchJoomlaCategories(connection);

            for (NewsItemPlacement placement : edition.getPlacements()) {
                NewsItem item = placement.getNewsItem();
                if (item.isEndState()) {
                    if (isCategoryMapped(placement)) {
                        try {
                            newArticle(connection, placement);
                        } catch (JoomlaActionException ex) {
                            LOG.log(Level.SEVERE, ex.getMessage());
                            LOG.log(Level.FINE, "", ex);
                        }
                    } else {
                        LOG.log(Level.INFO,
                                "News Item #{0}, Placement '" + placement.getSection()
                                + "' is not mapped to a category on Joomla and is therefore not uploaded",
                                placement.getNewsItem().getId());
                    }
                }
            }
        } else if (method.equalsIgnoreCase(XMLRPC_METHOD_DELETE_ARTICLE)) {
            for (NewsItemPlacement placement : edition.getPlacements()) {
                try {
                    deleteArticle(connection, placement.getNewsItem());
                } catch (JoomlaActionException ex) {
                    LOG.log(Level.SEVERE, ex.getMessage());
                    LOG.log(Level.FINE, "", ex);
                }
            }
        }
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

    @Override
    public ResourceBundle getBundle() {
        return bundle;
    }
}
