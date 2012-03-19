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

import dk.i2m.converge.core.plugin.PluginContext;
import dk.i2m.converge.core.workflow.OutletEditionAction;
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
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.jactiveresource.URLBuilder;

public class DrupalConnector {

    public static final String CONTENT_TYPE = "application/json";

    private List<Header> headers = new ArrayList<Header>();

    private HttpParams params = new BasicHttpParams();

    private DefaultHttpClient httpClient;

    private int connectionTimeout;

    private int socketTimeout;

    private String endPoint;

    private String server;

    public DrupalConnector() {
    }

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

        ThreadSafeClientConnManager cm = new ThreadSafeClientConnManager();
        cm.setDefaultMaxPerRoute(5);

        this.httpClient = new DefaultHttpClient(cm, this.params);

        this.endPoint = endPoint;
        this.server = server;
    }

    public URI getUri() {
        try {
            URLBuilder ub = new URLBuilder();
            URI base = new URI(server);
            ub.setBase(base);
            ub.add(endPoint);
            return URI.create(ub.toString());
        } catch (MalformedURLException ex) {
            Logger.getLogger(DrupalConnector.class.getName()).log(Level.SEVERE,
                    null, ex);
        } catch (URISyntaxException ex) {
            Logger.getLogger(DrupalConnector.class.getName()).log(Level.SEVERE,
                    null, ex);
        }

        return null;
    }

    public void shutdown() {
        this.httpClient.getConnectionManager().shutdown();
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public String getEndPoint() {
        return endPoint;
    }

    public void setEndPoint(String endPoint) {
        this.endPoint = endPoint;
    }

    public List<Header> getHeaders() {
        return headers;
    }

    public void setHeaders(List<Header> headers) {
        this.headers = headers;
    }

    public DefaultHttpClient getHttpClient() {
        return httpClient;
    }

    public void setHttpClient(DefaultHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public HttpParams getParams() {
        return params;
    }

    public void setParams(HttpParams params) {
        this.params = params;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public int getSocketTimeout() {
        return socketTimeout;
    }

    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }
}
