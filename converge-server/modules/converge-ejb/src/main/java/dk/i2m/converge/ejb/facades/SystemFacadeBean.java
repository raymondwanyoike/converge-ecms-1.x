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
import dk.i2m.converge.core.AppVersion;
import dk.i2m.converge.core.BackgroundTask;
import dk.i2m.converge.core.ConfigurationKey;
import dk.i2m.converge.core.content.Language;
import dk.i2m.converge.core.content.NewsItem;
import dk.i2m.converge.core.logging.LogEntry;
import dk.i2m.converge.core.logging.LogSeverity;
import dk.i2m.converge.core.logging.LogSubject;
import dk.i2m.converge.core.plugin.Plugin;
import dk.i2m.converge.core.plugin.PluginManager;
import dk.i2m.converge.core.security.UserAccount;
import dk.i2m.converge.core.utils.BeanComparator;
import dk.i2m.converge.core.workflow.WorkflowState;
import dk.i2m.converge.core.workflow.WorkflowStateTransition;
import dk.i2m.converge.domain.Property;
import dk.i2m.converge.ejb.services.*;
import java.io.File;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

/**
 * Session bean providing access to information about the system.
 *
 * @author Allan Lykke Christensen
 */
@Stateless
public class SystemFacadeBean implements SystemFacadeLocal {

    private static final Logger LOG = Logger.getLogger(SystemFacadeBean.class.
            getName());

    @EJB private UserServiceLocal userService;

    @EJB private ConfigurationServiceLocal cfgService;

    @EJB private DaoServiceLocal daoService;

    @EJB private NewsItemFacadeLocal newsItemFacade;

    @EJB private TimerServiceLocal timerService;

    /**
     * Creates a new instance of {@link SystemFacadeBean}.
     */
    public SystemFacadeBean() {
    }

