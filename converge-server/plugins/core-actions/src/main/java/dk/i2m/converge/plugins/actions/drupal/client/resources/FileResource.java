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
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.json.JSONObject;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.jactiveresource.URLBuilder;

/**
 * API for handling file uploads and server file management.
 *
 * @author Raymond Wanyoike
 */
public class FileResource {

    private static final Logger LOG = Logger.getLogger(FileResource.class.
            getName());

    private static final String FILE = "file";

    private DefaultHttpClient httpClient;

    private URI uri;

    /**
     * Create an empty File resource.
     */
    public FileResource() {
    }

    /**
     * Constructs a File resource from the given components.
     *
     * @param connector Connector instance
     */
    public FileResource(DrupalConnector connector) {
        this.uri = connector.getUri();
        this.httpClient = connector.getHttpClient();
    }

    /**
     * Adds a new file.
     *
     * @param message An object representing the file with a base64 encoded value
     * @return Populated {@link FileModule} instance
     */
    public FileCreateMessage createFile(DrupalMessage message) throws HttpResponseException {
        FileCreateMessage fileCreateMessage = new FileCreateMessage();

        JSONObject json = new JSONObject();

        for (String fieldName : message.getFields().keySet()) {
            json.put(fieldName, message.getFields().get(fieldName));
        }

        try {
            StringEntity input = new StringEntity(json.toString());
            String url = new URLBuilder(uri).add(FILE).toString();
            HttpPost post = new HttpPost(url);
            post.setEntity(input);

            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            HttpResponse responce = httpClient.execute(post);

            String handledResponse = responseHandler.handleResponse(responce);

            fileCreateMessage = (FileCreateMessage) JSONObject.toBean(
                    JSONObject.fromObject(responce), FileCreateMessage.class);

            EntityUtils.consume(responce.getEntity());
        } catch (HttpResponseException ex) {
            throw ex;
        } catch (ClientProtocolException ex) {
            Logger.getLogger(NodeResource.class.getName()).
                    log(Level.SEVERE, null, ex);
        } catch (MalformedURLException ex) {
            Logger.getLogger(NodeResource.class.getName()).
                    log(Level.SEVERE, null, ex);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(NodeResource.class.getName()).
                    log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(NodeResource.class.getName()).
                    log(Level.SEVERE, null, ex);
        }

        return fileCreateMessage;
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
