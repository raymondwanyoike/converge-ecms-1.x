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

/**
 * Action that can be executed on an item in the job queue.
 *
 * @author Allan Lykke Christensen
 */
public abstract class QueueAction {

    /**
     * Execute the action on the given {@link QueueItem}.
     * 
     * @param queueItem
     *          {@link QueueItem} to execute
     * @return {@code true} if the {@link QueueItem} executed successfully,
     *         otherwise {@code false}
     */
    public abstract boolean execute(QueueItem queueItem);
}
