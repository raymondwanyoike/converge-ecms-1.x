/*
 * Copyright (C) 2011 - 2012 Interactive Media Management
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later 
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT 
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with 
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package dk.i2m.converge.plugins.opencalais;

import dk.i2m.converge.core.DataNotFoundException;
import dk.i2m.converge.core.EnrichException;
import dk.i2m.converge.core.content.NewsItem;
import dk.i2m.converge.core.content.catalogue.MediaItem;
import dk.i2m.converge.core.content.catalogue.MediaItemRendition;
import dk.i2m.converge.core.content.catalogue.Rendition;
import dk.i2m.converge.core.metadata.Concept;
import dk.i2m.converge.core.plugin.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link PluginAction} for enriching {@link MediaItem}s and {@link NewsItem}s
 * using OpenCalais.
 *
 * @author Allan Lykke Christensen
 */
public class OpenCalaisAction extends PluginAction {

    public enum Parameter {

        RENDITION
    }

    public enum PluginActionProperty {

        ENRICH_RENDITION
    }
    private static final Logger LOG = Logger.getLogger(OpenCalaisAction.class.
            getName());
    private List<PluginActionPropertyDefinition> availableProperties = null;
    private Map<String, List<String>> instanceProperties =
            new HashMap<String, List<String>>();
    private Map<String, List<String>> parameters =
            new HashMap<String, List<String>>();
    private PluginContext ctx;

    /**
     * Constructs a new instance of {@link OpenCalaisAction}.
     */
    public OpenCalaisAction() {
        onInit();
    }

    @Override
    public void onInit() {
        setBundle("dk.i2m.converge.plugins.opencalais.Messages");
    }

    @Override
    public void execute(PluginContext ctx, String itemType, Long itemId,
            PluginConfiguration cfg, Map<String, List<String>> parameters)
            throws PluginActionException {

        Object obj = getObjectFromClassName(itemType);

        if (!(obj instanceof NewsItem || obj instanceof MediaItem)) {
            return;
        }

        this.instanceProperties = cfg.getPropertiesMap();
        this.parameters = parameters;
        this.ctx = ctx;
        try {
            if (obj instanceof NewsItem) {
                processNewsItem(ctx.findNewsItemById(itemId));
            } else {
                processMediaItem(ctx.findMediaItemById(itemId));
            }
        } catch (DataNotFoundException ex) {
            throw new PluginActionException(ex, true);
        }
    }

    @Override
    public List<PluginActionPropertyDefinition> getAvailableProperties() {
        if (availableProperties == null) {
            availableProperties =
                    new ArrayList<PluginActionPropertyDefinition>();

            availableProperties.add(new PluginActionPropertyDefinition(
                    PluginActionProperty.ENRICH_RENDITION.name(),
                    "PROPERTY_ENRICH_RENDITION",
                    "PROPERTY_ENRICH_RENDITION_TOOLTIP",
                    true,
                    "rendition",
                    true, 1));

        }
        return availableProperties;
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

    private void processNewsItem(NewsItem newsItem) {
    }

    /**
     * Enriches a {@link MediaItem} with meta data from OpenCalais. <b>Only
     * documents can be enriched by this plug-in</b>. The PluginConfiguration
     * should include {@link Parameter#RENDITION} containing the name of the
     * rendition that should be sent to OpenCalais. If the parameter is not
     * passed the original rendition of the {@link MediaItem}s {@link Catalogue}
     * will be sent.
     *
     * @param mediaItem {@link MediaItem} to process
     * @throws PluginActionException If the processing failed
     */
    private void processMediaItem(MediaItem mediaItem) throws
            PluginActionException {

        LOG.log(Level.FINE, "Processing Media Item");
        try {
            String enrichRendition = mediaItem.getCatalogue().
                    getOriginalRendition().
                    getName();
            LOG.log(Level.FINE, "Default rendition to use for enrichment: {0}", enrichRendition);

            if (instanceProperties.containsKey(PluginActionProperty.ENRICH_RENDITION.
                    name())) {
                enrichRendition =
                        instanceProperties.get(PluginActionProperty.ENRICH_RENDITION.
                        name()).iterator().next();
                LOG.log(Level.FINE, "Rendition #{0} specified in the PluginConfiguration", enrichRendition);
                Long renditionId = 0L;
                try {
                    renditionId = Long.valueOf(enrichRendition);
                    Rendition rendition = ctx.findRenditionById(renditionId);
                    enrichRendition = rendition.getName();
                } catch (NumberFormatException ex) {
                    LOG.log(Level.WARNING, "Invalid value specified for Rendition in PluginConfiguration: {0}", enrichRendition);
                    throw new PluginActionException(ex);
                } catch (DataNotFoundException ex) {
                    LOG.log(Level.WARNING, "Unknown  Rendition specified in PluginConfiguration: {0}", renditionId);
                    throw new PluginActionException(ex);
                }
            };


            MediaItemRendition rendition = mediaItem.findRendition(
                    enrichRendition);

            if (!rendition.isDocument()) {
                throw new PluginActionException("Rendition #"
                        + rendition.getId() + " is not a document", true);
            }

            try {
                String content = ctx.extractContent(rendition);

                List<Concept> concepts = ctx.enrich(content);
                concepts.addAll(rendition.getMediaItem().getConcepts());
                Set<Concept> uniqueConcepts = new HashSet<Concept>(concepts);
                concepts = new ArrayList<Concept>(uniqueConcepts);
                rendition.getMediaItem().setConcepts(concepts);
                LOG.log(Level.INFO, "{0} concepts discovered", concepts.size());
            } catch (EnrichException ex) {
                LOG.log(Level.SEVERE, ex.getMessage());
                throw new PluginActionException(ex, false);
            }
        } catch (Exception cex) {
            throw new PluginActionException(cex, true);
        }

    }
}
