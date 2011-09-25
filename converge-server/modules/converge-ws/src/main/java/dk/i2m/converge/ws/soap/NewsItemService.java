/*
 * Copyright (C) 2011 Interactive Media Management
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
package dk.i2m.converge.ws.soap;

import dk.i2m.converge.core.content.NewsItemPlacement;
import dk.i2m.converge.core.security.UserAccount;
import dk.i2m.converge.core.views.InboxView;
import dk.i2m.converge.core.workflow.Edition;
import dk.i2m.converge.ejb.facades.LockingException;
import dk.i2m.converge.ejb.facades.MediaDatabaseFacadeLocal;
import dk.i2m.converge.ejb.facades.NewsItemFacadeLocal;
import dk.i2m.converge.ejb.facades.NewsItemHolder;
import dk.i2m.converge.ejb.facades.OutletFacadeLocal;
import dk.i2m.converge.ejb.facades.UserFacadeLocal;
import dk.i2m.converge.ejb.facades.WorkflowStateTransitionException;
import dk.i2m.converge.ejb.services.DataNotFoundException;
import dk.i2m.converge.ws.model.ModelConverter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.jws.WebService;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.xml.ws.WebServiceContext;

/**
 * {@link WebService} for retrieving news items. 
 *
 * @author Allan Lykke Christensen
 */
@WebService(serviceName = "NewsItemService")
public class NewsItemService {

    private static final Logger LOG = Logger.getLogger(NewsItemService.class.getName());

    @EJB private NewsItemFacadeLocal newsItemFacade;
    @EJB private MediaDatabaseFacadeLocal mediaDatabaseFacade;

    @EJB private OutletFacadeLocal outletFacade;

    @EJB private UserFacadeLocal userFacade;

    @Resource private WebServiceContext context;

    /**
     * Gets all complete {@link NewsItem}s for a given edition.
     * 
     * @param id
     *          Unique identifier of the edition
     * @return {@link List} of complete {@link NewsItem}s in the given edition
     */
    @WebMethod(operationName = "getNewsItemsForEdition")
    public List<dk.i2m.converge.ws.model.NewsItem> getNewsItemsForEdition(@WebParam(name = "editionId") Long id) {
        List<dk.i2m.converge.ws.model.NewsItem> newsItems = new ArrayList<dk.i2m.converge.ws.model.NewsItem>();
        try {
            Edition edition = outletFacade.findEditionById(id);
            for (NewsItemPlacement placement : edition.getPlacements()) {
                if (placement.getNewsItem().isEndState()) {
                    newsItems.add(ModelConverter.toNewsItem(placement));

                }
            }
        } catch (DataNotFoundException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }

        return newsItems;
    }

    /**
     * Gets the assignments for the authenticated user.
     * 
     * @return {@link List} of {@link NewsItem}s representing 
     *         the current assignments of the authenticated user
     */
    @WebMethod(operationName = "getAssignments")
    public List<dk.i2m.converge.ws.model.NewsItem> getAssignments() {
        List<dk.i2m.converge.ws.model.NewsItem> output = new ArrayList<dk.i2m.converge.ws.model.NewsItem>();

        if (context.getUserPrincipal() == null) {
            LOG.log(Level.WARNING, "User is not authenticated");
            return output;
        }

        String username = context.getUserPrincipal().getName();
        LOG.log(Level.INFO, "Fetching assignments for {0}", username);

        List<InboxView> assignments = newsItemFacade.findInbox(username);
        LOG.log(Level.INFO, "{0} items for {1}", new Object[]{assignments.size(), username});

        for (InboxView assignment : assignments) {
            try {
                // TODO: Inefficient to check out each item. Create query similar to findInbox that will fetch required fields
                dk.i2m.converge.core.content.NewsItem newsItem = newsItemFacade.findNewsItemById(assignment.getId());

                for (dk.i2m.converge.core.content.NewsItemPlacement nip : newsItem.getPlacements()) {
                    dk.i2m.converge.ws.model.NewsItem ni = ModelConverter.toNewsItem(nip);
                    output.add(ni);
                }

            } catch (DataNotFoundException ex) {
                LOG.log(Level.SEVERE, "NewsItem in InboxView could not be found in database", ex);
            }
        }

        return output;
    }

