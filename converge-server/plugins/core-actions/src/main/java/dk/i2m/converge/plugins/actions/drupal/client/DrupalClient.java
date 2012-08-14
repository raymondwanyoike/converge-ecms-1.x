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

import dk.i2m.converge.core.utils.URLBuilder;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.Header;
import org.apache.http.client.params.AllClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.BasicClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.protocol.HTTP;

/**
 * 
 * @author Raymond Wanyoike
 */
public class DrupalClient {
    
    public static final Header JSON_HEADER = new BasicHeader("Content-Type", "application/json");

    private static final Logger LOG = Logger.getLogger("DrupalClient");

    private BasicHttpParams params = new BasicHttpParams();

    private DefaultHttpClient httpClient;

    private String hostname;

    private String endPoint;

    private int socketTimeout;

    private int connectionTimeout;

    /**
     * Constructs a DrupalClient from the given components.
     *
     * @param hostname URL to the host
     * @param endPoint Endpoint defined in the Services module
     * @param connectionTimeout Duration to wait before timing out when connecting, 0 is infinite
     * @param socketTimeout Duration to wait before timing out for a response, 0 is infinite
     */
    public DrupalClient(String hostname, String endPoint) {
        this.endPoint = endPoint;
        this.hostname = hostname;
    }

    /**
     * Setup the Http client.
     */
    public void setup() {
        params.setParameter(AllClientPNames.CONNECTION_TIMEOUT, connectionTimeout);
        params.setParameter(AllClientPNames.SO_TIMEOUT, socketTimeout);
        params.setParameter(AllClientPNames.HTTP_CONTENT_CHARSET, HTTP.UTF_8);
        params.setParameter(AllClientPNames.COOKIE_POLICY, CookiePolicy.BEST_MATCH);

        httpClient = new DefaultHttpClient(new BasicClientConnectionManager(), params);
    }

    /**
     * Return the full path to the host endpoint.
     *
     * @return A URI equivalent of the host endpoint URL
     */
    public URI uri() {
        try {
            URLBuilder ub = new URLBuilder(new URI(hostname)).add(endPoint);
            return URI.create(ub.toString());
        } catch (MalformedURLException ex) {
            LOG.log(Level.SEVERE, null, ex);
        } catch (URISyntaxException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }

        return null;
    }

    /**
     * Shutdown the Http client and release all resources.
     */
    public void close() {
        httpClient.getConnectionManager().shutdown();
    }

    /**
     * @return the params
     */
    public BasicHttpParams getParams() {
        return params;
    }

    /**
     * @param params the params to set
     */
    public void setParams(BasicHttpParams params) {
        this.params = params;
    }

    /**
     * @return the httpClient
     */
    public DefaultHttpClient getHttpClient() {
        return httpClient;
    }

    /**
     * @param httpClient the httpClient to set
     */
    public void setHttpClient(DefaultHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * @return the hostname
     */
    public String getHostname() {
        return hostname;
    }

    /**
     * @param hostname the hostname to set
     */
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    /**
     * @return the endPoint
     */
    public String getEndPoint() {
        return endPoint;
    }

    /**
     * @param endPoint the endPoint to set
     */
    public void setEndPoint(String endPoint) {
        this.endPoint = endPoint;
    }

    /**
     * @return the socketTimeout
     */
    public int getSocketTimeout() {
        return socketTimeout;
    }

    /**
     * @param socketTimeout the socketTimeout to set
     */
    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    /**
     * @return the connectionTimeout
     */
    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    /**
     * @param connectionTimeout the connectionTimeout to set
     */
    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }
}
