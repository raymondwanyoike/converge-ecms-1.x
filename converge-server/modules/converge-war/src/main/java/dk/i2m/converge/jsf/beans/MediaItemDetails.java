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
package dk.i2m.converge.jsf.beans;

import dk.i2m.commons.FileUtils;
import dk.i2m.converge.core.helpers.CatalogueHelper;
import dk.i2m.converge.core.content.MediaItem;
import dk.i2m.converge.core.metadata.Concept;
import dk.i2m.converge.core.metadata.GeoArea;
import dk.i2m.converge.core.metadata.Organisation;
import dk.i2m.converge.core.metadata.Person;
import dk.i2m.converge.core.metadata.PointOfInterest;
import dk.i2m.converge.core.metadata.Subject;
import dk.i2m.converge.core.security.UserAccount;
import dk.i2m.converge.core.security.UserRole;
import dk.i2m.converge.ejb.facades.MediaDatabaseFacadeLocal;
import dk.i2m.converge.ejb.facades.MetaDataFacadeLocal;
import dk.i2m.converge.ejb.services.DataNotFoundException;
import dk.i2m.jsf.JsfUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.event.ActionEvent;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import org.richfaces.component.html.HtmlTree;
import org.richfaces.event.NodeSelectedEvent;
import org.richfaces.event.UploadEvent;

/**
 * Backing bean for {@code /MediaItemDetails.jspx}.
 *
 * @author Allan Lykke Christensen
 */
public class MediaItemDetails {

    private static final Logger LOG = Logger.getLogger(MediaItemDetails.class.getName());

    @EJB private MediaDatabaseFacadeLocal mediaDatabaseFacade;

    @EJB private MetaDataFacadeLocal metaDataFacade;

    private MediaItem selectedMediaItem;

    private Long id;

    private DataModel discovered = new ListDataModel(new ArrayList());

    private boolean conceptAdded = false;

    private String newConcept = "";

    /** Dev Note: Could not use a Concept object for direct entry as it is abstract. */
    private String newConceptName = "";

    /** Dev Note: Could not use a Concept object for direct entry as it is abstract. */
    private String newConceptDescription = "";

    /** Dev Note: Could not use a Concept object for direct entry as it is abstract. */
    private String conceptType = "";

    /**
     * Creates a new instance of {@link MediaItemDetails}.
     */
    public MediaItemDetails() {
    }

    /**
     * Event handler for suggesting concepts based on inputted string. Note that
     * {@link dk.i2m.converge.core.metadata.Subject}s are not returned as they
     * are selected through the subject selection dialog.
     *
     * @param suggestion
     *          String for which to base the suggestions
     * @return {@link List} of suggested {@link Concept}s based on
     *         {@code suggestion}
     */
    public List<Concept> onConceptSuggestion(Object suggestion) {
        String conceptName = (String) suggestion;
        List<Concept> suggestedConcepts = new ArrayList<Concept>();
        suggestedConcepts = metaDataFacade.findConceptsByName(conceptName, Person.class, GeoArea.class, PointOfInterest.class, Organisation.class);

        return suggestedConcepts;
    }

