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
package dk.i2m.converge.plugins.actions.drupal.client;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author Raymond Wanyoike
 */
public class NodeModule {

    private Long id;

    private Long user;

    private Integer status;

    private Integer created;

    private Integer changed;

    private Integer promote;

    private Integer sticky;

    private String type;

    private String title;

    private Map<String, String> fields = new HashMap<String, String>();

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return the user
     */
    public Long getUser() {
        return user;
    }

    /**
     * @param user the user to set
     */
    public void setUser(Long user) {
        this.user = user;
    }

    /**
     * @return the status
     */
    public Integer getStatus() {
        return status;
    }

    /**
     * @param status the status to set
     */
    public void setStatus(Integer status) {
        this.status = status;
    }

    /**
     * @return the created
     */
    public Integer getCreated() {
        return created;
    }

    /**
     * @param created the created to set
     */
    public void setCreated(Integer created) {
        this.created = created;
    }

    /**
     * @return the changed
     */
    public Integer getChanged() {
        return changed;
    }

    /**
     * @param changed the changed to set
     */
    public void setChanged(Integer changed) {
        this.changed = changed;
    }

    /**
     * @return the promote
     */
    public Integer getPromote() {
        return promote;
    }

    /**
     * @param promote the promote to set
     */
    public void setPromote(Integer promote) {
        this.promote = promote;
    }

    /**
     * @return the sticky
     */
    public Integer getSticky() {
        return sticky;
    }

    /**
     * @param sticky the sticky to set
     */
    public void setSticky(Integer sticky) {
        this.sticky = sticky;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @return the fields
     */
    public Map<String, String> getFields() {
        return fields;
    }

    /**
     * @param fields the fields to set
     */
    public void setFields(Map<String, String> fields) {
        this.fields = fields;
    }

    
}