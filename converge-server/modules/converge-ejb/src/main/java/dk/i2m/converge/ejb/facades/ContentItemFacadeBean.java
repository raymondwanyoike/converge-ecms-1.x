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
package dk.i2m.converge.ejb.facades;

import dk.i2m.converge.core.DataNotFoundException;
import dk.i2m.converge.core.content.ContentItem;
import dk.i2m.converge.core.content.ContentItemActor;
import dk.i2m.converge.core.content.ContentItemPermission;
import dk.i2m.converge.core.security.UserAccount;
import dk.i2m.converge.core.security.UserRole;
import dk.i2m.converge.ejb.services.ContentItemServiceLocal;
import dk.i2m.converge.ejb.services.DaoServiceLocal;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 * Implementation of the {@link ContentItem} facade giving access to working 
 * with {@link ContentItem}s
 *
 * @author Allan Lykke Christensen
 */
@Stateless
public class ContentItemFacadeBean implements ContentItemFacadeLocal {

    private static final Logger LOG =
            Logger.getLogger(ContentItemFacadeBean.class.getName());

    @EJB private DaoServiceLocal daoService;

    @EJB private ContentItemServiceLocal contentItemService;

    @EJB private UserFacadeLocal userFacade;

    /** {@inheritDoc } */
    @Override
    public ContentItem findContentItemById(Long id) throws DataNotFoundException {
        return daoService.findById(ContentItem.class, id);
    }

    /** {@inheritDoc } */
    @Override
    public ContentItemPermission findContentItemPermissionById(Long id,
            String uid) throws
            DataNotFoundException {

        ContentItem ci = findContentItemById(id);
        UserAccount ua = userFacade.findById(uid);

        // Default state - unauthotised
        ContentItemPermission permission = ContentItemPermission.UNAUTHORIZED;

        // Check if user is among the actors
        for (ContentItemActor a : ci.getActors()) {
            if (ua.equals(a.getUser())) {
                permission = ContentItemPermission.ACTOR;
            }
        }

        // Check if the user has the role required for the state
        UserRole stateRole = ci.getCurrentState().getActorRole();

        if (ci.getCurrentState().isGroupPermission()) {
            if (ua.getUserRoles().contains(stateRole)) {
                permission = ContentItemPermission.ROLE;
            }
        } else {
            for (ContentItemActor a : ci.getActors()) {
                if (stateRole.equals(a.getRole()) && ua.equals(a.getUser())) {
                    permission = ContentItemPermission.USER;
                    break;
                }
            }
        }

        return permission;

    }

    @Override
    public void updateThumbnailLink(Long id) {
        try {
            ContentItem ci = findContentItemById(id);
            contentItemService.updateThumbnail(ci);
        } catch (DataNotFoundException ex) {
            LOG.log(Level.WARNING, "Could not update ThumbnailLink for ContentItem #"
                    + id, ex);
        }

    }
}
