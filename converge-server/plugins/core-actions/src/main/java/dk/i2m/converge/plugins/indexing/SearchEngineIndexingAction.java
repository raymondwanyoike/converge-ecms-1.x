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
package dk.i2m.converge.plugins.indexing;

import dk.i2m.converge.core.DataNotFoundException;
import dk.i2m.converge.core.content.ContentItem;
import dk.i2m.converge.core.content.NewsItem;
import dk.i2m.converge.core.content.catalogue.MediaItem;
import dk.i2m.converge.core.plugin.PluginAction;
import dk.i2m.converge.core.plugin.PluginActionException;
import dk.i2m.converge.core.plugin.PluginActionPropertyDefinition;
import dk.i2m.converge.core.plugin.PluginConfiguration;
import dk.i2m.converge.core.plugin.PluginContext;
import dk.i2m.converge.core.plugin.PropertyDefinitionNotFoundException;
import dk.i2m.converge.core.search.SearchEngineIndexingException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * {@link PluginAction} for indexing a {@link ContentItem} for the internal
 * search engine.
 *
 * @author Allan Lykke Christensen
 */
public class SearchEngineIndexingAction extends PluginAction {

    private PluginContext ctx;

    public SearchEngineIndexingAction() {
        onInit();
    }
    
    @Override
    public void onInit() {
        setBundle("dk.i2m.converge.plugins.indexing.Messages");
    }

    @Override
    public void execute(PluginContext ctx, String itemType, Long itemId,
            PluginConfiguration cfg,
            Map<String, List<String>> variables) throws PluginActionException {
        this.ctx = ctx;

        if (itemType.equals(ContentItem.class.getName())) {
            ContentItem ci;
            try {
                ci = ctx.findContentItemById(itemId);
            } catch (DataNotFoundException ex) {
                throw new PluginActionException("ContentItem #" + itemId
                        + " does not exist", ex, true);
            }

            // Type processing
            try {
                if (ci instanceof NewsItem) {
                    indexNewsItem((NewsItem) ci);
                } else if (ci instanceof MediaItem) {
                    indexMediaItem((MediaItem) ci);
                }
            } catch (SearchEngineIndexingException ex) {
                throw new PluginActionException(ex.getMessage(), ex, false);
            }

        } else {
            throw new PluginActionException(getName()
                    + " only supports processing of " + ContentItem.class.
                    getName(), true);
        }
    }

    private void indexNewsItem(NewsItem newsItem) throws
            SearchEngineIndexingException {
        if (newsItem.isEndState()) {
            ctx.index(newsItem);
        }
    }

    private void indexMediaItem(MediaItem mediaItem) throws
            SearchEngineIndexingException {
        ctx.index(mediaItem);
    }

    /**
     * The plug-in does not have any properties.
     * 
     * @return Empty {@link List}
     */
    @Override
    public List<PluginActionPropertyDefinition> getAvailableProperties() {
        return Collections.EMPTY_LIST;
    }
    
    /**
     * {@inheritDoc }
     */
    @Override
    public PluginActionPropertyDefinition findPropertyDefinition(String id)
            throws PropertyDefinitionNotFoundException {
        for (PluginActionPropertyDefinition d : getAvailableProperties()) {
            if (d.getId().equals(id)) {
                return d;
            }
        }
        throw new PropertyDefinitionNotFoundException(id + " not found");
    }
}
