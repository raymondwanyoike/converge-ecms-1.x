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

import dk.i2m.converge.core.content.MediaItem;
import dk.i2m.converge.core.content.MediaItemStatus;
import dk.i2m.converge.core.content.Rendition;
import dk.i2m.converge.core.content.MediaRepository;
import dk.i2m.converge.core.content.NewsItemMediaAttachment;
import dk.i2m.converge.core.search.QueueEntryOperation;
import dk.i2m.converge.core.search.QueueEntryType;
import dk.i2m.converge.core.security.UserAccount;
import dk.i2m.converge.ejb.services.DaoServiceLocal;
import dk.i2m.converge.ejb.services.InvalidMediaRepositoryException;
import dk.i2m.converge.ejb.services.MediaRepositoryIndexingException;
import dk.i2m.converge.ejb.services.QueryBuilder;
import dk.i2m.converge.ejb.services.DataNotFoundException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 * Stateless enterprise java bean providing a facade for interacting with
 * catalogues.
 *
 * @author Allan Lykke Christensen
 */
@Stateless
public class MediaDatabaseFacadeBean implements MediaDatabaseFacadeLocal {

    private static final Logger LOG = Logger.getLogger(MediaDatabaseFacadeBean.class.getName());

    @EJB private DaoServiceLocal daoService;

    @EJB private SearchEngineLocal searchEngineService;

    /**{@inheritDoc } */
    @Override
    public MediaRepository create(MediaRepository mediaRepository) {
        return daoService.create(mediaRepository);
    }

    /**{@inheritDoc } */
    @Override
    public List<MediaRepository> findAllMediaRepositories() {
        return daoService.findAll(MediaRepository.class);
    }

    /**{@inheritDoc } */
    @Override
    public List<MediaRepository> findWritableMediaRepositories() {
        return daoService.findWithNamedQuery(MediaRepository.FIND_WRITABLE);
    }

    /**{@inheritDoc } */
    @Override
    public MediaRepository findMediaRepositoryById(Long id) throws DataNotFoundException {
        return daoService.findById(MediaRepository.class, id);
    }

    /**{@inheritDoc } */
    @Override
    public MediaRepository update(MediaRepository mediaRepository) {
        return daoService.update(mediaRepository);
    }

    /**{@inheritDoc } */
    @Override
    public void deleteMediaRepositoryById(Long id) throws DataNotFoundException {
        daoService.delete(MediaRepository.class, id);
    }

    /**{@inheritDoc } */
    @Override
    public void indexMediaRepositories() throws InvalidMediaRepositoryException, MediaRepositoryIndexingException {
        Map<String, Object> parameters = QueryBuilder.with("status", MediaItemStatus.APPROVED).parameters();
        List<MediaItem> items = daoService.findWithNamedQuery(MediaItem.FIND_BY_STATUS, parameters);

        for (MediaItem item : items) {
            //try {
            //byte[] file = FileUtils.getBytes(new URL(item.getAbsoluteFilename()));
            //generateThumbnail(file, item);
            searchEngineService.addToIndexQueue(QueueEntryType.MEDIA_ITEM, item.getId(), QueueEntryOperation.UPDATE);
            //} catch (IOException ex) {
            //    logger.log(Level.SEVERE, ex.getMessage());
            //    logger.log(Level.FINE, "", ex);
            //}
        }
    }

    /** {@inheritDoc } */
    @Override
    public List<Rendition> findMediaItemVersionLabels() {
        return daoService.findAll(Rendition.class);
    }

    /** {@inheritDoc } */
    @Override
    public Rendition findMediaItemVersionLabelById(Long id) throws DataNotFoundException {
        return daoService.findById(Rendition.class, id);
    }

    /** {@inheritDoc } */
    @Override
    public Rendition create(Rendition label) {
        return daoService.create(label);
    }

    /** {@inheritDoc } */
    @Override
    public Rendition update(Rendition label) {
        return daoService.update(label);
    }

    /** {@inheritDoc } */
    @Override
    public void deleteMediaItemVersionLabel(Long id) {
        daoService.delete(Rendition.class, id);
    }

    /** {@inheritDoc } */
    @Override
    public MediaItem create(MediaItem mediaItem) {
        mediaItem.setCreated(Calendar.getInstance());
        mediaItem.setUpdated(mediaItem.getCreated());

        if (mediaItem.getId() == null) {
            mediaItem = daoService.create(mediaItem);
            if (mediaItem.getStatus() == null || !mediaItem.getStatus().equals(MediaItemStatus.APPROVED)) {
                searchEngineService.addToIndexQueue(QueueEntryType.MEDIA_ITEM, mediaItem.getId(), QueueEntryOperation.REMOVE);
            } else {
                searchEngineService.addToIndexQueue(QueueEntryType.MEDIA_ITEM, mediaItem.getId(), QueueEntryOperation.UPDATE);
            }
        }
        return mediaItem;
    }

