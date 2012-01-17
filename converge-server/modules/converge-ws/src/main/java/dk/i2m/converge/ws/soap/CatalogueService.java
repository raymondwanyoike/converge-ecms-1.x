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
package dk.i2m.converge.ws.soap;

import dk.i2m.converge.core.content.catalogue.*;
import dk.i2m.converge.core.security.UserAccount;
import dk.i2m.converge.core.security.UserRole;
import dk.i2m.converge.ejb.facades.CatalogueFacadeLocal;
import dk.i2m.converge.ejb.facades.UserFacadeLocal;
import dk.i2m.converge.ejb.services.DataNotFoundException;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.ws.WebServiceContext;

/**
 * SOAP Web Service for working with digital asset {@link Catalogue}s.
 *
 * @author Allan Lykke Christensen
 */
@WebService(serviceName = "DigitalAssetService")
public class CatalogueService {

    @EJB private CatalogueFacadeLocal catalogueFacade;

    @EJB private UserFacadeLocal userFacade;

    @Resource private WebServiceContext context;

    @WebMethod(operationName = "createDigitalAsset")
    public Long createDigitalAsset(
            @WebParam(name = "catalogueId") Long catalogueId,
            @WebParam(name = "title") String title,
            @WebParam(name = "byLine") String byLine,
            @WebParam(name = "description") String description,
            @WebParam(name = "editorialNote") String editorialNote,
            @WebParam(name = "assetDate") Date assetDate) throws
            ServiceSecurityException, CatalogueNotFoundException {

        Calendar now = Calendar.getInstance();

        Catalogue catalogue;
        try {
            catalogue = catalogueFacade.findCatalogueById(catalogueId);
        } catch (DataNotFoundException ex) {
            throw new CatalogueNotFoundException(ex);
        }

        UserAccount userAccount = getCaller();

        MediaItem mediaItem = new MediaItem();
        mediaItem.setCatalogue(catalogue);
        mediaItem.setByLine(byLine);
        mediaItem.setCreated(now);
        mediaItem.setDescription(description);

        mediaItem.setEditorialNote(editorialNote);
        Calendar mediaDate = Calendar.getInstance();
        mediaDate.setTime(assetDate);
        mediaItem.setMediaDate(mediaDate);
        mediaItem.setOwner(userAccount);
        mediaItem.setTitle(title);
        mediaItem.setUpdated(now);
        mediaItem.setStatus(MediaItemStatus.UNSUBMITTED);

        mediaItem = catalogueFacade.create(mediaItem);

        return mediaItem.getId();
    }

    @WebMethod(operationName = "uploadDigitalAssetRendition")
    public Long uploadDigitalAssetRendition(
            @WebParam(name = "digitalAssetId") Long digitalAssetId,
            @WebParam(name = "rendition") String renditionName,
            @WebParam(name = "filename") String filename,
            @WebParam(name = "contentType") String contentType,
            @WebParam(name = "file") File file
            ) throws DigitalAssetNotFoundException, RenditionNotFoundException, IOException {
        
        MediaItem item;
        try {
            item = catalogueFacade.findMediaItemById(digitalAssetId);
        } catch (DataNotFoundException ex) {
            throw new DigitalAssetNotFoundException(ex);
        }
       
        Rendition rendition;
        try {
            rendition = catalogueFacade.findRenditionByName(renditionName);
        } catch (DataNotFoundException ex) {
            throw new RenditionNotFoundException(ex);
        }
       
        MediaItemRendition mir = catalogueFacade.create(file, item, rendition, filename, contentType);
        
        return mir.getId();
    }

    @WebMethod(operationName = "changeStatus")
    public void changeStatus(
            @WebParam(name = "digitalAssetId") Long digitalAssetId,
            @WebParam(name = "status") String status) throws
            DigitalAssetNotFoundException, InvalidMediaItemStatusException,
            ServiceSecurityException {
        UserAccount userAccount = getCaller();

        Calendar now = Calendar.getInstance();

        MediaItemStatus mediaItemStatus;
        try {
            mediaItemStatus = MediaItemStatus.valueOf(status);
        } catch (IllegalArgumentException ex) {
            throw new InvalidMediaItemStatusException(ex);
        }

        MediaItem item;
        try {
            item = catalogueFacade.findMediaItemById(digitalAssetId);
        } catch (DataNotFoundException ex) {
            throw new DigitalAssetNotFoundException(ex);
        }

        UserRole catalogueEditor = item.getCatalogue().getEditorRole();
        if (!userAccount.getUserRoles().contains(catalogueEditor)) {
            throw new ServiceSecurityException(userAccount.getUsername()
                    + " is not in the catalogue editor role '"
                    + catalogueEditor.getName() + "'");
        }

        item.setStatus(mediaItemStatus);
        catalogueFacade.update(item);
    }

    /**
     * Obtain the caller of the web service.
     * <p/>
     * @return {@link UserAccount} of the caller
     * @throws ServiceSecurityException If the caller is not authenticated or
     * the wrong username and/or password was provided
     */
    private UserAccount getCaller() throws ServiceSecurityException {
        if (context.getUserPrincipal() == null) {
            throw new ServiceSecurityException("User is not authenticated");
        }

        String username = context.getUserPrincipal().getName();
        UserAccount userAccount = null;
        try {
            userAccount = userFacade.findById(username);
        } catch (DataNotFoundException ex) {
            throw new ServiceSecurityException(ex);
        }
        return userAccount;
    }
}
