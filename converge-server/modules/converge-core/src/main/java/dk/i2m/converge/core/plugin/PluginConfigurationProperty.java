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

import java.io.Serializable;
import javax.persistence.*;

/**
 * Property of a {@link PluginConfiguration}. A property defines a setting for a
 * {@link PluginConfiguration} and must be one of the properties defined in
 * {@link PluginAction#getAvailableProperties()}.
 *
 * @author Allan Lykke Christensen
 */
@Entity
@Table(name = "plugin_configuration_property")
public class PluginConfigurationProperty implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "plugin_configuration_id")
    private PluginConfiguration actionConfiguration;
    @Column(name = "property_key")
    private String key;
    @Column(name = "property_value")
    @Lob
    private String value;

    /**
     * Creates a new instance of {@link PluginConfigurationProperty}.
     */
    public PluginConfigurationProperty() {
    }

    /**
     * Gets the unique identifier of the {@link PluginConfigurationProperty}.
     *
     * @return Unique identifier of the {@link PluginConfigurationProperty}
     */
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public PluginConfiguration getWorkflowStepAction() {
        return this.actionConfiguration;
    }

    public void setWorkflowStepAction(
            PluginConfiguration actionConfiguration) {
        this.actionConfiguration = actionConfiguration;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PluginConfigurationProperty other =
                (PluginConfigurationProperty) obj;
        if (this.id != other.id
                && (this.id == null || !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public int hashCode() {
        int hash = 5;
        hash = 13 * hash + (this.id != null ? this.id.hashCode() : 0);
        return hash;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public String toString() {
        return getClass().getName() + "[id=" + id + ", actionConfiguration="
                + actionConfiguration + ", key=" + key + ", value=" + value + "]";
    }
}
