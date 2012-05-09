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
package dk.i2m.converge.core.content.catalogue;

import java.io.Serializable;
import javax.persistence.*;

/**
 * Channel for distributing {@link MediaItem}s. The purpose of a channel is to
 * distribute (upload) {@link MediaItem}s separately from {@link NewsItem}s.
 *
 * @author Allan Lykke Christensen
 */
@Entity
@Table(name = "channel")
public class Channel implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "enabled")
    private boolean enabled = true;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    @Lob
    private String description;

    /**
     * Creates a new instance of {@link Channel}.
     */
    public Channel() {
    }

    /**
     * Gets the unique identifier of the {@link Channel} assigned by the
     * underlying relational database.
     * 
     * @return Unique identifier of the {@link Channel}
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the unique identifier of the {@link Channel}.
     * 
     * @param id 
     *          Unique identifier of the {@link Channel}
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Gets the name of the {@link Channel}. The name is used for identification
     * by the user.
     * 
     * @return Name of the {@link Channel}
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the {@link Channel}.
     * 
     * @param name 
     *          Name of the {@link Channel}
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets a description of the {@link Channel}. The description is used for
     * describing the purpose of the {@link Channel}.
     * 
     * @return Description of the {@link Channel}
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of the {@link Channel}.
     * 
     * @param description 
     *          Description of the {@link Channel}
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Determine if the {@link Channel} is enabled. An enabled channel is
     * visible and usable to the users, whereas a disabled channel is not 
     * visible and can not be accessed.
     * 
     * @return {@code true} if the {@link Channel} is enabled, otherwise
     *         {@code false} 
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets the status of the {@link Channel}.
     * 
     * @param enabled
     *          {@code true} if the {@link Channel} is enabled, otherwise
     *          {@code false}
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /** {@inheritDoc } */
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    /**
     * {@link Channel}s with the same unique identifier are considered to be
     * equal, all others are not.
     * 
     * @param object 
     *          {@link Channel} to compare
     * @return {@code true} if the {@code object} has the same unique
     *         identifier, otherwise {@code false}
     **/
    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Channel)) {
            return false;
        }
        Channel other = (Channel) object;
        if ((this.id == null && other.id != null) || (this.id != null
                && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    /** {@inheritDoc } */
    @Override
    public String toString() {
        return getClass().getName() + "[id=" + this.id + "]";
    }
}
