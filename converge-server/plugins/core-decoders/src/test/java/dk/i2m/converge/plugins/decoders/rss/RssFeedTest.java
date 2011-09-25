/*
 * Copyright (C) 2011 Interactive Media Management
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
package dk.i2m.converge.plugins.decoders.rss;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import java.util.ArrayList;
import java.util.Iterator;
import dk.i2m.converge.core.newswire.NewswireItem;
import java.util.List;
import dk.i2m.converge.core.plugin.PluginContext;
import dk.i2m.converge.core.newswire.NewswireService;
import dk.i2m.converge.core.newswire.NewswireServiceProperty;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.easymock.EasyMock.*;

/**
 * Testing the RSS decoder with a valid feed.
 *
 * @author Allan Lykke Christensen
 */
public class RssFeedTest {

    @Test
    public void testValidRssFeed() throws Exception {
        PluginContext mockCtx = createMock(PluginContext.class);
        expect(mockCtx.findNewswireItemsByExternalId(anyObject(String.class))).andReturn(new ArrayList<NewswireItem>()).times(10);
        //expect(mockCtx.findOrCreateContentTag(anyObject(String.class))).andReturn(new ContentTag()).anyTimes();
        expect(mockCtx.createNewswireItem(anyObject(NewswireItem.class))).andAnswer(new IAnswer() {

            @Override
            public Object answer() throws Throwable {
                final Object[] args = EasyMock.getCurrentArguments();
                return args[0];
            }
        }).times(10);
        checkOrder(mockCtx, false);
        replay(mockCtx);

        NewswireService service = new NewswireService();
        service.setDecoderClass(RssDecoder.class.getName());

        service.setSource("CNN Top News");
        service.getProperties().add(new NewswireServiceProperty(service, RssDecoder.URL, "file:src/test/resources/dk/i2m/converge/plugins/decoders/rss/edition.rss"));
        //service.getProperties().add(new NewswireServiceProperty(service, RssDecoder.OPENCALAIS_ENABLE, "true"));
        //service.getProperties().add(new NewswireServiceProperty(service, RssDecoder.OPENCALAIS_ID, "vtptfh9uztqxy66rdp8q7sts"));

        List<NewswireItem> results = service.getDecoder().decode(mockCtx, service);

        assertNotNull(results);
        assertEquals(10, results.size());

        Iterator i = results.iterator();

        NewswireItem item = (NewswireItem) i.next();
        assertEquals("Incorrect title", "Tunisia: Parliament speaker becomes acting president", item.getTitle());
        assertEquals("Incorrect URL", "http://edition.cnn.com/2011/WORLD/africa/01/15/tunisia.protests/index.html?eref=edition", item.getUrl());
        assertEquals("Incorrect summary", "Tunisian state TV has reported that Fouad Mebazaa is the country's acting leader, with presidential elections to be held in 60 days. The moves follow unrest that forced former president Zine El Abidine Ben Ali to flee to Saudi Arabia.", item.getSummary());
        assertEquals("Incorrect external identifier", "http://edition.cnn.com/2011/WORLD/africa/01/15/tunisia.protests/index.html?eref=edition", item.getExternalId());
        assertEquals("Incorrect summary state", true, item.isSummarised());
    }

    @Test
    public void testInvalidRssFeed() throws Exception {
        PluginContext mockCtx = createMock(PluginContext.class);
        checkOrder(mockCtx, false);
        replay(mockCtx);

        NewswireService service = new NewswireService();
        service.setDecoderClass(RssDecoder.class.getName());

        service.setSource("CNN Top News");
        service.getProperties().add(new NewswireServiceProperty(service, RssDecoder.URL, "file:src/test/resources/notfound.rss"));

        List<NewswireItem> results = service.getDecoder().decode(mockCtx, service);

        assertNotNull(results);
        assertEquals(0, results.size());
    }
}