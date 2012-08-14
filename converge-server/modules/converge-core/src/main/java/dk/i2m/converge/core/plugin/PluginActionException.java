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
package dk.i2m.converge.core.plugin;

/**
 * Exception thrown if a {@link PluginAction} failed execution.
 * 
 * @author Allan Lykke Christensen
 */
public class PluginActionException extends Exception {

    private boolean permanent = false;

    public PluginActionException() {
        super();
    }
    
    public PluginActionException(Throwable cause) {
        super(cause);
    }

    public PluginActionException(String message, Throwable cause) {
        super(message, cause);
    }

    public PluginActionException(String message) {
        super(message);
    }

    public PluginActionException(Throwable cause, boolean permanent) {
        this(cause);
        this.permanent = permanent;
    }

    public PluginActionException(String message, Throwable cause,
            boolean permanent) {
        this(message, cause);
        this.permanent = permanent;
    }

    public PluginActionException(String message, boolean permanent) {
        this(message);
        this.permanent = permanent;
    }

    public PluginActionException(boolean permanent) {
        this();
        this.permanent = permanent;
    }

    /**
     * Determines if the error is permanent. A permanent error indicates that
     * there is no purpose in re-executing the plug-in at a later time. Some
     * plug-ins may throw a non-permanent exception indicating that the plug-in
     * could not finish executing at the moment, but upon retrying the execution
     * may succeed. 
     * 
     * @return {@code true} if the underlying problem is permanent, otherwise
     *         {@code false}
     */
    public boolean isPermanent() {
        return permanent;
    }
}
