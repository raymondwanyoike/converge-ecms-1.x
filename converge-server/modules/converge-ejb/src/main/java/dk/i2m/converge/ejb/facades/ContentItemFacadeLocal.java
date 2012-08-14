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
import dk.i2m.converge.core.content.ContentItemPermission;
import javax.ejb.Local;

/**
 * Local interface for the {@link ContentItemFacadeBean}.
 *
 * @author Allan Lykke Christensen
 */
@Local
public interface ContentItemFacadeLocal {

    /**
     * Finds a {@link ContentItem} by its unique identifier.
     * 
     * @param id
     *          Unique identifier of the {@link ContentItem}
     * @return {@link ContentItem} matching the given {@code id}.
     * @throws DataNotFoundException 
     *          If a {@link ContentItem} with the given {@code id} could not
     *          be found
     */
    ContentItem findContentItemById(Long id) throws DataNotFoundException;

    /**
     * Determines the permission for a given {@link ContentItem} for a given
     * {@link UserAccount}.
     * 
     * @param id
     *          Unique identifier of the {@link ContentItem}
     * @param uid
     *          Unique identifier of the {@link UserAccount}
     * @return {@link ContentItemPermission} defining the permission of the 
     *         {@link ContentItem} for given {@link UserAccount}
     * @throws DataNotFoundException 
     *          If a {@link ContentItem} with the given {@code id} could not be
     *          found
     */
    ContentItemPermission findContentItemPermissionById(Long id, String uid) throws
            DataNotFoundException;

    /**
     * Update the thumbnail cache link for a given {@link ContentItem}.
     * 
     * @param id 
     *          Unique identifier of the {@link ContentItem}.
     */
    void updateThumbnailLink(Long id);
}
