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
package dk.i2m.converge.plugins.actions.docpreview;

import dk.i2m.converge.core.annotations.CatalogueAction;
import dk.i2m.converge.core.content.catalogue.CatalogueHookInstance;
import dk.i2m.converge.core.content.catalogue.MediaItem;
import dk.i2m.converge.core.content.catalogue.MediaItemRendition;
import dk.i2m.converge.core.content.catalogue.Rendition;
import dk.i2m.converge.core.plugin.CatalogueEvent;
import dk.i2m.converge.core.plugin.CatalogueEventException;
import dk.i2m.converge.core.plugin.CatalogueHook;
import dk.i2m.converge.core.plugin.PluginContext;
import dk.i2m.converge.core.utils.FileUtils;
import dk.i2m.converge.core.utils.ImageUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.pdfbox.ExtractText;
import org.apache.pdfbox.PDFBox;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.encoding.PdfDocEncoding;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;

/**
 * {@link CatalogueHook} generating a preview image of documents and saves
 * the preview image as a {@link Rendition} of the {@link MediaItem}.
 *
 * @author Allan Lykke Christensen
 */
@CatalogueAction
public class DocPreviewHook implements CatalogueHook {

    public static final String ORIGINAL_RENDITION = "rendition_original";

    public static final String GENERATED_RENDITION = "rendition_generated";

    public static final String WIDTH = "width";

    public static final String HEIGHT = "height";

    public static final String QUALITY = "quality";

    private Map<String, String> instanceProperties = new HashMap<String, String>();

    private Map<String, String> availableProperties = null;

    private String originalRendition;

    private String generatedRendition;

    private Integer width;

    private Integer height;

    private Integer quality;

    private ResourceBundle msgs = ResourceBundle.getBundle("dk.i2m.converge.plugins.actions.docpreview.Messages");

    @Override
    public void execute(PluginContext ctx, CatalogueEvent event, CatalogueHookInstance instance) throws CatalogueEventException {

        // Check that we only re-act to the Upload of new renditions
        if (event.getType() != CatalogueEvent.Event.UploadRendition) {
            return;
        }

        instanceProperties = instance.getPropertiesAsMap();
        validateProperties();

        // Determine if we need to act on the uploaded rendition
        MediaItemRendition uploadRendition = event.getRendition();
        Rendition rendition = uploadRendition.getRendition();

        if (!rendition.getName().equalsIgnoreCase(originalRendition)) {
            return;
        }

        if (!uploadRendition.isDocument()) {
            return;
        }

        // Which rendition should be generated
        Rendition generateRendition = ctx.findRenditionByName(generatedRendition);

        if (generateRendition == null) {
            throw new CatalogueEventException("Rendition " + generatedRendition + " does not exist");
        }

        // Load the original item
        URL originalFile;
        try {
            originalFile = new URL(uploadRendition.getAbsoluteFilename());
            PDDocument doc = null;
            try {
                // Read PDF
                PDFParser parser = new PDFParser(originalFile.openStream());
                parser.parse();
                COSDocument cosDoc = parser.getDocument();
                PDDocument pdDoc = new PDDocument(cosDoc);
                
                PDFTextStripper stripper = new PDFTextStripper();
                String extracted = stripper.getText(pdDoc);
                
            } catch (IOException ex) {
                Logger.getLogger(DocPreviewHook.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                if (doc != null) {
                    try {
                        doc.close();
                    } catch (IOException ex) {
                        Logger.getLogger(DocPreviewHook.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }


        } catch (MalformedURLException ex) {
            throw new CatalogueEventException(ex);
        }
    }

    /**
     * Validates that the necessary properties have been provided. If not, a 
     * {@link CatalogueEventException} will be thrown. Validated properties
     * are initialised in the necessary instance properties.
     * 
     * @throws CatalogueEventException 
     *          If a required property is missing or specified incorrectly
     */
    private void validateProperties() throws CatalogueEventException {
        if (!instanceProperties.containsKey(ORIGINAL_RENDITION)) {
            throw new CatalogueEventException("Property " + ORIGINAL_RENDITION + " missing");
        } else {
            this.originalRendition = instanceProperties.get(ORIGINAL_RENDITION);
        }

        if (!instanceProperties.containsKey(GENERATED_RENDITION)) {
            throw new CatalogueEventException("Property " + GENERATED_RENDITION + " missing");
        } else {
            this.generatedRendition = instanceProperties.get(GENERATED_RENDITION);
        }

        if (instanceProperties.containsKey(WIDTH)) {
            String resizeWidth = instanceProperties.get(WIDTH);
            try {
                this.width = Integer.valueOf(resizeWidth.trim());
            } catch (NumberFormatException ex) {
                throw new CatalogueEventException("Property " + WIDTH + " is not a number");
            }
        } else {
            throw new CatalogueEventException("Property " + WIDTH + " missing");
        }

        if (instanceProperties.containsKey(HEIGHT)) {
            String resizeHeight = instanceProperties.get(HEIGHT);
            try {
                this.height = Integer.valueOf(resizeHeight.trim());
            } catch (NumberFormatException ex) {
                throw new CatalogueEventException("Property " + HEIGHT + " is not a number");
            }
        } else {
            throw new CatalogueEventException("Property " + HEIGHT + " missing");
        }

        if (instanceProperties.containsKey(QUALITY)) {
            String resizeQuality = instanceProperties.get(QUALITY);
            try {
                this.quality = Integer.valueOf(resizeQuality.trim());
            } catch (NumberFormatException ex) {
                throw new CatalogueEventException("Property " + QUALITY + " is not a number");
            }
        } else {
            throw new CatalogueEventException("Property " + QUALITY + " missing");
        }
    }

    @Override
    public Map<String, String> getAvailableProperties() {
        if (availableProperties == null) {
            availableProperties = new LinkedHashMap<String, String>();
            availableProperties.put(msgs.getString(ORIGINAL_RENDITION), ORIGINAL_RENDITION);
            availableProperties.put(msgs.getString(GENERATED_RENDITION), GENERATED_RENDITION);
            availableProperties.put(msgs.getString(WIDTH), WIDTH);
            availableProperties.put(msgs.getString(HEIGHT), HEIGHT);
            availableProperties.put(msgs.getString(QUALITY), QUALITY);
        }
        return availableProperties;
    }

    @Override
    public String getName() {
        return msgs.getString("PLUGIN_NAME");
    }

    @Override
    public String getDescription() {
        return msgs.getString("PLUGIN_DESCRIPTION");
    }

    @Override
    public String getVendor() {
        return msgs.getString("PLUGIN_VENDOR");
    }

    @Override
    public Date getDate() {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return format.parse(msgs.getString("PLUGIN_BUILD_TIME"));
        } catch (Exception ex) {
            return Calendar.getInstance().getTime();
        }
    }
}
