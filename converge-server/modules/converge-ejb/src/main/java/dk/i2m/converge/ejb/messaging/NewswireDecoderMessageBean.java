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

import dk.i2m.converge.core.newswire.NewswireDecoderException;
import dk.i2m.converge.core.newswire.NewswireService;
import dk.i2m.converge.core.plugin.NewswireDecoder;
import dk.i2m.converge.ejb.facades.SystemFacadeLocal;
import dk.i2m.converge.ejb.services.DaoServiceLocal;
import dk.i2m.converge.ejb.services.DataNotFoundException;
import dk.i2m.converge.ejb.services.PluginContextBeanLocal;
import dk.i2m.converge.ejb.services.QueryBuilder;
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

/**
 * Message bean for downloading newswires.
 *
 * @author Allan Lykke Christensen
 */
@MessageDriven(mappedName = "jms/newswireServiceQueue")
public class NewswireDecoderMessageBean implements MessageListener {

    private static final Logger LOG = Logger.getLogger(NewswireDecoderMessageBean.class.getName());

    @Resource private MessageDrivenContext mdc;

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

            if (newswireServiceId == null) {
                Map<String, Object> parameters = QueryBuilder.with("active", true).parameters();
                List<NewswireService> services = daoService.findWithNamedQuery(NewswireService.FIND_BY_STATUS, parameters);

                for (NewswireService service : services) {
                    fetchNewswire(service.getId());
                }
            } else {
                fetchNewswire(newswireServiceId);
            }

        } catch (JMSException ex) {
            LOG.log(Level.SEVERE, null, ex);
        } finally {
            systemFacade.removeBackgroundTask(taskId);
        }
    }

    private void fetchNewswire(Long newswireServiceId) {
        Long taskId = 0L;
        try {
            NewswireService service = daoService.findById(NewswireService.class, newswireServiceId);
            taskId = systemFacade.createBackgroundTask("Fetching newswire service " + service.getSource());
            LOG.log(Level.INFO, "Newswire Service {0}", service.getSource());
            NewswireDecoder decoder = service.getDecoder();
            decoder.decode(pluginContext, service);
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
}