    /**
     * Conducts a sanity check of the system.
     *
     * @return {@code true} if the sanity of the system is OK, otherwise {@code false}
     */
    @Override
    public boolean sanityCheck() {
        removeAllBackgroundTasks();

        int userCount = userService.findAll().size();
        LOG.log(Level.INFO,
                "{0} user {0, choice, 0#accounts|1#account|2#accounts} in the system",
                userCount);
        getPlugins();

        LOG.log(Level.INFO,
                "{0} stale {0, choice, 0#locks|1#lock|2#locks} removed",
                newsItemFacade.revokeAllLocks());

        String userPhotoDirectory = cfgService.getString(
                ConfigurationKey.WORKING_DIRECTORY) + System.getProperty(
                "file.separator") + "users" + System.getProperty(
                "file.separator");

        LOG.log(Level.INFO, "Checking if user photo directory ({0}) exists",
                new Object[]{userPhotoDirectory});

        File file = new File(userPhotoDirectory);
        if (!file.exists()) {
            LOG.log(Level.INFO, "{0} does not exist. Creating  directory",
                    new Object[]{userPhotoDirectory});
            file.mkdirs();
        } else {
            LOG.log(Level.INFO, "{0} exists", new Object[]{userPhotoDirectory});
        }

        timerService.startTimers();

        return true;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Map<String, Plugin> getPlugins() {
        return PluginManager.getInstance().getPlugins();
    }

    /**
     * Gets a {@link List} of the system properties and their values.
     *
     * @return {@link List} of system properties
     */
    @Override
    public List<Property> getSystemProperties() {
        List<Property> properties = new ArrayList<Property>();
        for (ConfigurationKey cfgKey : ConfigurationKey.values()) {
            if (!(cfgKey.equals(ConfigurationKey.VERSION) || cfgKey.equals(
                    ConfigurationKey.BUILD_TIME) || cfgKey.equals(
                    ConfigurationKey.APPLICATION_NEWSFEED))) {
                String key = cfgKey.name();
                String value = this.cfgService.getString(cfgKey);
                properties.add(new Property(key, value));
            }
        }
        return properties;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void updateSystemProperties(List<Property> properties) {
        for (Property property : properties) {
            this.cfgService.set(ConfigurationKey.valueOf(property.getKey()),
                    property.getValue().replaceAll(",", "\\,"));
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public String getProperty(ConfigurationKey key) {
        return cfgService.getString(key);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public String getApplicationVersion() {
        return cfgService.getLongVersion();
    }

    @Override
    public String getShortApplicationVersion() {
        return cfgService.getVersion();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public List<Announcement> getAnnouncements() {
        return daoService.findAll(Announcement.class);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public List<Announcement> getPublishedAnnouncements() {
        return daoService.findWithNamedQuery(Announcement.FIND_PUBLISHED);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Announcement updateAnnouncement(Announcement announcement) {
        return daoService.update(announcement);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Announcement createAnnouncement(Announcement announcement) {
        return daoService.create(announcement);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void deleteAnnouncement(Long id) {
        daoService.delete(Announcement.class, id);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Announcement findAnnouncementById(Long id) throws
            DataNotFoundException {
        return daoService.findById(Announcement.class, id);
    }

    /**
     * Gets all {@link Language}s from the database.
     *
     * @return {@link List} of all {@link Language}s in the database
     */
    @Override
    public List<Language> getLanguages() {
        return daoService.findAll(Language.class);
    }

    /**
     * Updates an existing {@link Language}.
     *
     * @param language {@link Language} to update
     * @return Updated {@link Language}
     */
    @Override
    public Language updateLanguage(Language language) {
        return daoService.update(language);
    }

    /**
     * Creates a new {@link Language}.
     *
     * @param language {@link Language} to create
     * @return Created {@link Language}
     */
    @Override
    public Language createLanguage(Language language) {
        return daoService.create(language);
    }

    /**
     * Delete an existing {@link Language}.
     *
     * @param id Unique identifier of the {@link Language}
     * @throws ReferentialIntegrityException If referential integrity is broken
     * by deleting the {@link Language}
     */
    @Override
    public void deleteLanguage(Long id) throws ReferentialIntegrityException {
        // TODO: Check if the language is being used in an Outlet or NewsItem
        daoService.delete(Language.class, id);
    }

    /**
     * Find existing {@link Language} in the database.
     *
     * @param id Unique identifier of the {@link Language}
     * @return {@link Language} matching the {@code id}
     * @throws DataNotFoundException If no {@link Language} could be matched to
     * the {@code id}
     */
    @Override
    public Language findLanguageById(Long id) throws DataNotFoundException {
        return daoService.findById(Language.class, id);
    }

    @Override
    public List<AppVersion> getVersionsForMigration() {
        try {
            AppVersion lastMigration =
                    daoService.findObjectWithNamedQuery(AppVersion.class,
                    AppVersion.FIND_LATEST_MIGRATIONS, Collections.EMPTY_MAP);

            Map<String, Object> parameters = QueryBuilder.with("fromVersion",
                    lastMigration.getToVersion()).parameters();
            return daoService.findWithNamedQuery(
                    AppVersion.FIND_BY_MIGRATION_AVAILABLE, parameters);

        } catch (DataNotFoundException ex) {
            LOG.log(Level.WARNING,
                    "Unable to find migration history. Latest migration is missing");
            return Collections.EMPTY_LIST;
        }
    }

    /**
     * Upgrades the database from one version to another.
     *
     * @param version Version to upgrade
     * @return Status of the upgrade
     */
    @Override
    public boolean upgrade(AppVersion version) {
        if (version == null) {
            return false;
        }

        if (version.getFromVersion().equals("1.0.8") && version.getToVersion().
                equals("1.0.9")) {
            // Update the submitted status
            Number numberOfNewsItems = daoService.count(NewsItem.class, "id");

            LOG.log(Level.INFO, "Migrating {0}", numberOfNewsItems.intValue());

            int page = 1000;
            for (int i = 0; i < numberOfNewsItems.intValue(); i += page) {
                LOG.log(Level.INFO, "Migrating records {0} to {1}",
                        new Object[]{i, i + page});
                List<NewsItem> newsItems = daoService.findAll(NewsItem.class, i,
                        page);


                for (NewsItem newsItem : newsItems) {
                    List<WorkflowStateTransition> history =
                            newsItem.getHistory();
                    if (history.size() > 1) {
                        Collections.sort(history, new BeanComparator("id"));

                        // Get the second state
                        WorkflowStateTransition candidate = history.get(1);

                        try {
                            // If the second state is not trash, mark it as the submitted state
                            WorkflowState candidateState = candidate.getState();
                            WorkflowState trashState = newsItem.getOutlet().
                                    getWorkflow().getTrashState();

                            if (candidateState == null) {
                                LOG.log(Level.WARNING,
                                        "The submitted workflow state of {0} no longer exist",
                                        newsItem.getId());
                            } else {
                                if (!candidateState.equals(trashState)) {
                                    candidate.setSubmitted(true);
                                }
                            }
                        } catch (Exception ex) {
                            LOG.log(Level.WARNING, "Couldn't update NewsItem "
                                    + newsItem.getId() + ". " + ex.getMessage(),
                                    ex);
                        }
                    }
                }
            }

            version.setMigrated(true);
            version.setMigratedDate(java.util.Calendar.getInstance().getTime());

            daoService.update(version);

            return true;
        }

        return false;
    }

    /**
     * Indicate that a {@link BackgroundTask} is running.
     *
     * @param name Name of the {@link BackgroundTask}
     * @return Unique identifier of the {@link BackgroundTask}
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Long createBackgroundTask(String name) {
        BackgroundTask task = new BackgroundTask();
        task.setTaskStart(Calendar.getInstance().getTime());
        task.setName(name);
        task = daoService.create(task);
        return task.getId();
    }

    /**
     * Indicate that a {@link BackgroundTask} has completed.
     *
     * @param id * Unique identifier of the {@link BackgroundTask}
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void removeBackgroundTask(Long id) {
        daoService.delete(BackgroundTask.class, id);
    }

    /**
     * Gets all running {@link BackgroundTask}s.
     *
     * @return {@link List} of running {@link BackgroundTask}s
     */
    @Override
    public List<BackgroundTask> getBackgroundTasks() {
        return daoService.findAll(BackgroundTask.class);
    }

    private void removeAllBackgroundTasks() {
        for (BackgroundTask t : getBackgroundTasks()) {
            removeBackgroundTask(t.getId());
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void log(LogSeverity severity, UserAccount actor,
            String message, String origin, String originId) {
        LogEntry entry = new LogEntry(severity, message, origin, originId);
        entry.setDate(Calendar.getInstance().getTime());
        entry.setActor(actor);
        daoService.create(entry);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void log(dk.i2m.converge.core.logging.LogSeverity severity,
            dk.i2m.converge.core.security.UserAccount actor,
            java.lang.String message, java.util.List<LogSubject> subjects) {
        LogEntry entry = new LogEntry(severity, message);
        entry.setDate(Calendar.getInstance().getTime());
        entry.setActor(actor);
        for (LogSubject subject : subjects) {
            entry.addSubject(subject);
        }

        daoService.create(entry);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void log(LogSeverity severity, UserAccount actor,
            String message, Object origin, String originId) {
        log(severity, actor, message, origin.getClass().getName(), originId);
    }

    @Override
    public List<LogEntry> findLogEntries(String origin, String originId) {
        Map<String, Object> parameters =
                QueryBuilder.with(LogEntry.PARAMETER_ENTITY, origin).and(
                LogEntry.PARAMETER_ENTITY_ID, originId).
                parameters();
        return daoService.findWithNamedQuery(LogEntry.FIND_BY_ENTITY, parameters);
    }

    @Override
    public List<LogEntry> findLogEntries(Object origin, String originId) {
        Map<String, Object> parameters =
                QueryBuilder.with(LogEntry.PARAMETER_ENTITY, origin.getClass().
                getName()).and(
                LogEntry.PARAMETER_ENTITY_ID, originId).parameters();
        return daoService.findWithNamedQuery(LogEntry.FIND_BY_ENTITY, parameters);
    }

    @Override
    public List<LogEntry> findLogEntries(Object origin, String originId,
            int start, int count) {
        Map<String, Object> parameters =
                QueryBuilder.with(LogEntry.PARAMETER_ENTITY, origin.getClass().
                getName()).and(
                LogEntry.PARAMETER_ENTITY_ID, originId).parameters();
        return daoService.findWithNamedQuery(LogEntry.FIND_BY_ENTITY, parameters,
                start, count);
    }

    @Override
    public List<LogEntry> findLogEntries(int start, int count) {
        return daoService.findAll(LogEntry.class, start, count, "date", false);
    }

    @Override
    public List<LogEntry> findLogEntries() {
        return daoService.findAll(LogEntry.class, "date", false);
    }

    @Override
    public void removeLogEntries(Object entryType, String entryId) {
        List<LogEntry> entries = findLogEntries(entryType, entryId);
        for (LogEntry entry : entries) {
            daoService.delete(LogEntry.class, entry.getId());
        }
    }
}