    /**
     * Event handler for deleting the selected {@link MediaItem}. The handler
     * will not allow for the item to be deleted if it is referenced.
     * 
     * @return {@code /inbox} if the {@link MediaItem} was deleted, otherwise
     *         {@code null}
     */
    public String onDelete() {
        boolean used = mediaDatabaseFacade.isMediaItemUsed(selectedMediaItem.getId());

        if (used) {
            JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_ERROR, "mediaitem_MEDIA_ITEM_REFERENCED_COULD_NOT_BE_DELETED");
            return null;
        } else {
            mediaDatabaseFacade.deleteMediaItemById(selectedMediaItem.getId());
            JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO, "mediaitem_MEDIA_ITEM_DELETED");
            return "/inbox";
        }
    }

    /**
     * Event handler for receiving uploaded media files.
     * 
     * @param event 
     *          Event that invoked the handler, including the uploaded file
     */
    public void onUploadFile(UploadEvent event) {
        LOG.log(Level.INFO, "Uploading file to MediaItem");
        if (event == null) {
            return;
        }

        org.richfaces.model.UploadItem item = event.getUploadItem();

        if (item.isTempFile()) {
            java.io.File tempFile = item.getFile();

            selectedMediaItem.setFilename(FileUtils.getFilename(item.getFileName()));
            selectedMediaItem.setContentType(item.getContentType());
            Map<String, String> props = CatalogueHelper.getInstance().store(tempFile, selectedMediaItem);

            //mediaDatabaseFacade.store(FileUtils.getBytes(tempFile), selectedMediaItem);
            List<DiscoveredProperty> discoProps = new ArrayList<DiscoveredProperty>();
            for (String s : props.keySet()) {
                discoProps.add(new DiscoveredProperty(s, props.get(s)));
            }

            discovered = new ListDataModel(discoProps);

        } else {
            LOG.log(Level.SEVERE, "RichFaces is not set-up to use tempFiles for storing file uploads");
        }

        JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO, false, "Media file updated", null);
    }

    public void onApply(ActionEvent event) {
        try {
            selectedMediaItem = mediaDatabaseFacade.update(selectedMediaItem);
            JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO, false, "Media item was saved", null);
        } catch (Exception ex) {
            JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_ERROR, false, "An error occurred. " + ex.getMessage(), null);
        }
    }

    public void onSelectSubject(NodeSelectedEvent event) {
        HtmlTree tree = (HtmlTree) event.getComponent();
        Subject subj = (Subject) tree.getRowData();
        this.selectedMediaItem.getConcepts().add(subj);
        JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO, false, subj.getFullTitle() + " has been added to " + selectedMediaItem.getTitle(), null);
    }

    public void onAddConcept(ActionEvent event) {
        try {
            Concept concept = metaDataFacade.findConceptByName(newConcept);
            if (!this.selectedMediaItem.getConcepts().contains(concept)) {
                this.selectedMediaItem.getConcepts().add(concept);
                JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO, false, concept.getFullTitle() + " has been added to " + selectedMediaItem.getTitle(), null);
            }
            this.newConcept = "";
            conceptAdded = true;
        } catch (DataNotFoundException ex) {
            this.conceptType = "";
            this.newConceptName = this.newConcept;
            conceptAdded = false;
        }
    }

    public void onAddNewConcept(ActionEvent event) {
        Concept c = null;
        if ("ORGANISATION".equalsIgnoreCase(conceptType)) {
            c = new Organisation(newConceptName, newConceptDescription);
        } else if ("PERSON".equalsIgnoreCase(conceptType)) {
            c = new Person(newConceptName, newConceptDescription);
        } else if ("LOCATION".equalsIgnoreCase(conceptType)) {
            c = new GeoArea(newConceptName, newConceptDescription);
        } else if ("POI".equalsIgnoreCase(conceptType)) {
            c = new PointOfInterest(newConceptName, newConceptDescription);
        } else {
            JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_ERROR, false, "Please select concept type", null);
        }

        if (c != null) {
            c = metaDataFacade.create(c);
            if (!this.selectedMediaItem.getConcepts().contains(c)) {
                this.selectedMediaItem.getConcepts().add(c);
                JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO, false, c.getFullTitle() + " has been added to " + selectedMediaItem.getTitle(), null);
            }
            this.newConcept = "";
        }
    }

    public boolean isAuthorized() {
        return isEditor() || isOwner();
    }

    public boolean isEditor() {
        UserRole editorRole = selectedMediaItem.getMediaRepository().getEditorRole();
        List<UserRole> userRoles = getUser().getUserRoles();

        return userRoles.contains(editorRole);
    }

    public boolean isOwner() {
        return getUser().equals(selectedMediaItem.getOwner());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;

        if (this.id != null && id != null && selectedMediaItem == null) {
            try {
                selectedMediaItem = mediaDatabaseFacade.findMediaItemById(id);
            } catch (DataNotFoundException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        }
    }

    public boolean isNotProcessed() {
        if (selectedMediaItem.getStatus() != null) {
            switch (selectedMediaItem.getStatus()) {
                case APPROVED:
                case REJECTED:
                case SELF_UPLOAD:
                    return false;
                default:
                    return true;
            }
        } else {
            return true;
        }
    }

    public boolean isChangable() {
        if (!isNotProcessed() && isOwner() && !isEditor()) {
            return false;
        } else {
            return true;
        }
    }

    public MediaItem getSelectedMediaItem() {
        return selectedMediaItem;
    }

    public void setSelectedMediaItem(MediaItem selectedMediaItem) {
        this.selectedMediaItem = selectedMediaItem;
    }

    private UserAccount getUser() {
        return (UserAccount) JsfUtils.getValueOfValueExpression("#{userSession.user}");
    }

    public DataModel getDiscovered() {
        return discovered;
    }

    public String getNewConcept() {
        return newConcept;
    }

    public void setNewConcept(String newConcept) {
        this.newConcept = newConcept;
    }

    public class DiscoveredProperty {

        private String property = "";

        private String value = "";

        public DiscoveredProperty() {
        }

        public DiscoveredProperty(String property, String value) {
            this.property = property;
            this.value = value;
        }

        public String getProperty() {
            return property;
        }

        public void setProperty(String property) {
            this.property = property;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    public void setRemoveConcept(Concept concept) {
        this.selectedMediaItem.getConcepts().remove(concept);
    }

    public String getNewConceptDescription() {
        return newConceptDescription;
    }

    public void setNewConceptDescription(String newConceptDescription) {
        this.newConceptDescription = newConceptDescription;
    }

    public boolean isConceptAdded() {
        return conceptAdded;
    }

    public String getConceptType() {
        return conceptType;
    }

    public void setConceptType(String conceptType) {
        this.conceptType = conceptType;
    }

    public String getNewConceptName() {
        return newConceptName;
    }

    public void setNewConceptName(String newConceptName) {
        this.newConceptName = newConceptName;
    }
}
