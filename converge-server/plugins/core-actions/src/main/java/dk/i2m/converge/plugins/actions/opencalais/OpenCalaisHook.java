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
import dk.i2m.converge.core.annotations.CatalogueAction;
import dk.i2m.converge.core.content.catalogue.CatalogueHookInstance;
import dk.i2m.converge.core.content.catalogue.MediaItemRendition;
import dk.i2m.converge.core.content.catalogue.Rendition;
import dk.i2m.converge.core.metadata.Concept;
import dk.i2m.converge.core.plugin.CatalogueEvent;
import dk.i2m.converge.core.plugin.CatalogueEventException;
import dk.i2m.converge.core.plugin.CatalogueHook;
import dk.i2m.converge.core.plugin.PluginContext;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Allan Lykke Christensen
 */
@CatalogueAction
public class OpenCalaisHook implements CatalogueHook {

    private Map<String, String> availableProperties = null;

    private Map<String, String> instanceProperties = new HashMap<String, String>();

    private static final Logger LOG = Logger.getLogger(OpenCalaisHook.class.getName());

    public static final String ENRICH_RENDITION = "ENRICH_RENDITION";

    private ResourceBundle msgs = ResourceBundle.getBundle("dk.i2m.converge.plugins.actions.opencalais.CatalogueHookMessages");

    private String enrichRendition;

    @Override
    public void execute(PluginContext ctx, CatalogueEvent event, CatalogueHookInstance instance) throws CatalogueEventException {
        LOG.log(Level.INFO, "Executing OpenCalaisWorkflowAction");
        
        // Check that we only re-act to the Upload of new renditions
        if (event.getType() != CatalogueEvent.Event.UploadRendition) {
            return;
        }

        instanceProperties = instance.getPropertiesAsMap();

        // Determine if we need to act on the uploaded rendition
        MediaItemRendition uploadRendition = event.getRendition();
        Rendition rendition = uploadRendition.getRendition();

        if (instanceProperties.containsKey(ENRICH_RENDITION)) {
            enrichRendition = instanceProperties.get(ENRICH_RENDITION);
        } else {
            try {
                enrichRendition = uploadRendition.getMediaItem().getCatalogue().getOriginalRendition().getName();
            } catch (Exception ex) {
                LOG.log(Level.INFO, ex.getMessage());
                return;
            }
        }

        if (!rendition.getName().equalsIgnoreCase(enrichRendition)) {
            return;
        }

        if (!uploadRendition.isDocument()) {
            return;
        }
        try {
            String content = ctx.extractContent(uploadRendition);
            List<Concept> concepts = ctx.enrich(content);
            concepts.addAll(uploadRendition.getMediaItem().getConcepts());
            Set<Concept> uniqueConcepts = new HashSet<Concept>(concepts);
            concepts = new ArrayList<Concept>(uniqueConcepts);
            uploadRendition.getMediaItem().setConcepts(concepts);
            LOG.log(Level.INFO, "{0} concepts discovered", concepts.size());
        } catch (EnrichException ex) {
            LOG.log(Level.SEVERE, ex.getMessage());
            throw new CatalogueEventException(ex);
        }
        
    }

    @Override
    public Map<String, String> getAvailableProperties() {
        if (availableProperties == null) {
            availableProperties = new LinkedHashMap<String, String>();
            availableProperties.put(ENRICH_RENDITION, msgs.getString("LBL_" + ENRICH_RENDITION));
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
