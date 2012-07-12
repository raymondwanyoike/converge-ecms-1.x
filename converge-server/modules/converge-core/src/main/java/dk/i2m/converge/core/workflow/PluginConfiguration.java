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
package dk.i2m.converge.core.workflow;

import dk.i2m.converge.core.plugin.WorkflowAction;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.*;

/**
 *
 * @author Allan Lykke Christensen
 */
@Entity
@Table(name="plugin_configuration")
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

    @OneToMany(mappedBy = "actionConfiguration")
    private List<PluginConfigurationProperty> properties =
            new ArrayList<PluginConfigurationProperty>();

    public PluginConfiguration() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getActionClass() {
        return actionClass;
    }

    public void setActionClass(String actionClass) {
        this.actionClass = actionClass;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<PluginConfigurationProperty> getProperties() {
        return properties;
    }

    public void setProperties(
            List<PluginConfigurationProperty> properties) {
        this.properties = properties;
    }

    /**
     * Creates an instance of the action specified in {@link WorkflowStepAction#getActionClass()}.
     *
     * @return Instance of the action
     * @throws WorkflowActionException
     *          If the action could not be instantiated
     */
    public WorkflowAction getAction() throws WorkflowActionException {
        try {
            Class c = Class.forName(getActionClass());
            WorkflowAction action = (WorkflowAction) c.newInstance();
            return action;
        } catch (ClassNotFoundException ex) {
            throw new WorkflowActionException("Could not find action: "
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
    
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

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

    @Override
    public String toString() {
        return getClass().getName() + "[id=" + id + "]";
    }
}
