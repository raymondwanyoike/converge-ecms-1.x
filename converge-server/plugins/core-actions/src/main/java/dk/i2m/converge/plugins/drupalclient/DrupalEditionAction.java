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
import dk.i2m.converge.core.content.NewsItemPlacement;
import dk.i2m.converge.core.plugin.EditionAction;
import dk.i2m.converge.core.plugin.PluginContext;
import dk.i2m.converge.core.workflow.Edition;
import dk.i2m.converge.core.workflow.OutletEditionAction;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Logger;

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
        SERVICE_ENDPOINT,
        SOCKET_TIMEOUT,
        UNDISCLOSED_AUTHOR,
        URL,
        USERNAME
    }

    private static final Logger LOG = Logger.
            getLogger(DrupalEditionAction.class.getName());

    private static final String SUBMITTED = "submitted";

    private static final String FAILED = "FAILED";

    private static final String UPLOADED = "UPLOADED";

    private static final String UPLOADING = "UPLOADING";

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

    private Map<Long, Long> sectionMapping;
    
    @Override
    public void execute(PluginContext ctx, Edition edition,
            OutletEditionAction action) {
        Map<String, String> properties = action.getPropertiesAsMap();

        String connectionTimeout = properties.get(Property.CONNECTION_TIMEOUT. name());
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
            throw new IllegalArgumentException("'publishImmediately' or 'publishDelay' cannot be null");
        }

        if (publishImmediately == null && publishDelay != null) {
            if (Integer.parseInt(publishDelay) <= 0) {
                throw new IllegalArgumentException("'publishDelay' cannot be <= 0");
            }
        }

        if (connectionTimeout == null) {
            connectionTimeout = "30000"; // 30 seconds
        }

        if (socketTimeout == null) {
            socketTimeout = "30000"; // 30 seconds
        }
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
}
