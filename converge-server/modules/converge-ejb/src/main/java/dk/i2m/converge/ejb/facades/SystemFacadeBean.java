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
import dk.i2m.converge.core.BackgroundTask;
import dk.i2m.converge.core.ConfigurationKey;
import dk.i2m.converge.core.DataNotFoundException;
import dk.i2m.converge.core.content.Language;
import dk.i2m.converge.core.logging.LogEntry;
import dk.i2m.converge.core.logging.LogSeverity;
import dk.i2m.converge.core.logging.LogSubject;
import dk.i2m.converge.core.newswire.NewswireService;
import dk.i2m.converge.core.plugin.*;
import dk.i2m.converge.core.workflow.JobQueue;
import dk.i2m.converge.core.workflow.JobQueueParameter;
import dk.i2m.converge.core.workflow.JobQueueStatus;
import dk.i2m.converge.domain.Property;
import dk.i2m.converge.ejb.services.*;
import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
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
    @EJB
    private UserServiceLocal userService;
    @EJB
    private ConfigurationServiceLocal cfgService;
    @EJB
    private DaoServiceLocal daoService;
    @EJB
    private NewsItemFacadeLocal newsItemFacade;
    @EJB
    private TimerServiceLocal timerService;
    @EJB
    private PluginContextBeanLocal pluginContext;
    @EJB
    private NotificationServiceLocal notificationService;
    @Resource
    private SessionContext ctx;

    /**
     * Creates a new instance of {@link SystemFacadeBean}.
     */
    public SystemFacadeBean() {
    }

    /**
     * Conducts a sanity check of the system.
     *
     * @return {@code true} if the sanity of the system is OK, otherwise
     * {@code false}
     */
    @Override
    public boolean sanityCheck() {
        removeAllBackgroundTasks();
        int reset = removeAllNewswireProcessing();
        LOG.log(Level.INFO,
                "{0} newswire {0, choice, 0#services|1#service|2#services} reset",
                reset);

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
        PluginService pluginService = PluginManager.createPluginService();

        return true;
    }

    @Override
    public Iterator<PluginAction> findPluginActions() {
        return PluginManager.createPluginService().getPlugins();
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
    public void log(LogSeverity severity,
            String message, String origin, String originId) {
        LogEntry entry = new LogEntry(severity, message, origin, originId);
        entry.setDate(Calendar.getInstance().getTime());
        daoService.create(entry);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void log(dk.i2m.converge.core.logging.LogSeverity severity,
            java.lang.String message, java.util.List<LogSubject> subjects) {
        LogEntry entry = new LogEntry(severity, message);
        entry.setDate(Calendar.getInstance().getTime());
        for (LogSubject subject : subjects) {
            entry.addSubject(subject);
        }

        daoService.create(entry);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void log(LogSeverity severity, String message, Object origin,
            String originId) {
        log(severity, message, origin.getClass().getName(), originId);
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

    /**
     * {@inheritDoc }
     */
    @Override
    public void clearAllLogEntries() {
        daoService.executeQuery(LogEntry.DELETE_ALL);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void clearOldLogEntries(Date date) {
        daoService.executeQuery(LogEntry.DELETE_OLD, QueryBuilder.with(LogEntry.PARAMETER_DATE, date));
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void clearOldLogEntries() {
        int daysToKeep = cfgService.getInteger(ConfigurationKey.LOG_KEEP);
        Calendar now = Calendar.getInstance();
        now.add(Calendar.DAY_OF_MONTH, -daysToKeep);
        clearOldLogEntries(now.getTime());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<JobQueue> findJobQueue() {
        return daoService.findAll(JobQueue.class, "executionTime", true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeJobQueue(Long id) {
        daoService.delete(JobQueue.class, id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JobQueue addToJobQueue(String name, String typeName, Long typeId,
            Long pluginConfigurationId, List<JobQueueParameter> parameters, Date scheduled)
            throws DataNotFoundException {

        JobQueue q = new JobQueue();
        q.setAdded(Calendar.getInstance().getTime());
        q.setName(name);
        if (scheduled == null) {
            q.setExecutionTime(Calendar.getInstance().getTime());
        } else {
            q.setExecutionTime(scheduled);
        }
        q.setParameters(parameters);
        q.setStatus(JobQueueStatus.WAITING);
        q.setPluginConfiguration(pluginConfigurationId);
        PluginConfiguration pluginCfg =
                daoService.findById(PluginConfiguration.class,
                pluginConfigurationId);
        q.setPluginAction(pluginCfg.getActionClass());
        q.setTypeClass(typeName);
        q.setTypeClassId(typeId);

        return daoService.create(q);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void executeJobQueue() {
        List<JobQueue> queue = findJobQueue();
        Calendar now = Calendar.getInstance();
        boolean itemsAddedDuringExecution = false;
        for (JobQueue item : queue) {
            try {
                LOG.log(Level.FINE, "Examining {0} {1}", new Object[]{item.getId(), item.getName()});
                if (item.getStatus().equals(JobQueueStatus.WAITING) && now.getTime().after(
                        item.getExecutionTime())) {
                    LOG.log(Level.FINE, "{0} {1} is ready for execution", new Object[]{item.getId(), item.getName()});
                    item.setStatus(JobQueueStatus.READY);
                }

                if (item.getStatus().equals(JobQueueStatus.READY) || item.
                        getStatus().equals(JobQueueStatus.FAILED)) {
                    LOG.log(Level.FINE, "Starting execution of {0} {1}", new Object[]{item.getId(), item.getName()});
                    item.setStatus(JobQueueStatus.EXECUTION);
                    item.setStarted(Calendar.getInstance().getTime());
                    daoService.update(item);
                    PluginAction action = item.getAction();

                    try {
                        PluginConfiguration cfg =
                                daoService.findById(PluginConfiguration.class,
                                item.getPluginConfiguration());
                        action.execute(pluginContext, item.getTypeClass(),
                                item.getTypeClassId(), cfg, item.
                                getParametersMap());

                        item.setFinished(Calendar.getInstance().getTime());
                        item.setStatus(JobQueueStatus.COMPLETED);

                        LOG.log(Level.FINE, "PluginConfiguratione executed successfully");
                        LOG.log(Level.FINE, "Adding 'oncomplete' PluginConfigurations to JobQueue");
                        for (PluginConfiguration completeCfg : cfg.getOnCompletePluginConfiguration()) {
                            LOG.log(Level.FINE, "+ {1} {0}", new Object[]{completeCfg.getName(), completeCfg.getId()});
                            addToJobQueue(completeCfg.getName(), item.getTypeClass(), item.getTypeClassId(), completeCfg.getId(), item.getParameters(), Calendar.getInstance().getTime());
                            itemsAddedDuringExecution = true;
                        }
                    } catch (DataNotFoundException ex) {
                        LOG.log(Level.FINE, "Failed execution of {0} {1}. " + ex.getMessage(), new Object[]{item.getId(), item.getName()});
                        item.setStatus(JobQueueStatus.FAILED_COMPLETED);
                        item.setFinished(Calendar.getInstance().getTime());
                    } catch (PluginActionException ex) {
                        if (ex.isPermanent()) {
                            item.setStatus(JobQueueStatus.FAILED_COMPLETED);
                        } else {
                            item.setStatus(JobQueueStatus.FAILED);
                        }
                        LOG.log(Level.FINE, "Failed execution of {0} {1}. " + ex.getMessage(), new Object[]{item.getId(), item.getName()});

                        item.setFinished(Calendar.getInstance().getTime());
                    }
                    daoService.update(item);
                }
            } catch (Exception ex) {
                LOG.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }

        daoService.executeQuery(JobQueue.REMOVE_COMPLETED);

        // If items were added to the job queue during the execution, process the queue again
        if (itemsAddedDuringExecution) {
            executeJobQueue();
        }

    }

    private int removeAllNewswireProcessing() {
        return daoService.executeQuery(NewswireService.RESET_PROCESSING);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public List<PluginConfiguration> findPluginConfigurations() {
        return daoService.findAll(PluginConfiguration.class);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public PluginConfiguration findPluginConfigurationById(Long id) throws
            DataNotFoundException {
        return daoService.findById(PluginConfiguration.class, id);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public PluginConfiguration createPluginConfiguration(
            PluginConfiguration pluginConfiguration) {
        return daoService.create(pluginConfiguration);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public PluginConfiguration updatePluginConfiguration(
            PluginConfiguration pluginConfiguration) {
        return daoService.update(pluginConfiguration);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void deletePluginConfigurationById(Long id) {
        daoService.delete(PluginConfiguration.class, id);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public PluginConfiguration initPluginConfiguration() {
        PluginConfiguration cfg = new PluginConfiguration();

        // Set the default action
        PluginService pluginService = PluginManager.createPluginService();
        Iterator<PluginAction> plugins = pluginService.getPlugins();
        if (!plugins.hasNext()) {
            throw new RuntimeException("No plugin actions found. Aborting");
        }
        PluginAction firstAction = plugins.next();
        cfg.setActionClass(firstAction.getClass().getName());

        return cfg;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void submitErrorReport(String errorDescription, String stacktrace, String browserData) {
        DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm");

        StringBuilder sb = new StringBuilder();

        sb.append("<table border=\"1\">");
        sb.append("    <tr>");
        sb.append("        <td><b>Message</b>:</td>");
        sb.append("        <td>");
        sb.append(errorDescription);
        sb.append("        </td>");
        sb.append("    </tr>");
        sb.append("    <tr>");
        sb.append("        <td><b>Time</b>:</td>");
        sb.append("        <td>");
        sb.append(df.format(Calendar.getInstance().getTime()));
        sb.append("        </td>");
        sb.append("    </tr>");
        sb.append("    <tr>");
        sb.append("        <td><b>Server IP address</b>:</td>");
        sb.append("        <td>");
        try {
            InetAddress addr = InetAddress.getLocalHost();
            sb.append(addr.getHostAddress());
        } catch (UnknownHostException e) {
            sb.append("Unknown");
        }
        sb.append("        </td>");
        sb.append("    </tr>");
        sb.append("    <tr>");
        sb.append("        <td><b>Version</b>:</td>");
        sb.append("        <td>");
        sb.append(getApplicationVersion());
        sb.append("        </td>");
        sb.append("    </tr>");
        sb.append("    <tr>");
        sb.append("        <td><b>User</b>:</td>");
        sb.append("        <td>");
        sb.append(ctx.getCallerPrincipal().getName());
        sb.append("        </td>");
        sb.append("    </tr>");
        sb.append("    <tr>");
        sb.append("        <td><b>Request</b>:</td>");
        sb.append("        <td><pre>");
        sb.append(browserData);
        sb.append("        </pre></td>");
        sb.append("    </tr>");
        sb.append("    <tr>");
        sb.append("        <td><b>Stacktrace</b>:</td>");
        sb.append("        <td><pre>");
        sb.append(stacktrace);
        sb.append("        </pre></td>");
        sb.append("    </tr>");
        sb.append("<table>");

        // TODO: Add e-mails to configuration
        notificationService.dispatchMail("errorreport.converge@i2m.dk",
                "converge@i2m.dk",
                "Error report",
                sb.toString(), "");
    }
}
