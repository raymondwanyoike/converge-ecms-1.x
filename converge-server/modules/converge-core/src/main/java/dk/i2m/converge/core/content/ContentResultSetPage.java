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

/**
 * Representation of a page in a {@link ContentResultSet}.
 *
 * @author Allan Lykke Christensen
 */
public class ContentResultSetPage {

    private long page;

    private long start;

    private long show;

    /**
     * Creates a new instance of {@link ContentResultsSetPage}.
     */
    public ContentResultSetPage() {
        this(1, 0, 0);
    }

    /**
     * Creates a new instance of {@link ContentResultSetPage}.
     * 
     * @param page
     *          Page number
     * @param start
     *          Page record start
     * @param show 
     *          Number of records to show on the page
     */
    public ContentResultSetPage(long page, long start, long show) {
        this.page = page;
        this.start = start;
        this.show = show;
    }

    /**
     * Gets the page number.
     * 
     * @return Page number
     */
    public long getPage() {
        return page;
    }

    /**
     * Sets the page number.
     * 
     * @param page 
     *          Page number
     */
    public void setPage(long page) {
        this.page = page;
    }

    /**
     * Gets the number of records to show on the page.
     * 
     * @return Number of records to show on the page
     */
    public long getShow() {
        return show;
    }

    /**
     * Sets the number of records to show on the page.
     * 
     * @param show
     *          Number of records to show on the page
     */
    public void setShow(long show) {
        this.show = show;
    }

    /**
     * Gets the number of the first record to show on the page.
     * 
     * @return Number of the first record to show on the page
     */
    public long getStart() {
        return start;
    }

    /**
     * Sets the number of the first record to show on the page.
     * 
     * @param start 
     *          Number of the first record to show on the page
     */
    public void setStart(long start) {
        this.start = start;
    }
}