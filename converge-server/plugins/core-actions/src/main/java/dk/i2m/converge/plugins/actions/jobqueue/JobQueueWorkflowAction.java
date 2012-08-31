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
package dk.i2m.converge.plugins.actions.jobqueue;

import dk.i2m.converge.core.DataNotFoundException;
import dk.i2m.converge.core.content.ContentItem;
import dk.i2m.converge.core.logging.LogSeverity;
import dk.i2m.converge.core.plugin.PluginConfiguration;
import dk.i2m.converge.core.plugin.PluginContext;
import dk.i2m.converge.core.plugin.WorkflowAction;
import dk.i2m.converge.core.security.UserAccount;
import dk.i2m.converge.core.workflow.JobQueue;
import dk.i2m.converge.core.workflow.JobQueueParameter;
import dk.i2m.converge.core.workflow.WorkflowStepAction;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 *
 * @author Allan Lykke Christensen
 */
@dk.i2m.converge.core.annotations.WorkflowAction
public class JobQueueWorkflowAction implements WorkflowAction {

    private ResourceBundle bundle = ResourceBundle.getBundle(
            "dk.i2m.converge.plugins.actions.jobqueue.WorkflowActionMessages");

    private Map<String, String> availableProperties = null;

    private Map<String, String> instanceProperties =
            new HashMap<String, String>();

    private PluginContext pluginCtx;

    private WorkflowStepAction actionInstance;

    private static final DateFormat DATE_PARSER = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss");

    /** Contains the unique identifier of the {@link PluginConfiguration} to add to the {@link JobQueue}. */
    private Long pluginConfigurationId;

    /** Properties available for configuring the plug-in. */
    enum Property {

        PLUGIN_CONFIGURATION
    }

    /** {@inheritDoc } */
    @Override
    public void execute(PluginContext ctx, ContentItem item,
            WorkflowStepAction stepAction, UserAccount user) {
        this.pluginCtx = ctx;
        this.actionInstance = stepAction;
        this.instanceProperties = stepAction.getPropertiesAsMap();

        // Read and validate properties
        if (!readProperties()) {
            return;
        }

        try {
            List<JobQueueParameter> params = new ArrayList<JobQueueParameter>();
            params.add(new JobQueueParameter("workflow_step_action", "" + stepAction.getId()));
            params.add(new JobQueueParameter("initiator", user.getUsername()));
            
            ctx.addToJobQueue(getName() + " #" + item.getId(), item.getClass().
                    getName(), item.getId(), this.pluginConfigurationId,
                    params, Calendar.getInstance().getTime());
        } catch (DataNotFoundException ex) {
            log(LogSeverity.WARNING, ex.getMessage());
        }
    }

    /** {@inheritDoc } */
    @Override
    public Map<String, String> getAvailableProperties() {
        if (availableProperties == null) {
            availableProperties = new LinkedHashMap<String, String>();
            for (JobQueueAction.Property p : JobQueueAction.Property.values()) {
                availableProperties.put(bundle.getString(p.name()), p.name());
            }
        }
        return availableProperties;
    }

    /** {@inheritDoc } */
    @Override
    public String getName() {
        return bundle.getString("PLUGIN_NAME");
    }

    /** {@inheritDoc } */
    @Override
    public String getAbout() {
        return bundle.getString("PLUGIN_ABOUT");
    }

    /** {@inheritDoc } */
    @Override
    public String getDescription() {
        return bundle.getString("PLUGIN_DESCRIPTION");
    }

    /** {@inheritDoc } */
    @Override
    public String getVendor() {
        return bundle.getString("PLUGIN_VENDOR");
    }

    /** {@inheritDoc } */
    @Override
    public Date getDate() {
        try {
            return DATE_PARSER.parse(bundle.getString("PLUGIN_BUILD_TIME"));
        } catch (Exception ex) {
            return Calendar.getInstance().getTime();
        }
    }

    /** {@inheritDoc } */
    @Override
    public ResourceBundle getBundle() {
        return bundle;
    }

    /**
     * Reads and validate the properties.
     * <p/>
     * @return {@code true} if the properties were read and validated, otherwise
     *         {@code false}
     */
    private boolean readProperties() {
        if (!isPropertySet(JobQueueAction.Property.PLUGIN_CONFIGURATION)) {
            log(LogSeverity.SEVERE, "LOG_PROPERTY_PLUGIN_CONFIGURATION_MISSING");
            return false;
        } else {
            this.pluginConfigurationId = getPropertyAsLong(
                    JobQueueAction.Property.PLUGIN_CONFIGURATION);
        }

        return true;
    }

    // -- Utility Methods -----
    private void log(LogSeverity severity, String msg) {
        log(severity, msg, new Object[]{});
    }

    private void log(LogSeverity severity, String msg, Object param) {
        log(severity, msg, new Object[]{param});
    }

    private void log(LogSeverity severity, String msg, Object[] params) {
        this.pluginCtx.log(severity, bundle.getString(msg), params,
                this.actionInstance,
                this.actionInstance.getId());
    }

    private boolean isPropertySet(JobQueueAction.Property p) {
        return instanceProperties.containsKey(p.name());
    }

    private String getProperty(JobQueueAction.Property p) {
        return instanceProperties.get(p.name());
    }

    private Boolean getPropertyAsBoolean(JobQueueAction.Property p) {
        return Boolean.parseBoolean(getProperty(p));
    }

    private Long getPropertyAsLong(JobQueueAction.Property p) {
        return Long.valueOf(getProperty(p));
    }
}
