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
package dk.i2m.converge.core.helpers;

import com.adobe.xmp.XMPException;
import com.adobe.xmp.XMPIterator;
import com.adobe.xmp.XMPMeta;
import com.adobe.xmp.XMPMetaFactory;
import com.adobe.xmp.properties.XMPProperty;
import dk.i2m.commons.FileUtils;
import dk.i2m.converge.core.content.MediaItem;
import dk.i2m.converge.core.content.MediaItemThumbnailGenerator;
import dk.i2m.converge.core.content.MediaRepository;
import dk.i2m.converge.core.content.ThumbnailGeneratorException;
import dk.i2m.converge.core.content.UnknownMediaItemException;
import dk.i2m.converge.core.search.CannotIndexException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.Sanselan;
import org.apache.sanselan.common.IImageMetadata;
import org.apache.sanselan.common.ImageMetadata;
import org.apache.sanselan.formats.tiff.TiffImageMetadata;
import org.apache.sanselan.formats.tiff.TiffImageMetadata.Item;
import org.blinkenlights.jid3.ID3Exception;
import org.blinkenlights.jid3.ID3Tag;
import org.blinkenlights.jid3.MP3File;
import org.blinkenlights.jid3.MediaFile;
import org.blinkenlights.jid3.v1.ID3V1_0Tag;
import org.blinkenlights.jid3.v2.ID3V2_3_0Tag;

/**
 * Helper for interacting with a catalogue.
 *
 * @author Allan Lykke Christensen
 */
public class CatalogueHelper {

    private static final Logger LOG = Logger.getLogger(CatalogueHelper.class.getName());

    private static CatalogueHelper instance = null;

    /** Namespace of the dublin core. */
    private String NS_DC = "http://purl.org/dc/elements/1.1/";

    /** Namespace of photoshop tags. */
    private String NS_PHOTOSHOP = "http://ns.adobe.com/photoshop/1.0/";

    private String generateUniqueFilename(MediaItem item) {
        StringBuilder filename = new StringBuilder();
        filename.append(item.getId()).append("-").append(item.getFilename());
        return filename.toString();
    }

