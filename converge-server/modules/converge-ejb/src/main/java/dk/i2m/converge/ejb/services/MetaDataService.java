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
package dk.i2m.converge.ejb.services;

import com.adobe.xmp.XMPException;
import com.adobe.xmp.XMPMeta;
import com.adobe.xmp.XMPMetaFactory;
import com.adobe.xmp.properties.XMPProperty;
import com.xuggle.xuggler.Global;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IProperty;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import org.apache.sanselan.ImageInfo;
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
 * Service bean used for extracting meta data from files.
 *
 * @author Allan Lykke Christensen
 */
@Stateless
public class MetaDataService implements MetaDataServiceLocal {

    private static final Logger LOG = Logger.getLogger(MetaDataService.class.getName());

    /** Namespace of the dublin core. */
    private String NS_DC = "http://purl.org/dc/elements/1.1/";

    /** Namespace of photoshop tags. */
    private String NS_PHOTOSHOP = "http://ns.adobe.com/photoshop/1.0/";

    /**
     * Extract meta data from any file.
     * 
     * @param location
     *          Location of the file
     * @return {@link Map} of meta data
     */
    @Override
    public Map<String, String> extract(String location) {
        Map<String, String> metaData = new HashMap<String, String>();

        try {
            metaData.putAll(extractFromMp3(location));
        } catch (CannotExtractMetaDataException ex) {
            LOG.log(Level.FINE, ex.getMessage());
        }

        try {
            metaData.putAll(extractXmp(location));
        } catch (CannotExtractMetaDataException ex) {
            LOG.log(Level.FINE, ex.getMessage());
        }

        try {
            metaData.putAll(extractIPTC(location));
        } catch (CannotExtractMetaDataException ex) {
            LOG.log(Level.FINE, ex.getMessage());
        }

        try {
            metaData.putAll(extractImageInfo(location));
        } catch (CannotExtractMetaDataException ex) {
            LOG.log(Level.FINE, ex.getMessage());
        }

        try {
            metaData.putAll(extractMediaContainer(location));
        } catch (CannotExtractMetaDataException ex) {
            LOG.log(Level.FINE, ex.getMessage());
        }

        return metaData;
    }

    /**
     * Extract MP3 meta data from audio file.
     * 
     * @param location
     *          Location of the file
     * @return {@link Map} of MP3 meta data
     * @throws CannotExtractMetaDataException
     *          If meta data could not be extracted from the given file
     */
    @Override
    public Map<String, String> extractFromMp3(String location) throws CannotExtractMetaDataException {
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
            throw new CannotExtractMetaDataException(ex);
        }

