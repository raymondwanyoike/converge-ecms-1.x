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
import dk.i2m.converge.core.content.catalogue.Rendition;
import dk.i2m.converge.core.plugin.PluginAction;
import dk.i2m.converge.core.plugin.PluginActionPropertyDefinition;
import dk.i2m.converge.core.plugin.PluginConfiguration;
import dk.i2m.converge.core.plugin.PluginConfigurationProperty;
import dk.i2m.converge.core.plugin.PropertyDefinitionNotFoundException;
import dk.i2m.converge.core.security.UserRole;
import dk.i2m.converge.core.workflow.Workflow;
import dk.i2m.converge.core.workflow.WorkflowActionException;
import dk.i2m.converge.core.workflow.WorkflowState;
import dk.i2m.converge.core.workflow.WorkflowStep;
import dk.i2m.converge.ejb.facades.CatalogueFacadeLocal;
import dk.i2m.converge.ejb.facades.SystemFacadeLocal;
import dk.i2m.converge.ejb.facades.UserFacadeLocal;
import dk.i2m.converge.ejb.facades.WorkflowFacadeLocal;
import dk.i2m.converge.jsf.beans.Bundle;
import dk.i2m.jsf.JsfUtils;
import static dk.i2m.jsf.JsfUtils.*;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.event.ActionEvent;

/**
 * Backing bean for {@code /administrator/PluginConfigurationDetail.jspx}.
 *
 * @author Allan Lykke Christensen
 */
public class PluginConfigurationDetail {

    private static final Logger LOG = Logger.getLogger(PluginConfigurationDetail.class.getName());
    @EJB
    private SystemFacadeLocal systemFacade;
    @EJB
    private CatalogueFacadeLocal catalogueFacade;
    @EJB
    private WorkflowFacadeLocal workflowFacade;
    @EJB
    private UserFacadeLocal userFacade;
    private PluginConfiguration pluginConfiguration;
    private PluginConfiguration eventPluginConfiguration;
    private PluginConfigurationProperty property = new PluginConfigurationProperty();
    private Long selectedId;
    private Map<String, String> pluginActions = null;
    private ResourceBundle bundle;
    private PluginActionPropertyDefinition propertyDefinition;

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
        LOG.log(Level.FINE, "Initialising PluginConfiguration");
        if (getRequestParameterMap().containsKey("id")) {
            LOG.log(Level.FINE, "Loading existing PluginConfiguration");
            this.selectedId = Long.valueOf(getRequestParameterMap().get("id"));
        }

