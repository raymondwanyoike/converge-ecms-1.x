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
package dk.i2m.converge.core.jobqueue;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.*;

/**
 * Item pending action in the job queue. A {@link QueueItem} contains
 * information about the type of pending item and which action that should
 * be executed on the item.
 *
 * @author Allan Lykke Christensen
 */
@Entity
@Table(name = "job_queue_item")
public class QueueItem implements Serializable {

    public enum ItemStatus {

        /** Ready and pending to be executed in the queue. */
        QUEUED,
        /** Error occurred at last attempt to execute the item. Item is queued and pending execution. */
        ERROR,
        /** Scheduled for execution at a given time. */
        SCHEDULED
    }

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    @Column(name = "scheduled_for")
    private Date scheduledFor;

    @Column(name = "last_execution")
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date lastTry;

    @Column(name = "number_of_tries")
    private Integer tries;

    @Column(name = "error_message") @Lob
    private String errorMessage;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private ItemStatus status;

    @Column(name = "title")
    private String title;

    @Column(name = "instance_type")
    private String instanceType;

    @Column(name = "instance_id")
    private String instanceId;

    @Column(name = "queue_action_type")
    private String queueActionType;

    public QueueItem() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getInstanceType() {
        return instanceType;
    }

    public void setInstanceType(String instanceType) {
        this.instanceType = instanceType;
    }

    public Date getLastTry() {
        return lastTry;
    }

    public void setLastTry(Date lastTry) {
        this.lastTry = lastTry;
    }

    public String getQueueActionType() {
        return queueActionType;
    }

    public void setQueueActionType(String queueActionType) {
        this.queueActionType = queueActionType;
    }

    public Date getScheduledFor() {
        return scheduledFor;
    }

    public void setScheduledFor(Date scheduledFor) {
        this.scheduledFor = scheduledFor;
    }

    public ItemStatus getStatus() {
        return status;
    }

    public void setStatus(ItemStatus status) {
        this.status = status;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getTries() {
        return tries;
    }

    public void setTries(Integer tries) {
        this.tries = tries;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * Execute the {@link QueueItem}.
     * 
     * @return {@code true} if the {@link QueueItem} successfully executed,
     *         otherwise {@code false}
     */
    public boolean execute() {
        return execute(false);
    }
    
    /**
     * Execute the {@link QueueItem}.
     * 
     * @param force 
     *          Force execution even through the {@link QueueItem} is 
     *          {@link ItemStatus#SCHEDULED} and not yet ready for execution.
     * @return {@code true} if the {@link QueueItem} successfully executed,
     *         otherwise {@code false}
     */
    public boolean execute(boolean force) {
        try {
            Class c = Class.forName(getQueueActionType());
            QueueAction action = (QueueAction) c.newInstance();
            return action.execute(this);
        } catch (Throwable t) {
            setErrorMessage(t.getMessage());
            return false;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final QueueItem other = (QueueItem) obj;
        if (this.id != other.id
                && (this.id == null || !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + (this.id != null ? this.id.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        return getClass().getName() + "[" + "id=" + this.id + "]";
    }
}
