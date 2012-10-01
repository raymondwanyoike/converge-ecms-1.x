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
package dk.i2m.converge.plugins.drupalclient;

import com.google.gson.Gson;
import dk.i2m.drupal.core.AbstractResourceCRUD;
import dk.i2m.drupal.core.DrupalClient;
import dk.i2m.drupal.message.NodeMessage;
import dk.i2m.drupal.util.URLBuilder;
import java.io.IOException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;

/**
 *
 * @author Raymond Wanyoike <rwa at i2m.dk>
 */
public class NewsItemResource extends AbstractResourceCRUD {

    public NewsItemResource(DrupalClient dc) {
        super(dc);
    }

    public NodeMessage retrieve(Long id) throws HttpResponseException,
            IOException {
        URLBuilder builder = new URLBuilder(getDc().getHostname());
        builder.add(getDc().getEndpoint());
        builder.add(getAlias());
        builder.add(id);

        HttpGet method = new HttpGet(builder.toURI());

        ResponseHandler<String> handler = new BasicResponseHandler();
        String response = getDc().getHttpClient().execute(method, handler);

        return new Gson().fromJson(response, NodeMessage.class);
    }

    @Override
    public String getAlias() {
        return "newsitem";
    }
}
