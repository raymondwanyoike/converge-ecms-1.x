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
package dk.i2m.converge.plugins.actions.drupal.client.messages;

/**
 * Holds Drupal response from a create method.
 *
 * @author Raymond Wanyoike <raymond.wanyoike@i2m.dk>
 */
public class FileCreateMessage {

    private Long fid;

    private String uri;

    /**
     * Create an empty message.
     */
    public FileCreateMessage() {
    }

    /**
     * Return created item ID.
     *
     * @return Item ID
     */
    public Long getFid() {
        return fid;
    }

    /**
     * Set created item ID.
     *
     * @param fid Item ID
     */
    public void setFid(Long fid) {
        this.fid = fid;
    }

    /**
     * Return created item URI.
     *
     * @return Drupal compliant URI
     */
    public String getUri() {
        return uri;
    }

    /**
     * Set created item URI.
     *
     * @param uri Drupal compliant URI
     */
    public void setUri(String uri) {
        this.uri = uri;
    }
}
