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
 * 
 * @author Raymond Wanyoike
 */
public class UserResource {

    private static final Logger LOG = Logger.getLogger("UserResource");

    private static final String USER = "user";

    private static final String LOGIN = "login";

    private static final String LOGOUT = "logout";

    private DefaultHttpClient httpClient;

    private URI uri;

    private String username;

    private String password;

    /**
     * Constructs a User resource from the given components.
     *
     * @param client Connector instance
     */
    public UserResource(DrupalClient client) {
        this.uri = client.uri();
        this.httpClient = client.getHttpClient();
    }

    /**
     * Login a user using the specified credentials. <br /><br />
     * 
     * Note this will transfer a plaintext password.
     */
    public void connect() throws IOException {
        DrupalMessage drupalMessage = new DrupalMessage();

        drupalMessage.getFields().put("username", this.getUsername());
        drupalMessage.getFields().put("password", this.getPassword());

        JSONObject json = new JSONObject();

        for (String fieldName : drupalMessage.getFields().keySet()) {
            json.put(fieldName, drupalMessage.getFields().get(fieldName));
        }

        try {
            StringEntity input = new StringEntity(json.toString());
            String url = new URLBuilder(uri).add(USER).add(LOGIN).toString();
            HttpPost post = new HttpPost(url);
            post.setEntity(input);

            ResponseHandler<String> handler = new BasicResponseHandler();
            httpClient.execute(post, handler);
        } catch (MalformedURLException ex) {
            LOG.log(Level.SEVERE, null, ex);
        } catch (ClientProtocolException ex) {
            LOG.log(Level.SEVERE, null, ex);
        } catch (UnsupportedEncodingException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Logout the current user.
     */
    public void disconnect() throws IOException {
        JSONObject json = new JSONObject();

        try {
            String url = new URLBuilder(uri).add(USER).add(LOGOUT).toString();
            HttpPost post = new HttpPost(url);
            // Create an empty string HttpEntity to fill the request body
            post.setEntity(new StringEntity(json.toString()));

            ResponseHandler<String> handler = new BasicResponseHandler();
            httpClient.execute(post, handler);
        } catch (MalformedURLException ex) {
            LOG.log(Level.SEVERE, null, ex);
        } catch (ClientProtocolException ex) {
            LOG.log(Level.SEVERE, null, ex);
        } catch (UnsupportedEncodingException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

    /**
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @param username the username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }
}
