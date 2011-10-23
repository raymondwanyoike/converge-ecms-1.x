/*
 * Copyright (C) 2010 - 2011 Interactive Media Management
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

import dk.i2m.converge.core.content.NewsItemPlacement;
import dk.i2m.converge.core.dto.EditionAssignmentView;
import dk.i2m.converge.core.dto.EditionView;
import dk.i2m.converge.core.dto.OutletActionView;
import dk.i2m.converge.core.utils.BeanComparator;
import dk.i2m.converge.core.workflow.EditionActionException;
import dk.i2m.converge.ejb.services.DataNotFoundException;
import dk.i2m.converge.core.workflow.Department;
import dk.i2m.converge.core.workflow.Edition;
import dk.i2m.converge.core.workflow.EditionPattern;
import dk.i2m.converge.core.workflow.Outlet;
import dk.i2m.converge.core.workflow.OutletEditionAction;
import dk.i2m.converge.core.workflow.Section;
import dk.i2m.converge.ejb.services.DaoServiceLocal;
import dk.i2m.converge.ejb.services.QueryBuilder;
import dk.i2m.converge.core.workflow.EditionCandidate;
import dk.i2m.converge.utils.CalendarUtils;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.MessageProducer;
import javax.jms.Session;

/**
 * Stateless session bean providing a facade to working with {@link Outlet}s.
 *
 * @author Allan Lykke Christensen
 */
@Stateless
public class OutletFacadeBean implements OutletFacadeLocal {

    private static final Logger LOG = Logger.getLogger(OutletFacadeBean.class.getName());

    @EJB private DaoServiceLocal daoService;

    @Resource(mappedName = "jms/editionServiceQueue") private Destination destination;

    @Resource(mappedName = "jms/connectionFactory") private ConnectionFactory jmsConnectionFactory;

    /**
     * Creates a new instance of {@link OutletFacadeBean}.
     */
    public OutletFacadeBean() {
    }

    /** {@inheritDoc} */
    @Override
    public Outlet createOutlet(Outlet outlet) {
        return daoService.create(outlet);
    }

    /** {@inheritDoc} */
    @Override
    public List<Outlet> findAllOutlets() {
        return daoService.findAll(Outlet.class);
    }

    /** {@inheritDoc} */
    @Override
    public Outlet findOutletById(Long id) throws DataNotFoundException {
        return daoService.findById(Outlet.class, id);
    }

    /** {@inheritDoc } */
    @Override
    public Outlet updateOutlet(Outlet outlet) {
        return daoService.update(outlet);
    }

    /** {@inheritDoc } */
    @Override
    public void deleteOutletById(Long id) {
        daoService.delete(Outlet.class, id);
    }

    /** {@inheritDoc } */
    @Override
    public Department createDepartment(Department department) {
        return daoService.create(department);
    }

    /** {@inheritDoc } */
    @Override
    public Department findDepartmentById(Long id) throws DataNotFoundException {
        return daoService.findById(Department.class, id);
    }

    /** {@inheritDoc } */
    @Override
    public void updateDepartment(Department department) {
        daoService.update(department);
    }

    /** {@inheritDoc } */
    @Override
    public void deleteDepartment(Long id) {
        //TODO: Determine what to do about news item current in this department
        daoService.delete(Department.class, id);
    }

    /** {@inheritDoc } */
    @Override
    public Edition createEdition(Edition edition) {
        return daoService.create(edition);
    }

    /**
     * Creates an {@link Edition} for the given outlet.
     * 
     * @param outletId
     *          Unique identifier of the {@link Outlet}
     * @param open
     *          Determines if the {@link Edition} is open for placements
     * @param publicationDate
     *          Date of publication of the {@link Edition}
     * @param expirationDate
     *          Date of expiration of the {@link Edition}
     * @param closeDate
     *          Date when the {@link Edition} closes for additions
     */
    @Override
    public Edition createEdition(Long outletId, Boolean open, Date publicationDate, Date expirationDate, Date closeDate) {
        Calendar pubDate = Calendar.getInstance();
        pubDate.setTime(publicationDate);

        Calendar expDate = Calendar.getInstance();
        expDate.setTime(expirationDate);

        Edition edition = new Edition();
        edition.setCloseDate(closeDate);
        edition.setPublicationDate(pubDate);
        edition.setExpirationDate(expDate);
        edition.setNumber(0);
        edition.setVolume(0);
        edition.setOpen(open);

        try {
            Outlet outlet = daoService.findById(Outlet.class, outletId);
            edition.setOutlet(outlet);
        } catch (DataNotFoundException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }

        return daoService.create(edition);
    }

