/*
 *  Copyright (C) 2012 Interactive Media Management
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

import java.util.ArrayList;
import java.util.List;

/**
 * Contains the state of a search.
 *
 * @author Allan Lykke Christensen
 */
public class ContentResultSet {

    private List<ContentItem> hits = new ArrayList<ContentItem>();

    private String query = "";

    private long start = 0;

    private long numberOfResults = 0;

    private int resultsPerPage = 10;

    private long searchTime = 0;

    private int maxPages = 30;

    private String suggestion = "";

    private List<ContentResultSetPage> pages = null;

    /**
     * Creates a new instance of {@link SearchResults}.
     */
    public ContentResultSet() {
    }

    /**
     * Get the number of pages in the search results.
     *
     * @return Number of pages in the search results
     */
    public long getNumberOfPages() {
        if (numberOfResults <= resultsPerPage) {
            return 1;
        } else {
            long pageCount = (numberOfResults + resultsPerPage - 1)
                    / resultsPerPage;
            return pageCount;
        }
    }

    /**
     * Gets the page currently contained in the {@link SearchResults}.
     *
     * @return Page currently contained in the {@link SearchResults}
     */
    public long getCurrentPage() {
        return start / resultsPerPage;
    }

    /**
     * Get the first record displayed in the results (zero-based).
     *
     * @return First record displayed in the results (zero-based)
     */
    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public List<ContentItem> getHits() {
        return hits;
    }

    public void setHits(List<ContentItem> hits) {
        this.hits = hits;
    }

    public long getNumberOfResults() {
        return numberOfResults;
    }

    public void setNumberOfResults(long numberOfResults) {
        this.numberOfResults = numberOfResults;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public int getResultsPerPage() {
        return resultsPerPage;
    }

    public void setResultsPerPage(int resultsPerPage) {
        this.resultsPerPage = resultsPerPage;
    }

    public long getSearchTime() {
        return searchTime;
    }

    public void setSearchTime(long searchTime) {
        this.searchTime = searchTime;
    }

    public double getSearchTimeInSeconds() {
        return ((double) searchTime) / 1000.0;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

    public int getMaxPages() {
        return maxPages;
    }

    public void setMaxPages(int maxPages) {
        this.maxPages = maxPages;
    }

    /**
     * Gets a {@link List} of place holders for pages in the result set.
     * 
     * @return {@link List} of page place holders
     */
    public List<ContentResultSetPage> getPages() {
        if (this.pages == null) {
            this.pages = new ArrayList<ContentResultSetPage>();

            long s_page;
            long e_page;
            if (getNumberOfPages() > getMaxPages()) {
                int backTrack = getMaxPages() / 2;
                if (getCurrentPage() == 0) {
                    s_page = 0;
                } else {
                    s_page = Math.max(0, getCurrentPage() - backTrack);
                }
                e_page = Math.min(s_page + getMaxPages(), getNumberOfPages());
            } else {
                s_page = 0;
                e_page = getNumberOfPages();
            }

            for (long l = s_page; l < e_page; l++) {
                ContentResultSetPage page = new ContentResultSetPage((l + 1), l
                        * getResultsPerPage(), getNumberOfPages());
                this.pages.add(page);
            }
        }
        return this.pages;
    }
}
