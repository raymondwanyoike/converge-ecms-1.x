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
package dk.i2m.converge.core.workflow;

import dk.i2m.converge.core.plugin.PluginAction;
import java.io.Serializable;
import java.util.*;
import javax.persistence.*;

/**
 * Item in the {@link JobQueue} for execution of an action at a given time.
 *
 * @author Allan Lykke Christensen
 */
@Entity
@Table(name = "job_queue")
@NamedQueries({
    @NamedQuery(name = JobQueue.REMOVE_COMPLETED,
    query =
    "DELETE FROM JobQueue j WHERE j.status = dk.i2m.converge.core.workflow.JobQueueStatus.COMPLETED")
})
public class JobQueue implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * Update query for removing all completed items from the JobQueue.
     */
    public static final String REMOVE_COMPLETED = "JobQueue.RemoveCompleted";
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @Column(name = "name")
    private String name;
    @Column(name = "type_class")
    @Lob
    private String typeClass;
    @Column(name = "type_class_id")
    private Long typeClassId;
    @Column(name = "plugin_action_class")
    private String pluginAction;
    @Column(name = "plugin_configuration_id")
    private Long pluginConfiguration;
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private JobQueueStatus status = JobQueueStatus.WAITING;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    @Column(name = "execution_time")
    private Date executionTime;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    @Column(name = "started")
    private Date started;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    @Column(name = "finished")
    private Date finished;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    @Column(name = "added")
    private Date added;
    @OneToMany(mappedBy = "jobQueue")
    private List<JobQueueParameter> parameters =
            new ArrayList<JobQueueParameter>();

    public JobQueue() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(Date executionTime) {
        this.executionTime = executionTime;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public JobQueueStatus getStatus() {
        return status;
    }

    public void setStatus(JobQueueStatus status) {
        this.status = status;
    }

    public String getTypeClass() {
        return typeClass;
    }

    public void setTypeClass(String typeClass) {
        this.typeClass = typeClass;
    }

    public Long getTypeClassId() {
        return typeClassId;
    }

    public void setTypeClassId(Long typeClassId) {
        this.typeClassId = typeClassId;
    }

    public String getPluginAction() {
        return pluginAction;
    }

    public void setPluginAction(String pluginAction) {
        this.pluginAction = pluginAction;
    }

    public Long getPluginConfiguration() {
        return pluginConfiguration;
    }

    public void setPluginConfiguration(Long pluginConfiguration) {
        this.pluginConfiguration = pluginConfiguration;
    }

    public Date getFinished() {
        return finished;
    }

    public Date getStarted() {
        return started;
    }

    public void setFinished(Date finished) {
        this.finished = finished;
    }

    public void setStarted(Date started) {
        this.started = started;
    }

    public Date getAdded() {
        return added;
    }

    public void setAdded(Date added) {
        this.added = added;
    }

    /**
     * Gets the {@link List} of parameters for the {@link JobQueue} item.
     *
     * @return {@link List} of parameters to set for the {@link JobQueue} item
     */
    public List<JobQueueParameter> getParameters() {
        return parameters;
    }

    /**
     * Sets the {@link List} of parameters for the {@link JobQueue} item.
     *
     * @param parameters {@link List} of parameters to set for the
     * {@link JobQueue} item
     */
    public void setParameters(List<JobQueueParameter> parameters) {
        this.parameters = parameters;
    }

    /**
     * Gets a {@link Map} containing the parameters. The {@link Map} uses the
     * parameter name as the key, and a {@link List} to contain the values for
     * the parameter. In many cases the {@link List} will only contain a single
     * value.
     *
     * @return {@link Map} containing the parameters.
     */
    public Map<String, List<String>> getParametersMap() {
        Map<String, List<String>> map = new HashMap<String, List<String>>();
        for (JobQueueParameter param : getParameters()) {
            if (!map.containsKey(param.getName())) {
                List<String> vals = new ArrayList<String>();
                map.put(param.getName(), vals);
            }

            map.get(param.getName()).add(param.getValue());
        }
        return map;
    }

    /**
     * Adds a parameter to the {@link JobQueue} item.
     *
     * @param name Name of the parameter
     * @param value Value of the parameter
     */
    public void addParameter(String name, String value) {
        JobQueueParameter param = new JobQueueParameter(name, value, this);
        getParameters().add(param);
    }

    /**
     * Creates an instance of the action specified in {@link #getQueueAction()}.
     *
     * @return Instance of the action
     * @throws WorkflowActionException If the action could not be instantiated
     */
    public PluginAction getAction() throws JobQueueActionException {
        try {
            Class c = Class.forName(getPluginAction());
            PluginAction action = (PluginAction) c.newInstance();
            return action;
        } catch (ClassNotFoundException ex) {
            throw new JobQueueActionException("Could not find action: "
                    + getPluginAction(), ex);
        } catch (InstantiationException ex) {
            throw new JobQueueActionException(
                    "Could not instantiate action [" + getPluginAction()
                    + "]. Check to ensure that the action has a public contructor with no arguments",
                    ex);
        } catch (IllegalAccessException ex) {
            throw new JobQueueActionException("Could not access action: "
                    + getPluginAction(), ex);
        }
    }

    /**
     * Calculate for how long the job ran or has been running.
     *
     * @return Duration of the job in milliseconds
     */
    public Long getDuration() {
        if (started == null) {
            return 0L;
        }

        Calendar end = Calendar.getInstance();
        Calendar start = Calendar.getInstance();
        start.setTime(started);

        if (end != null) {
            end.setTime(finished);
        }

        return end.getTimeInMillis() - start.getTimeInMillis();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    /**
     * A {@link JobQueue} is equal to this {@link JobQueue} if their
     * {@link #id}s are equal.
     *
     * @param object {@link Object} to determine if equal to this
     * {@link JobQueue}
     * @return {@code true} if the {@code object} is a {@link JobQueue} with the
     * same {@link #id} as this {@link JobQueue}
     */
    @Override
    public boolean equals(Object object) {
        if (!(object instanceof JobQueue)) {
            return false;
        }
        JobQueue other = (JobQueue) object;
        if ((this.id == null && other.id != null) || (this.id != null
                && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getClass().getName() + "[id=" + id + ", name=" + name + ", "
                + "executionTime=" + executionTime + "]";
    }
}
