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
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.json.JSONObject;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.jactiveresource.URLBuilder;

public class UserResource {

    private static final Logger LOG = Logger.getLogger(UserResource.class.
            getName());

    private static final String USER = "user";

    private static final String LOGIN = "login";

    private static final String LOGOUT = "logout";

    private DrupalConnector drupalConnector;

    private String username;

    private String password;

    /**
     * Create an empty User resource.
     */
    public UserResource() {
    }

    /**
     * Constructs a User resource from the given components.
     *
     * @param drupalConnector Connector instance
     */
    public UserResource(DrupalConnector drupalConnector) {
        this.drupalConnector = drupalConnector;
    }

    /**
     * Constructs a User resource from the given components.
     *
     * @param drupalConnector Connector instance
     * @param username Username to be logged in
     * @param password Password, must be plain text and not hashed
     */
    public UserResource(DrupalConnector drupalConnector, String username,
            String password) {
        this.drupalConnector = drupalConnector;
        this.username = username;
        this.password = password;
    }

    /**
     * Constructs a User resource from the given components.
     *
     * @param drupalConnector Connector instance
     * @param username Username to be logged in
     * @param password Password, must be plain text and not hashed
     * @param connect Should {@code connect()} be called?
     */
    public UserResource(DrupalConnector drupalConnector, String username,
            String password, Boolean connect) {
        this.drupalConnector = drupalConnector;
        this.username = username;
        this.password = password;

        if (connect) {
            // TODO: Handle "Problematic call in the constructor" warning
            this.connect();
        }
    }

    /**
     * Login a user using the specified credentials.<br /><br />Note this will transfer a plaintext password.
     */
    public void connect() {
        try {
            JSONObject json = new JSONObject();
            DrupalMessage drupalMessage = new DrupalMessage();

            drupalMessage.getFields().put("username", this.username);
            drupalMessage.getFields().put("password", this.password);

            for (String fieldName : drupalMessage.getFields().keySet()) {
                json.put(fieldName, drupalMessage.getFields().get(fieldName));
            }

            StringEntity input = new StringEntity(json.toString());
            String url = new URLBuilder(drupalConnector.getUri()).add(USER).add(
                    LOGIN).toString();
            HttpPost post = new HttpPost(url);
            post.setEntity(input);

            LOG.log(Level.INFO, "Sending login user request to Drupal: {0}",
                    json.toString());

            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            String responce =
                    drupalConnector.getHttpClient().execute(post,
                    responseHandler);

            LOG.log(Level.INFO, "Recieved login user responce from Drupal: {0}",
                    responce);

            post.abort();
        } catch (ClientProtocolException ex) {
            Logger.getLogger(UserResource.class.getName()).log(Level.SEVERE,
                    null, ex);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(UserResource.class.getName()).log(Level.SEVERE,
                    null, ex);
        } catch (IOException ex) {
            Logger.getLogger(UserResource.class.getName()).log(Level.SEVERE,
                    null, ex);
        }
    }

    /**
     * Logout the current user.
     */
    public void disconnect() {
        try {
            JSONObject json = new JSONObject();
            String url = new URLBuilder(drupalConnector.getUri()).add(USER).add(
                    LOGOUT).toString();
            HttpPost post = new HttpPost(url);

            // Create an empty string HttpEntity to fill the request body
            post.setEntity(new StringEntity(json.toString()));

            LOG.log(Level.INFO, "Sending logout user request to Drupal: {0}",
                    json.toString());

            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            String responce =
                    drupalConnector.getHttpClient().execute(post,
                    responseHandler);

            LOG.log(Level.INFO, "Recieved logout user responce from Drupal: {0}",
                    responce);

            post.abort();

        } catch (ClientProtocolException ex) {
            Logger.getLogger(UserResource.class.getName()).log(Level.SEVERE,
                    null, ex);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(UserResource.class.getName()).log(Level.SEVERE,
                    null, ex);
        } catch (IOException ex) {
            Logger.getLogger(UserResource.class.getName()).log(Level.SEVERE,
                    null, ex);
        }
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
     * Returns the password to login with.
     *
     * @return The plain text password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Set the password to login with.
     *
     * @param password Password, must be plain text and not hashed
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Returns the username set to login as.
     *
     * @return The username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Set the username to login as.
     *
     * @param username Username to be logged in
     */
    public void setUsername(String username) {
        this.username = username;
    }
}
