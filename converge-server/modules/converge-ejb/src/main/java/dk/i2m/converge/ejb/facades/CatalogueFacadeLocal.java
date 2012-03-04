/*
 * Copyright (C) 2010 - 2011 Interactive Media Management
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

import dk.i2m.converge.core.content.catalogue.*;
import dk.i2m.converge.core.newswire.NewswireItem;
import dk.i2m.converge.core.security.UserAccount;
import dk.i2m.converge.ejb.services.DataNotFoundException;
import dk.i2m.converge.ejb.services.InvalidMediaRepositoryException;
import dk.i2m.converge.ejb.services.MediaRepositoryIndexingException;
import java.io.IOException;
import java.util.List;
import javax.ejb.Local;

/**
 * Local interface for the {@link CatalogueFacadeBean}.
 *
 * @author Allan Lykke Christensen
 */
@Local
public interface CatalogueFacadeLocal {

    Catalogue create(Catalogue catalogue);

    Catalogue update(Catalogue catalogue);

    void deleteCatalogueById(Long id) throws DataNotFoundException;

    List<Catalogue> findAllCatalogues();

    Catalogue findCatalogueById(Long id) throws DataNotFoundException;

    void indexCatalogues() throws InvalidMediaRepositoryException, MediaRepositoryIndexingException;

    void scanDropPoints();

    List<Catalogue> findWritableCatalogues();

    List<Rendition> findRenditions();

    Rendition findRenditionById(Long id) throws DataNotFoundException;

    Rendition findRenditionByName(String name) throws DataNotFoundException;

    Rendition create(Rendition rendition);

    Rendition update(Rendition rendition);

    void deleteRendition(Long id);

    MediaItem create(MediaItem mediaItem);

    MediaItem create(NewswireItem newswireItem, Catalogue catalogue);

    MediaItem update(MediaItem mediaItem);

    void deleteMediaItemById(Long id);

    dk.i2m.converge.core.content.catalogue.MediaItem findMediaItemById(java.lang.Long id) throws dk.i2m.converge.ejb.services.DataNotFoundException;

    java.util.List<dk.i2m.converge.core.content.catalogue.MediaItem> findMediaItemsByStatus(MediaItemStatus status);

    java.util.List<dk.i2m.converge.core.content.catalogue.MediaItem> findMediaItemsByOwner(dk.i2m.converge.core.security.UserAccount owner);

    List<MediaItem> findCurrentMediaItems(UserAccount user, Long catalogueId);

    List<MediaItem> findCurrentMediaItems(UserAccount user, MediaItemStatus mediaItemStatus, Long catalogueId);

    boolean isMediaItemUsed(Long id);

    java.util.List<dk.i2m.converge.core.content.catalogue.MediaItemUsage> getMediaItemUsage(java.lang.Long id) throws DataNotFoundException;

    java.lang.String archive(java.io.File file, dk.i2m.converge.core.content.catalogue.Catalogue catalogue, java.lang.String fileName) throws java.io.IOException;

    dk.i2m.converge.core.content.catalogue.MediaItemRendition update(dk.i2m.converge.core.content.catalogue.MediaItemRendition rendition);

    void deleteMediaItemRenditionById(java.lang.Long id);

    dk.i2m.converge.core.content.catalogue.MediaItemRendition create(java.io.File file, dk.i2m.converge.core.content.catalogue.MediaItem item, dk.i2m.converge.core.content.catalogue.Rendition rendition, String filename, String contentType) throws IOException;

    dk.i2m.converge.core.content.catalogue.MediaItemRendition update(java.io.File file, java.lang.String filename, java.lang.String contentType, dk.i2m.converge.core.content.catalogue.MediaItemRendition mediaItemRendition) throws java.io.IOException;

    CatalogueHookInstance createCatalogueAction(CatalogueHookInstance action);

    CatalogueHookInstance updateCatalogueAction(CatalogueHookInstance action);

    void executeBatchHook(dk.i2m.converge.core.content.catalogue.CatalogueHookInstance hookInstance, java.lang.Long catalogueId) throws dk.i2m.converge.ejb.services.DataNotFoundException;

    void executeHook(java.lang.Long mediaItemId, java.lang.Long hookInstanceId) throws dk.i2m.converge.ejb.services.DataNotFoundException;

    java.util.List<dk.i2m.converge.core.content.catalogue.Catalogue> findCataloguesByUser(java.lang.String username);
}