    /** {@inheritDoc } */
    @Override
    public int closeOverdueEditions() {
        List<Edition> overdue = daoService.findWithNamedQuery(Edition.FIND_OVERDUE);
        int closed = 0;

        for (Edition edition : overdue) {
            closed++;
            edition.setOpen(false);
            updateEdition(edition);
            scheduleActions(edition.getId());
        }

        return closed;
    }

    /** {@inheritDoc } */
    @Override
    public Edition createEdition(EditionCandidate editionCandidate) {
        Edition edition = new Edition();
        edition.setCloseDate(editionCandidate.getCloseDate());
        edition.setExpirationDate(Calendar.getInstance());
        edition.getExpirationDate().setTime(editionCandidate.getExpirationDate());
        edition.setPublicationDate(Calendar.getInstance());
        edition.getPublicationDate().setTime(editionCandidate.getPublicationDate());
        try {
            edition.setOutlet(findOutletById(editionCandidate.getOutletId()));
        } catch (DataNotFoundException ex) {
            LOG.log(Level.INFO, ex.getMessage());
        }
        return createEdition(edition);
    }

    /** {@inheritDoc } */
    @Override
    public Edition findEditionById(long id) throws DataNotFoundException {
        return daoService.findById(Edition.class, id);
    }

    /** {@inheritDoc } */
    @Override
    public List<Edition> findEditionByOutletAndDate(long outletId, Calendar date) {
        Calendar startDate = Calendar.getInstance();
        Calendar endDate = Calendar.getInstance();
        startDate.setTime(date.getTime());
        startDate.set(Calendar.HOUR_OF_DAY, 0);
        startDate.set(Calendar.MINUTE, 0);
        startDate.set(Calendar.SECOND, 0);

        endDate.setTime(date.getTime());
        endDate.set(Calendar.HOUR_OF_DAY, 23);
        endDate.set(Calendar.MINUTE, 59);
        endDate.set(Calendar.SECOND, 59);
        Outlet outlet = null;
        try {
            outlet = findOutletById(outletId);
        } catch (DataNotFoundException ex) {
            return new ArrayList<Edition>();
        }
        Map params = QueryBuilder.with("outlet", outlet).
                and("start_date", startDate).
                and("end_date", endDate).parameters();

        return daoService.findWithNamedQuery(Edition.FIND_BY_OUTLET_AND_DATE, params);
    }

    /** {@inheritDoc} */
    @Override
    public List<Edition> findEditionsByStatus(boolean status, Outlet outlet) {
        Map<String, Object> params = QueryBuilder.with("outlet", outlet).and("status", status).parameters();
        return daoService.findWithNamedQuery(Edition.FIND_BY_STATUS, params);
    }

    /** {@inheritDoc} */
    @Override
    public Edition findNextEdition(Outlet outlet) throws DataNotFoundException {
        Calendar endSearch = Calendar.getInstance();
        endSearch.add(Calendar.MONTH, 1);
        Edition match = null;
        Calendar now = Calendar.getInstance();

        while (now.before(endSearch)) {
            List<Edition> candidates = findEditionsByDate(outlet, now);

            for (Edition candidate : candidates) {
                if (candidate.isOpen()) {
                    if (match == null) {
                        match = candidate;
                    }
                    if (candidate.getPublicationDate().after(now) && match.getPublicationDate().after(candidate.getPublicationDate())) {
                        match = candidate;
                    }
                }
            }
            now.add(Calendar.DAY_OF_MONTH, 1);
        }

        if (match == null) {
            throw new DataNotFoundException();
        }

        return match;
    }

