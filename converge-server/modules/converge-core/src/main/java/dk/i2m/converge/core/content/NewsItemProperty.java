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
package dk.i2m.converge.core.content;

import java.io.Serializable;
import javax.persistence.*;

/**
 * Property of a {@link NewsItem}. A property could be a system or user-relevant
 * property depending on the {@link #visibility} property.
 *
 * @author Allan Lykke Christensen
 */
@Entity
@Table(name = "news_item_property")
public class NewsItemProperty implements Serializable {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "property_name")
    private String name;

    @Column(name = "property_value")
    @Lob
    private String value;

    @Column(name = "property_visibility")
    private boolean visibility;
    
    @ManyToOne
    @JoinColumn(name="news_item_id")
    private NewsItem newsItem;

    /**
     * Creates a new instance of {@link NewsItemProperty}.
     */
    public NewsItemProperty() {
        this("", "", false, null);
    }

    /**
     * Creates a new instance of {@link NewsItemProperty}.
     * 
     * @param name 
     *          Name of the property
     * @param value 
     *          Value of the property
     * @param visibility 
     *          Visibility of the property
     * @param newsItem
     *          {@link NewsItem} owning the property
     */
    public NewsItemProperty(String name, String value, boolean visibility, NewsItem newsItem) {
        this.name = name;
        this.value = value;
        this.visibility = visibility;
        this.newsItem = newsItem;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Determines if the property is visible to the user.
     * 
     * @return {@code true} if the property is visible to the user, otherwise 
     *         {@code false}
     */
    public boolean isVisibility() {
        return visibility;
    }

    public void setVisibility(boolean visibility) {
        this.visibility = visibility;
    }

    public NewsItem getNewsItem() {
        return newsItem;
    }

    public void setNewsItem(NewsItem newsItem) {
        this.newsItem = newsItem;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final NewsItemProperty other = (NewsItemProperty) obj;
        if (this.id != other.id
                && (this.id == null || !this.id.equals(other.id))) {
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
