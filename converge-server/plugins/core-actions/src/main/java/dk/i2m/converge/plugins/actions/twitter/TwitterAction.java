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
package dk.i2m.converge.plugins.actions.twitter;

import dk.i2m.converge.core.content.NewsItem;
import dk.i2m.converge.core.plugin.PluginContext;
import dk.i2m.converge.core.plugin.WorkflowAction;
import dk.i2m.converge.core.security.UserAccount;
import dk.i2m.converge.core.workflow.WorkflowStepAction;
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
import org.apache.commons.lang.StringEscapeUtils;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

/**
 *
 * @author Allan Lykke Christensen
 */
@dk.i2m.converge.core.annotations.WorkflowAction
public class TwitterAction implements WorkflowAction {

    private static final String PROPERTY_USERNAME = "Username";

    private static final String PROPERTY_PASSWORD = "Password";

    private static final String PROPERTY_MESSAGE = "Message";

    private Map<String, String> availableProperties = null;

    private static final Logger LOG = Logger.getLogger(TwitterAction.class.getName());

    private ResourceBundle msgs = ResourceBundle.getBundle("dk.i2m.converge.plugins.actions.twitter.Messages");

    @Override
    public void execute(PluginContext ctx, NewsItem item, WorkflowStepAction stepAction, UserAccount user) {
        Map<String, String> properties = stepAction.getPropertiesAsMap();

        if (!properties.containsKey(PROPERTY_USERNAME)) {
            LOG.log(Level.WARNING, "{0} property missing from action properties", PROPERTY_USERNAME);
            return;
        }

        if (!properties.containsKey(PROPERTY_PASSWORD)) {
            LOG.log(Level.WARNING, "{0} property missing from action properties", PROPERTY_PASSWORD);
            return;
        }

        if (!properties.containsKey(PROPERTY_MESSAGE)) {
            LOG.log(Level.WARNING, "{0} property missing from action properties", PROPERTY_MESSAGE);
            return;
        }

        String username = properties.get(PROPERTY_USERNAME);
        String password = properties.get(PROPERTY_PASSWORD);
        String message = properties.get(PROPERTY_MESSAGE);

        StringTemplate template = new StringTemplate(properties.get(PROPERTY_MESSAGE), DefaultTemplateLexer.class);

        template.setAttribute("newsitem", item);
        template.setAttribute("html-newsitem-title", StringEscapeUtils.escapeHtml(item.getTitle()));
        template.setAttribute("initiator", user);
        String notificationMessage = template.toString();


        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true).setOAuthConsumerKey("---").setOAuthConsumerSecret("---").setOAuthAccessToken("---").setOAuthAccessTokenSecret("---");

        TwitterFactory tf = new TwitterFactory(cb.build());
        Twitter t = tf.getInstance();

        try {
            t.updateStatus(notificationMessage);
        } catch (TwitterException te) {
            te.printStackTrace();
        }

    }

    @Override
    public Map<String, String> getAvailableProperties() {
        if (availableProperties == null) {
            availableProperties = new LinkedHashMap<String, String>();
            availableProperties.put(msgs.getString("PROPERTY_USERNAME"), PROPERTY_USERNAME);
            availableProperties.put(msgs.getString("PROPERTY_PASSWORD"), PROPERTY_PASSWORD);
            availableProperties.put(msgs.getString("PROPERTY_MESSAGE"), PROPERTY_MESSAGE);
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
