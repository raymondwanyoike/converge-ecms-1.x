/*
 * Copyright (C) 2012 Interactive Media Management
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later 
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT 
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more 
 * details.
 *
 * You should have received a copy of the GNU General Public License along with 
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package dk.i2m.converge.jsf.beans.administrator;

import dk.i2m.converge.ejb.facades.SystemFacadeLocal;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;

/**
 * Backing bean for {@code /administrator/Plugins.jspx}.
 *
 * @author Allan Lykke Christensen
 */
public class Plugins {

    protected static final Logger LOG =
            Logger.getLogger(Plugins.class.getName());

    @EJB private SystemFacadeLocal systemFacade;

    private DataModel plugins = null;

    private DataModel pluginCfg = null;

    /**
     * Creates a new instance if {@link Plugins}.
     */
    public Plugins() {
    }

    /**
     * Gets a {@link DataModel} containing available 
     * {@link PluginConfiguration}s.
     * 
     * @return {@link DataModel} containing available 
     *         {@link PluginConfiguration}s.
     */
    public DataModel getPluginConfigurations() {
        if (pluginCfg == null) {
            this.pluginCfg = new ListDataModel(systemFacade.
                    findPluginConfigurations());
        }
        return pluginCfg;
    }

    /**
     * Get a {@link DataModel} containing {@link Plugin}s discovered by the
     * application.
     * 
     * @return {@link DataModel} containing {@link Plugin}s discovered by the
     *         application
     */
    public DataModel getDiscoveredPlugins() {
        if (this.plugins == null) {
            List discovered = new ArrayList(systemFacade.getPlugins().values());
            this.plugins = new ListDataModel(discovered);
        }
        return plugins;
    }
}
