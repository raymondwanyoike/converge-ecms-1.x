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

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.Header;
import org.apache.http.client.params.AllClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.BasicClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.jactiveresource.URLBuilder;

/**
 * Provides a generic but powerful API for Drupal's Services module REST server.
 */
public class DrupalConnector {

    private static final String CONTENT_TYPE = "application/json";

    private List<Header> headers = new ArrayList<Header>();

    private HttpParams params = new BasicHttpParams();

    private DefaultHttpClient httpClient;

    private int connectionTimeout;

    private int socketTimeout;

    private String endPoint;

    private String server;

    /**
     * Create an empty Drupal connector.
     */
    public DrupalConnector() {
    }

    /**
     * Constructs a Drupal connector from the given components.
     *
     * @param server URL to the host
     * @param endPoint Endpoint defined in the Services module
     * @param connectionTimeout Duration to wait before timing out when connecting, 0 is infinite
     * @param socketTimeout Duration to wait before timing out for a response, 0 is infinite
     */
    public DrupalConnector(String server, String endPoint, int connectionTimeout,
            int socketTimeout) {
        this.params.setParameter(AllClientPNames.CONNECTION_TIMEOUT,
                connectionTimeout);
        this.params.setParameter(AllClientPNames.SO_TIMEOUT, socketTimeout);
        this.params.setParameter(AllClientPNames.HTTP_CONTENT_CHARSET,
                HTTP.UTF_8);
        this.params.setParameter(AllClientPNames.COOKIE_POLICY,
                CookiePolicy.BEST_MATCH);
        this.params.setParameter(AllClientPNames.DEFAULT_HEADERS, this.headers);
        this.headers.add(new BasicHeader("Content-Type", CONTENT_TYPE));

//        ThreadSafeClientConnManager cm = new ThreadSafeClientConnManager();
//        cm.setDefaultMaxPerRoute(1);
//        cm.setMaxTotal(1);

        BasicClientConnectionManager cm = new BasicClientConnectionManager();

        this.httpClient = new DefaultHttpClient(cm, this.params);
        this.endPoint = endPoint;
        this.server = server;
    }

    /**
     * Return the full path to the host endpoint.
     *
     * @return A URI equivalent of the host endpoint URL
     */
    public URI getUri() {
        try {
            URI base = new URI(server);
            URLBuilder ub = new URLBuilder(base).add(endPoint);
            return URI.create(ub.toString());
        } catch (MalformedURLException ex) {
            Logger.getLogger(DrupalConnector.class.getName()).
                    log(Level.SEVERE, null, ex);
        } catch (URISyntaxException ex) {
            Logger.getLogger(DrupalConnector.class.getName()).
                    log(Level.SEVERE, null, ex);
        }

        return null;
    }

    /**
     * Shutdown the httpclient and release all resources.
     */
    public void shutdown() {
        this.httpClient.getConnectionManager().shutdown();
    }

    /**
     * Return the connection timeout.
     *
     * @return The connection timeout in ms
     */
    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    /**
     * Set the duration to wait before timing out when connecting, 0 is infinite.
     *
     * @param connectionTimeout The duration in ms
     */
    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    /**
     * Return the endpoint.
     *
     * @return The endpoint
     */
    public String getEndPoint() {
        return endPoint;
    }

    /**
     * Set the endpoint defined in the Services module.
     *
     * @param endPoint The path to endpoint
     */
    public void setEndPoint(String endPoint) {
        this.endPoint = endPoint;
    }

    /**
     * Return the list of set headers.
     * @return A list of headers
     */
    public List<Header> getHeaders() {
        return headers;
    }

    /**
     * Set the headers
     *
     * @param headers
     */
    public void setHeaders(List<Header> headers) {
        this.headers = headers;
        this.headers.add(new BasicHeader("Content-Type", CONTENT_TYPE));
    }

    /**
     * Return the httpclient in use.
     *
     * @return The httpclient
     */
    public DefaultHttpClient getHttpClient() {
        return httpClient;
    }

    /**
     * Set the httpclient to use.
     *
     * @param httpClient The httpclient
     */
    public void setHttpClient(DefaultHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * Return the httpclient parameters.
     *
     * @return The httpclient parameters
     */
    public HttpParams getParams() {
        return params;
    }

    /**
     * Set the httpclient parameters.
     *
     * @param params The httpclient parameters
     */
    public void setParams(HttpParams params) {
        this.params = params;
    }

    /**
     * Return the host URL.
     *
     * @return The host URL
     */
    public String getServer() {
        return server;
    }

    /**
     * Set the URL to the host.
     *
     * @param server The host URL
     */
    public void setServer(String server) {
        this.server = server;
    }

    /**
     * Return the socket timeout.
     *
     * @return The socket timeout in ms
     */
    public int getSocketTimeout() {
        return socketTimeout;
    }

    /**
     * Set the duration to wait before timing out for a response, 0 is infinite
     *
     * @param socketTimeout The duration in ms
     */
    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }
}
