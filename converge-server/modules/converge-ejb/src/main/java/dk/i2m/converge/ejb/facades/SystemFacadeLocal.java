/*
 * Copyright (C) 2010 - 2012 Interactive Media Management
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
import dk.i2m.converge.core.ConfigurationKey;
import dk.i2m.converge.core.DataNotFoundException;
import dk.i2m.converge.core.content.Language;
import dk.i2m.converge.core.logging.LogEntry;
import dk.i2m.converge.core.logging.LogSubject;
import dk.i2m.converge.core.plugin.PluginAction;
import dk.i2m.converge.core.plugin.PluginConfiguration;
import dk.i2m.converge.core.workflow.JobQueue;
import dk.i2m.converge.core.workflow.JobQueueParameter;
import dk.i2m.converge.domain.Property;
import java.util.Iterator;
import java.util.List;
import javax.ejb.Local;

/**
 * Local interface for the system facade enterprise session bean.
 *
 * @author Allan Lykke Christensen
 */
@Local
public interface SystemFacadeLocal {

    boolean sanityCheck();

    List<Property> getSystemProperties();

    /**
     * Updates a {@link List} of system properties.
     *
     * @param properties {@link List} of system properties
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
     * @param key {@link ConfigurationKey} for which to obtain the
     * {@link String} value
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
     * sorted descending by date
     */
    List<Announcement> getPublishedAnnouncements();

    /**
     * Updates an existing {@link Announcement}.
     *
     * @param announcement {@link Announcement} to update
     * @return Updated {@link Announcement}
     */
    Announcement updateAnnouncement(Announcement announcement);

    /**
     * Creates a new {@link Announcement}.
     *
     * @param announcement {@link Announcement} to create
     * @return Created {@link Announcement}
     */
    Announcement createAnnouncement(Announcement announcement);

    /**
     * Delete an existing {@link Announcement}.
     *
     * @param id Unique identifier of the {@link Announcement}
     */
    void deleteAnnouncement(Long id);

    /**
     * Find existing {@link Announcement} in the database.
     *
     * @param id Unique identifier of the {@link Announcement}
     * @return {@link Announcement} matching the {@code id}
     * @throws DataNotFoundException If no {@link Announcement} could be matched
     * to the {@code id}
     */
    Announcement findAnnouncementById(Long id) throws DataNotFoundException;

    List<Language> getLanguages();

    Language updateLanguage(Language language);

    Language createLanguage(Language language);

    void deleteLanguage(Long id) throws ReferentialIntegrityException;

    Language findLanguageById(Long id) throws DataNotFoundException;

    Long createBackgroundTask(String name);

    void removeBackgroundTask(Long id);

    java.util.List<dk.i2m.converge.core.BackgroundTask> getBackgroundTasks();

    java.lang.String getShortApplicationVersion();

    void log(dk.i2m.converge.core.logging.LogSeverity severity,
            java.lang.String message, java.lang.Object origin,
            java.lang.String originId);

    void log(dk.i2m.converge.core.logging.LogSeverity severity,
            java.lang.String message, java.lang.String origin,
            java.lang.String originId);

    void log(dk.i2m.converge.core.logging.LogSeverity severity,
            java.lang.String message, java.util.List<LogSubject> subjects);

    java.util.List<dk.i2m.converge.core.logging.LogEntry> findLogEntries(
            java.lang.String origin,
            java.lang.String originId);

    java.util.List<dk.i2m.converge.core.logging.LogEntry> findLogEntries(
            java.lang.Object origin,
            java.lang.String originId);

    java.util.List<dk.i2m.converge.core.logging.LogEntry> findLogEntries(
            Object origin,
            String originId, int start, int count);

    List<LogEntry> findLogEntries(int start, int count);

    List<LogEntry> findLogEntries();

    void removeLogEntries(Object entryType, String entryId);

    /**
     * Find all the items in the {@link JobQueue} sorted by execution time.
     *
     * @return {@link JobQueue} items sorted by execution time
     */
    List<JobQueue> findJobQueue();

    /**
     * Removes an item from the {@link JobQueue}.
     *
     * @param id Unique identifier of the {@link JobQueue} item
     */
    void removeJobQueue(Long id);

    /**
     * Refreshes and executes the {@link JobQueue}.
     */
    void executeJobQueue();

    /**
     * Adds an entry in the {@link JobQueue}.
     *
     * @param name Name of the job
     * @param typeName Type of item to act on
     * @param typeId Unique identifier of the item to act on
     * @param pluginConfigurationId Unique identifier of the plug-in
     * configuration to execute on the type
     * @param parameters Run-time parameters for the plug-in
     * @return {@link JobQueue} representing the entry in the queue
     * @throws DataNotFoundException If the {@link PluginConfiguration} does not
     * exist
     */
    JobQueue addToJobQueue(String name, String typeName, Long typeId,
            Long pluginConfigurationId, List<JobQueueParameter> parameters)
            throws DataNotFoundException;

    /**
     * Finds a {@link List} of available {@link PluginConfiguration}s.
     *
     * @return {@link List} of available {@link PluginConfiguration}s
     */
    List<PluginConfiguration> findPluginConfigurations();

    /**
     * Finds an existing {@link PluginConfiguration} by its unique identifier.
     *
     * @param id Unique identifier of the {@link PluginConfiguration}
     * @return {@link PluginConfiguration} with the given identified
     * @throws DataNotFoundException If a {@link PluginConfiguration} could not
     * be found with the given identifier
     */
    PluginConfiguration findPluginConfigurationById(Long id) throws
            DataNotFoundException;

    /**
     * Finds discovered {@link PluginAction}s.
     *
     * @return {@link List} of discovered {@link PluginAction}s
     */
    Iterator<PluginAction> findPluginActions();

    /**
     * Creates a new {@link PluginConfiguration}.
     *
     * @param pluginConfiguration {@link PluginConfiguration} to create
     * @return Created {@link PluginConfiguration}
     */
    PluginConfiguration createPluginConfiguration(PluginConfiguration pluginConfiguration);

    /**
     * Initialises a new {@link PluginConfiguration}.
     *
     * @return Default {@link PluginConfiguration}
     */
    PluginConfiguration initPluginConfiguration();

    /**
     * Updates an existing {@link PluginConfiguration}.
     *
     * @param pluginConfiguration {@link PluginConfiguration} to update
     * @return Updated {@link PluginConfiguration}
     */
    PluginConfiguration updatePluginConfiguration(PluginConfiguration pluginConfiguration);

    /**
     * Delete all recorded log entries.
     */
    void clearAllLogEntries();

    /**
     * Delete log entries that are older than the specified date.
     *
     * @param date Keep log entries after this date
     */
    void clearOldLogEntries(java.util.Date date);

    /**
     * Delete log entries older than the number of days to retain according to
     * the application configuration.
     */
    void clearOldLogEntries();

    void deletePluginConfigurationById(Long id);
}
