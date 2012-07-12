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
package dk.i2m.converge.core.content;

import dk.i2m.converge.core.security.UserRole;
import dk.i2m.converge.core.utils.BeanComparator;
import dk.i2m.converge.core.workflow.WorkflowState;
import dk.i2m.converge.core.workflow.WorkflowStateTransition;
import java.io.Serializable;
import java.util.*;
import javax.persistence.*;
import org.eclipse.persistence.annotations.PrivateOwned;

/**
 * Abstract class representing a piece of content with {@link Workflow} support.
 *
 * @author Allan Lykke Christensen
 */
@Entity
@Table(name = "content_item")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "content_type")
public abstract class ContentItem implements Serializable {

    /** Unique identifier of the {@link ContentItem}. */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Long id;

    /** Workflow transition history of the {@link ContentItem}. */
    @OneToMany(mappedBy = "contentItem", cascade = CascadeType.ALL, fetch =
    FetchType.EAGER)
    @OrderBy("timestamp DESC")
    private List<WorkflowStateTransition> history =
            new ArrayList<WorkflowStateTransition>();

    /** Current status of the {@link ContentItem}. */
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "current_state_id")
    private WorkflowState currentState;

    /** {@link List} of actors involved in the {@link ContentItem}. */
    @OneToMany(mappedBy = "contentItem", cascade = CascadeType.ALL, fetch =
    FetchType.EAGER)
    @PrivateOwned
    private List<ContentItemActor> actors = new ArrayList<ContentItemActor>();

    /** Date when the {@link ContentItem} was created. */
    @Column(name = "created")
    @Temporal(TemporalType.TIMESTAMP)
    private Date created = Calendar.getInstance().getTime();

    /** Date when the {@link ContentItem} was last updated. */
    @Column(name = "updated")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updated = Calendar.getInstance().getTime();

    /** Cached label containing the current actor. */
    @Column(name = "precalc_current_actor")
    private String precalculatedCurrentActor;

    /** URL to a thumbnail of the {@link ContentItem}. */
    @Column(name = "thumbnail_link")
    private String thumbnailLink;

    /**
     * Gets the unique identifier of the {@link ContentItem}.
     *
     * @return Unique identifier of the {@link ContentItem}.
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the unique identifier of the {@link ContentItem}.
     *
     * @param id 
     *          Unique identifier of the {@link ContentItem}.
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Gets the date and time when the {@link ContentItem} was created.
     *
     * @return Date and time when the {@link ContentItem} was created.
     */
    public Date getCreated() {
        return created;
    }

    /**
     * Sets the date and time when the {@link ContentItem} was created.
     *
     * @param created 
     *          Date and time when the {@link ContentItem} was created.
     */
    public void setCreated(Date created) {
        this.created = created;
    }

    /**
     * Gets the date and time when the {@link ContentItem} was last updated.
     *
     * @return Date and time when the {@link ConrentItem} was last updated.
     */
    public Date getUpdated() {
        return updated;
    }

    /**
     * Sets the date and time when the {@link ContentItem} was last updated.
     *
     * @param updated 
     *          Date and time when the {@link ContentItem} was last updated.
     */
    public void setUpdated(Date updated) {
        this.updated = updated;
    }

    /**
     * Gets the history of the workflow for the {@link ContentItem}.
     *
     * @return {@link List} of {@link WorkflowStateTransition}s that have
     *         occurred for the {@link ContentItem}
     */
    public List<WorkflowStateTransition> getHistory() {
        Collections.sort(history, new BeanComparator("timestamp", false));
        return history;
    }

    /**
     * Sets the history of the workflow for the {@link ContentItem}.
     *
     * @param history
     *          {@link List} of {@link WorkflowStateTransition}s that have
     *          occurred for the {@link ContentItem}
     */
    public void setHistory(List<WorkflowStateTransition> history) {
        this.history = history;
    }

    /**
     * Gets the current state of the {@link ContentItem}.
     *
     * @return Current state of the {@link ContentItem}
     */
    public WorkflowState getCurrentState() {
        return currentState;
    }

    /**
     * Sets the current state of the {@link ContentItem}.
     * 
     * @param currentState 
     *          Current state of the {@link ContentItem}
     */
    public void setCurrentState(WorkflowState currentState) {
        this.currentState = currentState;
    }

    /**
     * Gets the last {@link WorkflowStateTransition} that occurred for this
     * {@link ContentItem}.
     * 
     * @return Last {@link WorkflowStateTransition} that occurred
     */
    public WorkflowStateTransition getLatestTransition() {
        WorkflowStateTransition transition = null;
        for (WorkflowStateTransition t : getHistory()) {
            if (transition == null) {
                transition = t;
            } else {
                if (t.getTimestamp().after(transition.getTimestamp())) {
                    transition = t;
                }
            }
        }

        return transition;
    }

    /**
     * Determines if the {@link ContentItem} has been submitted. Submission is 
     * determined by the {@link Workflow} definition.
     * 
     * @see WorkflowStep#treatAsSubmitted
     * @return {@code true} if the {@link ContentItem} was submitted, otherwise 
     *         {@code false}
     */
    public boolean isSubmitted() {
        for (WorkflowStateTransition transition : getHistory()) {
            if (transition.isSubmitted()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the {@link Date} when the submission of the {@link ContentItem} took 
     * place. Submission is determined by the {@link Workflow} definition.
     * 
     * @see WorkflowStep#treatAsSubmitted
     * @return {@link Date} when the submission of the {@link ContentItem} took 
     *         place
     */
    public Date getSubmittedDate() {
        for (WorkflowStateTransition transition : getHistory()) {
            if (transition.isSubmitted()) {
                return transition.getTimestamp();
            }
        }
        return null;
    }

    /**
     * Gets a {@link List} of actors involved in the workflow of the 
     * {@link ContentItem}. 
     * 
     * @return {@link List} of actors involved in the {@link ContentItem}
     */
    public List<ContentItemActor> getActors() {
        return actors;
    }

    /**
     * Sets the {@link List} of actors involved in the workflow of the 
     * {@link ContentItem}.
     * 
     * @param actors 
     *          {@link List} of actors involved in the {@link ContentItem}
     */
    public void setActors(List<ContentItemActor> actors) {
        this.actors = actors;
    }

    /**
     * Gets the name of the current actor of the content item. If the current actor
     * is a group, the name of the group will be returned, if the current actor
     * is a user or multiple users, their names will be returned.
     *
     * @return {@link String} containing the name(s) of the current actor
     */
    public String getCurrentActor() {
        if (getCurrentState().isGroupPermission()) {
            return getCurrentState().getActorRole().getName();
        } else {
            StringBuilder sb = new StringBuilder();
            UserRole role = getCurrentState().getActorRole();

            if (role != null) {
                for (ContentItemActor actor : getActors()) {
                    if (actor.getRole() != null) {
                        if (actor.getRole().equals(role)) {
                            if (sb.length() > 0) {
                                sb.append(", ");
                            }
                            sb.append(actor.getUser().getFullName());
                        }
                    }
                }
            }
            return sb.toString();
        }
    }

    /**
     * Gets a {@link String} containing the pre-calculated (cached) current 
     * actor. The current actor is pre-calculated every time the 
     * {@link ContentItem} is saved. The purpose of this property is to cache
     * the current act to avoid too many database joins.
     * 
     * @return {@link String} containing the pre-calculated current actor
     */
    public String getPrecalculatedCurrentActor() {
        return precalculatedCurrentActor;
    }

    /**
     * Sets the pre-calculated (cached) current actor.
     * 
     * @param precalculatedCurrentActor 
     *          New value for the pre-calculated current actor
     */
    public void setPrecalculatedCurrentActor(String precalculatedCurrentActor) {
        this.precalculatedCurrentActor = precalculatedCurrentActor;
    }

    /**
     * Returns a URL of a thumbnail representing this item. This thumbnail
     * is used in grid views and similar.
     * 
     * @return URL of a thumbnail representing the item
     */
    public String getThumbnailLink() {
        return this.thumbnailLink;
    }

    /**
     * Sets the URL of a thumbnail representing this item. This thumbnail
     * is used in grid views and similar.
     * 
     * @param thumbnailLink
     *          URL of a thumbnail representing the item
     */
    public void setThumbnailLink(String thumbnailLink) {
        this.thumbnailLink = thumbnailLink;
    }
}
