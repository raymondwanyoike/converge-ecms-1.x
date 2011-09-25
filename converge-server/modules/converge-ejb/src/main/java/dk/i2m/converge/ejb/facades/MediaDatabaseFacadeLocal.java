/*
 * Copyright (C) 2010 Interactive Media Management
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

import dk.i2m.converge.core.content.MediaItem;
import dk.i2m.converge.core.content.MediaItemStatus;
import dk.i2m.converge.core.content.Rendition;
import dk.i2m.converge.core.content.MediaRepository;
import dk.i2m.converge.core.content.NewsItem;
import dk.i2m.converge.core.security.UserAccount;
import dk.i2m.converge.ejb.services.InvalidMediaRepositoryException;
import dk.i2m.converge.ejb.services.MediaRepositoryIndexingException;
import dk.i2m.converge.ejb.services.DataNotFoundException;
import java.util.List;
import java.util.Map;
import javax.ejb.Local;

/**
 * Local interface for the media database enterprise bean.
 *
 * @author Allan Lykke Christensen
 */
@Local
public interface MediaDatabaseFacadeLocal {

    /**
     * Creates a new {@link MediaRepository} in the database.
     *
     * @param mediaRepository
     *          {@link MediaRepository} to create
     * @return {@link MediaRepository} created with auto-generated properties set
     */
    MediaRepository create(MediaRepository mediaRepository);

    /**
     * Deletes an existing {@link MediaRepository} from the database.
     *
     * @param id
     *          Unique identifier of the {@link MediaRepository}
     * @throws DataNotFoundException
     *          If the given {@link MediaRepository} does not exist
     */
    void deleteMediaRepositoryById(Long id) throws DataNotFoundException;

    /**
     * Finds all media repositories in the database.
     *
     * @return {@link List} of all media repositories in the database
     */
    List<MediaRepository> findAllMediaRepositories();

    /**
     * Finds an existing {@link MediaRepository} in the database.
     *
     * @param id
     *          Unique identifier of the {@link MediaRepository}
     * @return {@link MediaRepository} matching the given <code>id</code>
     * @throws DataNotFoundException
     *          If no {@link MediaRepository} could be matched to the given
     *          <code>id</code>
     */
    MediaRepository findMediaRepositoryById(Long id) throws DataNotFoundException;

    /**
     * Indexes enabled media repositories.
     *
     * @throws dk.i2m.converge.service.InvalidMediaRepositoryException
     *          If the location of the {@link MediaRepository} is not valid
     * @throws dk.i2m.converge.service.MediaRepositoryIndexingException
     *          If the location of the {@link MediaRepository} could not be
     *          indexed
     */
    void indexMediaRepositories() throws InvalidMediaRepositoryException, MediaRepositoryIndexingException;

    /**
     * Updates an existing {@link MediaRepository} in the database.
     *
     * @param mediaRepository
     *          {@link MediaRepository} to update
     */
    MediaRepository update(MediaRepository mediaRepository);

    /**
     * Finds a {@link List} of all writable media repositories.
     *
     * @return {@link List} of writable media repositories
     */
    List<MediaRepository> findWritableMediaRepositories();

    /**
     * Finds a {@link List} of media item version labels.
     *
     * @return {@link List} of media item version labels
     */
    List<Rendition> findMediaItemVersionLabels();

    Rendition findMediaItemVersionLabelById(Long id) throws DataNotFoundException;

    Rendition create(Rendition label);

    Rendition update(Rendition update);

    void deleteMediaItemVersionLabel(Long id);

    MediaItem create(MediaItem mediaItem);

    /**
     * Updates an existing {@link MediaItem} in the database. Upon updating the
     * {@link MediaItem} will be updated and possible deleted from the search
     * engine index,
     *
     * @param mediaItem
     *          {@link MediaItem} to update
     * @return Updated {@link MediaItem}
     */
    MediaItem update(MediaItem mediaItem);

    /**
     * Deletes an existing {@link MediaItem} from the database. Upon deletion
     * the {@link MediaItem} will also be removed from the search engine index.
     *
     * @param id
     *          Unique identifier of the {@link MediaItem}
     */
    void deleteMediaItemById(Long id);

    public dk.i2m.converge.core.content.MediaItem findMediaItemById(java.lang.Long id) throws dk.i2m.converge.ejb.services.DataNotFoundException;

    public java.util.List<dk.i2m.converge.core.content.MediaItem> findMediaItemsByStatus(MediaItemStatus status);

    public java.util.List<dk.i2m.converge.core.content.MediaItem> findMediaItemsByOwner(dk.i2m.converge.core.security.UserAccount owner);

    List<MediaItem> findCurrentMediaItems(UserAccount user, Long mediaRepositoryId);

    List<MediaItem> findCurrentMediaItems(UserAccount user, MediaItemStatus mediaItemStatus, Long mediaRepositoryId);

    /**
     * Determines if the given {@link MediaItem} is referenced by a
     * {@link NewsItem}.
     *
     * @param id
     *          Unique identifier of the {@link MediaItem}
     * @return {@code true} if the {@link MediaItem} is referenced, otherwise
     *         {@code false}
     */
    boolean isMediaItemUsed(Long id);

    public void scanDropPoints();
    
}
