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
package dk.i2m.converge.jsf.converters;

import dk.i2m.converge.core.DataNotFoundException;
import dk.i2m.converge.core.content.catalogue.Catalogue;
import dk.i2m.converge.core.plugin.PluginConfiguration;
import dk.i2m.converge.ejb.facades.CatalogueFacadeLocal;
import dk.i2m.converge.ejb.facades.SystemFacadeLocal;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;

/**
 *
 * @author Allan Lykke Christensen
 */
public class PluginConfigurationConverter implements Converter {
    
    private static final Logger LOG = Logger.getLogger(PluginConfigurationConverter.class.getName());

    private SystemFacadeLocal systemFacade;

    public PluginConfigurationConverter(SystemFacadeLocal systemFacade) {
        this.systemFacade = systemFacade;
    }

    @Override
    public Object getAsObject(FacesContext ctx, UIComponent component, String value) {
        try {
            if (value == null) {
                return null;
            }
            return systemFacade.findPluginConfigurationById(Long.valueOf(value));

        } catch (DataNotFoundException ex) {
            LOG.log(Level.WARNING, "No PluginConfiguration matching [{0}]", value);
            return null;
        }
    }

    @Override
    public String getAsString(FacesContext ctx, UIComponent component, Object value) {
        if (value == null) {
            return "";
        } else {
            PluginConfiguration cfg = (PluginConfiguration) value;
            return String.valueOf(cfg.getId());
        }
    }
}
