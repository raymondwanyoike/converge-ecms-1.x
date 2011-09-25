/*
 *  Copyright (C) 2010 Interactive Media Management
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.i2m.converge.plugins.emailaction;

import dk.i2m.converge.core.plugin.WorkflowAction;
import dk.i2m.converge.core.content.NewsItem;
import dk.i2m.converge.core.plugin.PluginContext;
import dk.i2m.converge.core.security.UserAccount;
import dk.i2m.converge.core.workflow.WorkflowStepAction;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.language.DefaultTemplateLexer;

/**
 * Workflow action for sending an e-mail.
 *
 * For the properties, any of the following variables are available:
 *
 * <ul>
 *     <li>initiator</li>
 *     <li>newsitem</li>
 * </ul>
 *
 * All variables must be enclosed in dollar signs, e.g. $initiator.fullName$.
 *
 * @author Allan Lykke Christensen
 */
@dk.i2m.converge.core.annotations.WorkflowAction
public class EmailAction implements WorkflowAction {

    public static final String EMAIL_SENDER = "email.sender";

    public static final String EMAIL_RECIPIENT = "email.recipient";

    public static final String EMAIL_SUBJECT = "email.subject";

    public static final String EMAIL_BODY = "email.body";

    private Map<String, String> availableProperties = null;

    private static final Logger logger = Logger.getLogger(EmailAction.class.getName());

    private ResourceBundle msgs = ResourceBundle.getBundle("dk.i2m.converge.plugins.emailaction.Messages");

    private Calendar releaseDate = new GregorianCalendar(2010, Calendar.JULY, 9, 14, 40);

    /**
     * Creates a new instance of {@link AlertAction}.
     */
    public EmailAction() {
    }

    @Override
    public void execute(PluginContext ctx, NewsItem item, WorkflowStepAction stepAction, UserAccount user) {
        Map<String, String> properties = stepAction.getPropertiesAsMap();

        if (!properties.containsKey(EmailAction.EMAIL_RECIPIENT)) {
            logger.log(Level.WARNING, "{0} property missing from action properties", EmailAction.EMAIL_RECIPIENT);
            return;
        }

        if (!properties.containsKey(EmailAction.EMAIL_BODY)) {
            logger.log(Level.WARNING, "{0} property missing from action properties", EmailAction.EMAIL_BODY);
            return;
        }

        if (!properties.containsKey(EmailAction.EMAIL_SUBJECT)) {
            logger.log(Level.WARNING, "{0} property missing from action properties", EmailAction.EMAIL_SUBJECT);
            return;
        }

        String emailBody = compileTemplate(properties.get(EmailAction.EMAIL_BODY), item, user);
        String emailRecipient = compileTemplate(properties.get(EmailAction.EMAIL_RECIPIENT), item, user);
        String emailSubject = compileTemplate(properties.get(EmailAction.EMAIL_SUBJECT), item, user);
        String emailSender = compileTemplate(properties.get(EmailAction.EMAIL_SENDER), item, user);

        ctx.dispatchMail(emailRecipient, emailSender, emailSubject, emailBody);
    }

    private String compileTemplate(String template, NewsItem item, UserAccount user) {
        StringTemplate strTemplate = new StringTemplate(template, DefaultTemplateLexer.class);
        strTemplate.setAttribute("newsitem", item);
        strTemplate.setAttribute("initiator", user);
        return strTemplate.toString();
    }

    @Override
    public Map<String, String> getAvailableProperties() {
        if (availableProperties == null) {
            availableProperties = new LinkedHashMap<String, String>();
            availableProperties.put(msgs.getString("PROPERTY_EMAIL_SENDER"), EMAIL_SENDER);
            availableProperties.put(msgs.getString("PROPERTY_EMAIL_RECIPIENT"), EMAIL_RECIPIENT);
            availableProperties.put(msgs.getString("PROPERTY_SUBJECT"), EMAIL_SUBJECT);
            availableProperties.put(msgs.getString("PROPERTY_MESSAGE"), EMAIL_BODY);
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
        return releaseDate.getTime();
    }
}
