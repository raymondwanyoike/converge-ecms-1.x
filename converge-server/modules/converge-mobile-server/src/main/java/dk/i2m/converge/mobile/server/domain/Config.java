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
package dk.i2m.converge.mobile.server.domain;

import java.io.Serializable;
import javax.persistence.*;

/**
 * Persisted configuration.
 *
 * @author Allan Lykke Christensen
 */
@Entity
@Table(name = "config")
public class Config implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Property {

        OUTLET_ID,
        OUTLET_KEY,
        RENDITION_THUMB,
        RENDITION_STORY,
        IMAGE_LOCATION,
        IMAGE_URL,
        IMAGE_DOWNLOAD_TIMEOUT
    }

    @Id
    @Column(name = "cfg_key")
    @Enumerated(EnumType.STRING)
    private Property key;

    @Column(name = "cfg_value")
    @Lob
    private String value;

    public Config() {
        this(null, "");
    }

    public Config(Property key, String value) {
        this.key = key;
        this.value = value;
    }

    public Property getKey() {
        return key;
    }

    public void setKey(Property key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (key != null ? key.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Config)) {
            return false;
        }
        Config other = (Config) object;
        if ((this.key == null && other.key != null) || (this.key != null
                && !this.key.equals(other.key))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "dk.i2m.converge.mobile.server.domain.Config[ id=" + key + " ]";
    }
}
