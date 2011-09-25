/*
 *  Copyright (C) 2010 - 2011 Interactive Media Management
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
package dk.i2m.converge.plugins.decoders.rss;

import dk.i2m.converge.core.plugin.NewswireDecoder;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;
import dk.i2m.commons.StringUtils;
import dk.i2m.converge.core.DataExistsException;
import dk.i2m.converge.core.content.ContentTag;
import dk.i2m.converge.core.newswire.NewswireItem;
import dk.i2m.converge.core.newswire.NewswireService;
import dk.i2m.converge.core.plugin.PluginContext;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.json.JSONObject;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;

/**
 * Newswire decoder for RSS feeds. The decoder has a single property, the
 * {@link RssDecoder#URL} of the RSS feed.
 *
 * @author Allan Lykke Christensen
 */
@dk.i2m.converge.core.annotations.NewswireDecoder
public class RssDecoder implements NewswireDecoder {

    /** Property containing the URL of the RSS feed. */
    public static final String URL = "URL";

    /** Property containing the OpenCalais status. */
    public static final String OPENCALAIS_ENABLE = "Enable OpenCalais";

    /** Property containing the OpenCalais ID. */
    public static final String OPENCALAIS_ID = "OpenCalais ID";

    private static final Logger logger = Logger.getLogger(RssDecoder.class.getName());

    private Calendar releaseDate = new GregorianCalendar(2011, Calendar.APRIL, 12, 16, 20);

    private Map<String, String> availableProperties = null;

    private ResourceBundle msgs = ResourceBundle.getBundle("dk.i2m.converge.plugins.decoders.rss.Messages");

    private PluginContext pluginContext;

    private String propUrl = "";

    private boolean useOpenCalais = false;

    private String openCalaisId = "";

    private static final String OPENCALAIS_URL = "http://api.opencalais.com/tag/rs/enrich";

    /**
     * Creates a new instance of {@link RssDecoder}.
     */
    public RssDecoder() {
    }

    @Override
    public Map<String, String> getAvailableProperties() {
        if (availableProperties == null) {
            availableProperties = new HashMap<String, String>();
            availableProperties.put(msgs.getString("PROPERTY_URL"), URL);
            availableProperties.put(OPENCALAIS_ENABLE, OPENCALAIS_ENABLE);
            availableProperties.put(OPENCALAIS_ID, OPENCALAIS_ID);
        }
        return this.availableProperties;
    }

    @Override
    public List<NewswireItem> decode(PluginContext ctx, NewswireService newswire) {
        this.pluginContext = ctx;
        String url = newswire.getPropertiesMap().get(URL);

        if (newswire.getPropertiesMap().containsKey(OPENCALAIS_ENABLE)) {
            useOpenCalais = Boolean.parseBoolean(newswire.getPropertiesMap().get(OPENCALAIS_ENABLE));
        } else {
            useOpenCalais = false;
        }

        if (useOpenCalais) {
            if (newswire.getPropertiesMap().containsKey(OPENCALAIS_ID)) {
                openCalaisId = newswire.getPropertiesMap().get(OPENCALAIS_ID);
            }
        }

        logger.log(Level.FINE, "Downloading newswire webfeed {0} {1}", new Object[]{newswire.getSource(), url});
        int duplicates = 0;
        int newItems = 0;
        List<NewswireItem> createdItems = new ArrayList<NewswireItem>();
        try {
            URL feedSource = new URL(url);

            //final FeedFetcher feedFetcher = new HttpClientFeedFetcher();
            //SyndFeed feed = null;

            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feed = input.build(new XmlReader(feedSource));

            //        feed = feedFetcher.retrieveFeed(feedSource);

            for (SyndEntry entry : (List<SyndEntry>) feed.getEntries()) {
                try {
                    createdItems.add(create(entry, newswire));
                    newItems++;
                } catch (DataExistsException dee) {
                    duplicates++;
                }
            }
        } catch (MalformedURLException ex) {
            logger.log(Level.WARNING, "Problem with feed #{0} - {1} - {2}. {3}", new Object[]{newswire.getId(), newswire.getSource(), url, ex.getMessage()});
            logger.log(Level.FINE, "", ex);
        } catch (IllegalArgumentException ex) {
            logger.log(Level.WARNING, "Problem with feed #{0} - {1} - {2}. {3}", new Object[]{newswire.getId(), newswire.getSource(), url, ex.getMessage()});
            logger.log(Level.FINE, "", ex);
        } catch (FeedException ex) {
            logger.log(Level.WARNING, "Problem with feed #{0} - {1} - {2}. {3}", new Object[]{newswire.getId(), newswire.getSource(), url, ex.getMessage()});
            logger.log(Level.FINE, "", ex);
//        } catch (final FetcherException ex) {
//            logger.log(Level.WARNING, "Problem with feed {0} - {1} - {2}. {3}", new Object[]{newswire.getId(), newswire.getSource(), url, ex.getMessage()});
//            logger.log(Level.FINE, "", ex);
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Problem with feed {0} - {1} - {2}. {3}", new Object[]{newswire.getId(), newswire.getSource(), url, ex.getMessage()});
            logger.log(Level.FINE, "", ex);
        }

        logger.log(Level.INFO, "{2} had {0} {0, choice, 0#duplicates|1#duplicate|2#duplicates} and {1} new {1, choice, 0#items|1#item|2#items} ", new Object[]{duplicates, newItems, newswire.getSource()});

        return createdItems;
    }