    @Override
    public List<Edition> findEditionsByDate(Outlet outlet, Calendar date) {
        // 1. Check if there are any editions in the database matching the date and outlet
        Calendar startDate = CalendarUtils.getStartOfDay(date);
        Calendar endDate = CalendarUtils.getEndOfDay(date);

        Map<String, Object> params = QueryBuilder.with("outlet", outlet).and("start_date", startDate).and("end_date", endDate).parameters();
        List<Edition> editions = daoService.findWithNamedQuery(Edition.FIND_BY_OUTLET_AND_DATE, params);

        // 2. Generate editions based on pattern.
        List<EditionPattern> relavantPatterns = new ArrayList<EditionPattern>();
        for (EditionPattern pattern : outlet.getEditionPatterns()) {
            if (pattern.getDay() == date.get(java.util.Calendar.DAY_OF_WEEK)) {
                boolean add = true;
                for (Edition ex : editions) {
                    if (ex.getPublicationDate().get(java.util.Calendar.HOUR_OF_DAY) == pattern.getStartHour() && ex.getPublicationDate().get(java.util.Calendar.MINUTE) == pattern.getStartMinute()) {
                        add = false;
                    }
                }

                if (add) {
                    relavantPatterns.add(pattern);
                }
            }
        }

        for (EditionPattern relavantPattern : relavantPatterns) {
            Calendar start = (Calendar) date.clone();
            start.set(Calendar.HOUR_OF_DAY, relavantPattern.getStartHour());
            start.set(Calendar.MINUTE, relavantPattern.getStartMinute());
            start.set(Calendar.SECOND, 0);
            start.set(Calendar.MILLISECOND, 0);

            Calendar end = (Calendar) start.clone();
            end.add(Calendar.HOUR_OF_DAY, relavantPattern.getEndHour());
            end.add(Calendar.MINUTE, relavantPattern.getEndMinute());
            end.set(Calendar.SECOND, 0);
            end.set(Calendar.MILLISECOND, 0);

            Edition edition = new Edition();
            edition.setOutlet(outlet);
            edition.setPublicationDate(start);
            edition.setExpirationDate(end);

            if (start != null) {
                Calendar closeDate = (Calendar) start.clone();
                closeDate.add(Calendar.HOUR_OF_DAY, relavantPattern.getCloseHour());
                closeDate.add(Calendar.MINUTE, relavantPattern.getCloseMinute());
                edition.setCloseDate(closeDate.getTime());
                if (closeDate.before(Calendar.getInstance())) {
                    edition.setOpen(false);
                } else {
                    edition.setOpen(true);
                }

            } else {
                edition.setOpen(true);
            }

            edition.setVolume(0);
            edition.setNumber(0);
            editions.add(edition);
        }

        Collections.sort(editions, new BeanComparator("publicationDate"));

        return editions;
    }

    @Override
    public List<EditionView> findEditionViewsByDate(Long outletId, Date date, boolean includeOpen, boolean includeClosed) {
        // 1. Check if there are any editions in the database matching the date and outlet
        Calendar startDate = CalendarUtils.getStartOfDay(date);
        Calendar endDate = CalendarUtils.getEndOfDay(date);

        Outlet outlet;
        try {
            outlet = daoService.findById(Outlet.class, outletId);
        } catch (DataNotFoundException ex) {
            LOG.log(Level.SEVERE, ex.getMessage());
            return Collections.EMPTY_LIST;
        }

        Map<String, Object> params = QueryBuilder.with("outlet", outlet).and("start_date", startDate).and("end_date", endDate).parameters();
        List<EditionView> editions = daoService.findWithNamedQuery(Edition.VIEW_EDITION_PLANNING, params);

        // Load stories for editions
        for (EditionView edition : editions) {
            if (edition.isOpen() && includeOpen || !edition.isOpen() && includeClosed) {
                Map<String, Object> assignmentParams = QueryBuilder.with("edition", edition.getId()).parameters();
                List<EditionAssignmentView> assignments = daoService.findWithNamedQuery(NewsItemPlacement.VIEW_EDITION_ASSIGNMENTS, assignmentParams);
                edition.setAssignments(assignments);
            }
        }


        // 2. Generate editions based on pattern.
        List<EditionPattern> relavantPatterns = new ArrayList<EditionPattern>();
        for (EditionPattern pattern : outlet.getEditionPatterns()) {
            if (pattern.isMatchDay(date)) {
                boolean add = true;
                for (EditionView ev : editions) {
                    if (pattern.isMatchPublicationDate(ev.getPublicationDate())) {
                        add = false;
                    }
                }

                if (add) {
                    relavantPatterns.add(pattern);
                }
            }
        }

        for (EditionPattern relavantPattern : relavantPatterns) {
            Calendar start = Calendar.getInstance();
            start.setTime(date);

            start.set(Calendar.HOUR_OF_DAY, relavantPattern.getStartHour());
            start.set(Calendar.MINUTE, relavantPattern.getStartMinute());
            start.set(Calendar.SECOND, 0);
            start.set(Calendar.MILLISECOND, 0);

            Calendar end = Calendar.getInstance();
            end.setTime(date);
            end.add(Calendar.HOUR_OF_DAY, relavantPattern.getEndHour());
            end.add(Calendar.MINUTE, relavantPattern.getEndMinute());
            end.set(Calendar.SECOND, 0);
            end.set(Calendar.MILLISECOND, 0);

            EditionView edition = new EditionView();
            edition.setOutletId(outletId);
            edition.setOutletName(outlet.getTitle());
            edition.setPublicationDate(start.getTime());
            edition.setExpirationDate(end.getTime());

            if (start != null) {
                Calendar closeDate = (Calendar) start.clone();
                closeDate.add(Calendar.HOUR_OF_DAY, relavantPattern.getCloseHour());
                closeDate.add(Calendar.MINUTE, relavantPattern.getCloseMinute());
                edition.setCloseDate(closeDate.getTime());
                if (closeDate.before(Calendar.getInstance())) {
                    edition.setOpen(false);
                } else {
                    edition.setOpen(true);
                }

            } else {
                edition.setOpen(true);
            }

            editions.add(edition);
        }

        Collections.sort(editions, new BeanComparator("publicationDate"));

        return editions;
    }

