/*
 *  Copyright (C) 2011 Interactive Media Management. All Rights Reserved.
 * 
 *  NOTICE:  All information contained herein is, and remains the property of 
 *  INTERACTIVE MEDIA MANAGEMENT and its suppliers, if any.  The intellectual 
 *  and technical concepts contained herein are proprietary to INTERACTIVE MEDIA
 *  MANAGEMENT and its suppliers and may be covered by Danish and Foreign 
 *  Patents, patents in process, and are protected by trade secret or copyright 
 *  law. Dissemination of this information or reproduction of this material
 *  is strictly forbidden unless prior written permission is obtained from 
 *  INTERACTIVE MEDIA MANAGEMENT.
 */
package dk.i2m.converge.mobile.server.service;

import dk.i2m.converge.mobile.server.domain.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.ws.rs.*;

/**
 * Facade for registration and update of {@link Subscriber}s.
 *
 * @author Allan Lykke Christensen
 */
@Stateless
@Path("subscriber")
public class SubscriberFacade {

    @PersistenceContext(unitName = "cmsPU")
    private EntityManager em;

    private static final Logger LOG = Logger.getLogger(SubscriberFacade.class.
            getName());

    /**
     * Registers a {@link Subscriber} in the database. If the {@link Subscriber}
     * already exist in the database, the HTTP status code 403 is returned.
     * <p/>
     * If a subscription data is missing from the request, HTTP status code 400
     * is returned.
     * <p/>
     * @param subscriber * {@link Subscriber} to add to the database
     */
    @POST
    @Path("register")
    @Consumes({"application/json"})
    public void register(Subscriber subscriber) {
        if (subscriber == null) {
            LOG.warning("Empty subscriber sent to register method");
            throw new WebApplicationException(400);
        }

        if (subscriber.getId() != null) {
            LOG.log(Level.WARNING,
                    "ID of subscriber expected to be null, was {0}", subscriber.
                    getId());
            throw new WebApplicationException(400);
        }

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Subscriber> cq = cb.createQuery(Subscriber.class);
        Root<Subscriber> subs = cq.from(Subscriber.class);
        cq.select(subs).where(cb.equal(subs.get("phone"), subscriber.getPhone()));

        List<Subscriber> matches = em.createQuery(cq).getResultList();

        if (matches.size() > 0) {
            LOG.log(Level.WARNING, "{0} is already registered", subscriber.
                    getPhone());
            throw new WebApplicationException(403);
        }

        subscriber.setSubscriberSince(Calendar.getInstance().getTime());
        //em.persist(subscriber);

        // Subscribe to default categories
        CriteriaBuilder cbDefaults = em.getCriteriaBuilder();
        CriteriaQuery<SectionDefault> cqDefaults =
                cbDefaults.createQuery(SectionDefault.class);
        Root<SectionDefault> defaults = cqDefaults.from(SectionDefault.class);
        //cqDefaults.select(defaults).where(cbDefaults.equal(defaults.get("section"), subscriber.getPhone()));

        for (SectionDefault sectionDefault : em.createQuery(cqDefaults).
                getResultList()) {
            subscriber.getSubscriptions().add(sectionDefault.getSection());
        }

        em.persist(subscriber);

    }

    /**
     * Updates an existing {@link Subscriber} profile.
     * <p/>
     * @param phone
     * Header parameter containing the phone number identifying the
     *          {@link Subscriber}
     * @param password
     * Header parameter containing the password matching the phone
     * number of the {@link Subscriber}
     * @param subscriber * Updated {@link Subscriber} profiler
     */
    @GET
    @Path("authenticate")
    @Produces({"application/json"})
    public boolean authenticate(@HeaderParam(value = "phone") String phone,
            @HeaderParam(value = "password") String password) {
        try {
            getSubscriber(phone, password);
            return true;
        } catch (SubscriberNotFound ex) {
            return false;
        }
    }

