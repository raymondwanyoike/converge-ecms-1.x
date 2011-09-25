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
package dk.i2m.converge.ejb.services;

import dk.i2m.converge.core.ConfigurationKey;
import dk.i2m.converge.core.Notification;
import dk.i2m.converge.core.content.ContentTag;
import dk.i2m.converge.core.content.MediaRepository;
import dk.i2m.converge.core.content.NewsItem;
import dk.i2m.converge.core.content.forex.Rate;
import dk.i2m.converge.core.content.markets.MarketValue;
import dk.i2m.converge.core.content.weather.Forecast;
import dk.i2m.converge.core.newswire.NewswireDecoderException;
import dk.i2m.converge.core.newswire.NewswireItem;
import dk.i2m.converge.core.newswire.NewswireService;
import dk.i2m.converge.core.search.QueueEntryOperation;
import dk.i2m.converge.core.search.QueueEntryType;
import dk.i2m.converge.core.search.SearchEngineIndexingException;
import dk.i2m.converge.core.security.UserAccount;
import dk.i2m.converge.core.workflow.Outlet;
import dk.i2m.converge.ejb.facades.ListingFacadeLocal;
import dk.i2m.converge.ejb.facades.MediaDatabaseFacadeLocal;
import dk.i2m.converge.ejb.facades.NewsItemFacadeLocal;
import dk.i2m.converge.ejb.facades.SearchEngineLocal;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 * Implementation of the {@link PluginContext}
 *
 * @author Allan Lykke Christensen
 */
@Stateless
public class PluginContextBean implements PluginContextBeanLocal {

    @EJB private ConfigurationServiceLocal cfgService;

    @EJB private NewswireServiceLocal newswireService;

    @EJB private NotificationServiceLocal notificationService;

    @EJB private UserServiceLocal userService;

    @EJB private NewsItemFacadeLocal newsItemFacade;

    @EJB private SearchEngineLocal searchEngine;

    @EJB private ListingFacadeLocal listingFacade;

    @EJB private DaoServiceLocal daoService;

    @EJB private MediaDatabaseFacadeLocal catalogueFacade;

    @Override
    public String getWorkingDirectory() {
        return cfgService.getString(ConfigurationKey.WORKING_DIRECTORY);
    }

    @Override
    public NewswireItem createNewswireItem(NewswireItem item) {
        return newswireService.create(item);
    }

    @Override
    public List<NewswireItem> findNewswireItemsByExternalId(String externalId) {
        return newswireService.findByExternalId(externalId);
    }

    @Override
    public void fetch(NewswireService service) throws NewswireDecoderException {
        newswireService.downloadNewswireService(service.getId());
    }

    @Override
    public void dispatchMail(String to, String from, String subject, String content) {
        notificationService.dispatchMail(to, from, subject, content);
    }

    @Override
    public Notification createNotification(Notification notifcation) {
        return notificationService.create(notifcation);
    }

    @Override
    public List<UserAccount> findUserAccountsByRole(String roleName) {
        return userService.findUserAccountsByUserRoleName(roleName);
    }

    @Override
    public List<NewsItem> findNewsItemsByStateAndOutlet(String stateName, Outlet outlet) {
        return newsItemFacade.findByStateAndOutlet(stateName, outlet);
    }

    @Override
    public void index(NewsItem item) throws SearchEngineIndexingException {
        searchEngine.addToIndexQueue(QueueEntryType.NEWS_ITEM, item.getId(), QueueEntryOperation.UPDATE);
    }

    @Override
    public List<MarketValue> findMarketListing() {
        return listingFacade.findLatestMarketValues();
    }

    @Override
    public List<Rate> findForexListing() {
        return listingFacade.findLatestForexRates();
    }

    @Override
    public List<Forecast> findWeatherForecast() {
        return listingFacade.findLatestForecasts();
    }

    @Override
    public ContentTag findOrCreateContentTag(String name) {
        Map<String, Object> params = QueryBuilder.with("name", name).parameters();

        ContentTag tag;

        try {
            tag = daoService.findObjectWithNamedQuery(ContentTag.class, ContentTag.FIND_BY_NAME, params);
        } catch (DataNotFoundException ex) {
            tag = daoService.create(new ContentTag(name));
        }

        return tag;
    }

    @Override
    public MediaRepository findCatalogue(Long catalogueId) {
        try {
            return catalogueFacade.findMediaRepositoryById(catalogueId);
        } catch (DataNotFoundException ex) {
            return null;
        }
    }
}