    private NewswireItem create(SyndEntry entry, NewswireService source) throws DataExistsException {
        List<NewswireItem> results = pluginContext.findNewswireItemsByExternalId(entry.getUri());

        if (results.isEmpty()) {

            NewswireItem item = new NewswireItem();
            item.setExternalId(entry.getUri());
            item.setTitle(StringUtils.stripHtml(entry.getTitle()));
            item.setSummary(StringUtils.stripHtml(entry.getDescription().getValue()));
            item.setUrl(entry.getLink());
            item.setNewswireService(source);
            item.setAuthor(entry.getAuthor());
            Calendar now = Calendar.getInstance();
            item.setDate(now);
            item.setUpdated(now);

            if (entry.getPublishedDate() != null) {
                item.getDate().setTime(entry.getPublishedDate());
                item.getUpdated().setTime(entry.getPublishedDate());
            }

            if (entry.getUpdatedDate() != null) {
                item.getUpdated().setTime(entry.getUpdatedDate());
            }

            if (useOpenCalais) {
                enrich(pluginContext, item);
            }


            return pluginContext.createNewswireItem(item);
        } else {
            throw new DataExistsException("NewswireItem with external id [" + entry.getUri() + "] already downloaded");
        }
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
        return releaseDate.getTime();
    }

    private void enrich(PluginContext ctx, NewswireItem item) {
        PostMethod method = new PostMethod(OPENCALAIS_URL);
        method.setRequestHeader("x-calais-licenseID", openCalaisId);
        method.setRequestHeader("Content-Type", "text/raw; charset=UTF-8");
        method.setRequestHeader("Accept", "application/json");
        method.setRequestHeader("enableMetadataType", "SocialTags");
        method.setRequestEntity(new StringRequestEntity(item.getTitle() + " " + item.getSummary() + " " + item.getContent()));
        try {
            HttpClient client = new HttpClient();
            int returnCode = client.executeMethod(method);
            if (returnCode == HttpStatus.SC_NOT_IMPLEMENTED) {
                System.err.println("The Post method is not implemented by this URI");
                // still consume the response body
                method.getResponseBodyAsString();
            } else if (returnCode == HttpStatus.SC_OK) {

                JSONObject response = JSONObject.fromObject(method.getResponseBodyAsString());

                for (Object key : response.keySet()) {
                    String sKey = (String) key;

                    if (sKey.startsWith("http://d.opencalais.com/")) {
                        JSONObject entity = response.getJSONObject(sKey);

                        if (entity.containsKey("name")) {
                            String name = ((String) entity.get("name")).replaceAll("_", " ");
                            ContentTag tag = ctx.findOrCreateContentTag(name);
                            if (!item.getTags().contains(tag)) {
                                item.getTags().add(tag);
                            }
                        }
                    }
                }
            } else {
                logger.log(Level.WARNING, "Invalid response received from OpenCalais [{0}] {1}", new Object[]{returnCode, method.getResponseBodyAsString()});
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "", e);
        } finally {
            method.releaseConnection();
        }
    }
}
