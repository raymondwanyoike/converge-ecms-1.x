/*
 * Copyright (C) 2011 Interactive Media Management
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package dk.i2m.converge.plugins.decoders.rss;

import dk.i2m.converge.core.newswire.NewswireDecoderException;
import dk.i2m.converge.core.newswire.NewswireItem;
import dk.i2m.converge.core.newswire.NewswireService;
import dk.i2m.converge.core.newswire.NewswireServiceProperty;
import dk.i2m.converge.core.plugin.PluginContext;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.easymock.EasyMock;
import static org.easymock.EasyMock.*;
import org.easymock.IAnswer;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Testing the RSS decoder with a valid feed.
 *
 * @author Allan Lykke Christensen
 */
public class RssFeedTest {

    @Test
    public void testValidRssFeed() throws Exception {
        final int NUMBER_OF_ITEMS = 10;
        PluginContext mockCtx = createMock(PluginContext.class);

        // Expect that it will try to see if the item already exists in the database
        expect(mockCtx.findNewswireItemsByExternalId(anyObject(String.class))).andReturn(new ArrayList<NewswireItem>()).times(10);

        // Expect that it will create the items in the database
        expect(mockCtx.createNewswireItem(anyObject(NewswireItem.class))).andAnswer(new IAnswer() {

            @Override
            public Object answer() throws Throwable {
                final Object[] args = EasyMock.getCurrentArguments();
                return args[0];
            }
        }).times(NUMBER_OF_ITEMS);

        // Expect that it will index the items in the search engine
        mockCtx.index(anyObject(NewswireItem.class));
        expectLastCall().andAnswer(new IAnswer() {

            @Override
            public Object answer() throws Throwable {
                final Object[] args = EasyMock.getCurrentArguments();
                return args[0];
            }
        }).times(NUMBER_OF_ITEMS);

        checkOrder(mockCtx, false);
        replay(mockCtx);

        NewswireService service = new NewswireService();
        service.setDecoderClass(RssDecoder.class.getName());
        service.setSource("CNN Top News");
        service.getProperties().add(new NewswireServiceProperty(service, RssDecoder.Property.URL.name(),
                "file:src/test/resources/dk/i2m/converge/plugins/decoders/rss/edition.rss"));
        service.getDecoder().decode(mockCtx, service);
    }

    @Test
    public void testInvalidRssFeed() {
        PluginContext mockCtx = createMock(PluginContext.class);
        checkOrder(mockCtx, false);
        replay(mockCtx);

        NewswireService service = new NewswireService();
        service.setDecoderClass(RssDecoder.class.getName());

        service.setSource("CNN Top News");
        service.getProperties().add(new NewswireServiceProperty(service, RssDecoder.Property.URL.name(), "file:src/test/resources/notfound.rss"));

        try {
            service.getDecoder().decode(mockCtx, service);
            fail("Exception not thrown for invalid RSS feed");
        } catch (NewswireDecoderException ex) {
        }
    }
}