    @Override
    public List<EditionCandidate> findEditionCandidatesByDate(Outlet outlet, Calendar date, boolean includeClosed) {
        List<Edition> editions = findEditionsByDate(outlet, date);
        List<EditionCandidate> candidates = new ArrayList<EditionCandidate>();

        for (Edition edition : editions) {
            if (includeClosed || edition.isOpen()) {
                candidates.add(new EditionCandidate(edition));
            }
        }

        return candidates;
    }

    /** {@inheritDoc} */
    @Override
    public Edition updateEdition(Edition edition) {
        return daoService.update(edition);
    }

    /**
     * Updates an existing {@link Edition} in the database.
     * 
     * @param editionId
     *          Unique identifier of the {@link Edition} to update
     * @param open
     *          Determines if the {@link Edition} should be open
     * @param publicationDate
     *          New publication date of the {@link Edition}
     * @param expirationDate
     *          New expiration date of the {@link Edition}
     * @param closeDate
     *          New close date of the {@link Edition}
     * @return Update {@link Edition}
     * @throws DataNotFoundException 
     *          If no {@link Edition} exist with the given {@code editionId}
     */
    @Override
    public Edition updateEdition(Long editionId, Boolean open, Date publicationDate, Date expirationDate, Date closeDate) throws DataNotFoundException {
        Edition e = daoService.findById(Edition.class, editionId);
        e.setOpen(open);

        Calendar pubDate = Calendar.getInstance();
        pubDate.setTime(publicationDate);
        e.setPublicationDate(pubDate);

        Calendar expDate = Calendar.getInstance();
        expDate.setTime(expirationDate);
        e.setExpirationDate(expDate);

        e.setCloseDate(closeDate);

        return daoService.update(e);
    }

    /** {@inheritDoc } */
    @Override
    public void deleteEdition(Long id) {
        daoService.delete(Edition.class, id);
    }

    /** {@inheritDoc } */
    @Override
    public Section findSectionById(Long id) throws DataNotFoundException {
        return daoService.findById(Section.class, id);
    }

    /** {@inheritDoc } */
    @Override
    public Section createSection(Section section) {
        return daoService.create(section);
    }

    /** {@inheritDoc } */
    @Override
    public Section updateSection(Section section) {
        return daoService.update(section);
    }

    /** {@inheritDoc} */
    @Override
    public void deleteSection(Long id) throws EntityReferenceException {
        // TODO: Check if in use
        daoService.delete(Section.class, id);
    }

    /** {@inheritDoc} */
    @Override
    public OutletEditionAction createOutletAction(OutletEditionAction action) {
        return daoService.create(action);
    }

    /** {@inheritDoc} */
    @Override
    public OutletEditionAction updateOutletAction(OutletEditionAction action) {
        return daoService.update(action);
    }

    /** {@inheritDoc} */
    @Override
    public void deleteOutletActionById(Long id) {
        daoService.delete(OutletEditionAction.class, id);
    }

    /** {@inheritDoc} */
    @Override
    public void deleteEditionPatternById(Long id) {
        daoService.delete(EditionPattern.class, id);
    }

    /** {@inheritDoc} */
    @Override
    public EditionPattern createEditionPattern(EditionPattern editionPattern) {
        return daoService.create(editionPattern);
    }

    /** {@inheritDoc} */
    @Override
    public EditionPattern updateEditionPattern(EditionPattern editionPattern) {
        return daoService.update(editionPattern);
    }

