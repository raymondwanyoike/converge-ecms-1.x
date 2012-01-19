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
package dk.i2m.converge.jsf.beans;

import dk.i2m.converge.core.content.catalogue.*;
import dk.i2m.converge.core.metadata.*;
import dk.i2m.converge.core.security.UserAccount;
import dk.i2m.converge.core.security.UserRole;
import dk.i2m.converge.ejb.facades.CatalogueFacadeLocal;
import dk.i2m.converge.ejb.facades.MetaDataFacadeLocal;
import dk.i2m.converge.ejb.facades.UserFacadeLocal;
import dk.i2m.converge.ejb.services.DataNotFoundException;
import dk.i2m.converge.ejb.services.MetaDataServiceLocal;
import dk.i2m.jsf.JsfUtils;
import java.io.IOException;
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

    @EJB private CatalogueFacadeLocal catalogueFacade;

    @EJB private MetaDataFacadeLocal metaDataFacade;

    @EJB private MetaDataServiceLocal metaDataService;

    @EJB private UserFacadeLocal userFacade;

    private MediaItem selectedMediaItem;

    private Long id;

    private MediaItemRendition selectedRendition = new MediaItemRendition();

    private DataModel discovered = new ListDataModel(new ArrayList());

    private DataModel usage;

    private boolean conceptAdded = false;

    private String newConcept = "";

    /** Dev Note: Could not use a Concept object for direct entry as it is abstract. */
    private String newConceptName = "";

    /** Dev Note: Could not use a Concept object for direct entry as it is abstract. */
    private String newConceptDescription = "";

    /** Dev Note: Could not use a Concept object for direct entry as it is abstract. */
    private String conceptType = "";

    /** Editors of the MediaItem catalogue. */
    private List<UserAccount> editors = new ArrayList<UserAccount>();

    private Map<String, Rendition> renditions;

    private DataModel availableRenditions;

    private Rendition uploadRendition;

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
     * @param suggestion String for which to base the suggestions
     * @return {@link List} of suggested {@link Concept}s based on
     *         {@code suggestion}
     */
    public List<Concept> onConceptSuggestion(Object suggestion) {
        String conceptName = (String) suggestion;
        List<Concept> suggestedConcepts;
        suggestedConcepts = metaDataFacade.findConceptsByName(conceptName, 
                Person.class, GeoArea.class, PointOfInterest.class, 
                Organisation.class);

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
        boolean used = catalogueFacade.isMediaItemUsed(selectedMediaItem.getId());

        if (used) {
            JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_ERROR, "mediaitem_MEDIA_ITEM_REFERENCED_COULD_NOT_BE_DELETED");
            return null;
        } else {
            catalogueFacade.deleteMediaItemById(selectedMediaItem.getId());
            JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO, "mediaitem_MEDIA_ITEM_DELETED");
            return "/inbox";
        }
    }

    public void onApply(ActionEvent event) {
        try {
            if (!selectedMediaItem.isOriginalAvailable()) {
                JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_ERROR, "i18n", "MediaItemDetails_ORIGINAL_RENDITION_X_MISSING", selectedMediaItem.getCatalogue().getOriginalRendition().getLabel());
                return;
            }

            selectedMediaItem = catalogueFacade.update(selectedMediaItem);
            JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO, "i18n", "MediaItemDetails_MEDIA_ITEM_WAS_SAVED", null);
        } catch (Exception ex) {
            JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_ERROR, "i18n", "MediaItemDetails_ERROR_OCCURED_X", new Object[]{ex.getMessage()});
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

    /**
     * Determine if the current user is authorised to
     * view and work with the selected {@link MediaItem}.
     * 
     * @return {@code true} if the user is authorised,
     *         otherwise {@code false}
     */
    public boolean isAuthorized() {
        if (selectedMediaItem == null) {
            return false;
        }

        return isEditor() || isOwner();
    }

    /**
     * Determine if the current user is an editor of the {@link Catalogue} of
     * the {@link MediaItem}.
     * 
     * @return {@code true} if the user is an editor of the {@link Catalogue} of
     *         the {@link MediaItem}, otherwise {@code false}
     */
    public boolean isEditor() {
        UserRole editorRole = selectedMediaItem.getCatalogue().getEditorRole();
        return getUser().getUserRoles().contains(editorRole);
    }

    public boolean isOwner() {
        return getUser().equals(selectedMediaItem.getOwner());
    }

    public Long getId() {
        return id;
    }

    public MediaItemRendition getSelectedRendition() {
        return selectedRendition;
    }

    public void setSelectedRendition(MediaItemRendition selectedRendition) {
        this.selectedRendition = selectedRendition;
    }

    public Map<String, Rendition> getRenditions() {
        return renditions;
    }

    public void newRenditionUploadListener(UploadEvent event) {
        if (event == null) {
            return;
        }
        org.richfaces.model.UploadItem item = event.getUploadItem();
        LOG.log(Level.FINE, "Processing rendition ''{0}'' of content-type ''{1}''", new Object[]{item.getFileName(), item.getContentType()});

        if (item.isTempFile()) {
            java.io.File tempFile = item.getFile();

            try {
                selectedRendition = catalogueFacade.create(tempFile, selectedMediaItem, uploadRendition, item.getFileName(), item.getContentType());
                JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO, "media_item_rendition_UPLOADED");
            } catch (IOException ex) {
                JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_ERROR, "media_item_rendition_UPLOAD_ERROR");
                LOG.log(Level.WARNING, "Could not upload MediaItemRendition.", ex);
            }

        } else {
            LOG.severe("RichFaces is not set-up to use tempFiles for storing file uploads");
        }
        this.availableRenditions = null;
    }

    /**
     * Listener for uploading a new rendition.
     * 
     * @param event
     *          Event that invoked the listener
     */
    public void renditionUploadListener(UploadEvent event) {
        if (event == null) {
            return;
        }
        org.richfaces.model.UploadItem item = event.getUploadItem();
        LOG.log(Level.FINE, "Processing rendition ''{0}'' of content-type ''{1}''", new Object[]{item.getFileName(), item.getContentType()});

        if (item.isTempFile()) {
            java.io.File tempFile = item.getFile();

            try {
                selectedRendition = catalogueFacade.update(tempFile, item.getFileName(), item.getContentType(), selectedRendition);
            } catch (IOException ex) {
                JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_ERROR, "media_item_rendition_UPLOAD_ERROR");
                LOG.log(Level.WARNING, "Could not upload MediaItemRendition.", ex);
            }

        } else {
            LOG.severe("RichFaces is not set-up to use tempFiles for storing file uploads");
        }
        this.availableRenditions = null;
    }

    public void onNewRendition(ActionEvent event) {
        this.selectedRendition = new MediaItemRendition();
        this.selectedRendition.setMediaItem(this.selectedMediaItem);
    }

    public void onSaveNewRendition(ActionEvent event) {
        this.selectedMediaItem.getRenditions().add(selectedRendition);
        this.selectedMediaItem = catalogueFacade.update(selectedMediaItem);
        this.selectedMediaItem = null;
        setId(getId());

        JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO, "media_item_rendition_CREATED");
    }

    public void onSaveRendition(ActionEvent event) {
        catalogueFacade.update(selectedRendition);
        this.availableRenditions = null;
        JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO, "media_item_rendition_UPDATED");
    }

    public void onDeleteRendition(ActionEvent event) {
        catalogueFacade.deleteMediaItemRenditionById(this.selectedRendition.getId());
        selectedMediaItem.getRenditions().remove(this.selectedRendition);
        this.availableRenditions = null;
        JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO, "media_item_rendition_DELETED");
    }

    /**
     * Initialises the bean by retrieving the {@link MediaItem} and related data
     * from the database.
     * 
     * @param id 
     *          Unique identifier of the {@link MediaItem} to open
     */
    public void setId(Long id) {
        this.id = id;

        if (this.id != null && id != null && selectedMediaItem == null) {
            try {
                selectedMediaItem = catalogueFacade.findMediaItemById(id);
                usage = new ListDataModel(catalogueFacade.getMediaItemUsage(id));
                editors = userFacade.getMembers(selectedMediaItem.getCatalogue().getEditorRole());
                this.availableRenditions = null;

            } catch (DataNotFoundException ex) {
                LOG.log(Level.SEVERE, ex.getMessage());
            }
        }
    }

    public DataModel getAvailableRenditions() {
        if (this.availableRenditions == null) {
            Catalogue catalogue = selectedMediaItem.getCatalogue();

            List<AvailableMediaItemRendition> availableMediaItemRenditions = new ArrayList<AvailableMediaItemRendition>();
            for (Rendition rendition : catalogue.getRenditions()) {
                try {
                    availableMediaItemRenditions.add(new AvailableMediaItemRendition(rendition, selectedMediaItem.findRendition(rendition)));
                } catch (RenditionNotFoundException rnfe) {
                    availableMediaItemRenditions.add(new AvailableMediaItemRendition(rendition));
                }
            }

            availableRenditions = new ListDataModel(availableMediaItemRenditions);
        }
        return this.availableRenditions;
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

    /**
     * Executes a {@link CatalogueHookInstance} on the selected {@link MediaItem}.
     * 
     * @param instance 
     *          {@link CatalogueHookInstance} to execute
     */
    public void setExecuteHook(CatalogueHookInstance instance) {
        try {
            catalogueFacade.executeHook(getSelectedMediaItem().getId(), instance.getId());
            this.availableRenditions = null;
            this.selectedMediaItem = null;
            setId(getId());
            JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO, "i18n", "MediaItemDetails_EXECUTE_HOOK_DONE", instance.getLabel());
        } catch (DataNotFoundException ex) {
            JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_ERROR, "i18n", "MediaItemDetails_EXECUTE_HOOK_FAILED", instance.getLabel());
            Logger.getLogger(MediaItemDetails.class.getName()).log(Level.SEVERE, null, ex);
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

    public DataModel getUsage() {
        return usage;
    }

    public String getNewConcept() {
        return newConcept;
    }

    public void setNewConcept(String newConcept) {
        this.newConcept = newConcept;
    }

    public List<UserAccount> getEditors() {
        return editors;
    }

    public Rendition getUploadRendition() {
        return uploadRendition;
    }

    public void setUploadRendition(Rendition uploadRendition) {
        this.uploadRendition = uploadRendition;
    }

    public class AvailableMediaItemRendition {

        private MediaItemRendition mediaItemRendition;

        private Rendition rendition;

        public AvailableMediaItemRendition(Rendition rendition, MediaItemRendition mediaItemRendition) {
            this.rendition = rendition;
            this.mediaItemRendition = mediaItemRendition;
        }

        public AvailableMediaItemRendition(Rendition rendition) {
            this(rendition, null);
        }

        public boolean isAvailable() {
            return this.mediaItemRendition != null;
        }

        public MediaItemRendition getMediaItemRendition() {
            return this.mediaItemRendition;
        }

        public Rendition getRendition() {
            return this.rendition;
        }
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
