/*
 *  Copyright (C) 2010 Interactive Media Management
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
package dk.i2m.converge.jsf.functions;

import org.apache.commons.lang.StringUtils;

/**
 * JSF EL functions for working with {@link String}s
 *
 * @author Allan Lykke Christensen
 */
public class StringFunctions {

    /**
     * Abbreviates a given {@link String}.
     *
     * @param string
     *          {@link String} to abbreviate
     * @param maxWidth
     *          Maximum length of the {@link String}
     * @return Abbreviated {@link String}
     * @see org.apache.commons.lang.StringUtils#abbreviate(java.lang.String, int)
     */
    public static String abbreviate(String string, int maxWidth) {
        return StringUtils.abbreviate(string, maxWidth);
    }

}
