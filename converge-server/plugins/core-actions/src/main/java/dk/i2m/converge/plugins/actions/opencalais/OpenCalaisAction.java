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
package dk.i2m.converge.plugins.actions.opencalais;

import dk.i2m.converge.core.EnrichException;
import dk.i2m.converge.core.content.NewsItemPlacement;
import dk.i2m.converge.core.metadata.Concept;
import dk.i2m.converge.core.plugin.EditionAction;
import dk.i2m.converge.core.plugin.PluginContext;
import dk.i2m.converge.core.workflow.Edition;
import dk.i2m.converge.core.workflow.OutletEditionAction;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link EditionAction} for enriching {@link NewsItem}s with {@link Concept}s
 * from OpenCalais.
 *
 * @author Allan Lykke Christensen
 */
@dk.i2m.converge.core.annotations.OutletAction
public class OpenCalaisAction implements EditionAction {

    private Map<String, String> availableProperties = null;

    private static final Logger LOG = Logger.getLogger(OpenCalaisAction.class.getName());

    private ResourceBundle msgs = ResourceBundle.getBundle("dk.i2m.converge.plugins.actions.opencalais.Messages");

    @Override
    public void execute(PluginContext ctx, Edition edition, OutletEditionAction action) {
        for (NewsItemPlacement placements : edition.getPlacements()) {
            
            List<Concept> concepts = new ArrayList<Concept>();
            try {
                concepts = ctx.enrich(placements.getNewsItem().getStory());
            } catch (EnrichException ex) {
                LOG.log(Level.SEVERE, ex.getMessage());
            }
            placements.getNewsItem().getConcepts().addAll(concepts);
            Set<Concept> set = new HashSet<Concept>(placements.getNewsItem().getConcepts());
            ArrayList<Concept> unique = new ArrayList<Concept>(set);
            placements.getNewsItem().setConcepts(unique);
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
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return format.parse(msgs.getString("PLUGIN_BUILD_TIME"));
        } catch (Exception ex) {
            return Calendar.getInstance().getTime();
        }
    }
}
