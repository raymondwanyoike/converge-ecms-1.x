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

import dk.i2m.commons.BeanComparator;
import dk.i2m.converge.core.content.catalogue.*;
import dk.i2m.converge.core.DataNotFoundException;
import dk.i2m.converge.core.content.ContentItemActor;
import dk.i2m.converge.core.content.ContentItemPermission;
import dk.i2m.converge.core.metadata.*;
import dk.i2m.converge.core.security.UserAccount;
import dk.i2m.converge.core.security.UserRole;
import dk.i2m.converge.core.workflow.WorkflowStep;
import dk.i2m.converge.ejb.facades.*;
import dk.i2m.jsf.JsfUtils;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.event.ActionEvent;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import org.richfaces.component.html.HtmlTree;
import org.richfaces.event.NodeSelectedEvent;

/**
 * Backing bean for {@code /MediaItemDetails.jspx}.
 *
 * @author Allan Lykke Christensen
 */
public class MediaItemDetails {

    private static final Logger LOG = Logger.getLogger(MediaItemDetails.class.
            getName());

    @EJB private CatalogueFacadeLocal catalogueFacade;

    @EJB private MetaDataFacadeLocal metaDataFacade;

    @EJB private UserFacadeLocal userFacade;

    private MediaItem selectedMediaItem;

    private Long id;

    private MediaItemRendition selectedRendition = new MediaItemRendition();

    private DataModel discovered = new ListDataModel(new ArrayList());

    private DataModel usage;

    private boolean conceptAdded = false;

    private String newConcept = "";

    private boolean renditionUploadFailed = false;

    private int renditionUploadFailedSize = 0;

    /** User selected {@link WorkflowStep}. */
    private WorkflowStep selectedWorkflowStep = null;

    /**
     * Dev Note: Could not use a Concept object for direct entry as it is
     * abstract.
     */
    private String newConceptName = "";

    /**
     * Dev Note: Could not use a Concept object for direct entry as it is
     * abstract.
     */
    private String newConceptDescription = "";

    /**
     * Dev Note: Could not use a Concept object for direct entry as it is
     * abstract.
     */
    private String conceptType = "";

    private Map<String, Rendition> renditions;

    private DataModel availableRenditions;

    private Rendition uploadRendition;

    /** State containing the permission of the current user to the selected media item. */
    private ContentItemPermission permission;

    /** Lazy loaded Map of available user roles for the Catalogue of the MediaItem. */
    private Map<String, UserRole> availableRoles;

    /** Placeholder of creating a new actor. */
    private ContentItemActor newActor;

    /** Top-level subjects from the classification selector. */
    private Subject[] topSubjects = null;

    /**
     * Creates a new instance of {@link MediaItemDetails}.
     */
    public MediaItemDetails() {
    }

