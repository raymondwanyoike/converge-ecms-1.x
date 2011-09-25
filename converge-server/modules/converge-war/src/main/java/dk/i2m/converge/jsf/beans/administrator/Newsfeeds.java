/*
 * Copyright 2010 - 2011 Interactive Media Management
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
package dk.i2m.converge.jsf.beans.administrator;

import dk.i2m.converge.core.newswire.NewswireService;
import dk.i2m.converge.core.newswire.NewswireServiceProperty;
import dk.i2m.converge.ejb.services.NewswireServiceLocal;
import dk.i2m.converge.ejb.services.DataNotFoundException;
import dk.i2m.converge.plugins.decoders.rss.RssDecoder;
import dk.i2m.jsf.JsfUtils;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.event.ActionEvent;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;

/**
 * Managed backing-bean for <code>/administrator/Newsfeeds.jspx</code>.
 *
 * @author Allan Lykke Christensen
 */
public class Newsfeeds {

    @EJB private NewswireServiceLocal newswire;

    private DataModel newswires = null;

    private NewswireService selectedNewsfeed = null;

    private NewswireServiceProperty selectedNewswireProperty = new NewswireServiceProperty();

    private NewswireServiceProperty deletedProperty = null;

    private String selectedTab = "tabDetails";

    /**
     * Creates a new instance of {@link Newsfeeds}.
     */
    public Newsfeeds() {
    }

    public void onNew(ActionEvent e) {
        selectedNewsfeed = new NewswireService();
        selectedNewsfeed.setDecoderClass(RssDecoder.class.getName());
        selectedNewswireProperty = new NewswireServiceProperty();
        selectedTab = "tabDetails";
    }

    public void onSave(ActionEvent event) {
        if (isAddMode()) {
            selectedNewsfeed = newswire.create(selectedNewsfeed);
            JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO, "NEWSWIRE_ADDED");
        } else {
            newswire.update(selectedNewsfeed);
            JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO, "NEWSWIRE_UPDATED");
        }
        this.newswires = null;
    }

    public void onDelete(ActionEvent e) {
        try {
            newswire.delete(selectedNewsfeed.getId());
            this.newswires = null;
            JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO, "NEWSWIRE_DELETED");
        } catch (DataNotFoundException ex) {
            JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_WARN, "NEWSWIRE_ERROR_DELETING");
        }
        this.newswires = null;
    }

    public void onDownloadFeeds(ActionEvent event) {
        newswire.downloadNewswireServices();
        JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO, "newswire_DOWNLOAD_SCHEDULED");
    }

    public void onEmptyNewswireService(ActionEvent event) {
        int deleted = newswire.emptyNewswireService(selectedNewsfeed.getId());
        JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO, "newswire_SERVICE_EMPTIED", new Object[]{deleted, selectedNewsfeed.getSource()});
        this.newswires = null;
    }

    public void onEmptyNewswireServices(ActionEvent event) {
        for (NewswireService service : newswire.getNewswireServices()) {
            newswire.emptyNewswireService(service.getId());
        }
        JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO, "newswire_SERVICES_EMPTIED");

        this.newswires = null;
    }

    public void onUpdateStatus(ActionEvent event) {
        selectedNewsfeed.setActive(!selectedNewsfeed.isActive());
        newswire.update(selectedNewsfeed);

        JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO, "newswire_STATUS_TOGGLED", new Object[]{});
        this.newswires = null;
    }

    public void onDownloadNewswireService(ActionEvent event) {
        newswire.downloadNewswireService(selectedNewsfeed.getId());
        JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO, "newswire_DOWNLOAD_SCHEDULED", new Object[]{});
    }

    public void onAddProperty(ActionEvent event) {
        selectedNewswireProperty.setNewswireService(selectedNewsfeed);
        selectedNewsfeed.getProperties().add(selectedNewswireProperty);
        selectedNewswireProperty = new NewswireServiceProperty();
    }

    public NewswireService getSelectedNewsfeed() {
        return selectedNewsfeed;
    }

    public void setSelectedNewsfeed(NewswireService selectedNewsfeed) {
        this.selectedNewsfeed = selectedNewsfeed;
    }

    public NewswireServiceProperty getSelectedNewswireProperty() {
        return selectedNewswireProperty;
    }

    public void setSelectedNewswireProperty(NewswireServiceProperty selectedNewswireProperty) {
        this.selectedNewswireProperty = selectedNewswireProperty;
    }

    public NewswireServiceProperty getDeletedProperty() {
        return deletedProperty;
    }

    public void setDeletedProperty(NewswireServiceProperty deletedProperty) {
        this.deletedProperty = deletedProperty;
        selectedNewsfeed.getProperties().remove(deletedProperty);
    }

    /**
     * Gets the {@link DataModel} of {@link NewswireService}s.
     *
     * @return {@link DataModel} of {@link NewswireService}s.
     */
    public DataModel getNewsFeeds() {
        if (this.newswires == null) {
            this.newswires = new ListDataModel(newswire.getNewswireServicesWithSubscribersAndItems());
        }
        return this.newswires;
    }

    public boolean isEditMode() {
        if (selectedNewsfeed == null || selectedNewsfeed.getId() == null) {
            return false;
        } else {
            return true;
        }
    }

    public boolean isAddMode() {
        return !isEditMode();
    }

    public String getSelectedTab() {
        return selectedTab;
    }

    public void setSelectedTab(String selectedTab) {
        this.selectedTab = selectedTab;
    }
}
