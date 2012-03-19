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
import dk.i2m.converge.plugins.actions.drupal.client.modules.NodeModule;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.json.JSONObject;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.jactiveresource.URLBuilder;

public class NodeResource {

    private static final Logger LOG = Logger.getLogger(NodeResource.class.
            getName());

    private static final String NODE = "node";

    private DrupalConnector drupalConnector;

    private UserResource userResource;

    public NodeResource() {
    }

    public NodeResource(DrupalConnector drupalConnector) {
        this.drupalConnector = drupalConnector;
    }

    public NodeResource(DrupalConnector drupalConnector,
            UserResource userResource) {
        this.drupalConnector = drupalConnector;
        this.userResource = userResource;
    }

    public NodeModule createNode(DrupalMessage message) {
        try {
            JSONObject json = new JSONObject();

            for (String fieldName : message.getFields().keySet()) {
                json.put(fieldName, message.getFields().get(fieldName));
            }

            StringEntity input = new StringEntity(json.toString());
            String url = new URLBuilder(drupalConnector.getUri()).add(NODE).
                    toString();
            HttpPost post = new HttpPost(url);
            post.setEntity(input);

            LOG.log(Level.INFO, "Sending create node request to Drupal: {0}",
                    json.toString());

            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            String responce = drupalConnector.getHttpClient().execute(post,
                    responseHandler);

            LOG.log(Level.INFO, "Recieved create node responce from Drupal: {0}",
                    responce);

            post.abort();
        } catch (HttpResponseException ex) {
            // TODO: Handle exception
            Logger.getLogger(NodeResource.class.getName()).log(Level.SEVERE,
                    null, ex);
        } catch (ClientProtocolException ex) {
            Logger.getLogger(NodeResource.class.getName()).log(Level.SEVERE,
                    null, ex);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(NodeResource.class.getName()).log(Level.SEVERE,
                    null, ex);
        } catch (MalformedURLException ex) {
            Logger.getLogger(NodeResource.class.getName()).log(Level.SEVERE,
                    null, ex);
        } catch (IOException ex) {
            Logger.getLogger(NodeResource.class.getName()).log(Level.SEVERE,
                    null, ex);
        }

        return null;
    }

    public DrupalConnector getDrupalConnector() {
        return drupalConnector;
    }

    public void setDrupalConnector(DrupalConnector drupalConnector) {
        this.drupalConnector = drupalConnector;
    }

    public UserResource getUserResource() {
        return userResource;
    }

    public void setUserResource(UserResource userResource) {
        this.userResource = userResource;
    }
}