    /**
     * Schedules the execution of an edition action.
     * 
     * @param editionId
     *          Unique identifier of the edition
     * @param actionId 
     *          Unique identifier of the action
     */
    @Override
    public void scheduleAction(Long editionId, Long actionId) {
        Connection connection = null;
        try {
            connection = jmsConnectionFactory.createConnection();
            Session session = connection.createSession(true, Session.AUTO_ACKNOWLEDGE);

            MessageProducer producer = session.createProducer(destination);

            MapMessage message = session.createMapMessage();
            message.setLongProperty("editionId", editionId);
            message.setLongProperty("actionId", actionId);
            producer.send(message);

            session.close();
            connection.close();
        } catch (JMSException ex) {
            Logger.getLogger(OutletFacadeBean.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception e) {
                }
            }
        }
    }

    /**
     * Schedules the execution of all actions of an edition
     * 
     * @param editionId
     *          Unique identifier of the edition
     */
    @Override
    public void scheduleActions(Long editionId) {
        Connection connection = null;
        try {
            Edition edition = daoService.findById(Edition.class, editionId);
            List<OutletEditionAction> actions = edition.getOutlet().getAutomaticEditionActions();

            connection = jmsConnectionFactory.createConnection();
            Session session = connection.createSession(true, Session.AUTO_ACKNOWLEDGE);

            MessageProducer producer = session.createProducer(destination);

            for (OutletEditionAction action : actions) {
                MapMessage message = session.createMapMessage();
                message.setLongProperty("editionId", editionId);
                message.setLongProperty("actionId", action.getId());
                producer.send(message);
            }
            session.close();
            connection.close();

        } catch (DataNotFoundException ex) {
            LOG.log(Level.WARNING, ex.getMessage());
        } catch (JMSException ex) {
            Logger.getLogger(OutletFacadeBean.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception e) {
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public void scheduleActionsOnOutlet(Long outletId) {
        Connection connection = null;
        try {
            Outlet outlet = findOutletById(outletId);

            List<Edition> editions = findEditionsByStatus(false, outlet);

            connection = jmsConnectionFactory.createConnection();
            Session session = connection.createSession(true, Session.AUTO_ACKNOWLEDGE);
            MessageProducer producer = session.createProducer(destination);

            for (Edition edition : editions) {

                List<OutletEditionAction> actions = edition.getOutlet().getAutomaticEditionActions();

                for (OutletEditionAction action : actions) {
                    MapMessage message = session.createMapMessage();
                    message.setLongProperty("editionId", edition.getId());
                    message.setLongProperty("actionId", action.getId());
                    producer.send(message);
                }
            }
            session.close();
            connection.close();

        } catch (DataNotFoundException ex) {
            LOG.log(Level.WARNING, ex.getMessage());
        } catch (JMSException ex) {
            Logger.getLogger(OutletFacadeBean.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (connection != null) {
                try {

                    connection.close();
                } catch (Exception e) {
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public void scheduleActionOnOutlet(Long outletId, Long actionId) {
        Connection connection = null;
        try {
            Outlet outlet = findOutletById(outletId);

            List<Edition> editions = findEditionsByStatus(false, outlet);

            connection = jmsConnectionFactory.createConnection();
            Session session = connection.createSession(true, Session.AUTO_ACKNOWLEDGE);
            MessageProducer producer = session.createProducer(destination);

            for (Edition edition : editions) {
                MapMessage message = session.createMapMessage();
                message.setLongProperty("editionId", edition.getId());
                message.setLongProperty("actionId", actionId);
                producer.send(message);
            }
            session.close();
            connection.close();

        } catch (DataNotFoundException ex) {
            LOG.log(Level.WARNING, ex.getMessage());
        } catch (JMSException ex) {
            Logger.getLogger(OutletFacadeBean.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (connection != null) {
                try {

                    connection.close();
                } catch (Exception e) {
                }
            }
        }
    }

    @Override
    public List<OutletActionView> findOutletActions(Long id) throws DataNotFoundException {
        Outlet o = findOutletById(id);
        List<OutletEditionAction> actions = o.getEditionActions();
        List<OutletActionView> outletActions = new ArrayList<OutletActionView>();
        for (OutletEditionAction action : actions) {
            String actionName = "";
            try {
                actionName = action.getAction().getName();
            } catch (EditionActionException ex) {
                LOG.log(Level.SEVERE, "Could not extract information about OutletEditionAction", ex);
                actionName = "";
            }
            OutletActionView a = new OutletActionView(action.getId(), action.getLabel(), actionName, action.isManualAction());
            outletActions.add(a);
        }
        return outletActions;
    }
}
