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
 * Exception thrown if a requested property definition could not be found from a
 * a {@link PluginAction}.
 *
 * @author Allan Lykke Christensen
 */
public class PropertyDefinitionNotFoundException extends Exception {

    public PropertyDefinitionNotFoundException() {
        super();
    }

    public PropertyDefinitionNotFoundException(String string) {
        super(string);
    }

    public PropertyDefinitionNotFoundException(String string, Throwable thrwbl) {
        super(string, thrwbl);
    }

    public PropertyDefinitionNotFoundException(Throwable thrwbl) {
        super(thrwbl);
    }
}
