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

/**
 * Definition of a property for a plug-in action.
 *
 * @author Allan Lykke Christensen
 */
public class PluginActionPropertyDefinition {

    private String id;

    private String label;

    private String tooltip;

    private boolean required;

    private String type;

    private boolean multiple;

    private int displayOrder;

    public PluginActionPropertyDefinition() {
    }

    public PluginActionPropertyDefinition(String id,
            boolean required, String type, boolean multiple, int displayOrder) {
        this.id = id;
        this.label = "PROPERTY_" + this.id;
        this.tooltip = this+label + "_TOOLTIP";
        this.required = required;
        this.type = type;
        this.multiple = multiple;
        this.displayOrder = displayOrder;
    }

    public PluginActionPropertyDefinition(String id, String label,
            String tooltip,
            boolean required, String type, boolean multiple, int displayOrder) {
        this.id = id;
        this.label = label;
        this.tooltip = tooltip;
        this.required = required;
        this.type = type;
        this.multiple = multiple;
        this.displayOrder = displayOrder;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public boolean isMultiple() {
        return multiple;
    }

    public void setMultiple(boolean multiple) {
        this.multiple = multiple;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public String getTooltip() {
        return tooltip;
    }

    public void setTooltip(String tooltip) {
        this.tooltip = tooltip;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PluginActionPropertyDefinition other =
                (PluginActionPropertyDefinition) obj;
        if ((this.id == null) ? (other.id != null) : !this.id.equals(other.id)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 43 * hash + (this.id != null ? this.id.hashCode() : 0);
        return hash;
    }
}
