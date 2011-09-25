/*
 * Copyright (C) 2010 Interactive Media Management
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

import dk.i2m.commons.FileUtils;
import java.io.IOException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for the {@link MediaItemThumbnailGenerator}.
 *
 * @author Allan Lykke Christensen
 */
public class MediaItemThumbnailGeneratorTest {

    public MediaItemThumbnailGeneratorTest() {
    }

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

    @Test
    public void generateImageJpeg() throws IOException {
        try {
            MediaItem item = new MediaItem();
            item.setId(1L);
            item.setTitle("Media Item Title");
            item.setContentType("image/jpg");
            byte[] file = FileUtils.getBytes(getClass().getResourceAsStream("/dk/i2m/converge/content/media-item-image.JPG"));
            byte[] thumb = MediaItemThumbnailGenerator.getInstance().generateThumbnail(file, item);
            FileUtils.writeToFile(thumb, "target/thumb-img.jpg");

            assertNotNull(thumb);
        } catch (UnknownMediaItemException ex) {
            fail(ex.getMessage());
        } catch (ThumbnailGeneratorException ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void generateUnknown() throws IOException {
        try {
            MediaItem item = new MediaItem();
            item.setId(1L);
            item.setTitle("Document");
            item.setContentType("application/pdf");
            byte[] file = FileUtils.getBytes(getClass().getResourceAsStream("/dk/i2m/converge/content/media-item-doc.pdf"));
            byte[] thumb = MediaItemThumbnailGenerator.getInstance().generateThumbnail(file, item);
            FileUtils.writeToFile(thumb, "target/thumb-doc.jpg");

            assertNotNull(thumb);
        } catch (UnknownMediaItemException ex) {
            fail(ex.getMessage());
        } catch (ThumbnailGeneratorException ex) {
            fail(ex.getMessage());
        }
    }
}