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
package dk.i2m.converge.core.plugin;

import dk.i2m.converge.core.workflow.WorkflowActionException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.*;
import org.eclipse.persistence.annotations.PrivateOwned;

/**
 * Configuration of a {@link PluginAction}. A {@link PluginConfiguration} can be
 * re-used by workflow actions without having to re-defined the configuration.
 *
 * @author Allan Lykke Christensen
 */
@Entity
@Table(name = "plugin_configuration")
public class PluginConfiguration implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @Column(name = "name")
    private String name = "";
    @Column(name = "description")
    @Lob
    private String description = "";
    @Column(name = "action_class")
    private String actionClass = null;
    @OneToMany(mappedBy = "actionConfiguration", cascade = CascadeType.ALL)
    @PrivateOwned
    private List<PluginConfigurationProperty> properties =
            new ArrayList<PluginConfigurationProperty>();
    /**
     * List of PluginConfigurations to executed upon completion of this
     * configuration.
     */
    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "plugin_configuration_oncomplete",
    joinColumns =
    @JoinColumn(name = "main_configuration_id", referencedColumnName = "id"),
    inverseJoinColumns =
    @JoinColumn(name = "complete_configuration_id", referencedColumnName = "id"))
    private List<PluginConfiguration> completeCfg =
            new ArrayList<PluginConfiguration>();
    /**
     * List of PluginConfigurations that execute this configuration upon
     * completion.
     */
    @ManyToMany(mappedBy = "completeCfg")
    private List<PluginConfiguration> mainCfg =
            new ArrayList<PluginConfiguration>();

    /**
     * Creates a new instance of {@link PluginConfiguration}.
     */
    public PluginConfiguration() {
    }

    /**
     * Gets the unique identifier of the {@link PluginConfiguration}.
     *
     * @return Unique identifier of the {@link PluginConfiguration}
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the unique identifier of the {@link PluginConiguration}.
     *
     * @param id Unique identifier of the {@link PluginConfiguration}
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Gets the name of the configuration for identification purposes.
     *
     * @return Name of the configuration for identification purposes
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the configuration for identification purposes.
     *
     * @param name Name of the configuration for identification purposes
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the description of the configuration for documentation purposes.
     *
     * @return Description of the configuration for documentation purposes
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of the configuration for documentation purposes.
     *
     * @param description Description of the configuration for documentation
     * purposes
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the name of the {@link PluginAction} class of the configuration.
     *
     * @return Class name of the {@link PluginAction} that is subject of this
     * configuration
     */
    public String getActionClass() {
        return actionClass;
    }

    /**
     * Sets the name of the {@link PluginAction} class of the configuration.
     *
     * @param actionClass Class name of the {@link PluginAction} that is subject
     * of this configuration
     */
    public void setActionClass(String actionClass) {
        this.actionClass = actionClass;
    }

    /**
     * Gets a {@link List} of the properties making up the configuration of the
     * action.
     *
     * @return {@link List} of the properties making up the configuration of the
     * action
     */
    public List<PluginConfigurationProperty> getProperties() {
        return properties;
    }

    /**
     * Sets a {@link List} of the properties making up the configuration of the
     * action.
     *
     * @param properties {@link List} of the properties making up the
     * configuration of the action
     */
    public void setProperties(
            List<PluginConfigurationProperty> properties) {
        this.properties = properties;
    }

    /**
     * Creates an instance of the action specified in {@link #getActionClass()}.
     *
     * @return Instance of the action
     * @throws WorkflowActionException If the action could not be instantiated
     */
    public PluginAction getAction() throws WorkflowActionException {
        try {
            Class c = Class.forName(getActionClass());
            PluginAction action = (PluginAction) c.newInstance();
            action.onInit();
            return action;
        } catch (ClassNotFoundException ex) {
            throw new WorkflowActionException("Could not find PluginAction: "
                    + getActionClass(), ex);
        } catch (InstantiationException ex) {
            throw new WorkflowActionException(
                    "Could not instantiate action [" + getActionClass()
                    + "]. Check to ensure that the action has a public contructor with no arguments",
                    ex);
        } catch (IllegalAccessException ex) {
            throw new WorkflowActionException("Could not access action: "
                    + getActionClass(), ex);
        }
    }

    /**
     * Gets a {@link Map} containing the properties. The {@link Map} uses the
     * properties name as the key, and a {@link List} to contain the values for
     * the property. In many cases the {@link List} will only contain a single
     * value.
     *
     * @return {@link Map} containing the properties.
     */
    public Map<String, List<String>> getPropertiesMap() {
        Map<String, List<String>> map = new HashMap<String, List<String>>();
        for (PluginConfigurationProperty property : properties) {
            if (!map.containsKey(property.getKey())) {
                List<String> vals = new ArrayList<String>();
                map.put(property.getKey(), vals);
            }

            map.get(property.getKey()).add(property.getValue());
        }
        return map;
    }

    /**
     * Gets a {@link List} of {@link PluginConfiguration}s that should be
     * executed after this {@link PluginConfiguration} has completed
     * successfully.
     *
     * @return {@link List} of {@link PluginConfiguration}s that should be
     * executed after this {@link PluginConfiguration} has completed
     * successfully.
     */
    public List<PluginConfiguration> getOnCompletePluginConfiguration() {
        return completeCfg;
    }

    /**
     * Sets a {@link List} of {@link PluginConfiguration}s that should be
     * executed after this {@link PluginConfiguration} has completed
     * successfully.
     *
     * @param completeCfg {@link List} of {@link PluginConfiguration}s that
     * should be executed after this {@link PluginConfiguration} has completed
     * successfully.
     */
    public void setOnCompletePluginConfiguration(List<PluginConfiguration> completeCfg) {
        this.completeCfg = completeCfg;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean equals(Object object) {
        if (!(object instanceof PluginConfiguration)) {
            return false;
        }
        PluginConfiguration other = (PluginConfiguration) object;
        if ((this.id == null && other.id != null) || (this.id != null
                && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public String toString() {
        return getClass().getName() + "[id=" + id + "]";
    }
}
