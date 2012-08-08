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
package dk.i2m.converge.ejb.services;

import dk.i2m.converge.core.content.ContentItem;
import dk.i2m.converge.core.workflow.WorkflowStateTransitionException;
import javax.ejb.Local;

/**
 * Local interface for the {@link ContentItemServiceBean}.
 *
 * @author Allan Lykke Christensen
 */
@Local
public interface ContentItemServiceLocal {

    /**
     * Starts the workflow of a new {@link ContentItem}.
     *
     * @param contentItem
     *          {@link ContentItem} to start.
     * @throws WorkflowStateTransitionException 
     *          If the workflow could not be started for the {@code contentItem}
     */
    ContentItem start(ContentItem contentItem) throws
            WorkflowStateTransitionException;

    /**
     * Promotes the {@link ContentItem} in the workflow.
     *
     * @param contentItem
     *          {@link ContentItem} to promote
     * @param step
     *          Unique identifier of the next step
     * @param stateTransition 
     *          Is the step a state transition (skipping the WorkflowOption) or
     *          is it a WorkflowOption transition. A state transition can be used
     *          to move from one state to another by-passing declared workflow 
     *          options.
     * @return Promoted {@link ContentItem}
     * @throws WorkflowStateTransitionException 
     *          If the next state is not legal or if the step failed
     */
    ContentItem step(ContentItem contentItem, Long step, boolean stateTransition) throws
            WorkflowStateTransitionException;

    /**
     * Re-generate the thumbnail links for all {@link ContentItem}s. 
     * <strong>Warning</strong> this can take a long time to execute depending 
     * on the number of {@link ContentItem}s in the database.
     */
    void generateThumbnailLinks();

    /**
     * Updates the {@link ContentItem#thumbnailLink}.
     */
    void updateThumbnail(ContentItem ci);
}
