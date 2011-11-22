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

import dk.i2m.converge.core.Announcement;
import dk.i2m.converge.core.content.Language;
import dk.i2m.converge.domain.Property;
import dk.i2m.converge.core.ConfigurationKey;
import dk.i2m.converge.ejb.services.DataNotFoundException;
import java.util.List;
import javax.ejb.Local;

/**
 * Local interface for the system facade enterprise session bean.
 *
 * @author Allan Lykke Christensen
 */
@Local
public interface SystemFacadeLocal {

    /**
     * Conducts a sanity check of the system.
     *
     * @return <code>true</code> if the sanity of the system is OK, otherwise
     * <code>false</code>
     */
    boolean sanityCheck();

    /**
     * Gets a {@link List} of the system properties and their values.
     *
     * @return {@link List} of system properties
     */
    List<Property> getSystemProperties();

    /**
     * Updates a {@link List} of system properties.
     *
     * @param properties
     * {@link List} of system properties
     */
    void updateSystemProperties(List<Property> properties);

    /**
     * Gets the version of the currently installed system.
     * 
     * @return Version of the currently installed system
     */
    String getApplicationVersion();

    /*
     * Gets a {@link Map} of the discovered {@link Plugin}s.
     *
     * @return {@link Map} of discovered {@link Plugin}s
     */
    java.util.Map<java.lang.String, dk.i2m.converge.core.plugin.Plugin> getPlugins();

    /**
     * Gets the {@link String} value of a given {@link ConfigurationKey}.
     *
     * @param key
     *          {@link ConfigurationKey} for which to obtain the {@link String}
     *          value
     * @return {@link String} value of the {@link ConfigurationKey}
     */
    String getProperty(ConfigurationKey key);

    /**
     * Gets all {@link Announcement}s from the database.
     *
     * @return {@link List} of all {@link Announcement}s in the database
     */
    List<Announcement> getAnnouncements();

    /**
     * Gets all published {@link Announcement}s from the database
     *
     * @return {@link List} of published {@link Announcement}s in the database
     *         sorted descending by date
     */
    List<Announcement> getPublishedAnnouncements();

    /**
     * Updates an existing {@link Announcement}.
     *
     * @param announcement
     *          {@link Announcement} to update
     * @return Updated {@link Announcement}
     */
    Announcement updateAnnouncement(Announcement announcement);

    /**
     * Creates a new {@link Announcement}.
     *
     * @param announcement
     *          {@link Announcement} to create
     * @return Created {@link Announcement}
     */
    Announcement createAnnouncement(Announcement announcement);

    /**
     * Delete an existing {@link Announcement}.
     *
     * @param id
     *          Unique identifier of the {@link Announcement}
     */
    void deleteAnnouncement(Long id);

    /**
     * Find existing {@link Announcement} in the database.
     *
     * @param id
     *          Unique identifier of the {@link Announcement}
     * @return {@link Announcement} matching the {@code id}
     * @throws DataNotFoundException
     *          If no {@link Announcement} could be matched to the {@code id}
     */
    Announcement findAnnouncementById(Long id) throws DataNotFoundException;

    /**
     * Gets all {@link Language}s from the database.
     *
     * @return {@link List} of all {@link Language}s in the database
     */
    List<Language> getLanguages();

    /**
     * Updates an existing {@link Language}.
     *
     * @param language
     *          {@link Language} to update
     * @return Updated {@link Language}
     */
    Language updateLanguage(Language language);

    /**
     * Creates a new {@link Language}.
     *
     * @param language
     *          {@link Language} to create
     * @return Created {@link Language}
     */
    Language createLanguage(Language language);

    /**
     * Delete an existing {@link Language}.
     *
     * @param id
     *          Unique identifier of the {@link Language}
     * @throws ReferentialIntegrityException
     *          If referential integrity is broken by deleting the
     *          {@link Language}
     */
    void deleteLanguage(Long id) throws ReferentialIntegrityException;

    /**
     * Find existing {@link Language} in the database.
     *
     * @param id
     *          Unique identifier of the {@link Language}
     * @return {@link Language} matching the {@code id}
     * @throws DataNotFoundException
     *          If no {@link Language} could be matched to the {@code id}
     */
    Language findLanguageById(Long id) throws DataNotFoundException;

    boolean upgrade(dk.i2m.converge.core.AppVersion version);

    java.util.List<dk.i2m.converge.core.AppVersion> getVersionsForMigration();

    Long createBackgroundTask(String name);

    void removeBackgroundTask(Long id);
    
    java.util.List<dk.i2m.converge.core.BackgroundTask> getBackgroundTasks();

    java.lang.String getShortApplicationVersion();
}
