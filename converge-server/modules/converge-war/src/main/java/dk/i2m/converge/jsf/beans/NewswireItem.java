/*
 *  Copyright (C) 2010 - 2011 Interactive Media Management
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.i2m.converge.jsf.beans;

import dk.i2m.commons.StringUtils;
import dk.i2m.converge.core.content.MediaItem;
import dk.i2m.converge.core.content.MediaItemStatus;
import dk.i2m.converge.core.content.MediaRepository;
import dk.i2m.converge.core.helpers.CatalogueHelper;
import dk.i2m.converge.core.newswire.NewswireItemAttachment;
import dk.i2m.converge.core.security.UserAccount;
import dk.i2m.converge.ejb.facades.MediaDatabaseFacadeLocal;
import dk.i2m.converge.ejb.services.DataNotFoundException;
import dk.i2m.converge.ejb.services.NewswireServiceLocal;
import dk.i2m.jsf.JsfUtils;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;

/**
 * JSF backing bean for {@code /NewswireItem.jspx}
 *
 * @author Allan Lykke Christensen
 */
public class NewswireItem {

    private static final Logger LOG = Logger.getLogger(NewswireItem.class.getName());

    @EJB private NewswireServiceLocal newswireService;

    @EJB private MediaDatabaseFacadeLocal mediaDatabase;

    private Long id = 0L;

    private dk.i2m.converge.core.newswire.NewswireItem selectedItem = null;

    public NewswireItem() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        LOG.log(Level.FINE, "Setting Newswire Item #{0}", id);
        this.id = id;

        if (selectedItem == null || (selectedItem.getId() != id)) {
            try {
                this.selectedItem = newswireService.findNewswireItemById(id);
            } catch (DataNotFoundException ex) {
                this.selectedItem = null;
            }
        }
    }

    /**
     * Determines if a news item has been loaded. If a news item cannot be
     * retrieved from {@link NewsItem#getSelectedNewsItem()} it is not loaded.
     *
     * @return {@code true} if a news item has been selected and loaded,
     *         otherwise {@code false}
     */
    public boolean isItemLoaded() {
        if (getSelectedItem() == null) {
            return false;
        } else {
            return true;
        }
    }

    public dk.i2m.converge.core.newswire.NewswireItem getSelectedItem() {
        return selectedItem;
    }

    private UserAccount getUser() {
        return (UserAccount) JsfUtils.getValueOfValueExpression("#{userSession.user}");
    }

    public void setAddToCatalogue(NewswireItemAttachment attachment) {
        LOG.log(Level.INFO, "Adding newswire attachment to catalogue");
        
        MediaRepository catalogue = getUser().getDefaultMediaRepository();
        MediaItem item = new MediaItem();
        item.setByLine(selectedItem.getAuthor());
        item.setContentType(attachment.getContentType());
        java.util.Calendar now = java.util.Calendar.getInstance();
        item.setCreated(now);
        item.setUpdated(now);
        
        if (selectedItem.isSummarised()) {
            LOG.log(Level.INFO, "Using summary for caption");
            item.setDescription(selectedItem.getSummary());
        } else {
            LOG.log(Level.INFO, "Using description for caption");
            item.setDescription(StringUtils.stripHtml(selectedItem.getContent()));
        }
        
        item.setTitle(selectedItem.getTitle());
        item.setOwner(getUser());
        item.setMediaRepository(catalogue);
        item.setStatus(MediaItemStatus.APPROVED);
        item = mediaDatabase.create(item);
        
        
        if (attachment.isStoredInCatalogue()) {
            LOG.log(Level.INFO, "Storing in catalogue");
            item.setFilename(attachment.getFilename());
            File mediaFile = new File(attachment.getCatalogueFileLocation());
            CatalogueHelper.getInstance().store(mediaFile, item);
            item = mediaDatabase.update(item);
        }
        
        JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO, "newswire_archive_ATTACHMENT_ADDED_TO_CATALOGUE", new Object[]{attachment.getDescription(), catalogue.getName(), item.getId()});
    }
}
