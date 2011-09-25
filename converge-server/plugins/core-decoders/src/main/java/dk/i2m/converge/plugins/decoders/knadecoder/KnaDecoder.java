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
package dk.i2m.converge.plugins.decoders.knadecoder;

import dk.i2m.converge.core.newswire.NewswireItem;
import dk.i2m.converge.core.newswire.NewswireService;
import dk.i2m.converge.core.plugin.NewswireDecoder;
import dk.i2m.converge.core.plugin.PluginContext;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.Address;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;

/**
 * Decoder for KNA newswire packages delivered via IMAP. The KNA newswire
 * packages are semi structured plain text. The following algorithm is used
 * for detecting news items:
 *
 * <ol>
 *    <li>Locate beginning of story: KNA <code>number</code></li>
 *    <li>Next non-empty line is heading</li>
 *    <li>Remaining lines until the beginning of the next story (or end of file)
 *        is the newswire story.</li>
 * </ol>
 *
 * @author Allan Lykke Christensen
 */
@dk.i2m.converge.core.annotations.NewswireDecoder
public class KnaDecoder implements NewswireDecoder {

    /** Possible value for {@link KnaDecoder#TRANSPORT}. */
    public static final String TRANSPORT_IMAP = "imap";

    /** IMAPS transport (secure). Possible value for {@link KnaDecoder#TRANSPORT}. */
    public static final String TRANSPORT_IMAPS = "imaps";

    /** How is the newswire fetched. For now only IMAP is supported. */
    public static final String TRANSPORT = "TRANSPORT";

    /** IMAP server name or IP address. */
    public static final String TRANSPORT_IMAP_SERVER = "TRANSPORT.IMAP.SERVER";

    /** IMAP server port. */
    public static final String TRANSPORT_IMAP_PORT = "TRANSPORT.IMAP.PORT";

    /** IMAP user name. */
    public static final String TRANSPORT_IMAP_USERNAME = "TRANSPORT.IMAP.USERNAME";

    /** IMAP password. */
    public static final String TRANSPORT_IMAP_PASSWORD = "TRANSPORT.IMAP.PASSWORD";

    /** IMAP folder containing the newswire mails. */
    public static final String TRANSPORT_IMAP_FOLDER_NEWSWIRE = "TRANSPORT.IMAP.FOLDER.NEWSWIRE";

    /** IMAP folder to move the newswire once processed. */
    public static final String TRANSPORT_IMAP_FOLDER_PROCESSED = "TRANSPORT.IMAP.FOLDER.PROCESSED";

    /** Should IMAP mails be deleted after being processed or moved to the processed directory. */
    public static final String TRANSPORT_IMAP_DELETE_PROCESSED = "TRANSPORT.IMAP.DELETE_PROCESSED";

    private static final Logger logger = Logger.getLogger(KnaDecoder.class.getName());

    private ResourceBundle pluginMessages = ResourceBundle.getBundle("dk.i2m.converge.plugins.decoders.knadecoder.Messages");

    private Map<String, String> availableProperties = null;

    private Calendar releaseDate = new GregorianCalendar(2010, Calendar.AUGUST, 25, 22, 00);

    @Override
    public Map<String, String> getAvailableProperties() {
        if (availableProperties == null) {
            availableProperties = new LinkedHashMap<String, String>();
            availableProperties.put(pluginMessages.getString("PROPERTY_TRANSPORT"), TRANSPORT);
            availableProperties.put(pluginMessages.getString("PROPERTY_IMAP_SERVER"), TRANSPORT_IMAP_SERVER);
            availableProperties.put(pluginMessages.getString("PROPERTY_IMAP_PORT"), TRANSPORT_IMAP_PORT);
            availableProperties.put(pluginMessages.getString("PROPERTY_IMAP_USERNAME"), TRANSPORT_IMAP_USERNAME);
            availableProperties.put(pluginMessages.getString("PROPERTY_IMAP_PASSWORD"), TRANSPORT_IMAP_PASSWORD);
            availableProperties.put(pluginMessages.getString("PROPERTY_IMAP_FOLDER_NEWSWIRE"), TRANSPORT_IMAP_FOLDER_NEWSWIRE);
            availableProperties.put(pluginMessages.getString("PROPERTY_IMAP_FOLDER_PROCESSED"), TRANSPORT_IMAP_FOLDER_PROCESSED);
            availableProperties.put(pluginMessages.getString("PROPERTY_IMAP_DELETE_PROCESSED"), TRANSPORT_IMAP_DELETE_PROCESSED);
        }
        return this.availableProperties;
    }

