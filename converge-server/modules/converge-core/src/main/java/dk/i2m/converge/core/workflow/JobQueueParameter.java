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

import java.io.Serializable;
import javax.persistence.*;

/**
 * Execution parameter for a {@link JobQueue} item. A parameter typically 
 * contains information determined at runtime when the item was dropped into
 * the {@link JobQueue}. It could contain information related to the item that
 * will be processed in the {@link JobQueue}. A parameter consist of a name
 * (acting as an identifier) and a value.
 *
 * @author Allan Lykke Christensen
 */
@Entity
@Table(name = "job_queue_parameter")
public class JobQueueParameter implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Unique identifier of the parameter. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** Name of the parameter. */
    @Column(name = "param_name")
    private String name;

    /** Value of the parameter. */
    @Column(name = "param_value")
    @Lob
    private String value;

    /** Reference to the {@link JobQueue} item that owns the parameter. */
    @ManyToOne
    @JoinColumn(name = "job_queue_id")
    private JobQueue jobQueue;

    /**
     * Creates a new instance of {@link JobQueueParameter}.
     */
    public JobQueueParameter() {
        this("", "");
    }

    /**
     * Creates a new instance of {@link JobQueueParameter}.
     * 
     * @param name
     *          Name of the parameter
     * @param value
     *          Value of the parameter
     */
    public JobQueueParameter(String name, String value) {
        this(name, value, null);
    }

    /**
     * Creates a new instance of {@link JobQueueParameter}.
     * 
     * @param name
     *          Name of the parameter
     * @param value
     *          Value of the parameter
     * @param jobQueue
     *         {@link JobQueue} item that owns the parameter
     */
    public JobQueueParameter(String name, String value, JobQueue jobQueue) {
        this.name = name;
        this.value = value;
        this.jobQueue = jobQueue;
    }

    /**
     * Gets the unique identifier of the parameter. 
     * 
     * @return Unique identifier of the parameter
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the unique identifier of the parameter. 
     * 
     * @param id
     *          Unique identifier of the parameter
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Gets the name of the parameter.
     * 
     * @return Name of the parameter
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the parameter.
     * 
     * @param name
     *          Name of the parameter
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the value of the parameter.
     * 
     * @return Value of the parameter
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value of the parameter.
     * 
     * @return Value of the parameter
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Gets the {@link JobQueue} item that owns the parameter.
     * 
     * @return {@link JobQueue} item that owns the parameter
     */
    public JobQueue getJobQueue() {
        return jobQueue;
    }

    /**
     * Sets the {@link JobQueue} item that owns the parameter.
     * 
     * @param jobQueue
     *          {@link JobQueue} item that owns the parameter
     */
    public void setJobQueue(JobQueue jobQueue) {
        this.jobQueue = jobQueue;
    }

    /**
     * A {@link JobQueuePararameter} is equal to another 
     * {@link JobQueueParameter} only if the {@link #id} and {@link #name} is 
     * the same for both this object and {@code obj}. 
     * 
     * @param obj
     *          {@link Object} to test
     * @return {@code true} if the the {@link #id} and {@link #name} is the same
     *         for both {@code obj} and this.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final JobQueueParameter other = (JobQueueParameter) obj;
        if (this.id != other.id
                && (this.id == null || !this.id.equals(other.id))) {
            return false;
        }
        if ((this.name == null) ? (other.name != null)
                : !this.name.equals(other.name)) {
            return false;
        }
        return true;
    }

    /** {@inheritDoc } */
    @Override
    public int hashCode() {
        int hash = 3;
        hash = 59 * hash + (this.id != null ? this.id.hashCode() : 0);
        hash = 59 * hash + (this.name != null ? this.name.hashCode() : 0);
        return hash;
    }
}
