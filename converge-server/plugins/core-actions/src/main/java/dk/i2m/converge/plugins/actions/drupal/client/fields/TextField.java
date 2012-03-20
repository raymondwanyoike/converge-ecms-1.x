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
package dk.i2m.converge.plugins.actions.drupal.client.fields;

/**
 * Defines a simple text field type.
 */
public class TextField {

    private String summary;

    private String value;

    private String format;

    /**
     * Create an empty TextField.
     */
    public TextField() {
    }

    /**
     * Constructs a TextField from the given components.
     *
     * @param summary This allows authors to input an explicit summary
     * @param value The value of the textfield
     * @param format The format of the textfield (plaintext, html etc)
     */
    public TextField(String summary, String value, String format) {
        this.summary = summary;
        this.value = value;
        this.format = format;
    }

    /**
     * Return the format of the textfield.
     *
     * @return The format
     */
    public String getFormat() {
        return format;
    }

    /**
     * Sets the format of the textfield
     * @param format The format (plaintext, html etc)
     */
    public void setFormat(String format) {
        this.format = format;
    }

    /**
     * Return the field summary.
     *
     * @return The field summary
     */
    public String getSummary() {
        return summary;
    }

    /**
     * Sets the field summary, this allows authors to input an explicit summary.
     *
     * @param summary The field summary
     */
    public void setSummary(String summary) {
        this.summary = summary;
    }

    /**
     * Return the field value
     *
     * @return The field value
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value of the field
     *
     * @param value The field value
     */
    public void setValue(String value) {
        this.value = value;
    }
}
