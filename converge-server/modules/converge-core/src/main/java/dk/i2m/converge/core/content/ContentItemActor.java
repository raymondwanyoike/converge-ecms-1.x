/*
 *  Copyright (C) 2010 - 2012 Interactive Media Management
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.i2m.converge.core.content;

import dk.i2m.converge.core.security.UserAccount;
import dk.i2m.converge.core.security.UserRole;
import java.io.Serializable;
import javax.persistence.*;

/**
 * Entity representing the role of a user in a {@link ContentItem}.
 *
 * @author Allan Lykke Christensen
 */
@Entity
@Table(name = "content_item_actor")
@NamedQueries({})
public class ContentItemActor implements Serializable {

    /** Unique identifier of the {@link ContentItemActor}. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserAccount user;

    @ManyToOne
    @JoinColumn(name = "role_id")
    private UserRole role;

    @ManyToOne
    @JoinColumn(name = "content_item_id")
    private ContentItem contentItem;

    public ContentItemActor() {
    }

    public ContentItemActor(UserAccount user, UserRole role,
            ContentItem contentItem) {
        this.user = user;
        this.role = role;
        this.contentItem = contentItem;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Gets the {@link UserRole} of the news item actor.
     *
     * @return {@link UserRole} of the news item actor
     */
    public UserRole getRole() {
        return role;
    }

    /**
     * Sets the {@link UserRole} of the news item actor.
     *
     * @param role
     *          {@link UserRole} of the news item actor
     */
    public void setRole(UserRole role) {
        this.role = role;
    }

    public UserAccount getUser() {
        return user;
    }

    public void setUser(UserAccount user) {
        this.user = user;
    }

    public ContentItem getContentItem() {
        return contentItem;
    }

    public void setContentItem(ContentItem contentItem) {
        this.contentItem = contentItem;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ContentItemActor other = (ContentItemActor) obj;
        if (this.id != other.id
                && (this.id == null || !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 71 * hash + (this.id != null ? this.id.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        return getClass().getName() + "[id=" + id + "]";
    }
}