    @Override
    public List<NewswireItem> decode(PluginContext ctx, NewswireService newswire) {
        logger.log(Level.INFO, "Processing newswire service {0}", newswire.getSource());
        List<NewswireItem> result = new ArrayList<NewswireItem>();
        Map<String, String> properties = newswire.getPropertiesMap();

        if (properties.containsKey(TRANSPORT)) {
            String transport = properties.get(TRANSPORT);

            if (transport.equalsIgnoreCase(TRANSPORT_IMAP) || transport.equalsIgnoreCase(TRANSPORT_IMAPS)) {
                String host = properties.get(TRANSPORT_IMAP_SERVER);
                String port = properties.get(TRANSPORT_IMAP_PORT);
                String username = properties.get(TRANSPORT_IMAP_USERNAME);
                String password = properties.get(TRANSPORT_IMAP_PASSWORD);
                String folder_newswire = properties.get(TRANSPORT_IMAP_FOLDER_NEWSWIRE);
                String folder_processed = properties.get(TRANSPORT_IMAP_FOLDER_PROCESSED);
                boolean deleteAfterProcess = Boolean.parseBoolean(properties.get(TRANSPORT_IMAP_DELETE_PROCESSED));

                Properties props = new Properties();
                Session session = Session.getDefaultInstance(props, null);

                javax.mail.Store store;
                try {
                    store = session.getStore(transport);
                    store.connect(host, Integer.valueOf(port), username, password);

                    Folder folder = store.getFolder(folder_newswire);
                    folder.open(Folder.READ_WRITE);

                    for (Message msg : folder.getMessages()) {
                        Address[] recipients = msg.getFrom();

                        Calendar msgSent = Calendar.getInstance();
                        if (msg.getSentDate() != null) {
                            msgSent.setTime(msg.getSentDate());
                            logger.log(Level.FINEST, "Sent date was not set on the mail");
                        } else if (msg.getReceivedDate() != null) {
                            msgSent.setTime(msg.getReceivedDate());
                            logger.log(Level.FINEST, "Received date was not set on the mail");
                        } else {
                            logger.log(Level.FINEST, "Using current timestamp as newswire item date");
                        }

                        StringBuilder fromString = new StringBuilder();
                        if (recipients != null) {
                            for (Address from : recipients) {
                                fromString.append(from.toString());
                                fromString.append(" ");
                            }
                        }

                        logger.log(Level.FINE, "Processing mail from {0} with subject ''{1}'' and content type {2}", new Object[]{fromString.toString(), msg.getSubject(), msg.getContentType()});

                        if (msg.isMimeType("text/plain")) {
                            result.addAll(processKnaMail((String) msg.getContent(), msgSent));
                        } else if (msg.getContent() instanceof Multipart) {
                            Multipart multipart = (Multipart) msg.getContent();
                            result.addAll(processMultipart(multipart, msgSent));
                        } else {
                            logger.log(Level.FINE, "Mail does not contain expected multiparts. Skipping");
                        }

                        // Don't delete or move if nothing was found in the mail, hence the format probably wasn't recognised
                        if (!result.isEmpty()) {

                            if (!deleteAfterProcess) {
                                Folder processedFolder = store.getFolder(folder_processed);

                                if (processedFolder.exists()) {
                                    processedFolder.open(Folder.READ_WRITE);
                                    processedFolder.appendMessages(new Message[]{msg});
                                    processedFolder.close(true);
                                } else {
                                    logger.log(Level.WARNING, "{0} does not exist on in mail account {1} on {2}", new Object[]{folder_processed, username, host});
                                    logger.log(Level.FINEST, "Available mail folders:");
                                    for (Folder f : store.getDefaultFolder().list()) {
                                        logger.log(Level.FINEST, f.getFullName());

                                        for (Folder f2 : f.list()) {
                                            logger.log(Level.FINEST, "- {0}", f2.getFullName());
                                        }
                                    }
                                }
                            }

                            msg.setFlag(Flags.Flag.DELETED, true);
                        } else {
                            logger.log(Level.FINE, "No KNA newswire items found in mail. Mail left in folder");
                        }
                    }
                    folder.close(true);
                    store.close();

                } catch (MessagingException ex) {
                    logger.log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }

                for (NewswireItem item : result) {
                    item.setNewswireService(newswire);
                    item = ctx.createNewswireItem(item);
                }

            } else {
                logger.log(Level.SEVERE, "Unknown transport ''{0}''", transport);
            }
        } else {
            logger.log(Level.SEVERE, "Property '" + TRANSPORT + "' is missing");
        }
        return result;
    }

