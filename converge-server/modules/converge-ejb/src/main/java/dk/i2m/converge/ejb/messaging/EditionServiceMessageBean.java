/*
 * Copyright (C) 2011 Interactive Media Management
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
package dk.i2m.converge.ejb.messaging;

import dk.i2m.converge.core.plugin.EditionAction;
import dk.i2m.converge.core.workflow.Edition;
import dk.i2m.converge.core.workflow.EditionActionException;
import dk.i2m.converge.core.workflow.OutletEditionAction;
import dk.i2m.converge.ejb.facades.OutletFacadeLocal;
import dk.i2m.converge.ejb.services.DaoServiceLocal;
import dk.i2m.converge.ejb.services.DataNotFoundException;
import dk.i2m.converge.ejb.services.PluginContextBeanLocal;
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
 * Message driven bean responding to edition service actions.
 *
 * @author Allan Lykke Christensen
 */
@MessageDriven(mappedName = "jms/editionServiceQueue")
public class EditionServiceMessageBean implements MessageListener {

    private static final Logger LOG = Logger.getLogger(EditionServiceMessageBean.class.getName());

    @Resource private MessageDrivenContext mdc;

    @EJB private OutletFacadeLocal outletFacade;

    @EJB private DaoServiceLocal daoService;

    @EJB private PluginContextBeanLocal pluginContext;

    @Override
    public void onMessage(Message msg) {
        try {
            Long editionId = msg.getLongProperty("editionId");
            Long actionId = msg.getLongProperty("actionId");

            try {
                OutletEditionAction action = daoService.findById(OutletEditionAction.class, actionId);
                EditionAction editionAction = action.getAction();
                Edition edition = outletFacade.findEditionById(editionId);
                // Fetch Placements
                edition.getPlacements();
                editionAction.execute(pluginContext, edition, action);
            } catch (DataNotFoundException ex) {
                LOG.log(Level.WARNING, ex.getMessage());
            } catch (EditionActionException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        } catch (JMSException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }
}
