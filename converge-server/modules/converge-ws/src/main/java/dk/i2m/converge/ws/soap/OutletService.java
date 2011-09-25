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
package dk.i2m.converge.ws.soap;

import dk.i2m.converge.core.content.NewsItemPlacement;
import dk.i2m.converge.ejb.facades.OutletFacadeLocal;
import dk.i2m.converge.ejb.services.DataNotFoundException;
import dk.i2m.converge.ws.model.Edition;
import dk.i2m.converge.ws.model.ModelConverter;
import dk.i2m.converge.ws.model.NewsItem;
import dk.i2m.converge.ws.model.Outlet;
import dk.i2m.converge.ws.model.Section;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;

/**
 * Service for accessing {@link Outlet}s.
 *
 * @author Allan Lykke Christensen
 */
@WebService(serviceName = "OutletService")
public class OutletService {

    private static final Logger LOG = Logger.getLogger(OutletService.class.getName());

    @EJB private OutletFacadeLocal outletFacade;

    /**
     * Obtains a given {@link Outlet} and its {@link Section}s.
     * 
     * @param id
     *          Unique identifier of the {@link Outlet}
     * @return {@link Outlet} matching the given {@code id}
     */
    @WebMethod(operationName = "getOutlet")
    public Outlet getOutlet(@WebParam(name = "outletId") Long id) {
        Outlet outlet = new Outlet();
        try {
            dk.i2m.converge.core.workflow.Outlet convergeOutlet = outletFacade.findOutletById(id);
            outlet = ModelConverter.toOutlet(convergeOutlet);
        } catch (DataNotFoundException ex) {
            LOG.log(Level.WARNING, "Unknown outlet requested");
        }
        return outlet;
    }

    /**
     * Obtains the published news items in a given edition.
     * 
     * @param id
     *          Unique identifier of the {@link Edition}
     * @return {@link Edition} containing the published {@link NewsItem}s
     */
    @WebMethod(operationName = "getPublishedEdition")
    public Edition getPublishedEdition(@WebParam(name = "editionId") Long id) {
        Edition edition = new Edition();

        try {
            dk.i2m.converge.core.workflow.Edition convergeEdition = outletFacade.findEditionById(id);

            edition.setId(convergeEdition.getId());
            edition.setCloseDate(convergeEdition.getCloseDate());
            edition.setPublicationDate(convergeEdition.getPublicationDate().getTime());
            edition.setExpirationDate(convergeEdition.getExpirationDate().getTime());

            for (NewsItemPlacement nip : convergeEdition.getPlacements()) {
                if (nip.getNewsItem().isEndState()) {
                    edition.getItems().add(ModelConverter.toNewsItem(nip));
                }
            }
        } catch (DataNotFoundException ex) {
            LOG.log(Level.WARNING, "Unknown outlet requested");
        }
        return edition;
    }
}
