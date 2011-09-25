/*
 * Copyright (C) 2011 Interactive Media Management
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
package dk.i2m.converge.plugins.actions.urlcallback;

import dk.i2m.converge.core.plugin.EditionAction;
import dk.i2m.converge.core.plugin.PluginContext;
import dk.i2m.converge.core.workflow.Edition;
import dk.i2m.converge.core.workflow.OutletEditionAction;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.language.DefaultTemplateLexer;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

/**
 * {@link EditionAction} for executing a URL. This action can be used
 * as a callback for external system that needs to be notified of an
 * {@link Edition} closing.
 *
 * @author Allan Lykke Christensen
 */
@dk.i2m.converge.core.annotations.OutletAction
public class UrlCallbackAction implements EditionAction {

    private static final String PROPERTY_CALLBACK_URL = "Callback URL";

    private static final String PROPERTY_TIMEOUT = "Timeout";
    
    private static final int DEFAULT_TIMEOUT = 30000;

    private Map<String, String> availableProperties = null;

    private static final Logger LOG = Logger.getLogger(UrlCallbackAction.class.getName());

    private ResourceBundle msgs = ResourceBundle.getBundle("dk.i2m.converge.plugins.actions.urlcallback.Messages");

    @Override
    public void execute(PluginContext ctx, Edition edition, OutletEditionAction action) {
        Map<String, String> properties = action.getPropertiesAsMap();

        if (!properties.containsKey(PROPERTY_CALLBACK_URL)) {
            LOG.log(Level.WARNING, "{0} property missing from properties", PROPERTY_CALLBACK_URL);
            return;
        }

        int timeout = DEFAULT_TIMEOUT;
        if (properties.containsKey(PROPERTY_TIMEOUT)) {
            String rawTimeout = "";
            try {
                rawTimeout = properties.get(PROPERTY_TIMEOUT);
                timeout = Integer.valueOf(rawTimeout);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Invalid value set for {0}: {1}. Using {2}", new Object[]{PROPERTY_TIMEOUT, rawTimeout, timeout});
            }
        }

        // Replace template tags in URL
        String rawUrl = properties.get(PROPERTY_CALLBACK_URL);
        StringTemplate template = new StringTemplate(rawUrl, DefaultTemplateLexer.class);
        template.setAttribute("edition", edition);
        String url = template.toString();

        HttpClient client = new HttpClient();
        client.getHttpConnectionManager().getParams().setConnectionTimeout(timeout);
        HttpMethod method = new GetMethod(url);
        method.setFollowRedirects(true);
        
        try {
            client.executeMethod(method);
        } catch (HttpException ex) {
            LOG.log(Level.WARNING, "Could not execute callback URL. " + ex.getMessage(), ex);
        } catch (IOException ex) {
            LOG.log(Level.WARNING, "Could not execute callback URL. " + ex.getMessage(), ex);
        } finally {
            method.releaseConnection();
        }
    }

    @Override
    public Map<String, String> getAvailableProperties() {
        if (availableProperties == null) {
            availableProperties = new LinkedHashMap<String, String>();
            availableProperties.put(msgs.getString("PROPERTY_CALLBACK_URL"), PROPERTY_CALLBACK_URL);
            availableProperties.put(msgs.getString("PROPERTY_TIMEOUT"), PROPERTY_TIMEOUT);
        }
        return availableProperties;
    }

    @Override
    public String getName() {
        return msgs.getString("PLUGIN_NAME");
    }

    @Override
    public String getDescription() {
        return msgs.getString("PLUGIN_DESCRIPTION");
    }

    @Override
    public String getVendor() {
        return msgs.getString("PLUGIN_VENDOR");
    }

    @Override
    public Date getDate() {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return format.parse(msgs.getString("PLUGIN_BUILD_TIME"));
        } catch (Exception ex) {
            return Calendar.getInstance().getTime();
        }
    }
}