    /**
     * Gets the unique identifier of the {@link MediaItem} opened.
     * 
     * @return Unique identifier of the opened {@link MediaItem}
     */
    public Long getId() {
        return id;
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
                this.availableRenditions = null;

                // Authorisation check
                this.permission = ContentItemPermission.UNAUTHORIZED;
                for (ContentItemActor a : selectedMediaItem.getActors()) {
                    if (a.getUser().equals(getUser())) {
                        this.permission = ContentItemPermission.ACTOR;
                    }
                }

                UserRole stateRole = selectedMediaItem.getCurrentState().
                        getActorRole();

                if (selectedMediaItem.getCurrentState().isGroupPermission()) {
                    if (getUser().getUserRoles().contains(stateRole)) {
                        this.permission = ContentItemPermission.ROLE;
                    }
                } else {
                    for (ContentItemActor a : selectedMediaItem.getActors()) {
                        if (a.getRole().equals(stateRole) && a.getUser().equals(
                                getUser())) {
                            this.permission = ContentItemPermission.USER;
                            break;
                        }
                    }
                }
            } catch (DataNotFoundException ex) {
                LOG.log(Level.SEVERE, ex.getMessage());
            }
        }
    }

    /**
     * Event handler for saving changes made to the {@link MediaItem}.
     * 
     * @param event 
     *          Event that occurred
     */
    public void onApply(ActionEvent event) {
        try {
            if (!selectedMediaItem.isOriginalAvailable()) {
                JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_ERROR,
                        Bundle.i18n.name(),
                        "MediaItemDetails_ORIGINAL_RENDITION_X_MISSING",
                        new Object[]{
                            selectedMediaItem.getCatalogue().
                            getOriginalRendition().
                            getLabel()});
                return;
            }

            selectedMediaItem = catalogueFacade.update(selectedMediaItem);
            JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO,
                    Bundle.i18n.name(),
                    "MediaItemDetails_MEDIA_ITEM_WAS_SAVED");
        } catch (Exception ex) {
            JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_ERROR,
                    Bundle.i18n.name(), "Generic_AN_ERROR_OCCURRED_X",
                    new Object[]{ex.getMessage()});
        }
    }

    public String selectWorkflowOption() {
        try {
            selectedMediaItem = catalogueFacade.step(selectedMediaItem,
                    getSelectedWorkflowStep().
                    getId());
            JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO,
                    Bundle.i18n.name(), "MediaItemDetails_MEDIA_ITEM_SUBMITTED");
            return "/inbox";
        } catch (WorkflowStateTransitionValidationException ex) {
            if (ex.isLocalisedMessage()) {
                JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_ERROR,
                        Bundle.i18n.name(), ex.getMessage(), ex.
                        getLocalisationParameters());
            } else {
                JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_ERROR,
                        ex.getMessage());
            }
        } catch (WorkflowStateTransitionException ex) {
            JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_ERROR, ex.
                    getMessage());
        }
        return null;
    }

    public void onSelectSubject(NodeSelectedEvent event) {
        HtmlTree tree = (HtmlTree) event.getComponent();
        Subject subj = (Subject) tree.getRowData();
        this.selectedMediaItem.getConcepts().add(subj);
        this.selectedMediaItem = catalogueFacade.update(selectedMediaItem);

        JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO,
                Bundle.i18n.name(),
                "MediaItemDetails_CONCEPT_X_ADDED_TO_MEDIA_ITEM_Y",
                new Object[]{subj.getFullTitle(), selectedMediaItem.getTitle()});
    }

    public void onAddConcept(ActionEvent event) {
        try {
            Concept concept = metaDataFacade.findConceptByName(newConcept);
            if (!this.selectedMediaItem.getConcepts().contains(concept)) {
                this.selectedMediaItem.getConcepts().add(concept);
                this.selectedMediaItem = catalogueFacade.update(
                        selectedMediaItem);
                JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO,
                        Bundle.i18n.name(),
                        "MediaItemDetails_CONCEPT_X_ADDED_TO_MEDIA_ITEM_Y",
                        new Object[]{concept.getFullTitle(), selectedMediaItem.
                            getTitle()});
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
            JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_ERROR,
                    Bundle.i18n.name(),
                    "MediaItemDetails_CONCEPT_TYPE_MISSING");
        }

        if (c != null) {
            c = metaDataFacade.create(c);
            if (!this.selectedMediaItem.getConcepts().contains(c)) {
                this.selectedMediaItem.getConcepts().add(c);
                this.selectedMediaItem = catalogueFacade.update(
                        selectedMediaItem);
                JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO,
                        Bundle.i18n.name(),
                        "MediaItemDetails_CONCEPT_X_ADDED_TO_MEDIA_ITEM_Y",
                        new Object[]{c.getFullTitle(), selectedMediaItem.
                            getTitle()});
            }
            this.newConcept = "";
        }
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
        List<Concept> suggestedConcepts;
        suggestedConcepts = metaDataFacade.findConceptsByName(conceptName,
                Person.class, GeoArea.class, PointOfInterest.class,
                Organisation.class);

        return suggestedConcepts;
    }

    /**
     * Determine if the current user is authorised to view and work with the
     * selected {@link MediaItem}.
     * 
     * @return {@code true} if the user is authorised, otherwise {@code false}
     */
    public boolean isAuthorized() {
        if (permission == null) {
            return false;
        }

        switch (permission) {
            case UNAUTHORIZED:
                return false;
            default:
                return true;
        }
    }

    /**
     * Determines if the current user is the current actor of the
     * {@link ContentItem}.
     *
     * @return {@code true} if the current user is among the current actors of
     *         the {@link ContentItem}, otherwise {@code false}
     */
    public boolean isCurrentActor() {
        boolean currentActor;
        if (permission == null) {
            return false;
        }

        switch (permission) {
            case USER:
            case ROLE:
                currentActor = true;
                break;
            default:
                currentActor = false;
        }
        return currentActor;
    }

    /**
     * Event handler for starting the creation of a new actor.
     * 
     * @param event
     *          Event that invoked the handler
     */
    public void onActorSelect(ActionEvent event) {
        this.newActor = new ContentItemActor();
    }

    /**
     * Property handler for removing an actor from the {@link MediaItem}.
     * 
     * @param actor 
     *          Actor to remove from the item
     */
    public void setRemoveActor(ContentItemActor actor) {
        if (actor != null) {
            getSelectedMediaItem().getActors().remove(actor);
            setSelectedMediaItem(catalogueFacade.update(selectedMediaItem));
            JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO,
                    Bundle.i18n.name(), "MediaItemDetails_ACTOR_REMOVED");
        }
    }

    /**
     * Event handler for adding a new actor to the MediaItem.
     * 
     * @param event 
     *          Event that invoked the handler
     */
    public void onAddActor(ActionEvent event) {
        if (this.newActor != null && this.newActor.getRole() != null
                && this.newActor.getUser() != null) {

            boolean dup = false;

            for (ContentItemActor cia : getSelectedMediaItem().getActors()) {
                if (cia.getRole().equals(getNewActor().getRole())
                        && cia.getUser().equals(getNewActor().getUser())) {
                    dup = true;
                    JsfUtils.createMessage("frmPage",
                            FacesMessage.SEVERITY_ERROR, Bundle.i18n.name(),
                            "MediaItemDetails_ACTOR_DUPLICATE_USER_ROLE",
                            new Object[]{getNewActor().getUser().getFullName(),
                                getNewActor().getRole().getName()});
                }
            }

            // If the actor is not a duplicate
            if (!dup) {

                getNewActor().setContentItem(getSelectedMediaItem());
                getSelectedMediaItem().getActors().add(newActor);
                setSelectedMediaItem(catalogueFacade.update(
                        getSelectedMediaItem()));
                JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO,
                    Bundle.i18n.name(),
                    "MediaItemDetails_ACTOR_X_ADDED_AS_Y_TO_ITEM", new Object[]{getNewActor().getUser().getFullName(), getNewActor().getRole().getName()});
            }
            this.newActor = new ContentItemActor();

        } else {
            JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_ERROR,
                    Bundle.i18n.name(),
                    "MediaItemDetails_ACTOR_SELECT_USER_AND_ROLE");
        }
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

    public void onNewRendition(ActionEvent event) {
        this.selectedRendition = new MediaItemRendition();
        this.selectedRendition.setMediaItem(this.selectedMediaItem);
    }

    public void onSaveNewRendition(ActionEvent event) {
        if (renditionUploadFailed) {
            JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_ERROR,
                    Bundle.i18n.name(),
                    "MediaItemDetails_RENDITION_UPLOAD_SIZE_ERROR",
                    new Object[]{renditionUploadFailedSize});
            renditionUploadFailed = false;
            renditionUploadFailedSize = 0;
            return;
        }

        this.selectedMediaItem.getRenditions().add(selectedRendition);
        this.selectedMediaItem = catalogueFacade.update(selectedMediaItem);
        this.selectedMediaItem = null;
        setId(getId());

        JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO,
                Bundle.i18n.name(), "MediaItemDetails_RENDITION_CREATED");
    }

    /**
     * Reloads the current {@link MediaItem}. This is used after changes has
     * been made to a rendition.
     * 
     * @param event
     *          Event that invoked the handler
     */
    public void onReload(ActionEvent event) {
        this.selectedMediaItem = null;
        setId(getId());
    }

    public void onSaveRendition(ActionEvent event) {
        if (renditionUploadFailed) {
            JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_ERROR,
                    Bundle.i18n.name(),
                    "MediaItemDetails_RENDITION_UPLOAD_SIZE_ERROR",
                    new Object[]{renditionUploadFailedSize});
            renditionUploadFailed = false;
            renditionUploadFailedSize = 0;
            return;
        }

        catalogueFacade.update(selectedRendition);
        this.availableRenditions = null;
        Long theId = this.selectedMediaItem.getId();
        this.selectedMediaItem = null;
        setId(theId);
        JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO,
                Bundle.i18n.name(), "MediaItemDetails_RENDITION_UPDATED");
    }

    public void onDeleteRendition(ActionEvent event) {
        catalogueFacade.deleteMediaItemRenditionById(selectedRendition.getId());
        selectedMediaItem.getRenditions().remove(this.selectedRendition);
        this.availableRenditions = null;
        JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO,
                Bundle.i18n.name(), "MediaItemDetails_RENDITION_DELETED");
    }

    public DataModel getAvailableRenditions() {
        if (this.availableRenditions == null) {
            Catalogue catalogue = selectedMediaItem.getCatalogue();

            List<AvailableMediaItemRendition> availableMediaItemRenditions =
                    new ArrayList<AvailableMediaItemRendition>();
            for (Rendition rendition : catalogue.getRenditions()) {
                try {
                    availableMediaItemRenditions.add(new AvailableMediaItemRendition(
                            rendition,
                            selectedMediaItem.findRendition(rendition)));
                } catch (RenditionNotFoundException rnfe) {
                    availableMediaItemRenditions.add(new AvailableMediaItemRendition(
                            rendition));
                }
            }

            availableRenditions =
                    new ListDataModel(availableMediaItemRenditions);
        }
        return this.availableRenditions;
    }

    /**
     * Executes a {@link CatalogueHookInstance} on the selected 
     * {@link MediaItem}.
     *
     * @param instance {@link CatalogueHookInstance} to execute
     */
    public void setExecuteHook(CatalogueHookInstance instance) {
        try {
            catalogueFacade.executeHook(getSelectedMediaItem().getId(),
                    instance.getId());
            this.availableRenditions = null;
            this.selectedMediaItem = null;
            setId(getId());
            JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO,
                    Bundle.i18n.name(), "MediaItemDetails_EXECUTE_HOOK_DONE",
                    new Object[]{instance.getLabel()});
        } catch (DataNotFoundException ex) {
            JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_ERROR,
                    Bundle.i18n.name(), "MediaItemDetails_EXECUTE_HOOK_FAILED",
                    new Object[]{instance.getLabel()});
        }
    }

    public MediaItem getSelectedMediaItem() {
        return selectedMediaItem;
    }

    public void setSelectedMediaItem(MediaItem selectedMediaItem) {
        this.selectedMediaItem = selectedMediaItem;
    }

    /**
     * Gets a {@link Map} of available {@link UserRole}s for the 
     * {@link Catalogue} of the selected {@link MediaItem}. The {@link Map} uses
     * the name of the {@link UserRole} as the key and the {@link UserRole} as
     * the value.
     * 
     * @return {@link Map} of available {@link UserRole}s
     */
    public Map<String, UserRole> getAvailableRoles() {
        if (this.availableRoles == null) {
            this.availableRoles = new LinkedHashMap<String, UserRole>();

            Catalogue c = getSelectedMediaItem().getCatalogue();
            for (UserRole role : c.getUserRoles()) {
                String key = role.getName();
                if (this.availableRoles.containsKey(key)) {
                    key = key + " (" + role.getId() + ")";
                }
                this.availableRoles.put(key, role);
            }
        }
        return this.availableRoles;
    }

    /**
     * Gets a {@link Map} of {@link UserAccount}s in the selected 
     * {@link UserRole}. The {@link Map} uses the full name of the 
     * {@link UserAccount} as the key and the {@link UserAccount} as the value.
     * 
     * @return {@link Map} of {@link UserAccount}s for the selected 
     *         {@link UserRole}
     */
    public Map<String, UserAccount> getRoleUsers() {
        Map<String, UserAccount> users =
                new LinkedHashMap<String, UserAccount>();

        if (getNewActor() != null && getNewActor().getRole() != null) {
            List<UserAccount> accounts = userFacade.getMembers(
                    getNewActor().getRole());
            Collections.sort(accounts, new BeanComparator("fullName"));

            for (UserAccount user : accounts) {
                String key = user.getFullName();
                if (users.containsKey(key)) {
                    key = key + " (" + user.getId() + ")";
                }
                users.put(key, user);
            }
        }


        return users;
    }

    /**
     * Gets the placeholder for creating a new actor for the {@link MediaItem}.
     * 
     * @return New actor for the {@link MediaItem}
     */
    public ContentItemActor getNewActor() {
        if (newActor == null) {
            this.newActor = new ContentItemActor();
        }
        return newActor;
    }

    /**
     * Sets the placeholder for creating a new actor for the {@link MediaItem}.
     * 
     * @param newActor
     *          New actor for the {@link MediaItem}
     */
    public void setNewActor(ContentItemActor newActor) {
        this.newActor = newActor;
    }
    
    public Subject[] getParentSubjects() {
        if (this.topSubjects == null) {
            List<Subject> parents = metaDataFacade.findTopLevelSubjects();
            this.topSubjects = parents.toArray(new Subject[parents.size()]);
        }
        return this.topSubjects;
    }

    private UserAccount getUser() {
        return (UserAccount) JsfUtils.getValueOfValueExpression(
                "#{userSession.user}");
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

    public Rendition getUploadRendition() {
        return uploadRendition;
    }

    public void setUploadRendition(Rendition uploadRendition) {
        this.uploadRendition = uploadRendition;


    }

    public class AvailableMediaItemRendition {

        private MediaItemRendition mediaItemRendition;

        private Rendition rendition;

        public AvailableMediaItemRendition(Rendition rendition,
                MediaItemRendition mediaItemRendition) {
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

    /**
     * Gets the {@link WorkflowStep} selected by the user.
     * 
     * @return {@link WorkflowStep} selected by the user
     */
    public WorkflowStep getSelectedWorkflowStep() {
        return selectedWorkflowStep;
    }

    /**
     * Sets the {@link WorkflowStep} selected by the user.
     * 
     * @param selectedWorkflowStep 
     *          {@link WorkflowStep} selected by the user
     */
    public void setSelectedWorkflowStep(WorkflowStep selectedWorkflowStep) {
        this.selectedWorkflowStep = selectedWorkflowStep;
    }

    /**
     * Gets the permission of the {@link #selectedMediaItem} for the current
     * {@link UserAccount}.
     * 
     * @return Permission for the current {@link UserAccount}
     */
    public ContentItemPermission getPermission() {
        return permission;
    }
}
