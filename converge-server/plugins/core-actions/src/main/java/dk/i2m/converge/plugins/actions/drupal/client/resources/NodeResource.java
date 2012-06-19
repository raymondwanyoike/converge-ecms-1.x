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
package dk.i2m.converge.plugins.actions.drupal.client.resources;

import dk.i2m.converge.plugins.actions.drupal.client.DrupalConnector;
import dk.i2m.converge.plugins.actions.drupal.client.messages.DrupalMessage;
import dk.i2m.converge.plugins.actions.drupal.client.messages.NodeCreateMessage;
import java.net.URI;
import net.sf.json.JSONObject;
import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.jactiveresource.URLBuilder;

/**
 * The core that allows node content to be submitted to the site.
 *
 * @author Raymond Wanyoike
 */
public class NodeResource {

    private static final String NODE = "node";

    private DefaultHttpClient httpClient;

    private URI uri;

    private HttpResponse responce;

    /**
     * Create an empty Node resource.
     */
    public NodeResource() {
    }

    /**
     * Constructs a Node resource from the given components.
     *
     * @param connector Connector instance
     */
    public NodeResource(DrupalConnector connector) {
        this.uri = connector.getUri();
        this.httpClient = connector.getHttpClient();
    }

    /**
     * Creates a new node based on submitted values.
     *
     * @param message Instance representing the attributes a node edit form would submit
     * @return A populated {@link NodeCreateMessage} instance
     */
    public NodeCreateMessage create(DrupalMessage message) throws Exception {
        NodeCreateMessage nodeCreateMessage = new NodeCreateMessage();

        JSONObject json = new JSONObject();

        for (String fieldName : message.getFields().keySet()) {
            json.put(fieldName, message.getFields().get(fieldName));
        }

        try {
            StringEntity input = new StringEntity(json.toString());
            String url = new URLBuilder(uri).add(NODE).toString();
            HttpPost post = new HttpPost(url);
            post.setEntity(input);

            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            responce = httpClient.execute(post);

            String handledResponse = responseHandler.handleResponse(responce);

            nodeCreateMessage =
                    (NodeCreateMessage) JSONObject.toBean(JSONObject.fromObject(
                    handledResponse), NodeCreateMessage.class);

        } finally {
            EntityUtils.consume(responce.getEntity());
        }

        return nodeCreateMessage;
    }

    /**
     * Set the connector to be used by this class.
     *
     * @param connector Connector to use
     */
    public void setDrupalConnector(DrupalConnector connector) {
        this.uri = connector.getUri();
        this.httpClient = connector.getHttpClient();
    }
}