    @Override
    public String getName() {
        return pluginMessages.getString("PLUGIN_NAME");
    }

    @Override
    public String getDescription() {
        return pluginMessages.getString("PLUGIN_DESCRIPTION");
    }

    @Override
    public String getVendor() {
        return pluginMessages.getString("PLUGIN_VENDOR");
    }

    @Override
    public Date getDate() {
        return releaseDate.getTime();
    }

    /**
     * Recursive method for detecting and processing KNA content.
     *
     * @param multipart
     *          {@link Multipart} to process
     * @param msgSent
     *          Date when the mail was received.
     * @return {@link List} of detected {@link NewswireItem}s
     */
    private List<NewswireItem> processMultipart(Multipart multipart, Calendar msgSent) {
        List<NewswireItem> result = new ArrayList<NewswireItem>();

        try {
            logger.log(Level.FINE, "Processing {0} with {1} parts", new Object[]{multipart.getContentType(), multipart.getCount()});
            for (int i = 0, n = multipart.getCount(); i < n; i++) {
                Part part = multipart.getBodyPart(i);

                if (part.getContent() instanceof Multipart) {
                    logger.log(Level.FINE, "Part is multipart");
                    result.addAll(processMultipart((Multipart) part.getContent(), msgSent));
                }

                logger.log(Level.FINE, "Part is {0}", new Object[]{part.getContentType()});
                if (part.isMimeType("text/plain")) {
                    logger.log(Level.FINE, "Processing");
                    result.addAll(processKnaMail((String) part.getContent(), msgSent));
                }
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        } catch (MessagingException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        return result;
    }

    /**
     * Processes a {@link String} containing a suspected KNA mail.
     *
     * @param content
     *          Suspected KNA mail content
     * @param msgSent
     *          Date when the message was sent. This is used for generating the
     *          {link NewswireItem}s.
     * @return {@link List} of {@link NewswireItem}s detected in the KNA mail
     *         content
     */
    private List<NewswireItem> processKnaMail(String content, Calendar msgSent) {
        List<NewswireItem> items = new ArrayList<NewswireItem>();

        String str;
        BufferedReader reader = new BufferedReader(new StringReader(content));

        try {
            KnaReader state = KnaReader.STATE_UNKNOWN;
            NewswireItem newswireItem = new NewswireItem();
            newswireItem.setDate(msgSent);

            while ((str = reader.readLine()) != null) {

                switch (state) {
                    case STATE_BEGINING:
                        if (str.trim().isEmpty()) {
                            continue;
                        } else {
                            newswireItem.setTitle(str);
                            state = KnaReader.STATE_CONTENT;
                        }
                        break;
                    case STATE_CONTENT:
                        if (!str.trim().toLowerCase().startsWith("kna ")) {
                            newswireItem.addContent(str);
                            newswireItem.addContent("<br/>");
                            if (newswireItem.getSummary().isEmpty()) {
                                newswireItem.setSummary(str);
                            }
                        } else {
                            items.add(newswireItem);
                            newswireItem = new NewswireItem();
                            newswireItem.setExternalId(str);
                            newswireItem.setDate(msgSent);
                            state = KnaReader.STATE_BEGINING;
                        }
                        break;
                    default:
                        if (str.trim().toLowerCase().startsWith("kna ")) {
                            state = KnaReader.STATE_BEGINING;
                            newswireItem.setExternalId(str);
                        }
                }
            }

            if (state == KnaReader.STATE_CONTENT) {
                items.add(newswireItem);
            }

            logger.log(Level.INFO, "Found {0} items", items.size());

        } catch (IOException e) {
            logger.log(Level.SEVERE, e.getMessage());
            logger.log(Level.FINE, "", e);
        }
        return items;
    }

    enum KnaReader {

        STATE_UNKNOWN, STATE_BEGINING, STATE_CONTENT;
    }
}
