/*
 *  Copyright (C) 2010 - 2011 Interactive Media Management
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.i2m.converge.core.plugin;

import dk.i2m.converge.core.Notification;
import dk.i2m.converge.core.content.catalogue.Catalogue;
import dk.i2m.converge.core.content.NewsItem;
import dk.i2m.converge.core.content.catalogue.Rendition;
import dk.i2m.converge.core.content.forex.Rate;
import dk.i2m.converge.core.content.markets.FinancialMarket;
import dk.i2m.converge.core.content.markets.MarketValue;
import dk.i2m.converge.core.content.weather.Forecast;
import dk.i2m.converge.core.newswire.NewswireDecoderException;
import dk.i2m.converge.core.newswire.NewswireItem;
import dk.i2m.converge.core.newswire.NewswireService;
import dk.i2m.converge.core.search.SearchEngineIndexingException;
import dk.i2m.converge.core.security.UserAccount;
import dk.i2m.converge.core.workflow.Outlet;
import java.util.List;

/**
 * Context of which a plug-in is being executed.
 *
 * @author Allan Lykke Christensen
 */
public interface PluginContext {

    /**
     * Gets the working directory of Converge.
     *
     * @return String containing the working directory of Converge
     */
    String getWorkingDirectory();

    /**
     * Creates a {@link NewswireItem} in the Converge database.
     *
     * @param item
     *          {@link NewswireItem} to create
     * @return Created {@link NewswireItem}
     */
    NewswireItem createNewswireItem(NewswireItem item);

    /**
     * Finds a {@link List} of {@link NewswireItem}s with a given external
     * identifier.
     *
     * @param externalId
     *          External identifier of the {@link NewswireItem}
     * @return {@link List} of {@link NewswireItem}s with the given
     *         {@code externalId}
     */
    List<NewswireItem> findNewswireItemsByExternalId(String externalId);

    /**
     * Fetches the newswire items of a given {@link NewswireService}.
     * 
     * @param service
     *          {@link NewswireService} to fetch
     * @return {@link List} of {@link NewswireItem}s in the {@link NewswireService}
     * @throws NewswireDecoderException 
     *          If the decoder of the service could not fetch the items
     */
    void fetch(NewswireService service) throws NewswireDecoderException;

    /**
     * Dispatches e-mail.
     *
     * @param to
     *          Recipient
     * @param from
     *          Addressee
     * @param subject
     *          Subject of the e-mail
     * @param content
     *          Body of the e-mail
     */
    void dispatchMail(String to, String from, String subject, String content);

    /**
     * Creates a {@link Notification}.
     *
     * @param notifcation
     *          {@link Notification} to create
     * @return Created {@link Notification}
     */
    Notification createNotification(Notification notifcation);

    List<UserAccount> findUserAccountsByRole(String roleName);

    List<NewsItem> findNewsItemsByStateAndOutlet(String stateName, Outlet outlet);

    /**
     * Indexes a given {@link NewsItem} in the search engine.
     *
     * @param item
     *          {@link NewsItem} to index
     * @throws SearchEngineIndexingException
     *          If the {@link NewsItem} could not be indexed
     */
    void index(NewsItem item) throws SearchEngineIndexingException;

    /**
     * Gets a {@link List} of the latest {@link FinancialMarket} values.
     * 
     * @return {@link List} of latest {@link FinancialMarket} values
     */
    List<MarketValue> findMarketListing();

    List<Rate> findForexListing();

    List<Forecast> findWeatherForecast();

    dk.i2m.converge.core.content.ContentTag findOrCreateContentTag(java.lang.String name);

    Catalogue findCatalogue(Long catalogueId);

    Rendition findRenditionByName(String name);

    /**
     * Archives a {@link File} in a {@link dk.i2m.converge.core.content.catalogue.Catalogue}.
     * 
     * @param file
     *          {@link File} to archive
     * @param catalogueId
     *          Unique identifier of the {@link dk.i2m.converge.core.content.catalogue.Catalogue}
     *          where the file should be archived
     * @param fileName
     *          Name of the file
     * @return Path and name of the archived file
     * @throws ArchiveException
     *          If the file could not be archived in the given catalogue
     */
    java.lang.String archive(java.io.File file, Long catalogueId, String fileName) throws ArchiveException;

    dk.i2m.converge.core.content.catalogue.MediaItemRendition createMediaItemRendition(java.io.File file, java.lang.Long mediaItemId, java.lang.Long renditionId, java.lang.String filename, java.lang.String contentType) throws java.lang.IllegalArgumentException, java.io.IOException;

    dk.i2m.converge.core.content.catalogue.MediaItemRendition updateMediaItemRendition(java.io.File file, String filename, String contentType, dk.i2m.converge.core.content.catalogue.MediaItemRendition mediaItemRendition) throws java.io.IOException;
}
