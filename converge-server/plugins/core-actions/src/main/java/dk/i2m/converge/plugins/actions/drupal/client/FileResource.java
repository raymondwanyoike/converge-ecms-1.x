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

import dk.i2m.converge.core.content.catalogue.MediaItemRendition;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

/**
 * API for handling file uploads and server file management.
 *
 * @author Raymond Wanyoike
 */
public class FileResource {

    private static final Logger LOG = Logger.getLogger("FileResource");

    private static final String FILE = "file";
    
    private static final String CREATE_RAW = "create_raw";

    private DefaultHttpClient httpClient;

    private URI uri;
    
    /**
     * Constructs a File resource from the given components.
     *
     * @param client Client instance
     */
    public FileResource(DrupalClient client) {
        this.uri = client.uri();
        this.httpClient = client.getHttpClient();
    }
    
    /**
     * Adds a new file.
     * 
     * @param message An object representing the file with a base64 encoded value
     * @return Populated {@link FileModule} instance
     * @throws IOException 
     */
    public ArrayList<FileCreateMessage> create(List<MediaItemRendition> renditions) throws IOException {
        ArrayList<FileCreateMessage> messages = new ArrayList<FileCreateMessage>();
        
        try {
            String url = new URLBuilder(uri).add(FILE).add(CREATE_RAW).toString();
            MultipartEntity mpEntity = new MultipartEntity();
            
            for (int i = 0; i < renditions.size(); i++) {
                MediaItemRendition rendition = renditions.get(i);
                File file = new File(rendition.getFileLocation());
                ContentBody contentBody = new FileBody(file, rendition.getContentType());
                mpEntity.addPart("files[" + i + "]", contentBody);
            }
            
            HttpPost post = new HttpPost(url);
            post.setEntity(mpEntity);

            ResponseHandler<String> handler = new BasicResponseHandler();
            String response = httpClient.execute(post, handler);

            JSONArray jsonArray = JSONArray.fromObject(response);

            if (jsonArray != null) {
                int len = jsonArray.size();

                for (int i = 0; i < len; i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    Object message = JSONObject.toBean(jsonObject, FileCreateMessage.class);
                    messages.add((FileCreateMessage) message);
                }
            }
            
            return messages;
        } catch (MalformedURLException ex) {
            LOG.log(Level.SEVERE, null, ex);
        } catch (ClientProtocolException ex) {
            LOG.log(Level.SEVERE, null, ex);
        } catch (UnsupportedEncodingException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }

        return messages;
    }
}
