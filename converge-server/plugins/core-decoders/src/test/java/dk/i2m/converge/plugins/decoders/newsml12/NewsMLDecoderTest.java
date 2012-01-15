/*
 *  Copyright (C) 2011 - 2012 Interactive Media Management
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
package dk.i2m.converge.plugins.decoders.newsml12;

import dk.i2m.converge.core.content.ContentTag;
import dk.i2m.converge.core.newswire.NewswireItem;
import dk.i2m.converge.core.newswire.NewswireService;
import dk.i2m.converge.core.newswire.NewswireServiceProperty;
import dk.i2m.converge.core.plugin.PluginContext;
import java.util.Collections;
import static org.easymock.EasyMock.*;
import org.junit.*;

/**
 * Unit tests for {@link NewsMLDecoder}.
 *
 * @author Allan Lykke Christensen
 */
public class NewsMLDecoderTest {

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Testing that AFP stories can be decoded.
     */
    @Test
    @Ignore
    public void testDecodeLookupExistingItems() {
        System.out.println("testDecodeLookupExistingItems");
        final String AFP_STORY_DIR = "src/test/resources/dk/i2m/converge/plugins/decoders/newsml12/afp/stories/";
        final int STORY_COUNT = 20;

        PluginContext ctx = createMock(PluginContext.class);
        expect(ctx.findNewswireItemsByExternalId(anyObject(String.class))).andReturn(Collections.EMPTY_LIST).times(STORY_COUNT);

        checkOrder(ctx, false);
        replay(ctx);

        NewsMLDecoder decoder = new NewsMLDecoder();
        NewswireService service = new NewswireService();
        service.setId(999L);
        service.getProperties().add(new NewswireServiceProperty(service, NewsMLDecoder.Property.LOCATION.name(), AFP_STORY_DIR));
        service.getProperties().add(new NewswireServiceProperty(service, NewsMLDecoder.Property.DELETE_AFTER_PROCESS.name(), "false"));
        decoder.decode(ctx, service);
    }
    
    /**
     * Testing that AFP stories can be decoded.
     */
    @Test
    @Ignore
    public void testDecodeAfpStories() {
        System.out.println("testDecodeAfpStories");
        final String AFP_STORY_DIR = "src/test/resources/dk/i2m/converge/plugins/decoders/newsml12/afp/stories/";
        final int STORY_COUNT = 20;

        PluginContext ctx = createMock(PluginContext.class);
        expect(ctx.getWorkingDirectory()).andReturn(AFP_STORY_DIR).anyTimes();
        expect(ctx.findOrCreateContentTag(anyObject(String.class))).andReturn(new ContentTag()).anyTimes();
        expect(ctx.findNewswireItemsByExternalId(anyObject(String.class))).andReturn(Collections.EMPTY_LIST).times(STORY_COUNT);
        expect(ctx.createNewswireItem(anyObject(NewswireItem.class))).andReturn(new NewswireItem()).times(STORY_COUNT);

        checkOrder(ctx, false);
        replay(ctx);

        NewsMLDecoder decoder = new NewsMLDecoder();
        NewswireService service = new NewswireService();
        service.setId(999L);
        service.getProperties().add(new NewswireServiceProperty(service, NewsMLDecoder.Property.LOCATION.name(), AFP_STORY_DIR));
        service.getProperties().add(new NewswireServiceProperty(service, NewsMLDecoder.Property.DELETE_AFTER_PROCESS.name(), "false"));
        decoder.decode(ctx, service);
    }
}
