/*
 *  Copyright (C) 2011 Interactive Media Management. All Rights Reserved.
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
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * {@link Outlet} exposing news to mobile subscribers. An outlet can be
 * synchronised with an outlet of <em>Converge Editorial</em> by setting the
 * {@link #externalUrl} to the URL of <em>Converge Editorial</em> and 
 * {@link #externalId} to the unique ID of the outlet on <em>Converge 
 * Editorial</em>.
 *
 * @author Allan Lykke Christensen
 */
@Entity
@Table(name = "outlet")
@XmlRootElement
public class Outlet implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @Column(name = "title")
    private String title;
    @Column(name = "private_key")
    private String key;
    @OneToMany()
    @JoinTable(name = "outlet_section", joinColumns =
    @JoinColumn(name = "outlet_id"), inverseJoinColumns =
    @JoinColumn(name = "section_id"))
    private List<Section> sections = new ArrayList<Section>();
    @Column(name = "external_id")
    private String externalId;
    @Column(name = "external_url")
    private String externalUrl;
    @Column(name = "external_uid")
    private String externalUid;
    @Column(name = "external_pwd")
    private String externalPwd;

    /**
     * Creates a new instance of {@link Outlet}.
     */
    public Outlet() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public List<Section> getSections() {
        return sections;
    }
    
    public void setSections(List<Section> sections) {
        this.sections = sections;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getExternalUrl() {
        return externalUrl;
    }

    public void setExternalUrl(String externalUrl) {
        this.externalUrl = externalUrl;
    }

    public String getExternalPwd() {
        return externalPwd;
    }

    public void setExternalPwd(String externalPwd) {
        this.externalPwd = externalPwd;
    }

    public String getExternalUid() {
        return externalUid;
    }

    public void setExternalUid(String externalUid) {
        this.externalUid = externalUid;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Outlet)) {
            return false;
        }
        Outlet other = (Outlet) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "dk.i2m.converge.mobile.server.domain.Outlet[ id=" + id + " ]";
    }
}