        if (this.selectedId == null) {
            this.pluginConfiguration = systemFacade.initPluginConfiguration();
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

    public String onDelete() {
        systemFacade.deletePluginConfigurationById(this.pluginConfiguration.getId());
        return "/administration/plugins";
    }

    public void onPrepareAddProperty(ActionEvent event) {
        this.property = new PluginConfigurationProperty();
        this.property.setWorkflowStepAction(getPluginConfiguration());
        this.propertyDefinition = null;
    }

    public void onAddProperty(ActionEvent event) {
        LOG.log(Level.INFO, "Adding Property");
        if (this.property.getId() == null) {
            this.property.setKey(getPropertyDefinition().getId());
            getPluginConfiguration().getProperties().add(property);
        }
        onSave();
    }

    public void onUpdateProperty(ActionEvent event) {
    }

    public void removeProperty(PluginConfigurationProperty property) {
        boolean removed = getPluginConfiguration().getProperties().remove(property);
        LOG.log(Level.INFO, "Removing {0}", removed);
    }

    public String convertPropertyValue(PluginConfigurationProperty property) {
        try {
            PluginActionPropertyDefinition def = getPluginConfiguration().getAction().findPropertyDefinition(property.getKey());

            if (def.getType().equalsIgnoreCase("rendition")) {
                Rendition rendition = catalogueFacade.findRenditionById(Long.valueOf(property.getValue()));
                return rendition.getLabel();
            } else if (def.getType().equalsIgnoreCase("workflow_state_step")) {
                WorkflowStep step = workflowFacade.findWorkflowStepById(Long.valueOf(property.getValue()));
                return step.getName();
            } else if (def.getType().equalsIgnoreCase("user_role")) {
                UserRole userRole = userFacade.findUserRoleById(Long.valueOf(property.getValue()));
                return userRole.getName();
            } else {
                return property.getValue();
            }

        } catch (WorkflowActionException ex) {
            LOG.log(Level.SEVERE, ex.getMessage());
            return property.getValue();
        } catch (PropertyDefinitionNotFoundException ex) {
            LOG.log(Level.SEVERE, ex.getMessage());
            return property.getValue();
        } catch (DataNotFoundException ex) {
            LOG.log(Level.SEVERE, ex.getMessage());
            return property.getValue();
        }
    }

    public void onPrepareAddPluginConfiguration(ActionEvent event) {
        this.eventPluginConfiguration = null;
    }

    public Map<String, PluginConfiguration> getExistingPluginConfigurations() {
        List<PluginConfiguration> cfgs = systemFacade.findPluginConfigurations();
        Map<String, PluginConfiguration> cfgMap = new LinkedHashMap<String, PluginConfiguration>();
        for (PluginConfiguration cfg : cfgs) {
            cfgMap.put(cfg.getName(), cfg);
        }

        return cfgMap;
    }

    public PluginConfiguration getEventPluginConfiguration() {
        return eventPluginConfiguration;
    }

    public void setEventPluginConfiguration(PluginConfiguration eventPluginConfiguration) {
        this.eventPluginConfiguration = eventPluginConfiguration;
    }

    public void onAddEventPluginConfiguration(ActionEvent event) {
        getPluginConfiguration().getOnCompletePluginConfiguration().add(getEventPluginConfiguration());
        onSave();
    }

    public void removeEventPluginConfiguration(PluginConfiguration cfg) {
        getPluginConfiguration().getOnCompletePluginConfiguration().remove(cfg);
        onSave();
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
     * of a new {@link PluginConfiguration}
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

    /**
     * Gets the {@link ResourceBundle} of the {@link PluginAction}. If the
     * bundle could not be fetched, the JSF resource bundle will be returned.
     *
     * @return {@link ResourceBundle} of the {@link PluginAction}
     */
    public ResourceBundle getPluginActionBundle() {
        if (this.bundle == null) {
            try {
                this.bundle = getPluginConfiguration().getAction().getBundle();
            } catch (WorkflowActionException ex) {
                LOG.log(Level.SEVERE, "Could not instantiate PluginAction for "
                        + "PluginConfiguration #{0}",
                        getPluginConfiguration().getId());
                LOG.log(Level.FINEST, "", ex);
                this.bundle = JsfUtils.getResourceBundle(Bundle.i18n.name());
            }
        }
        if (this.bundle == null) {
            LOG.log(Level.SEVERE, "Bundle is null ({0})", new Object[]{getPluginConfiguration().getActionClass()});
        }

        return this.bundle;
    }

    /**
     * Gets a {@link Map} of available properties for the selected
     * {@link PluginAction}.
     *
     * @return {@link Map} of available properties for the selected
     * {@link PluginAction}.
     */
    public Map<String, PluginActionPropertyDefinition> getAvailableProperties() {
        Map<String, PluginActionPropertyDefinition> availableProperties =
                new LinkedHashMap<String, PluginActionPropertyDefinition>();

        try {
            for (PluginActionPropertyDefinition p : getPluginConfiguration().
                    getAction().getAvailableProperties()) {

                String lbl = p.getLabel();
                if (getPluginActionBundle().containsKey(p.getLabel())) {
                    lbl = getPluginActionBundle().getString(p.getLabel());
                }

                availableProperties.put(lbl, p);
            }

        } catch (WorkflowActionException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        return availableProperties;
    }

    public Map<String, String> getRenditions() {
        Map<String, String> renditions = new LinkedHashMap<String, String>();

        List<Rendition> results = catalogueFacade.findRenditions();

        for (Rendition rendition : results) {
            renditions.put(rendition.getLabel(), "" + rendition.getId());
        }

        return renditions;
    }

    public Map<String, String> getWorkflowStateSteps() {
        Map<String, String> steps = new LinkedHashMap<String, String>();

        List<Workflow> results = workflowFacade.findAllWorkflows();

        for (Workflow workflow : results) {
            for (WorkflowState state : workflow.getStates()) {
                for (WorkflowStep step : state.getNextStates()) {
                    StringBuilder label = new StringBuilder();
                    label.append(workflow.getName())
                            .append(" : ")
                            .append(state.getName())
                            .append(" : ")
                            .append(step.getName());
                    steps.put(label.toString(), "" + step.getId());
                }
            }
        }

        return steps;
    }

    public Map<String, String> getUserRoles() {
        Map<String, String> roles = new LinkedHashMap<String, String>();

        List<UserRole> results = userFacade.getUserRoles();

        for (UserRole role : results) {
            String lbl = role.getName();
                    
            if (roles.containsKey(role.getName())) {
                lbl = role.getName() + " (#" + role.getId() + ")";
            }
            roles.put(lbl, "" + role.getId());
        }

        return roles;
    }

    /**
     * JSF {@link Converter} for obtaining the selected
     * {@link PluginActionPropertyDefinition}. Implemented as an anonymous inner
     * class to be able to access the selected {@link PluginConfiguration}.
     *
     * @return {@link Converter} for converting
     * {@link PluginActionPropertyDefinition}s
     */
    public Converter getPluginActionPropertyDefinition() {
        return new Converter() {
            @Override
            public Object getAsObject(FacesContext context, UIComponent component, String value) {
                try {
                    PluginAction action = getPluginConfiguration().getAction();
                    List<PluginActionPropertyDefinition> availableProperties = action.getAvailableProperties();
                    for (PluginActionPropertyDefinition def : availableProperties) {
                        if (def.getId().equals(value)) {
                            return def;
                        }
                    }
                } catch (WorkflowActionException ex) {
                    LOG.log(Level.SEVERE, "Could not instantiate PluginAction "
                            + "from PluginConfiguration {0}",
                            getPluginConfiguration());
                }
                return null;
            }

            @Override
            public String getAsString(FacesContext context, UIComponent component, Object value) {
                if (value == null) {
                    return "";
                }
                return ((PluginActionPropertyDefinition) value).getId();
            }
        };

    }

    public PluginActionPropertyDefinition getPropertyDefinition() {
        return this.propertyDefinition;
    }

    public void setPropertyDefinition(PluginActionPropertyDefinition propertyDefinition) {
        this.propertyDefinition = propertyDefinition;
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
     * @param property Property being updated or added
     */
    public void setProperty(PluginConfigurationProperty property) {
        LOG.log(Level.INFO, "Setting property: {0}", property);
        this.property = property;
        try {
            PluginAction action = getPluginConfiguration().getAction();
            setPropertyDefinition(action.findPropertyDefinition(this.property.getKey()));
        } catch (WorkflowActionException ex) {
            LOG.log(Level.SEVERE, "Could not instantiate PluginAction");
            LOG.log(Level.FINEST, "", ex);
        } catch (PropertyDefinitionNotFoundException ex) {
            LOG.log(Level.SEVERE, "Property definition could not be found for property");
            LOG.log(Level.FINEST, "", ex);
        }
    }
}
