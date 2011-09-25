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
package dk.i2m.converge.core.plugin;

import dk.i2m.converge.core.newswire.NewswireItem;
import dk.i2m.converge.core.newswire.NewswireService;
import java.util.List;
import java.util.Map;

/**
 * Interface for implementing a newswire decoder. A newswire decoder for
 * decoding a specific newswire format.
 *
 * @author Allan Lykke Christensen
 */
public interface NewswireDecoder extends Plugin {

    /**
     * Provides a map of possible properties for the decoder.
     *
     * @return Map of possible decoder properties
     */
    public abstract Map<String, String> getAvailableProperties();

    /**
     * Decodes the newswire.
     *
     * @param ctx
     *          Plug-in context
     * @param newswire
     *          Service for which the decoder is used
     * @return {@link List} of news wire items decoded
     */
    public abstract List<NewswireItem> decode(PluginContext ctx, NewswireService newswire);
}
