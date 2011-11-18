/*
 *  Copyright (C) 2010 Interactive Media Management
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
package dk.i2m.converge.plugins.indexedition;

import dk.i2m.converge.core.plugin.EditionAction;
import dk.i2m.converge.core.workflow.Edition;
import dk.i2m.converge.core.workflow.OutletEditionAction;
import dk.i2m.converge.core.content.NewsItemPlacement;
import dk.i2m.converge.core.plugin.PluginContext;
import dk.i2m.converge.core.search.SearchEngineIndexingException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link EditionAction} for indexing the news items of an {@link Edition}.
 *
 * @author Allan Lykke Christensen
 */
@dk.i2m.converge.core.annotations.OutletAction
public class IndexEditionAction implements EditionAction {

    private static final Logger logger = Logger.getLogger(IndexEditionAction.class.getName());

    private Map<String, String> availableProperties = null;

    private ResourceBundle msgs = ResourceBundle.getBundle("dk.i2m.converge.plugins.indexedition.Messages");

    private Calendar releaseDate = new GregorianCalendar(2010, Calendar.AUGUST, 2, 0, 40);

    @Override
    public void execute(PluginContext ctx, Edition edition, OutletEditionAction action) {
        for (NewsItemPlacement placement : edition.getPlacements()) {
            logger.log(Level.INFO, "Indexing #{0}", placement.getNewsItem().getId());
            if (placement.getNewsItem().isEndState()) {
                try {
                    ctx.index(placement.getNewsItem());
                } catch (SearchEngineIndexingException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
            } else {
                logger.log(Level.INFO, "#{0} is not at the end state. Indexing skipped for #{0}", placement.getNewsItem().getId());
            }
        }
    }

    @Override
    public Map<String, String> getAvailableProperties() {
        if (availableProperties == null) {
            availableProperties = new LinkedHashMap<String, String>();
        }
        return availableProperties;
    }

    @Override
    public String getName() {
        return msgs.getString("PLUGIN_NAME");
    }

    @Override
    public String getDescription() {
        return msgs.getString("PLUGIN_DESCRIPTION");
    }

    @Override
    public String getVendor() {
        return msgs.getString("PLUGIN_VENDOR");
    }

    @Override
    public Date getDate() {
        return releaseDate.getTime();
    }
}
