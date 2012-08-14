/*
 * Copyright (C) 2010 - 2012 Interactive Media Management
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package dk.i2m.converge.plugins.createnotification;

import dk.i2m.converge.core.DataNotFoundException;
import dk.i2m.converge.core.Notification;
import dk.i2m.converge.core.content.ContentItem;
import dk.i2m.converge.core.content.ContentItemActor;
import dk.i2m.converge.core.plugin.PluginAction;
import dk.i2m.converge.core.plugin.PluginActionException;
import dk.i2m.converge.core.plugin.PluginActionPropertyDefinition;
import dk.i2m.converge.core.plugin.PluginConfiguration;
import dk.i2m.converge.core.plugin.PluginContext;
import dk.i2m.converge.core.plugin.PropertyDefinitionNotFoundException;
import dk.i2m.converge.core.security.UserAccount;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.language.DefaultTemplateLexer;
import org.apache.commons.lang.StringEscapeUtils;

/**
 * {@link PluginAction} for creating {@link Notification}s. For the
 * {@link CreateNotificationAction.PluginActionProperty#MESSAGE_TEMPLATE} and
 * {@link CreateNotificationAction.PluginActionProperty#LINK_TEMPLATE}
 * properties, the following variables are available:
 *
 * <ul> <li>#{@link CreateNotificationAction.MessageVariable#INITIATOR}
 * containing the {@link UserAccount} that initiated the action</li>
 * <li>#{@link CreateNotificationAction.MessageVariable#CONTENT_ITEM} containing
 * the {@link ContentItem} that is the subject of the action</li>
 * <li>#{@link CreateNotificationAction.MessageVariable#HTML_ENCODED_TITLE}
 * containing an HTML encoded version of the {@link ContentItem#title}</li>
 * </ul>
 *
 * All variables must be enclosed in dollar signs, e.g.
 * {@code $action-initiator$}.
 *
 * @author Allan Lykke Christensen
 */
public class CreateNotificationAction extends PluginAction {

    /**
     * Properties available for the plug-in.
     */
    public enum PluginActionProperty {

        RECIPIENT_USER,
        RECIPIENT_ROLE,
        MESSAGE_TEMPLATE,
        LINK_TEMPLATE
    }

    /**
     * Variables that can be used in the message and link templates.
     */
    public enum MessageVariable {

        CONTENT_ITEM,
        INITIATOR,
        HTML_ENCODED_TITLE
    }
    private List<PluginActionPropertyDefinition> availableProperties = null;
    private static final Logger LOG = Logger.getLogger(CreateNotificationAction.class.getName());

