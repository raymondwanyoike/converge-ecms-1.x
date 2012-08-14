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
package dk.i2m.converge.jsf.beans.administrator;

import dk.i2m.converge.ejb.facades.SystemFacadeLocal;
import java.util.List;
import javax.ejb.EJB;

/**
 * Backing bean for {@code /administrator/JobQueue.jspx}.
 *
 * @author Allan Lykke Christensen
 */
public class JobQueue {

    @EJB private SystemFacadeLocal systemFacade;

    /**
     * Creates a new instance of {@link JobQueue}.
     */
    public JobQueue() {
    }

    /**
     * Gets a {@link List} of the items in the {@link JobQueue}.
     * 
     * @return {@link List} of items in the {@link JobQueue}
     */
    public List<dk.i2m.converge.core.workflow.JobQueue> getJobQueue() {
        return systemFacade.findJobQueue();
    }
}
