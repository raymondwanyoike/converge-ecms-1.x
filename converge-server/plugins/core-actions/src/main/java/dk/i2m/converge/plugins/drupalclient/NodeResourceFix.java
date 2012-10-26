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
import com.google.gson.reflect.TypeToken;
import dk.i2m.drupal.core.DrupalClient;
import dk.i2m.drupal.message.FileMessage;
import dk.i2m.drupal.resource.NodeResource;
import dk.i2m.drupal.util.URLBuilder;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.http.Consts;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.io.IOUtils;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;

/**
 *
 * @author Raymond Wanyoike <rwa at i2m.dk>
 */
public class NodeResourceFix extends NodeResource {
    
    public NodeResourceFix(DrupalClient dc) {
        super(dc);
    }
    
    public List<FileMessage> attachFilesFix(Long id, Map<File, Map<String, String>> files, String fieldName, boolean attach) throws HttpResponseException, MimeTypeException, IOException {
        URLBuilder builder = new URLBuilder(getDc().getHostname());
        builder.add(getDc().getEndpoint());
        builder.add(getAlias());
        builder.add(id);
        builder.add("attach_file");
        
        MultipartEntity entity = getMultipartEntity(files);
        entity.addPart("field_name", new StringBody(fieldName));
        entity.addPart("attach", new StringBody((attach ? "1" : "0")));

        HttpPost method = new HttpPost(builder.toURI());
        method.setEntity(entity);

        ResponseHandler<String> handler = new BasicResponseHandler();
        String response = getDc().getHttpClient().execute(method, handler);

        return new Gson().fromJson(response,
                new TypeToken<List<FileMessage>>() {
                }.getType());
    }
    
    private MultipartEntity getMultipartEntity(Map<File, Map<String, String>> files) throws MimeTypeException, IOException {
        MimeTypes mimeTypes = TikaConfig.getDefaultConfig().getMimeRepository();
        MultipartEntity multipartEntity = new MultipartEntity();
        int x = 0;
        
        for (Map.Entry<File, Map<String, String>> entry : files.entrySet()) {
            Map<String, String> values = entry.getValue();
            File file = entry.getKey();
            
            String name = null;
            String alt = null;
            String title = null;
            
            for (Map.Entry<String, String> e : values.entrySet()) {
                String key = e.getKey();
                String value = e.getValue();
                
                if (key.equals("name")) {
                    name = value;
                }
                if (key.equals("alt")) {
                    alt = value;
                } 
                if (key.equals("title")) {
                    title = value;
                }
            }

            byte[] buf = IOUtils.toByteArray(new FileInputStream(file));
            MediaType mediaType = mimeTypes.
                    detect(new ByteArrayInputStream(buf), new Metadata());
            MimeType mimeType = mimeTypes.forName(mediaType.toString());
            FileBody fb = new FileBody(file, name + mimeType.getExtension(),
                    mimeType.getName(), Consts.UTF_8.name());

            multipartEntity.addPart("files[" + x + "]", fb);
            
            if (alt != null) {
                multipartEntity.addPart("field_values[" + x + "][alt]", new StringBody(alt));
            }
            if (title != null) {
                multipartEntity.addPart("field_values[" + x + "][title]", new StringBody(title));
            }
            
            x++;
        }

        return multipartEntity;
    }
}
