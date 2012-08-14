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

import java.io.File;

/**
 * Basic image field.
 * 
 * @author Raymond Wanyoike
 */
public class ImageField {

    private Long fid;

    private String alt;

    private String title;

    private String contentType;

    private File file;

    /**
     * Construct an image field.
     */
    public ImageField() {
    }

    /**
     * Construct an image field.
     * 
     * @param fid fid
     * @param alt alt
     * @param title title
     * @param file file
     */
    public ImageField(Long fid, String alt, String title, String contentType, File file) {
        this.fid = fid;
        this.alt = alt;
        this.title = title;
        this.contentType = contentType;
        this.file = file;
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

    /**
     * @return the contentType
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * @param contentType the contentType to set
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * @return the file
     */
    public File getFile() {
        return file;
    }

    /**
     * @param file the file to set
     */
    public void setFile(File file) {
        this.file = file;
    }
}
