/*
 * Copyright (C) 2010 - 2012 Interactive Media Management
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
package dk.i2m.converge.ejb.services;

import dk.i2m.converge.core.ConfigurationKey;

/**
 * Enumeration of available system timers and the configuration key that
 * contains they timing interval.
 *
 * @author Allan Lykke Christensen
 */
public enum PeriodicTimer {

    NEWSWIRE(ConfigurationKey.NEWSWIRE_INTERVAL),
    EDITION(ConfigurationKey.EDITION_INTERVAL),
    CATALOGUE_WATCH(ConfigurationKey.CATALOGUE_WATCH_INTERVAL),
    SEARCH_ENGINE_INDEXING(ConfigurationKey.SEARCH_ENGINE_INDEXING_INTERVAL),
    NEWSWIRE_BASKET(ConfigurationKey.NEWSWIRE_BASKET_INTERVAL),
    NEWSWIRE_PURGE(ConfigurationKey.NEWSWIRE_PURGE_INTERVAL),
    JOB_QUEUE(ConfigurationKey.JOB_QUEUE_INTERVAL),
    /**
     * Generic timer executed daily.
     */
    DAILY(ConfigurationKey.DAILY_INTERVAL);
    /**
     * Interval between executions of the timer.
     */
    private final ConfigurationKey interval;

    PeriodicTimer(ConfigurationKey intervalKey) {
        interval = intervalKey;
    }

    public ConfigurationKey interval() {
        return interval;
    }
}