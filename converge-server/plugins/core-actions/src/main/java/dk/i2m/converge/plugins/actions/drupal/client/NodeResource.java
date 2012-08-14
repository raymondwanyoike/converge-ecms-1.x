/*
 * Copyright (C) 2012 Interactive Media Management
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
package dk.i2m.converge.plugins.actions.drupal.client;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.json.JSONObject;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

/**
 * The core that allows nodes to be submitted to the site.
 *
 * @author Raymond Wanyoike
 */
public class NodeResource {

    private static final Logger LOG = Logger.getLogger("NodeResource");

    private static final String NODE = "node";

    private DefaultHttpClient httpClient;

    private URI uri;

    /**
     * Constructs a Node resource from the given components.
     *
     * @param client Client instance
     */
    public NodeResource(DrupalClient client) {
        this.uri = client.uri();
        this.httpClient = client.getHttpClient();
    }

    /**
     * Creates a new node based on submitted values.
     *
     * @param message  Instance representing the attributes a node edit form would submit
     * @return A populated {@link NodeCreateMessage} instance
     * @throws IOException 
     */
    public NodeCreateMessage create(DrupalMessage message) throws IOException {
        JSONObject json = new JSONObject();

        for (String fieldName : message.getFields().keySet()) {
            json.put(fieldName, message.getFields().get(fieldName));
        }

        try {
            StringEntity input = new StringEntity(json.toString());
            String url = new URLBuilder(uri).add(NODE).toString();
            
            HttpPost post = new HttpPost(url);
            post.setEntity(input);
            post.addHeader(DrupalClient.JSON_HEADER);

            ResponseHandler<String> handler = new BasicResponseHandler();
            String response = httpClient.execute(post, handler);

            JSONObject object = JSONObject.fromObject(response);
            Object toBean = JSONObject.toBean(object, NodeCreateMessage.class);

            return (NodeCreateMessage) toBean;
        } catch (MalformedURLException ex) {
            LOG.log(Level.SEVERE, null, ex);
        } catch (ClientProtocolException ex) {
            LOG.log(Level.SEVERE, null, ex);
        } catch (UnsupportedEncodingException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }

        return null;
    }
}
