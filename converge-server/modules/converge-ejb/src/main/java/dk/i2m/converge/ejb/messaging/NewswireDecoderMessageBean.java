/*
 * Copyright (C) 2011 Interactive Media Management
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package dk.i2m.converge.ejb.messaging;

import dk.i2m.converge.core.ConfigurationKey;
import dk.i2m.converge.core.content.ContentTag;
import dk.i2m.converge.core.newswire.NewswireDecoderException;
import dk.i2m.converge.core.newswire.NewswireItem;
import dk.i2m.converge.core.newswire.NewswireService;
import dk.i2m.converge.core.plugin.NewswireDecoder;
import dk.i2m.converge.core.search.SearchEngineIndexingException;
import dk.i2m.converge.ejb.facades.SystemFacadeLocal;
import dk.i2m.converge.ejb.services.*;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.ejb.MessageDrivenContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.BinaryRequestWriter;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.common.SolrInputDocument;

/**
 * Message bean for downloading newswires.
 *
 * @author Allan Lykke Christensen
 */
@MessageDriven(mappedName = "jms/newswireServiceQueue")
public class NewswireDecoderMessageBean implements MessageListener {

    private static final Logger LOG = Logger.getLogger(NewswireDecoderMessageBean.class.getName());

    @Resource private MessageDrivenContext mdc;

    @EJB private ConfigurationServiceLocal cfgService;

    @EJB private DaoServiceLocal daoService;

    @EJB private PluginContextBeanLocal pluginContext;

    @EJB private SystemFacadeLocal systemFacade;

    @Override
    public void onMessage(Message msg) {
        Long taskId = 0L;
        try {
            Long newswireServiceId = null;
            try {
                newswireServiceId = msg.getLongProperty("newswireServiceId");
                LOG.log(Level.INFO, "Fetching single newswire service");
                taskId = systemFacade.createBackgroundTask("Fetching newswire service manually");
            } catch (NumberFormatException ex) {
                LOG.log(Level.INFO, "Fetching all newswire services");
                taskId = systemFacade.createBackgroundTask("Fetching all newswire services manually");
            }
            SolrServer solrServer = getSolrServer();

            if (newswireServiceId == null) {
                Map<String, Object> parameters = QueryBuilder.with("active", true).parameters();
                List<NewswireService> services = daoService.findWithNamedQuery(NewswireService.FIND_BY_STATUS, parameters);

                for (NewswireService service : services) {
                    fetchNewswire(service.getId(), solrServer);
                }
            } else {
                fetchNewswire(newswireServiceId, solrServer);
            }

        } catch (JMSException ex) {
            LOG.log(Level.SEVERE, null, ex);
        } finally {
            systemFacade.removeBackgroundTask(taskId);
        }
    }

    private void fetchNewswire(Long newswireServiceId, SolrServer solrServer) {
        Long taskId = 0L;
        try {
            NewswireService service = daoService.findById(NewswireService.class, newswireServiceId);
            taskId = systemFacade.createBackgroundTask("Fetching newswire service " + service.getSource());
            LOG.log(Level.INFO, "Newswire Service {0}", service.getSource());
            NewswireDecoder decoder = service.getDecoder();

            List<NewswireItem> items = decoder.decode(pluginContext, service);

            for (NewswireItem newswireItem : items) {
                try {
                    index(newswireItem, solrServer);
                } catch (SearchEngineIndexingException seie) {
                    LOG.log(Level.SEVERE, "Could not index newswire item. " + seie.getMessage(), seie);
                }
            }

            service.setLastFetch(Calendar.getInstance());
            daoService.update(service);

        } catch (DataNotFoundException ex) {
            LOG.log(Level.WARNING, ex.getMessage());
        } catch (NewswireDecoderException ex) {
            LOG.log(Level.SEVERE, null, ex);
        } finally {
            systemFacade.removeBackgroundTask(taskId);
        }
    }

    /**
     * Gets the instance of the Apache Solr server used for indexing.
     *
     * @return Instance of the Apache Solr server
     * @throws IllegalStateException
     *          If the search engine is not properly configured
     */
    private SolrServer getSolrServer() {
        try {
            String url = cfgService.getString(ConfigurationKey.SEARCH_ENGINE_NEWSWIRE_URL);
            Integer socketTimeout = cfgService.getInteger(ConfigurationKey.SEARCH_ENGINE_SOCKET_TIMEOUT);
            Integer connectionTimeout = cfgService.getInteger(ConfigurationKey.SEARCH_ENGINE_CONNECTION_TIMEOUT);
            Integer maxTotalConnectionsPerHost = cfgService.getInteger(ConfigurationKey.SEARCH_ENGINE_MAX_TOTAL_CONNECTIONS_PER_HOST);
            Integer maxTotalConnections = cfgService.getInteger(ConfigurationKey.SEARCH_ENGINE_MAX_TOTAL_CONNECTIONS);
            Integer maxRetries = cfgService.getInteger(ConfigurationKey.SEARCH_ENGINE_MAX_RETRIES);
            Boolean followRedirects = cfgService.getBoolean(ConfigurationKey.SEARCH_ENGINE_FOLLOW_REDIRECTS);
            Boolean allowCompression = cfgService.getBoolean(ConfigurationKey.SEARCH_ENGINE_ALLOW_COMPRESSION);

            CommonsHttpSolrServer solrServer = new CommonsHttpSolrServer(url);
            solrServer.setRequestWriter(new BinaryRequestWriter());
            solrServer.setSoTimeout(socketTimeout);
            solrServer.setConnectionTimeout(connectionTimeout);
            solrServer.setDefaultMaxConnectionsPerHost(maxTotalConnectionsPerHost);
            solrServer.setMaxTotalConnections(maxTotalConnections);
            solrServer.setFollowRedirects(followRedirects);
            solrServer.setAllowCompression(allowCompression);
            solrServer.setMaxRetries(maxRetries);

            return solrServer;
        } catch (MalformedURLException ex) {
            LOG.log(Level.SEVERE, "Invalid search engine configuration. {0}", ex.getMessage());
            LOG.log(Level.FINE, "", ex);
            throw new IllegalStateException("Invalid search engine configuration", ex);
        }
    }

    private void index(NewswireItem ni, SolrServer solrServer) throws SearchEngineIndexingException {
        SolrInputDocument solrDoc = new SolrInputDocument();
        solrDoc.addField("id", ni.getId(), 1.0f);
        solrDoc.addField("headline", ni.getTitle(), 1.0f);
        solrDoc.addField("provider", ni.getNewswireService().getSource());
        solrDoc.addField("provider-id", ni.getNewswireService().getId());
        solrDoc.addField("story", dk.i2m.converge.core.utils.StringUtils.stripHtml(ni.getContent()));
        solrDoc.addField("caption", ni.getSummary());
        solrDoc.addField("author", ni.getAuthor());
        solrDoc.addField("date", ni.getDate().getTime());
        if (ni.isThumbnailAvailable()) {
            solrDoc.addField("thumb-url", ni.getThumbnailUrl());
        }

        for (ContentTag tag : ni.getTags()) {
            solrDoc.addField("tag", tag.getTag().toLowerCase());
        }

        try {
            solrServer.add(solrDoc);
        } catch (SolrServerException ex) {
            throw new SearchEngineIndexingException(ex);
        } catch (IOException ex) {
            throw new SearchEngineIndexingException(ex);
        }
    }
}