    /**
     * Updates an existing {@link Subscriber} profile.
     * <p/>
     * @param phone
     * Header parameter containing the phone number identifying the
     *          {@link Subscriber}
     * @param password
     * Header parameter containing the password matching the phone
     * number of the {@link Subscriber}
     * @param subscriber * Updated {@link Subscriber} profiler
     */
    @PUT
    @Path("update")
    @Consumes({"application/json"})
    public void update(@HeaderParam(value = "phone") String phone,
            @HeaderParam(value = "password") String password,
            Subscriber subscriber) {

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery cq = cb.createQuery();
        Root subs = cq.from(Subscriber.class);

        Predicate phoneMatch = cb.equal(subs.get("phone"), phone);
        Predicate passwordMatch = cb.equal(subs.get("password"), password);
        Predicate condition = cb.and(phoneMatch, passwordMatch);
        cq.select(subs).where(condition);

        List<Subscriber> matches = em.createQuery(cq).getResultList();

        if (matches.isEmpty()) {
            throw new WebApplicationException(401);
        }

        Subscriber profile = matches.iterator().next();

        profile.setDob(subscriber.getDob());
        profile.setGender(subscriber.isGender());
        profile.setName(subscriber.getName());
        profile.setPhone(subscriber.getPhone());
        profile.setPassword(subscriber.getPassword());
        profile.setLastUpdate(Calendar.getInstance().getTime());
        profile.getSubscriptions().clear();

        for (Section s : subscriber.getSubscriptions()) {
            Section section = em.find(Section.class, s.getId());
            profile.getSubscriptions().add(section);
        }

        em.merge(profile);
    }

    /**
     * Updates the category subscription of a given {@link Subscriber}.
     * <p/>
     * @param phone
     * Header parameter containing the phone number identifying the
     *          {@link Subscriber}
     * @param password
     * Header parameter containing the password matching the phone
     * number of the {@link Subscriber}
     * @param categories
     * List of subscribed categories
     */
    @POST
    @Path("subscribe")
    @Consumes({"application/json"})
    public void subscribe(@HeaderParam(value = "phone") String phone,
            @HeaderParam(value = "password") String password, String categories) {

        // Decode JSON array
        String allCats = categories.replaceAll("\\[", "").replaceAll("\\]", "").
                replaceAll("\\\"", "");
        String[] cats = allCats.split(",");


        try {
            Subscriber s = getSubscriber(phone, password);
            s.setLastUpdate(Calendar.getInstance().getTime());
            s.getSubscriptions().clear();

            for (String catId : cats) {
                Section section = em.find(Section.class, Long.valueOf(catId));
                s.getSubscriptions().add(section);
            }
            em.merge(s);
        } catch (SubscriberNotFound ex) {
            throw new WebApplicationException(401);
        }
    }

    /**
     * Updates the category subscription of a given {@link Subscriber}.
     * <p/>
     * @param phone
     * Header parameter containing the phone number identifying the
     *          {@link Subscriber}
     * @param password
     * Header parameter containing the password matching the phone
     * number of the {@link Subscriber}
     * @param items
     * List of subscribed categories
     */
    @POST
    @Path("readNews")
    @Consumes({"application/json"})
    public void read(@HeaderParam(value = "phone") String phone,
            @HeaderParam(value = "password") String password, String items) {

        // Decode JSON array
        String allItems = items.replaceAll("\\[", "").replaceAll("\\]", "").
                replaceAll("\\\"", "");
        String[] newsItems = allItems.split(",");

        System.out.println(allItems);
        Calendar now = Calendar.getInstance();

        try {
            Subscriber s = getSubscriber(phone, password);

            for (String itemId : newsItems) {
                if (!itemId.trim().isEmpty()) {
                    NewsItem newsItem = em.find(NewsItem.class, Long.valueOf(
                            itemId));

                    Read read = new Read();
                    read.setDate(now.getTime());
                    read.setNewsItem(newsItem);
                    read.setSubscriber(s);
                    em.persist(read);
                }
            }

        } catch (SubscriberNotFound ex) {
            throw new WebApplicationException(401);
        }
    }

    /**
     * Gets the profile of a {@link Subscriber}.
     * <p/>
     * @param phone    Phone number of the {@link Subscriber}
     * @param password Password matching the phone number
     * @return {@link Subscriber} matching the phone number and password. If the
     * phone number and password does not match a 401 HTTP status code
     * is thrown
     * @throws WebApplicationException If the phone and/or password is incorrect
     */
    @GET
    @Produces({"application/json"})
    public Subscriber get(@HeaderParam("phone") String phone,
            @HeaderParam(value = "password") String password) {

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery cq = cb.createQuery();
        Root subs = cq.from(Subscriber.class);
        cq.select(subs).where(cb.equal(subs.get("phone"), phone)).
                where(cb.equal(subs.get("password"), password));

        List<Subscriber> matches = em.createQuery(cq).getResultList();

        if (matches.isEmpty()) {
            throw new WebApplicationException(401);
        }

        return matches.iterator().next();
    }