    private static void copyFile(File sourceFile, File destFile) throws IOException {
        if (!destFile.exists()) {
            destFile.createNewFile();
        }

        FileChannel source = null;
        FileChannel destination = null;
        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        } finally {
            if (source != null) {
                source.close();
            }
            if (destination != null) {
                destination.close();
            }
        }
    }

    /**
     * Stores a file in {@link MediaItem}. After the file has been stored
     * a thumbnail is generated for the file depending on the type of file.
     * 
     * @param file
     *          Byte array containing the file to store in the {@link MediaItem}
     * @param item
     *          {@link MediaItem} to store the <code>file</code>
     * @return {@link Map} of meta data properties found in the <code>file</code>
     */
    public Map<String, String> store(File file, MediaItem item) {

        String catalogueLocation = item.getMediaRepository().getLocation();
        String fileName = generateUniqueFilename(item);
        item.setFilename(fileName);

        // Get the repository location
        File dir = new File(catalogueLocation);

        // Check if it exist
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // Determine the location and name of the file being uploaded
        File mediaFile = new File(dir, fileName);

        // Move file to the new location
        LOG.log(Level.INFO, "Moving file to {0}", mediaFile.getAbsolutePath());
        try {
            copyFile(file, mediaFile);
        } catch (IOException ex) {
            Logger.getLogger(CatalogueHelper.class.getName()).log(Level.SEVERE, null, ex);
        }

        // Generate thumbnail
        byte[] originalContent = null;
        try {
            originalContent = FileUtils.getBytes(mediaFile);

            byte[] thumb = MediaItemThumbnailGenerator.getInstance().generateThumbnail(originalContent, item);
            FileUtils.writeToFile(thumb, catalogueLocation + File.separator + item.getId() + "-thumb.jpg");
        } catch (UnknownMediaItemException ex) {
            LOG.log(Level.INFO, ex.getMessage());
            return Collections.EMPTY_MAP;
        } catch (ThumbnailGeneratorException ex) {
            LOG.log(Level.INFO, ex.getMessage());
            return Collections.EMPTY_MAP;
        } catch (IOException ex) {
            LOG.log(Level.INFO, ex.getMessage());
            return Collections.EMPTY_MAP;
        }

        // Retrieve metadata
        if (item.getFilename().trim().toLowerCase().endsWith("mp3")) {
            try {
                return retrieveMp3MetaData(item.getFileLocation());
            } catch (CannotIndexException ex) {
                LOG.log(Level.WARNING, "Could not retrieve metadata from {0}", item.getFilename());
                return Collections.EMPTY_MAP;
            }
        }

        try {
            return indexXmp(originalContent);
        } catch (CannotIndexException ex) {
            LOG.log(Level.WARNING, "Could not index media file " + item.getFilename(), ex);
            return Collections.EMPTY_MAP;
        }
    }

    /**
     * Archives a {@link NewswireItemAttachment} in a {@link dk.i2m.converge.core.content.MediaRepository}.
     * 
     * @param attachment
     *          Attachment to archive
     * @param catalogue
     *          Catalogue used for archiving the item
     * @return Absolute location of the file
     */
    public String archive(File file, MediaRepository catalogue) throws IOException {
        Calendar now = Calendar.getInstance();

        StringBuilder cataloguePath = new StringBuilder();
        cataloguePath.append(now.get(Calendar.YEAR)).append(File.separator).append(now.get(Calendar.MONTH) + 1).append(File.separator).append(now.get(Calendar.DAY_OF_MONTH));
        
        StringBuilder catalogueLocation = new StringBuilder(catalogue.getLocation());
        catalogueLocation.append(File.separator).append(cataloguePath.toString());


        // Get the repository location
        File dir = new File(catalogueLocation.toString());

        // Check if it exist
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // Determine the location and name of the file being uploaded
        File mediaFile = new File(dir, file.getName());

        // Move file to the new location
        LOG.log(Level.INFO, "Archiving {0} at {1}", new Object[]{file.getAbsolutePath(), mediaFile.getAbsolutePath()});
        copyFile(file, mediaFile);
        return cataloguePath.toString();
    }

    public Map<String, String> retrieveMp3MetaData(String location) throws CannotIndexException {
        MediaFile oMediaFile = new MP3File(new File(location));
        Map<String, String> properties = new HashMap<String, String>();
        try {
            ID3Tag[] tags = oMediaFile.getTags();
            for (ID3Tag tag : tags) {
                // check to see if we read a v1.0 tag, or a v2.3.0 tag (just for example..)
                if (tag instanceof ID3V1_0Tag) {
                    ID3V1_0Tag tagV1 = (ID3V1_0Tag) tag;
                    // does this tag happen to contain a title?
                    if (tagV1.getTitle() != null) {
                        properties.put("headline", tagV1.getTitle());
                        properties.put("title", tagV1.getTitle());
                    }

                    if (tagV1.getComment() != null) {
                        properties.put("description", tagV1.getComment());
                    }
                } else if (tag instanceof ID3V2_3_0Tag) {
                    ID3V2_3_0Tag tagV2 = (ID3V2_3_0Tag) tag;
                    if (tagV2.getTitle() != null) {
                        properties.put("title", tagV2.getTitle());
                        properties.put("headline", tagV2.getTitle());
                    }

                    if (tagV2.getTIT2TextInformationFrame() != null) {
                        properties.put("headline", tagV2.getTIT2TextInformationFrame().getTitle());
                    }

                    if (tagV2.getComment() != null) {
                        properties.put("description", tagV2.getComment());
                    }
                }
            }

        } catch (ID3Exception ex) {
            throw new CannotIndexException(ex);
        }

        return properties;
    }

    public Map<String, String> indexXmp(byte[] file) throws CannotIndexException {
        Map<String, String> properties = new HashMap<String, String>();
        String xml;
        try {
            xml = Sanselan.getXmpXml(file);

            if (xml == null) {
                try {
                    IImageMetadata meta = Sanselan.getMetadata(file);

                    if (meta != null) {
                        ArrayList items = meta.getItems();
                        for (int i = 0; i < items.size(); i++) {
                            try {
                                ImageMetadata.Item item = (ImageMetadata.Item) items.get(i);

                                if (item instanceof TiffImageMetadata.Item) {
                                    TiffImageMetadata.Item tiff = (Item) item;
                                    properties.put(tiff.getTiffField().getTagName(), "" + tiff.getTiffField().getValue());
                                } else {
                                    properties.put(item.getKeyword(), item.getText());
                                }
                            } catch (Exception ex) {
                                LOG.log(Level.INFO, ex.getMessage());
                                LOG.log(Level.FINE, "", ex);
                            }
                        }
                    }
                } catch (ImageReadException ex) {
                    LOG.log(Level.INFO, "Image reading exception", ex.getMessage());
                    LOG.log(Level.FINE, "", ex);
                } catch (IOException ex) {
                    LOG.log(Level.SEVERE, "", ex);
                }
            } else {

                XMPMeta xmpMeta = XMPMetaFactory.parseFromString(xml);

                XMPIterator iterator = xmpMeta.iterator();

                //while (iterator.hasNext()) {
                //Object obj = iterator.next();
                //logger.log(Level.INFO, "" + obj.getClass().getName() + " " + obj);
                //}

                if (xmpMeta.doesPropertyExist(NS_PHOTOSHOP, "Headline")) {
                    XMPProperty headlineProperty = xmpMeta.getProperty(NS_PHOTOSHOP, "Headline");
                    properties.put("headline", ((String) headlineProperty.getValue()).trim());
                }

                if (xmpMeta.doesArrayItemExist(NS_DC, "description", 1)) {
                    XMPProperty descriptionProperty = xmpMeta.getArrayItem(NS_DC, "description", 1);
                    properties.put("description", ((String) descriptionProperty.getValue()).trim());
                }

                if (xmpMeta.doesArrayItemExist(NS_DC, "title", 1)) {
                    XMPProperty titleProperty = xmpMeta.getArrayItem(NS_DC, "title", 1);
                    properties.put("title", ((String) titleProperty.getValue()).trim());
                }

                int subjectCount = xmpMeta.countArrayItems(NS_DC, "subject");
                if (subjectCount > 0) {

                    for (int i = 1; i <= subjectCount; i++) {
                        XMPProperty subjectProperty = xmpMeta.getArrayItem(NS_DC, "subject", i);
                        properties.put("subject-" + i, ((String) subjectProperty.getValue()).trim());
                    }
                }
            }

        } catch (XMPException ex) {
            LOG.log(Level.SEVERE, "", ex);
        } catch (ImageReadException ex) {
            LOG.log(Level.INFO, "Image reading exception from file: {0}", new Object[]{ex.getMessage()});
        } catch (IOException ex) {
            LOG.log(Level.INFO, "File reading exception from file: {0}", new Object[]{ex.getMessage()});
        }

        return properties;
    }

    public static CatalogueHelper getInstance() {
        if (instance == null) {
            instance = new CatalogueHelper();
        }
        return instance;
    }

    private CatalogueHelper() {
    }
}