    /**
     * Gets the privileged outlets for the authenticated user.
     * 
     * @return {@link List} of privileged {@link Outlet}s for the
     *         authenticated user
     */
    @WebMethod(operationName = "getOutlets")
    public List<dk.i2m.converge.ws.model.Outlet> getOutlets() {
        List<dk.i2m.converge.ws.model.Outlet> output = new ArrayList<dk.i2m.converge.ws.model.Outlet>();

        if (context.getUserPrincipal() == null) {
            LOG.log(Level.WARNING, "User is not authenticated");
            return output;
        }

        String username = context.getUserPrincipal().getName();
        UserAccount userAccount = null;
        try {
            userAccount = userFacade.findById(username);
        } catch (DataNotFoundException ex) {
            LOG.log(Level.SEVERE, null, ex);
            return output;
        }

        for (dk.i2m.converge.core.workflow.Outlet outlet : userAccount.getPrivilegedOutlets()) {
            output.add(ModelConverter.toOutlet(outlet));
        }

        return output;
    }

    /**
     * Checks out a {@link NewsItem} using its unique identifier. Upon
     * checking out the {@link NewsItem} it becomes locked for editing
     * by other users.
     * 
     * @return {@link dk.i2m.converge.ws.model.NewsItem} matching the
     *          unique identifier
     * @throws NewsItemNotFoundException
     *          If the requested {@link NewsItem} could not be found
     * @throws NewsItemLockedException
     *          If the requested {@link NewsItem} is locked by another user
     * @throws NewsItemReadOnlyException
     *          If the requested {@link NewsItem} is not in a state for 
     *          being checked out
     */
    @WebMethod(operationName = "checkout")
    public dk.i2m.converge.ws.model.NewsItem checkout(Long id) throws NewsItemNotFoundException, NewsItemLockingException, NewsItemReadOnlyException {
        dk.i2m.converge.ws.model.NewsItem output = null;

        if (context.getUserPrincipal() == null) {
            LOG.log(Level.WARNING, "User is not authenticated");
            return output;
        }
        try {
            NewsItemHolder nih = newsItemFacade.checkout(id);

            if (!nih.isCheckedOut()) {
                throw new NewsItemLockingException(id + " is checked out by another user");
            }

            if (nih.isReadOnly()) {
                throw new NewsItemReadOnlyException(id + " cannot be checked out. You do not have permission to edit the story.");
            }

            for (NewsItemPlacement nip : nih.getNewsItem().getPlacements()) {
                // Override the last one - keep the latest
                output = ModelConverter.toNewsItem(nip);
            }

            if (output == null) {
                output = ModelConverter.toNewsItem(nih.getNewsItem());
            }

        } catch (DataNotFoundException ex) {
            throw new NewsItemNotFoundException(id + " does not exist in the database");
        }

        return output;
    }

    /**
     * Checks in a {@link NewsItem}.
     * 
     * @throws NewsItemNotFoundException
     *          If a corresponding {@link NewsItem} could not be found
     * @throws NewsItemLockingException
     *          If the corresponding {@link NewsItem} is not locked,
     *          one can only check-in an item that has been checked-out
     */
    @WebMethod(operationName = "checkin")
    public void checkin(dk.i2m.converge.ws.model.NewsItem newsItem) throws NewsItemLockingException, NewsItemNotFoundException {

        if (context.getUserPrincipal() == null) {
            LOG.log(Level.WARNING, "User is not authenticated");
        }
        try {
            dk.i2m.converge.core.content.NewsItem ni = newsItemFacade.findNewsItemById(newsItem.getId());
            ni.setTitle(newsItem.getTitle());
            ni.setByLine(newsItem.getByLine());
            ni.setStory(newsItem.getStory());
            ni.setBrief(newsItem.getBrief());
            
            try {
                newsItemFacade.checkin(ni);
            } catch (LockingException ex) {
                throw new NewsItemLockingException(ex.getMessage());
            }
        } catch (DataNotFoundException ex) {
            throw new NewsItemNotFoundException(newsItem.getId() + " does not exist in the database");
        }
    }
    
    /**
     * Workflow step for the given NewsItem.
     * 
     * @throws NewsItemNotFoundException
     *          If a corresponding {@link NewsItem} could not be found
     * @throws NewsItemWorkflowException
     *          If the workflow step is illegal
     */
    @WebMethod(operationName = "step")
    public void step(Long newsItemId, Long workflowStep, String comment) throws NewsItemNotFoundException, NewsItemWorkflowException {

        if (context.getUserPrincipal() == null) {
            LOG.log(Level.WARNING, "User is not authenticated");
        }
        try {
            dk.i2m.converge.core.content.NewsItem ni = newsItemFacade.findNewsItemById(newsItemId);
            try {
                newsItemFacade.step(ni, workflowStep, comment);
            } catch (WorkflowStateTransitionException ex) {
                throw new NewsItemWorkflowException(ex);
            }
            
            
        } catch (DataNotFoundException ex) {
            throw new NewsItemNotFoundException(newsItemId + " does not exist in the database");
        }
    }
}
