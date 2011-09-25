/*
 * Copyright (C) 2010 Interactive Media Management
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
package dk.i2m.converge.core.content;

import dk.i2m.converge.core.newswire.NewswireItemAttachment;
import dk.i2m.converge.core.security.UserRole;
import java.io.Serializable;
import java.util.Calendar;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;

/**
 * Entity representing a repository containing {@link MediaItem}s.
 *
 * @author Allan Lykke Christensen
 */
@Entity()
@Table(name = "media_repository")
@NamedQueries({
    @NamedQuery(name = MediaRepository.FIND_ENABLED, query = "SELECT mr FROM MediaRepository mr WHERE mr.enabled=true"),
    @NamedQuery(name = MediaRepository.FIND_WRITABLE, query = "SELECT mr FROM MediaRepository mr WHERE mr.readOnly=false")
})
public class MediaRepository implements Serializable {

    /** Query for finding the list of enabled media repositories. */
    public static final String FIND_ENABLED = "MediaRepository.findEnabled";

    /** Query for finding the list of writable media repositories. */
    public static final String FIND_WRITABLE = "MediaRepository.findWritable";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name")
    private String name = "";

    @Column(name = "description") @Lob
    private String description = "";

    @Column(name = "location") @Lob
    private String location = "";

    @Column(name = "web_access") @Lob
    private String webAccess = "";

    @Column(name = "watch_location") @Lob
    private String watchLocation = "";

    @Column(name = "read_only")
    private boolean readOnly = false;

    @Column(name = "enabled")
    private boolean enabled = true;

    @Column(name = "last_index")
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Calendar lastIndex;

    @Column(name = "last_item_count")
    private int itemCount = 0;

    @ManyToOne
    @JoinColumn(name = "editor_role_id")
    private UserRole editorRole;

    @OneToMany(mappedBy = "catalogue")
    private List<NewswireItemAttachment> newswireItemAttachments;

    /**
     * Creates a new instance of {@link MediaRepository}.
     */
    public MediaRepository() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Calendar getLastIndex() {
        return lastIndex;
    }

    public void setLastIndex(Calendar lastIndex) {
        this.lastIndex = lastIndex;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getWatchLocation() {
        return watchLocation;
    }

    public void setWatchLocation(String watchLocation) {
        this.watchLocation = watchLocation;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public int getItemCount() {
        return itemCount;
    }

    public void setItemCount(int itemCount) {
        this.itemCount = itemCount;
    }

    public UserRole getEditorRole() {
        return editorRole;
    }

    public void setEditorRole(UserRole editorRole) {
        this.editorRole = editorRole;
    }

    public String getWebAccess() {
        return webAccess;
    }

    public void setWebAccess(String webAccess) {
        this.webAccess = webAccess;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MediaRepository other = (MediaRepository) obj;
        if (this.id != other.id && (this.id == null || !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + (this.id != null ? this.id.hashCode() : 0);
        return hash;
    }
}
