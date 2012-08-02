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
package dk.i2m.converge.jsf.beans.administrator;

import dk.i2m.converge.core.DataNotFoundException;
import dk.i2m.converge.core.plugin.PluginAction;
import dk.i2m.converge.core.plugin.PluginActionPropertyDefinition;
import dk.i2m.converge.core.plugin.PluginConfiguration;
import dk.i2m.converge.core.plugin.PluginConfigurationProperty;
import dk.i2m.converge.core.workflow.WorkflowActionException;
import dk.i2m.converge.ejb.facades.SystemFacadeLocal;
import dk.i2m.jsf.JsfUtils;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.event.ActionEvent;

/**
 * Backing bean for {@code /administrator/PluginConfigurationDetail.jspx}.
 * 
 * @author Allan Lykke Christensen
 */
public class PluginConfigurationDetail {

    private static final Logger LOG =
            Logger.getLogger(PluginConfigurationDetail.class.getName());

    @EJB
    private SystemFacadeLocal systemFacade;

    private PluginConfiguration pluginConfiguration;

    private PluginConfigurationProperty property =
            new PluginConfigurationProperty();

    private Long selectedId;

    private Map<String, String> pluginActions = null;

    /**
     * Creates a new instance of {@link PluginConfigurationDetail}.
     */
    public PluginConfigurationDetail() {
    }

    /**
     * Prepares the backing bean by obtaining the existing 
     * {@link PluginConfiguration} if the bean should be used for updating.
     */
    @PostConstruct
    public void onInit() {
        LOG.log(Level.INFO, "Initialising PluginConfiguration");
        this.selectedId = Long.valueOf(JsfUtils.getRequestParameterMap().get("id"));
        if (this.selectedId == null) {
            this.pluginConfiguration = new PluginConfiguration();
        } else {
            try {
                this.pluginConfiguration = systemFacade.
                        findPluginConfigurationById(this.selectedId);
            } catch (DataNotFoundException ex) {
                LOG.log(Level.WARNING,
                        "PluginConfiguration #{0} does not exist",
                        this.selectedId);
                this.pluginConfiguration = new PluginConfiguration();
            }
        }
    }

    public String onSave() {
        if (isNew()) {
            this.pluginConfiguration =
                    systemFacade.createPluginConfiguration(
                    this.pluginConfiguration);
        } else {
            this.pluginConfiguration =
                    systemFacade.updatePluginConfiguration(
                    this.pluginConfiguration);
        }

        return "/administration/plugins";
    }

    public void onAddProperty(ActionEvent event) {
        LOG.log(Level.INFO, "Adding Property");
        this.property = new PluginConfigurationProperty();
    }

    public void onRemoveProperty(ActionEvent event) {
        LOG.log(Level.INFO, "Remove Property");
        String propertyId =
                JsfUtils.getRequestParameterMap().get("propertyId");
        LOG.log(Level.INFO, "{0}", propertyId);
    }

    /**
     * Gets the {@link PluginConfiguration} being updated or created.
     * 
     * @see #isNew() 
     * @return {@link PluginConfiguration} being updated or created
     */
    public PluginConfiguration getPluginConfiguration() {
        return pluginConfiguration;
    }

    /**
     * Determine if the backing bean should be used for the creation of a new
     * {@link PluginConfiguration}.
     * 
     * @return {@code true} if the backing bean should be used for the creation
     *         of a new {@link PluginConfiguration}
     */
    public boolean isNew() {
        if (getPluginConfiguration().getId() == null) {
            return true;
        } else {
            return false;
        }
    }

    public Long getSelectedId() {
        return selectedId;
    }

    public void setSelectedId(Long selectedId) {
        this.selectedId = selectedId;
    }

    /**
     * Constructs a {@link Map} of discovered {@link PluginAction} with the name
     * of the {@link PluginAction} as the key and the full class name of the
     * {@link PluginAction} as the value.
     * 
     * @return {@link Map} of discovered {@link PluginAction}
     */
    public Map<String, String> getPluginsActions() {
        if (this.pluginActions == null) {
            this.pluginActions = new LinkedHashMap<String, String>();
            Iterator<PluginAction> iterator = systemFacade.findPluginActions();
            while (iterator.hasNext()) {
                PluginAction pa = iterator.next();
                this.pluginActions.put(pa.getName(), pa.getClass().getName());
            }
        }
        return this.pluginActions;
    }

    public Map<String, PluginActionPropertyDefinition> getAvailableProperties() {
        Map<String, PluginActionPropertyDefinition> availableProperties =
                new LinkedHashMap<String, PluginActionPropertyDefinition>();
        try {
            for (PluginActionPropertyDefinition p : getPluginConfiguration().
                    getAction().getAvailableProperties()) {
                availableProperties.put(p.getLabel(), p);
            }

        } catch (WorkflowActionException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        return availableProperties;
    }

    /**
     * Gets the property being updated or added.
     * 
     * @return Property being updated or added
     */
    public PluginConfigurationProperty getProperty() {
        return property;
    }

    /**
     * Sets the property being updated or added.
     * 
     * @param property
     *          Property being updated or added
     */
    public void setProperty(PluginConfigurationProperty property) {
        this.property = property;
    }
}