    /** {@inheritDoc } */
    @Override
    public MediaItem update(MediaItem mediaItem) {
        mediaItem.setUpdated(Calendar.getInstance());
        mediaItem = daoService.update(mediaItem);

        if (mediaItem.getStatus() == null || !mediaItem.getStatus().equals(MediaItemStatus.APPROVED)) {
            searchEngineService.addToIndexQueue(QueueEntryType.MEDIA_ITEM, mediaItem.getId(), QueueEntryOperation.REMOVE);
        } else {
            searchEngineService.addToIndexQueue(QueueEntryType.MEDIA_ITEM, mediaItem.getId(), QueueEntryOperation.UPDATE);
        }

        return mediaItem;
    }

    /** {@inheritDoc } */
    @Override
    public void deleteMediaItemById(Long id) {
        searchEngineService.addToIndexQueue(QueueEntryType.MEDIA_ITEM, id, QueueEntryOperation.REMOVE);
        daoService.delete(MediaItem.class, id);
    }

    @Override
    public MediaItem findMediaItemById(Long id) throws DataNotFoundException {
        return daoService.findById(MediaItem.class, id);
    }

    @Override
    public List<MediaItem> findMediaItemsByStatus(MediaItemStatus status) {
        Map<String, Object> params = QueryBuilder.with("status", status).parameters();
        return daoService.findWithNamedQuery(MediaItem.FIND_BY_STATUS, params);
    }

    @Override
    public List<MediaItem> findMediaItemsByOwner(UserAccount owner) {
        Map<String, Object> params = QueryBuilder.with("owner", owner).parameters();
        return daoService.findWithNamedQuery(MediaItem.FIND_BY_OWNER, params);
    }

    @Override
    public List<MediaItem> findCurrentMediaItems(UserAccount user, Long mediaRepositoryId) {
        try {
            MediaRepository mr = daoService.findById(MediaRepository.class, mediaRepositoryId);
            Map<String, Object> params = QueryBuilder.with("user", user).and("mediaRepository", mr).parameters();

            List<MediaItem> items = new ArrayList<MediaItem>();

            items.addAll(daoService.findWithNamedQuery(MediaItem.FIND_CURRENT_AS_OWNER, params));
            items.addAll(daoService.findWithNamedQuery(MediaItem.FIND_CURRENT_AS_EDITOR, params));

            Set set = new HashSet(items);
            return new ArrayList(set);
        } catch (DataNotFoundException ex) {
            return Collections.EMPTY_LIST;
        }
    }

    @Override
    public List<MediaItem> findCurrentMediaItems(UserAccount user, MediaItemStatus status, Long mediaRepositoryId) {
        try {
            MediaRepository mr = daoService.findById(MediaRepository.class, mediaRepositoryId);
            Map<String, Object> params = QueryBuilder.with("user", user).and("status", status).and("mediaRepository", mr).parameters();
            return daoService.findWithNamedQuery(MediaItem.FIND_BY_OWNER_AND_STATUS, params, 200);
        } catch (DataNotFoundException ex) {
            return Collections.EMPTY_LIST;
        }
    }

    /** {@inheritDoc } */
    @Override
    public boolean isMediaItemUsed(Long id) {
        try {
            MediaItem mediaItem = daoService.findById(MediaItem.class, id);
            Map<String, Object> params = QueryBuilder.with("mediaItem", mediaItem).parameters();
            List results = daoService.findWithNamedQuery(NewsItemMediaAttachment.FIND_BY_MEDIA_ITEM, params);

            if (results.isEmpty()) {
                return false;
            } else {
                return true;
            }

        } catch (DataNotFoundException ex) {
            return false;
        }
    }

    /**
     * Scans all the active catalogue drop points for files and processes new files.
     */
    public void scanDropPoints() {
        //TODO: Drop point scanning not yet complete
//        List<MediaRepository> catalogues = daoService.findAll(MediaRepository.class);
//
//        for (MediaRepository catalogue : catalogues) {
//            if (catalogue.isEnabled() && !catalogue.isReadOnly()) {
//                logger.log(Level.INFO, "Scanning ''{0}'' drop point [{1}]", new Object[]{catalogue.getName(), catalogue.getWatchLocation()});
//                File dropPoint = new File(catalogue.getWatchLocation());
//                if (!dropPoint.exists()) {
//                    logger.log(Level.WARNING, "{0} does not exist", new Object[]{catalogue.getWatchLocation()});
//                    continue;
//                }
//
//                if (!dropPoint.isDirectory()) {
//                    logger.log(Level.WARNING, "{0} is not a directory", new Object[]{catalogue.getWatchLocation()});
//                    continue;
//                }
//
//                for (File found : dropPoint.listFiles()) {
//                    logger.log(Level.INFO, "Found ''{0}''", found.getName());
//                    try {
//                        Map<String, String> xmp = indexXmp(FileUtils.getBytes(found));
//                        for (String key : xmp.keySet()) {
//                            logger.log(Level.INFO, "{0} = {1}", new Object[]{key, xmp.get(key)});
//                        }
//
//                    } catch (IOException ex) {
//                        Logger.getLogger(MediaDatabaseFacadeBean.class.getName()).log(Level.SEVERE, null, ex);
//                    } catch (CannotIndexException ex) {
//                        Logger.getLogger(MediaDatabaseFacadeBean.class.getName()).log(Level.SEVERE, null, ex);
//                    }
//                }
//            }
//        }
    }
}