    /**
     * Gets the {@link Section}s available for subscription by the given
     * {@link Subscriber}.
     * <p/>
     * @param phone
     * Phone number of the {@link Subscriber}
     * @param password
     * Password of the {@link Subscriber}
     * @param outlet
     * Unique identifier of the {@link Outlet}
     * @param key
     * Secret key of the {@link Outlet}
     * @return {@link List} of available sections for subscription. If the
     * phone number and password is incorrect, or the outlet and key
     * doesn't match a 401 HTTP status code is returned
     */
    @GET
    @Path("sections")
    @Produces({"application/json"})
    public List<Section> getSections(
            @HeaderParam("phone") String phone,
            @HeaderParam(value = "password") String password,
            @HeaderParam(value = "outlet") String outlet,
            @HeaderParam(value = "key") String key) {

        try {
            getSubscriber(phone, password);
        } catch (SubscriberNotFound ex) {
            throw new WebApplicationException(401);
        }

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery cq = cb.createQuery();
        Root o = cq.from(Outlet.class);
        cq.select(o).where(cb.equal(o.get("id"), outlet)).
                where(cb.equal(o.get("key"), key));

        List<Outlet> matches = em.createQuery(cq).getResultList();

        if (matches.isEmpty()) {
            throw new WebApplicationException(401);
        }

        Outlet match = matches.iterator().next();

        return match.getSections();
    }

    /**
     * Get the news available for the authenticated user.
     * <p/>
     * @param phone    Phone number of the user
     * @param password Password of the user
     * @return {@link List} of {@link NewsItem}s available for the user
     * @throws WebApplicationException If the user provided an invalid phone number or password
     */
    @GET
    @Path("news")
    @Produces({"application/json"})
    public List<NewsItem> getNews(@HeaderParam("phone") String phone,
            @HeaderParam(value = "password") String password) {

        try {
            Subscriber subscriber = getSubscriber(phone, password);
            subscriber.setLastUpdate(Calendar.getInstance().getTime());
            if (subscriber.getSubscriptions().isEmpty()) {
                return Collections.EMPTY_LIST;
            }

            CriteriaBuilder builder = em.getCriteriaBuilder();
            CriteriaQuery query = builder.createQuery();
            Root ni = query.from(NewsItem.class);
            query.select(ni).where(builder.equal(ni.get("available"), true)).
                    orderBy(builder.desc(ni.get("displayOrder")));

            List<NewsItem> matches = em.createQuery(query).getResultList();
            List<NewsItem> output = new ArrayList<NewsItem>();
            for (NewsItem newsItem : matches) {
                // Don't store changes made to the news item before sending it out
                em.detach(newsItem);

                if (newsItem.getSection() != null
                        && subscriber.getSubscriptions().contains(newsItem.
                        getSection())) {
                    newsItem.setStory(replaceHtmlEntities(newsItem.getStory()));
                    output.add(newsItem);
                }
            }

            return output;

        } catch (SubscriberNotFound ex) {
            throw new WebApplicationException(401);
        }
    }

    public String replaceHtmlEntities(String input) {
        input = input.replaceAll("&rsquo;", "'");
        input = input.replaceAll("&lsquo;", "'");
        input = input.replaceAll("&rdquo;", "\"");
        input = input.replaceAll("&ldquo;", "\"");
        input = input.replaceAll("&nbsp;", " ");
        input = input.replaceAll("&ndash;", "-");
        input = input.replaceAll("&mdash;", "-");
        input = input.replaceAll("\r\n", " ");
        input = input.replaceAll("\n", " ");
        return input;
    }

    private Subscriber getSubscriber(String phone, String password) throws
            SubscriberNotFound {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery cq = cb.createQuery();
        Root subs = cq.from(Subscriber.class);
        cq.select(subs).where(cb.equal(subs.get("phone"), phone)).
                where(cb.equal(subs.get("password"), password));

        List<Subscriber> matches = em.createQuery(cq).getResultList();

        if (matches.isEmpty()) {
            throw new SubscriberNotFound();
        }

        return matches.iterator().next();
    }
}
