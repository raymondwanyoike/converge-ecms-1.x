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
import dk.i2m.converge.plugins.actions.drupal.client.messages.FileCreateMessage;
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

/**
 * API for handling file uploads and server file management.
 */
public class FileResource {

    private static final Logger LOG = Logger.getLogger(FileResource.class.
            getName());

    private static final String File = "file";

    private DrupalConnector drupalConnector;

    private UserResource userResource;

    /**
     * Create an empty File resource.
     */
    public FileResource() {
    }

    /**
     * Constructs a File resource from the given components.
     *
     * @param drupalConnector Connector instance
     */
    public FileResource(DrupalConnector drupalConnector) {
        this.drupalConnector = drupalConnector;
    }

    /**
     * Constructs a File resource from the given components.
     *
     * @param drupalConnector Connector instance
     * @param userResource User to perform actions as
     */
    public FileResource(DrupalConnector drupalConnector,
            UserResource userResource) {
        this.drupalConnector = drupalConnector;
        this.userResource = userResource;
    }

    /**
     * Adds a new file.
     *
     * @param message An object representing the file with a base64 encoded value
     * @return Populated {@link FileModule} instance
     */
    public FileCreateMessage createFile(DrupalMessage message) {
        try {
            JSONObject json = new JSONObject();

            for (String fieldName : message.getFields().keySet()) {
                json.put(fieldName, message.getFields().get(fieldName));
            }

            StringEntity input = new StringEntity(json.toString());
            String url = new URLBuilder(drupalConnector.getUri()).add(File).
                    toString();
            HttpPost post = new HttpPost(url);
            post.setEntity(input);

            LOG.log(Level.INFO, "Sending create file request to Drupal: {0}",
                    json.toString());

            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            String responce = drupalConnector.getHttpClient().execute(post,
                    responseHandler);

            LOG.log(Level.INFO, "Recieved create file responce from Drupal: {0}",
                    responce);

            post.abort();
            return (FileCreateMessage) JSONObject.toBean(
                    JSONObject.fromObject(responce), FileCreateMessage.class);
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

    /**
     * Returns the Drupal connector in use by this class.
     *
     * @return The connector in use
     */
    public DrupalConnector getDrupalConnector() {
        return drupalConnector;
    }

    /**
     * Set the Drupal connector to be used by this class.
     *
     * @param drupalConnector The connector to use
     */
    public void setDrupalConnector(DrupalConnector drupalConnector) {
        this.drupalConnector = drupalConnector;
    }

    /**
     * Returns the User resource in use by this class.
     *
     * @return The User resource in use
     */
    public UserResource getUserResource() {
        return userResource;
    }

    /**
     * Set the User resource to be used by this class.
     *
     * @param userResource The User resource to use
     */
    public void setUserResource(UserResource userResource) {
        this.userResource = userResource;
    }
}
