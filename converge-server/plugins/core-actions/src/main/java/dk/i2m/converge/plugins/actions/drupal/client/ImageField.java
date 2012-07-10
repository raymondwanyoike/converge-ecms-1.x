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

/**
 * Basic image field.
 * 
 * @author Raymond Wanyoike
 */
public class ImageField {

    private Long fid;

    private Integer display;

    private String alt;

    private String title;

    /**
     * Construct an image field.
     */
    public ImageField() {
    }

    /**
     * Construct an image field.
     * 
     * @param fid file id of the image
     * @param display display field' checkbox (0 or 1)
     * @param alt alternate text
     * @param title title
     */
    public ImageField(Long fid, Integer display, String alt, String title) {
        this.fid = fid;
        this.display = display;
        this.alt = alt;
        this.title = title;
    }

    /**
     * @return the fid
     */
    public Long getFid() {
        return fid;
    }

    /**
     * @param fid the fid to set
     */
    public void setFid(Long fid) {
        this.fid = fid;
    }

    /**
     * @return the display
     */
    public Integer getDisplay() {
        return display;
    }

    /**
     * @param display the display to set
     */
    public void setDisplay(Integer display) {
        this.display = display;
    }

    /**
     * @return the alt
     */
    public String getAlt() {
        return alt;
    }

    /**
     * @param alt the alt to set
     */
    public void setAlt(String alt) {
        this.alt = alt;
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
}
