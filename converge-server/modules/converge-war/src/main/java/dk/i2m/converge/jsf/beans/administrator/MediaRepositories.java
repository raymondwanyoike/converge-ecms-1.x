/*
 *  Copyright (C) 2010 Interactive Media Management
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
package dk.i2m.converge.jsf.beans.administrator;

import dk.i2m.converge.core.content.MediaRepository;
import dk.i2m.converge.ejb.facades.MediaDatabaseFacadeLocal;
import dk.i2m.converge.ejb.services.DataNotFoundException;
import dk.i2m.jsf.JsfUtils;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.event.ActionEvent;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;

/**
 * Backing bean for {@code /administrator/MediaRepositories.jspx}.
 *
 * @author Allan Lykke Christensen
 */
public class MediaRepositories {

    @EJB private MediaDatabaseFacadeLocal mediaDatabaseFacade;

    private DataModel repositories = null;

    private MediaRepository selectedMediaRepository = null;

    public MediaRepositories() {
    }

    public void onIndex(ActionEvent event) {
        try {
            mediaDatabaseFacade.indexMediaRepositories();
            JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO, false, "Indexing complete", null);
        } catch (Exception ex) {
            JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_ERROR, false, ex.getMessage(), null);
        }
    }

    public void onNew(ActionEvent event) {
        selectedMediaRepository = new MediaRepository();
    }

    public void onSave(ActionEvent event) {
        repositories = null;
        if (isEditMode()) {
            selectedMediaRepository = mediaDatabaseFacade.update(selectedMediaRepository);
            JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO, "mediaitem_MEDIA_REPOSITORY_UPDATED");
        } else {
            selectedMediaRepository = mediaDatabaseFacade.create(selectedMediaRepository);
            JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO, "mediaitem_MEDIA_REPOSITORY_CREATED");
        }
    }

    public void onDelete(ActionEvent event) {
        try {
            mediaDatabaseFacade.deleteMediaRepositoryById(selectedMediaRepository.getId());
            JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO, "mediaitem_MEDIA_REPOSITORY_DELETED");
        } catch (DataNotFoundException ex) {
            Logger.getLogger(MediaRepositories.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public DataModel getRepositories() {
        if (repositories == null) {
            repositories = new ListDataModel(mediaDatabaseFacade.findAllMediaRepositories());
        }
        return repositories;
    }

    public MediaRepository getSelectedMediaRepository() {
        return selectedMediaRepository;
    }

    public void setSelectedMediaRepository(MediaRepository selectedMediaRepository) {
        this.selectedMediaRepository = selectedMediaRepository;
    }

    public boolean isEditMode() {
        if (selectedMediaRepository == null || selectedMediaRepository.getId() == null) {
            return false;
        } else {
            return true;
        }
    }
}