    /**
     * Creates a new instance of {@link CreateNotificationAction}.
     */
    public CreateNotificationAction() {
        onInit();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void onInit() {
        setBundle("dk.i2m.converge.plugins.createnotification.Messages");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public List<PluginActionPropertyDefinition> getAvailableProperties() {
        if (availableProperties == null) {
            availableProperties = new ArrayList<PluginActionPropertyDefinition>();

            availableProperties.add(new PluginActionPropertyDefinition(
                    PluginActionProperty.RECIPIENT_USER.name(),
                    "PROPERTY_RECIPIENT_USER",
                    "PROPERTY_RECIPIENT_USER_TOOLTIP",
                    false,
                    "user_role",
                    false,
                    1));

            availableProperties.add(new PluginActionPropertyDefinition(
                    PluginActionProperty.RECIPIENT_ROLE.name(),
                    "PROPERTY_RECIPIENT_ROLE",
                    "PROPERTY_RECIPIENT_TOOLTIP",
                    false,
                    "user_role",
                    false,
                    2));

            availableProperties.add(new PluginActionPropertyDefinition(
                    PluginActionProperty.MESSAGE_TEMPLATE.name(),
                    "PROPERTY_MESSAGE_TEMPLATE",
                    "PROPERTY_MESSAGE_TEMPLATE_TOOLTIP",
                    true,
                    "textarea",
                    false,
                    3));

            availableProperties.add(new PluginActionPropertyDefinition(
                    PluginActionProperty.LINK_TEMPLATE.name(),
                    "PROPERTY_LINK_TEMPLATE",
                    "PROPERTY_LINK_TEMPLATE_TOOLTIP",
                    false,
                    "textarea",
                    false,
                    4));
        }
        return availableProperties;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public PluginActionPropertyDefinition findPropertyDefinition(String id) throws PropertyDefinitionNotFoundException {
        for (PluginActionPropertyDefinition d : getAvailableProperties()) {
            if (d.getId().equals(id)) {
                return d;
            }
        }
        throw new PropertyDefinitionNotFoundException(id + " not found");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void execute(PluginContext ctx, String itemType, Long itemId, PluginConfiguration cfg, Map<String, List<String>> variables) throws PluginActionException {

        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "Executing {0}", getClass().getName());

            if (variables.isEmpty()) {
                LOG.log(Level.FINE, "There are not variables for the JobQueue");
            } else {
                LOG.log(Level.FINE, "These are the variables sent to the JobQueue");
            }

            for (String key : variables.keySet()) {
                LOG.log(Level.FINE, "{0} = {1}", new Object[]{key, variables.get(key)});
            }
        }

        Object obj = getObjectFromClassName(itemType);

        if (!(obj instanceof ContentItem)) {
            throw new PluginActionException("Item type is not a ContentItem", true);
        }

        ContentItem item;
        try {
            item = ctx.findContentItemById(itemId);
        } catch (DataNotFoundException ex) {
            LOG.log(Level.SEVERE, "ContentItem #{0} does not exist", itemId);
            LOG.log(Level.FINEST, "", ex);
            throw new PluginActionException("ContentItem #" + itemId + " does not exist", true);
        }


//        Map<String, String> properties = stepAction.getPropertiesAsMap();
//
//        if (!properties.containsKey(AlertAction.PROPERTY_MESSAGE)) {
//            LOG.log(Level.WARNING,
//                    "{0} property missing from action properties",
//                    AlertAction.PROPERTY_MESSAGE);
//            return;
//        }
//
//        if (!properties.containsKey(AlertAction.PROPERTY_RECIPIENT_USER)
//                && !properties.containsKey(AlertAction.PROPERTY_RECIPIENT_ROLE)) {
//            LOG.log(Level.WARNING,
//                    "{0} or {1} property missing from action properties",
//                    new Object[]{AlertAction.PROPERTY_RECIPIENT_USER,
//                        AlertAction.PROPERTY_RECIPIENT_ROLE});
//            return;
//        }

        boolean sendToUser = false;
        Long sendToUserRole = 0L;

        boolean sendToRole = false;
        Long sendToRoleRole = 0L;

        if (isPropertyUsed(cfg, PluginActionProperty.RECIPIENT_USER)) {
            sendToUser = true;
            sendToUserRole = Long.valueOf(getSingleProperty(cfg, PluginActionProperty.RECIPIENT_USER));
            LOG.log(Level.CONFIG, "Sending to actors with role: {0}", sendToUserRole);
        }

        if (isPropertyUsed(cfg, PluginActionProperty.RECIPIENT_ROLE)) {
            sendToRole = true;
            sendToRoleRole = Long.valueOf(getSingleProperty(cfg, PluginActionProperty.RECIPIENT_ROLE));
            LOG.log(Level.CONFIG, "Sending to all users with role: {0}", sendToUserRole);
        }

        String link = "";
        if (isPropertyUsed(cfg, PluginActionProperty.LINK_TEMPLATE)) {
            link = getSingleProperty(cfg, PluginActionProperty.LINK_TEMPLATE);
            LOG.log(Level.CONFIG, "Link template: {0}", link);
        }

        String message = "";
        if (isPropertyUsed(cfg, PluginActionProperty.MESSAGE_TEMPLATE)) {
            message = getSingleProperty(cfg, PluginActionProperty.MESSAGE_TEMPLATE);
            LOG.log(Level.CONFIG, "Message template: {0}", message);
        }

        UserAccount user = null;
        if (variables.containsKey("initiator")) {
            String uid = variables.get("initiator").iterator().next();
            LOG.log(Level.CONFIG, "Sending message from user: {0}", uid);
            try {
                user = ctx.findUserAccountByUsername(uid);
            } catch (DataNotFoundException ex) {
                LOG.log(Level.SEVERE, "Unknown initiator: {0}", uid);
            }
        } else {
            LOG.log(Level.CONFIG, "Sending message from system user");
            user = ctx.findSystemUserAccount();
        }

        String notificationMessage = processTemplate(message, item, user);
        link = processTemplate(link, item, user);

        List<UserAccount> usersToNotify = new ArrayList<UserAccount>();

        if (sendToUser) {
            LOG.log(Level.FINE, "Finding actors by role");
            for (ContentItemActor actor : item.getActors()) {
                if (actor.getRole().getId().longValue() == sendToUserRole.longValue()) {
                    usersToNotify.add(actor.getUser());
                }
            }
        }

        if (sendToRole) {
            LOG.log(Level.FINE, "Finding users by role");
            usersToNotify.addAll(ctx.findUserAccountsByRole(sendToRoleRole));
        }

        for (UserAccount ua : usersToNotify) {
            LOG.log(Level.FINE, "Notify:  {0}", new Object[]{ua.getUsername()});
            Notification notification = new Notification();
            notification.setMessage(notificationMessage);
            notification.setAdded(Calendar.getInstance());
            notification.setRecipient(ua);
            notification.setSender(user);
            notification.setLink(link);
            ctx.createNotification(notification);
        }
    }

    private boolean isPropertyUsed(PluginConfiguration cfg, PluginActionProperty property) {
        return cfg.getPropertiesMap().containsKey(property.name());
    }

    private String getSingleProperty(PluginConfiguration cfg, PluginActionProperty property) {
        return cfg.getPropertiesMap().get(property.name()).iterator().next();
    }

    private String processTemplate(String message, ContentItem item, UserAccount user) {
        StringTemplate template = new StringTemplate(message, DefaultTemplateLexer.class);
        template.setAttribute(MessageVariable.CONTENT_ITEM.name(), item);
        template.setAttribute(MessageVariable.INITIATOR.name(), user);
        template.setAttribute(MessageVariable.HTML_ENCODED_TITLE.name(), StringEscapeUtils.escapeHtml(item.getTitle()));
        return template.toString();
    }
}
