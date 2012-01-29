/*
 *  Copyright (C) 2011 - 2012 Interactive Media Management. All Rights Reserved.
 * 
 *  NOTICE:  All information contained herein is, and remains the property of 
 *  INTERACTIVE MEDIA MANAGEMENT and its suppliers, if any.  The intellectual 
 *  and technical concepts contained herein are proprietary to INTERACTIVE MEDIA
 *  MANAGEMENT and its suppliers and may be covered by Danish and Foreign 
 *  Patents, patents in process, and are protected by trade secret or copyright 
 *  law. Dissemination of this information or reproduction of this material
 *  is strictly forbidden unless prior written permission is obtained from 
 *  INTERACTIVE MEDIA MANAGEMENT.
 */
package dk.i2m.converge.mobile.server.domain;

import java.io.Serializable;
import javax.persistence.*;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Persisted domain object representing a subscribable {@link Section} of an
 * {@link Outlet}.
 *
 * @author Allan Lykke Christensen
 */
@Entity
@Table(name = "section")
@XmlRootElement
public class Section implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "external_id")
    private Long externalId;

    @Column(name = "title")
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "display_order")
    private int displayOrder;

    @Column(name = "special")
    private boolean special;

    @Column(name = "imgurl")
    private String imgurl;

    @Column(name = "story_image_url")
    @Lob
    private String defaultStoryImageUrl;

    @Column(name = "story_thumb_image_url")
    @Lob
    private String defaultStoryThumbImageUrl;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getExternalId() {
        return externalId;
    }

    public void setExternalId(Long externalId) {
        this.externalId = externalId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public boolean isSpecial() {
        return special;
    }

    public void setSpecial(boolean special) {
        this.special = special;
    }

    public String getImgurl() {
        return imgurl;
    }

    public void setImgurl(String imgurl) {
        this.imgurl = imgurl;
    }

    public String getDefaultStoryImageUrl() {
        return defaultStoryImageUrl;
    }

    public void setDefaultStoryImageUrl(String defaultStoryImageUrl) {
        this.defaultStoryImageUrl = defaultStoryImageUrl;
    }

    public String getDefaultStoryThumbImageUrl() {
        return defaultStoryThumbImageUrl;
    }

    public void setDefaultStoryThumbImageUrl(String defaultStoryThumbImageUrl) {
        this.defaultStoryThumbImageUrl = defaultStoryThumbImageUrl;
    }

    /**
     * Determine if a default story image is set for this {@link Section}.
     * <p/>
     * @return {@code true} if a default story image is set, otherwise {@code false}
     */
    public boolean isDefaultStoryImageAvailable() {
        if (this.defaultStoryImageUrl == null
                || this.defaultStoryImageUrl.trim().isEmpty()) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Determine if a default story thumb image is set for this {@link Section}.
     * <p/>
     * @return {@code true} if a default story thumb image is set, otherwise {@code false}
     */
    public boolean isDefaultStoryThumbImageAvailable() {
        if (this.defaultStoryThumbImageUrl == null
                || this.defaultStoryThumbImageUrl.trim().isEmpty()) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Section)) {
            return false;
        }
        Section other = (Section) object;
        if ((this.id == null && other.id != null) || (this.id != null
                && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "dk.i2m.converge.mobile.server.domain.Section[ id=" + id + " ]";
    }
}