        return properties;
    }

    /**
     * Extract XMP meta data from a media file.
     * 
     * @param location
     *          Location of the file
     * @return {@link Map} of XMP meta data
     * @throws CannotExtractMetaDataException
     *          If meta data could not be extracted from
     *          the given file
     */
    @Override
    public Map<String, String> extractXmp(String location) throws CannotExtractMetaDataException {
        Map<String, String> properties = new HashMap<String, String>();
        try {
            String xml = Sanselan.getXmpXml(new File(location));

            if (xml == null) {
                return properties;
            }

            XMPMeta xmpMeta = XMPMetaFactory.parseFromString(xml);

            //XMPIterator iterator = xmpMeta.iterator();
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

        } catch (XMPException ex) {
            throw new CannotExtractMetaDataException(ex);
        } catch (ImageReadException ex) {
            throw new CannotExtractMetaDataException(ex);
        } catch (IOException ex) {
            throw new CannotExtractMetaDataException(ex);
        }

        return properties;
    }

    /**
     * Extract IPTC meta data from an image file.
     * 
     * @param location
     *          Location of the file
     * @return {@link Map} of IPTC meta data
     * @throws CannotExtractMetaDataException
     *          If meta data could not be extracted from
     *          the given file
     */
    @Override
    public Map<String, String> extractIPTC(String location) throws CannotExtractMetaDataException {
        Map<String, String> properties = new HashMap<String, String>();

        try {
            IImageMetadata meta = Sanselan.getMetadata(new File(location));

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
            throw new CannotExtractMetaDataException(ex);
        } catch (IOException ex) {
            throw new CannotExtractMetaDataException(ex);
        }

        return properties;
    }

    /**
     * Extract IPTC meta data from an image file.
     * 
     * @param location
     *          Location of the file
     * @return {@link Map} of IPTC meta data
     * @throws CannotExtractMetaDataException
     *          If meta data could not be extracted from
     *          the given file
     */
    @Override
    public Map<String, String> extractImageInfo(String location) throws CannotExtractMetaDataException {
        Map<String, String> properties = new HashMap<String, String>();

        try {
            ImageInfo imageInfo = Sanselan.getImageInfo(new File(location));

            if (imageInfo != null) {
                properties.put("colourSpace", imageInfo.getColorTypeDescription());
                properties.put("height", String.valueOf(imageInfo.getHeight()));
                properties.put("width", String.valueOf(imageInfo.getWidth()));
                properties.put("progressive", String.valueOf(imageInfo.getIsProgressive()));
            }
        } catch (ImageReadException ex) {
            throw new CannotExtractMetaDataException(ex);
        } catch (IOException ex) {
            throw new CannotExtractMetaDataException(ex);
        }

        return properties;
    }

    public Map<String, String> extractMediaContainer(String location) throws CannotExtractMetaDataException {
        Map<String, String> properties = new HashMap<String, String>();


        IContainer container = IContainer.make();

        // Open up the container
        if (container.open(location, IContainer.Type.READ, null) < 0) {
            throw new CannotExtractMetaDataException("could not open file: " + location);
        }

        if (container.queryStreamMetaData() < 0) {
            throw new CannotExtractMetaDataException("couldn't query stream meta data for some reason...");
        }

        for (int i = 0; i < container.getNumProperties(); i++) {
            IProperty prop = container.getPropertyMetaData(i);
            properties.put(prop.getName(), container.getPropertyAsString(prop.getName()));
        }

        properties.put("streams", String.valueOf(container.getNumStreams()));
        if (container.getDuration() == Global.NO_PTS) {
            properties.put("duration", String.valueOf(container.getDuration()));
        } else {
            properties.put("duration", String.valueOf(container.getDuration() / 1000));
        }

        if (container.getStartTime() == Global.NO_PTS) {
            properties.put("startTime", String.valueOf(container.getStartTime()));
        } else {
            properties.put("startTime", String.valueOf(container.getStartTime() / 1000));
        }
        properties.put("bitrate", String.valueOf(container.getBitRate()));


        for (String meta : container.getMetaData().getKeys()) {
            properties.put("container." + meta, container.getMetaData().getValue(meta));
        }

        for (int i = 0; i < container.getNumStreams(); i++) {
            IStream stream = container.getStream(i);
            // Get the pre-configured decoder that can decode this stream;
            IStreamCoder coder = stream.getStreamCoder();

            for (String meta : stream.getMetaData().getKeys()) {
                properties.put("stream." + i + ".meta." + meta, stream.getMetaData().getValue(meta));
            }
            properties.put("stream." + i + ".type", coder.getCodecType().name());
            properties.put("stream." + i + ".codec", coder.getCodecID().name());
            properties.put("stream." + i + ".duration", String.valueOf(stream.getDuration()));

            if (coder.getCodecType() == ICodec.Type.CODEC_TYPE_AUDIO) {
                properties.put("stream." + i + ".sampleRate", String.valueOf(coder.getSampleRate()));
                properties.put("stream." + i + ".channels", String.valueOf(coder.getChannels()));
                properties.put("stream." + i + ".format", coder.getSampleFormat().name());
            } else if (coder.getCodecType() == ICodec.Type.CODEC_TYPE_VIDEO) {
                properties.put("stream." + i + ".width", String.valueOf(coder.getWidth()));
                properties.put("stream." + i + ".height", String.valueOf(coder.getHeight()));
                properties.put("stream." + i + ".format", coder.getPixelType().name());
                properties.put("stream." + i + ".frameRate", String.valueOf(coder.getFrameRate().getDouble()));
            }
        }

        return properties;
    }
}
