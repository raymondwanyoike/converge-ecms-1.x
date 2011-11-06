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

import dk.i2m.commons.BeanComparator;
import dk.i2m.converge.core.workflow.Edition;
import dk.i2m.converge.core.workflow.Outlet;
import dk.i2m.converge.domain.search.SearchResult;
import dk.i2m.converge.core.workflow.Section;
import dk.i2m.converge.core.content.ContentItemPermission;
import dk.i2m.converge.core.content.catalogue.MediaItem;
import dk.i2m.converge.core.content.catalogue.MediaItemStatus;
import dk.i2m.converge.core.content.catalogue.Catalogue;
import dk.i2m.converge.core.content.NewsItemActor;
import dk.i2m.converge.core.content.NewsItemField;
import dk.i2m.converge.core.content.NewsItemMediaAttachment;
import dk.i2m.converge.core.content.NewsItemPlacement;
import dk.i2m.converge.core.content.catalogue.MediaItemRendition;
import dk.i2m.converge.core.security.UserAccount;
import dk.i2m.converge.core.security.UserRole;
import dk.i2m.converge.core.metadata.Concept;
import dk.i2m.converge.core.metadata.GeoArea;
import dk.i2m.converge.core.metadata.Organisation;
import dk.i2m.converge.core.metadata.Person;
import dk.i2m.converge.core.metadata.PointOfInterest;
import dk.i2m.converge.core.metadata.Subject;
import dk.i2m.converge.core.plugin.WorkflowValidatorException;
import dk.i2m.converge.core.utils.HttpUtils;
import dk.i2m.converge.core.workflow.EditionCandidate;
import dk.i2m.converge.domain.search.SearchResults;
import dk.i2m.converge.core.workflow.WorkflowStateTransition;
import dk.i2m.converge.core.workflow.WorkflowStep;
import dk.i2m.converge.core.workflow.WorkflowStepValidator;
import dk.i2m.converge.ejb.facades.LockingException;
import dk.i2m.converge.ejb.facades.CatalogueFacadeLocal;
import dk.i2m.converge.ejb.facades.MetaDataFacadeLocal;
import dk.i2m.converge.ejb.facades.NewsItemFacadeLocal;
import dk.i2m.converge.ejb.facades.NewsItemHolder;
import dk.i2m.converge.ejb.facades.OutletFacadeLocal;
import dk.i2m.converge.ejb.facades.SearchEngineLocal;
import dk.i2m.converge.ejb.facades.UserFacadeLocal;
import dk.i2m.converge.ejb.facades.WorkflowStateTransitionException;
import dk.i2m.converge.ejb.services.DataNotFoundException;
import dk.i2m.converge.ejb.services.MetaDataServiceLocal;
import dk.i2m.jsf.JsfUtils;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import org.apache.commons.io.FilenameUtils;
import org.richfaces.component.html.HtmlTree;
import org.richfaces.event.NodeSelectedEvent;
import org.richfaces.event.UploadEvent;

/**
 * Managed backing bean for {@code /NewsItem.jspx}. The backing bean is kept
 * alive by the JSF file. Loading a news item is done by setting the ID of the
 * item using {@link NewsItem#setId(java.lang.Long)}.
 *
 * @author Allan Lykke Christensen
 */
public class NewsItem {

    private static final Logger LOG = Logger.getLogger(NewsItem.class.getName());

    @EJB private CatalogueFacadeLocal catalogueFacade;

    @EJB private NewsItemFacadeLocal newsItemFacade;

    @EJB private MetaDataFacadeLocal metaDataFacade;

    @EJB private MetaDataServiceLocal metaDataService;

    @EJB private OutletFacadeLocal outletFacade;

    @EJB private UserFacadeLocal userFacade;

    @EJB private SearchEngineLocal searchEngine;

    private dk.i2m.converge.core.content.NewsItem selectedNewsItem = null;

    private WorkflowStep selectedStep = null;

    private WorkflowStateTransition selectedWorkflowStateTransition;

    private NewsItemActor selectedActor;

    private NewsItemActor newActor = new NewsItemActor();

    private Concept selectedMetaDataConcept;

    private Long id = 0L;

    private DataModel versions = new ListDataModel();

    private boolean validWorkflowStep = false;

    private String comment = "";

    private boolean readOnly = false;

    private boolean pullbackAvailable = false;

    private ContentItemPermission permission;

    private NewsItemMediaAttachment selectedAttachment;

    private String keyword = "";

    private DataModel searchResults = new ListDataModel();

    private Long selectedMediaItemId = null;

    private NewsItemMediaAttachment deleteMediaItem = null;

    private String newConcept = "";

    /** Dev Note: Could not use a Concept object for direct entry as it is abstract. */
    private String newConceptName = "";

    /** Dev Note: Could not use a Concept object for direct entry as it is abstract. */
    private String newConceptDescription = "";

    /** Dev Note: Could not use a Concept object for direct entry as it is abstract. */
    private String conceptType = "";

    private boolean conceptAdded = false;

    private Map<String, Boolean> fieldVisibible = new HashMap<String, Boolean>();

    private Date editionDate;

    private NewsItemPlacement selectedNewsItemPlacement;

    private EditionCandidate editionCandidate;

    private Map<String, EditionCandidate> editionCandidates = new LinkedHashMap<String, EditionCandidate>();

    private Catalogue selectedCatalogue = null;

    private MediaItem userSubmission = null;

    private boolean showClosedEditions = false;

    /**
     * Creates a new instance of {@link NewsItem}.
     */
    public NewsItem() {
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

    public void onAddConcept(ActionEvent event) {
        try {
            Concept concept = metaDataFacade.findConceptByName(newConcept);
            if (!this.selectedNewsItem.getConcepts().contains(concept)) {
                this.selectedNewsItem.getConcepts().add(concept);
                JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO, false, concept.getFullTitle() + " has been added to " + selectedNewsItem.getTitle(), null);
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
            if (!this.selectedNewsItem.getConcepts().contains(c)) {
                this.selectedNewsItem.getConcepts().add(c);
                JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO, false, c.getFullTitle() + " has been added to " + selectedNewsItem.getTitle(), null);
            }
            this.newConcept = "";
        }
    }

    public void onSelectSubject(NodeSelectedEvent event) {
        HtmlTree tree = (HtmlTree) event.getComponent();
        Subject subj = (Subject) tree.getRowData();
        this.selectedNewsItem.getConcepts().add(subj);
        JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO, false, subj.getFullTitle() + " has been added to " + selectedNewsItem.getTitle(), null);
    }

    /**
     * Gets the unique identifier of the loaded news item.
     *
     * @return Unique identifier of the loaded news item
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the id of the news item to load. Upon setting the identifier, the
     * news item will be checked-out from the database.
     *
     * @param id
     *          Unique identifier of the news item to load
     */
    public void setId(Long id) {
        LOG.log(Level.FINE, "Setting News Item #{0}", id);
        this.id = id;

        if (id == null) {
            return;
        }

        if (selectedNewsItem == null || (selectedNewsItem.getId() != id)) {
            String username = getUser().getUsername();
            LOG.log(Level.FINE, "Checking if {0} is permitted to open news item #{1}", new Object[]{username, id});

            NewsItemHolder nih;
            try {
                nih = newsItemFacade.checkout(id);
                this.permission = nih.getPermission();
                this.readOnly = nih.isReadOnly();
                this.selectedNewsItem = nih.getNewsItem();
//                if (selectedNewsItem.getEdition() != null && selectedNewsItem.getEdition().getPublicationDate() != null) {
//                    setEditionDate(this.selectedNewsItem.getEdition().getPublicationDate().getTime());
//                }
                this.pullbackAvailable = nih.isPullbackAvailable();
                this.fieldVisibible = nih.getFieldVisibility();
                this.versions = new ListDataModel(nih.getVersions());
                if (!nih.isCheckedOut() && (this.permission == ContentItemPermission.USER || this.permission == ContentItemPermission.ROLE)) {
                    JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_ERROR, "newsitem_MSG_OPEN_READ_ONLY", new Object[]{selectedNewsItem.getCheckedOutBy().getFullName()});
                }
            } catch (DataNotFoundException ex) {
                this.permission = ContentItemPermission.UNAUTHORIZED;
                this.readOnly = true;
                this.selectedNewsItem = null;
                this.pullbackAvailable = false;
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
    public boolean isNewsItemLoaded() {
        if (getSelectedNewsItem() == null) {
            return false;
        } else {
            return true;
        }
    }

    public void setSelectedNewsItem(dk.i2m.converge.core.content.NewsItem newsItem) {
        this.selectedNewsItem = newsItem;
    }

    public dk.i2m.converge.core.content.NewsItem getSelectedNewsItem() {
        return selectedNewsItem;
    }

    /**
     * Gets a {@link Map} containing the visibility indicators for each field.
     * The Map key is the name of the news item field,
     * corresponding to the full name of a {@link NewsItemField}.
     *
     * @return Visibility indicators for the news item fields
     */
    public Map<String, Boolean> getFieldVisible() {
        return this.fieldVisibible;
    }

    public String onSubmit() {
        if (getSelectedStep() == null) {
            try {
                selectedNewsItem = newsItemFacade.checkin(selectedNewsItem);
            } catch (LockingException ex) {
                JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_ERROR, false, "Could not save story. The story was locked by another user", null);
                return null;
            }
            return "/inbox";
        } else {
            try {
                selectedNewsItem = newsItemFacade.step(selectedNewsItem, selectedStep.getId(), getComment());
                comment = "";
                return "/inbox";
            } catch (WorkflowStateTransitionException ex) {
                JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO, false, ex.getMessage(), null);
                return null;
            }
        }
    }

    public void onApply(ActionEvent event) {
        try {
            selectedNewsItem = newsItemFacade.save(selectedNewsItem);
            JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO, false, "Your changes were saved. If you want to pass on the assignment to the next person in the workflow, select the appropriate option and click 'Submit'", null);
        } catch (LockingException ex) {
            LOG.log(Level.INFO, ex.getMessage());
            LOG.log(Level.FINE, "", ex);
            JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_ERROR, false, "Could not save story. The story was locked by another user", null);

        }
    }

    /**
     * Event handler invoked upon executing the periodic auto-save
     * poller.
     * 
     * @param event 
     *          Event that invoked the auto-save
     */
    public void onAutoSave(ActionEvent event) {
        try {
            selectedNewsItem = newsItemFacade.save(selectedNewsItem);
        } catch (LockingException ex) {
            JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_ERROR, false, "Could not auto-save story. The story was locked by another user", null);
        }
    }

    public String onSave() {
        try {
            selectedNewsItem = newsItemFacade.checkin(selectedNewsItem);
            JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO, false, "The assignment was saved", null);
        } catch (LockingException ex) {
            JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_ERROR, false, "Could not save story. The story was locked by another user", null);
        }
        return "/inbox";
    }

    public String onClose() {
        newsItemFacade.revokeLock(selectedNewsItem.getId());
        return "/inbox";
    }

    public void onPullback(ActionEvent event) {
        try {
            newsItemFacade.pullback(selectedNewsItem.getId());
            Long theId = selectedNewsItem.getId();
            selectedNewsItem = null;
            setId(theId);
        } catch (LockingException ex) {
            JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_ERROR, false, "Could not pullback story. The story was locked by another user", null);
        } catch (WorkflowStateTransitionException ex) {
            LOG.log(Level.SEVERE, "", ex);
            JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_ERROR, false, "Could not pullback story. Contact IT department", null);
        }
    }

    public void onActorSelect(ActionEvent event) {
        this.newActor = new NewsItemActor();
    }

    public void setDeleteActor(NewsItemActor actor) {
        if (actor != null) {
            try {
                selectedNewsItem.getActors().remove(actor);
                selectedNewsItem = newsItemFacade.save(selectedNewsItem);
                JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO, false, "The actor has been removed", null);
            } catch (LockingException ex) {
                JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_ERROR, false, ex.getMessage(), null);
            }
        }
    }

    public void onMediaFileUpload(UploadEvent event) {
        if (event == null) {
            return;
        }
        org.richfaces.model.UploadItem item = event.getUploadItem();
        LOG.log(Level.INFO, "Processing media file upload ''{0}'' of content-type ''{1}''", new Object[]{item.getFileName(), item.getContentType()});

        if (item.isTempFile()) {
            java.io.File tempFile = item.getFile();

            try {
                //FileUtils.getBytes(tempFile);
                String fileName = item.getFileName();
                String mime = item.getContentType();

                if (fileName.endsWith("bmp")) {
                    mime = "image/bmp";
                } else if (fileName.endsWith("jpg")) {
                    mime = "image/jpeg";
                } else if (fileName.endsWith("gif")) {
                    mime = "image/gif";
                } else if (fileName.endsWith("png")) {
                    mime = "image/png";
                }

                if (mime.startsWith("image/")) {
                    LOG.log(Level.INFO, "Processing ''{0}'' as photo", new Object[]{item.getFileName()});
                    MediaItem photoItem = new MediaItem();
// TODO: Fix
                    //photoItem.setContentType(mime);
                    //photoItem.setFilename(item.getFileName());
                    photoItem.setDescription("");
                    photoItem.setCreated(java.util.Calendar.getInstance());
                    photoItem.setEditorialNote("");
                    photoItem.setTitle(item.getFileName());
                    photoItem.setUpdated(photoItem.getCreated());
                    photoItem = newsItemFacade.create(photoItem);

                    selectedAttachment = new NewsItemMediaAttachment();
                    selectedAttachment.setCaption("");
                    selectedAttachment.setNewsItem(selectedNewsItem);
                    selectedAttachment.setMediaItem(photoItem);
                    selectedAttachment.setDisplayOrder(selectedNewsItem.getNextAssetAttachmentDisplayOrder());
                    selectedAttachment = newsItemFacade.create(selectedAttachment);
                    try {
                        selectedNewsItem = newsItemFacade.findNewsItemById(selectedNewsItem.getId());
                    } catch (DataNotFoundException ex) {
                        JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_ERROR, false, ex.getMessage(), null);
                    }
                } else {
                    LOG.log(Level.INFO, "Unknown media type (''{1}'') for ''{0}''", new Object[]{item.getFileName(), mime});
                    JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_ERROR, false, "The format of " + item.getFileName() + " was not recognised", null);
                }
            } catch (Exception ex) {
                LOG.severe("Could not read file");
            }
        } else {
            LOG.severe("RichFaces is not set-up to use tempFiles for storing file uploads");
        }

        JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO, "profile_PHOTO_UPDATED_MSG");
    }

    public void setUpdateAttachment(NewsItemMediaAttachment attachment) {
        attachment = newsItemFacade.update(attachment);
        JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO, false, "The attachment was updated", null);
    }

    public void onDeleteSelectedActor(ActionEvent event) {
//        if (selectedActor != null) {
//            try {
//                selectedNewsItem = newsItemFacade.save(selectedNewsItem);
//                selectedNewsItem = newsItemFacade.removeActorFromNewsItem(selectedActor);
//                JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO, false, "The actor has been removed", null);
//            } catch (LockingException ex) {
//                JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_ERROR, false, ex.getMessage(), null);
//            }
//        } else {
//            JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_ERROR, false, "Select actor", null);
//        }
    }

    public void onAddActor(ActionEvent event) {
        if (this.newActor != null && this.newActor.getRole() != null && this.newActor.getUser() != null) {
            boolean dup = false;

            for (NewsItemActor nia : selectedNewsItem.getActors()) {
                if (nia.getRole().equals(this.newActor.getRole()) && nia.getUser().equals(this.newActor.getUser())) {
                    dup = true;
                    JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_ERROR, "newsitem_DUPLICATE_USER_ROLE", new Object[]{this.newActor.getUser().getFullName(), this.newActor.getRole().getName()});
                }
            }

            if (!dup) {

                this.newActor.setNewsItem(selectedNewsItem);
                newsItemFacade.addActorToNewsItem(newActor);
                try {
                    selectedNewsItem = newsItemFacade.findNewsItemById(selectedNewsItem.getId());
                } catch (DataNotFoundException ex) {
                    JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_ERROR, false, ex.getMessage(), null);
                }
            }
            this.newActor = new NewsItemActor();

        } else {
            JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_ERROR, false, "Please select a role and user before clicking 'Add'", null);
        }
    }

    public void onSelectMetaData(ActionEvent event) {
//        selectedNewsItem = newsItemFacade.checkin(selectedNewsItem);
        try {
            selectedNewsItem.getConcepts().add(selectedMetaDataConcept);
            selectedNewsItem = newsItemFacade.save(selectedNewsItem);

            JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO, "newsitem_META_DATA_X_ADDED_TO_ASSIGNMENT", new Object[]{getSelectedMetaDataConcept().getName()});
        } catch (LockingException ex) {
            JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_ERROR, false, ex.getMessage(), null);
        }
    }

    public void onDeleteSelectedConcept(ActionEvent event) {
//        selectedNewsItem = newsItemFacade.checkin(selectedNewsItem);
        try {
            selectedNewsItem.getConcepts().remove(selectedMetaDataConcept);
            selectedNewsItem = newsItemFacade.save(selectedNewsItem);

            JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO, "newsitem_META_DATA_X_REMOVED_FROM_ASSIGNMENT", new Object[]{getSelectedMetaDataConcept().getName()});
        } catch (LockingException ex) {
            JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_ERROR, false, ex.getMessage(), null);
        }
    }

    public void onReplaceHeadline(ActionEvent event) {
        selectedNewsItem.setTitle(selectedWorkflowStateTransition.getHeadlineVersion());
    }

    public void onReplaceBrief(ActionEvent event) {
        selectedNewsItem.setBrief(selectedWorkflowStateTransition.getBriefVersion());
    }

    public void onReplaceStory(ActionEvent event) {
        selectedNewsItem.setStory(selectedWorkflowStateTransition.getStoryVersion());
    }

    /**
     * Handler for validating the selected {@link WorkflowStep}.
     *
     * @param event
     *          Event that invoked the handler
     */
    public void onValidateWorkflowStep(ActionEvent event) {
        this.validWorkflowStep = true;

        if (selectedStep == null) {
            this.validWorkflowStep = false;
            JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_ERROR, "newsitem_VALIDATE_SELECT_OPTION");
        }

        boolean isRoleValidated = false;
        UserRole requiredRole = selectedStep.getToState().getActorRole();
        boolean isUserRole = selectedStep.getToState().isUserPermission();

        if (isUserRole) {
            for (NewsItemActor actor : selectedNewsItem.getActors()) {
                if (actor.getRole().equals(requiredRole)) {
                    // User role was already added
                    isRoleValidated = true;
                    break;
                }
            }
        } else {
            isRoleValidated = true;
        }

        if (!isRoleValidated) {
            this.validWorkflowStep = false;
            JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_ERROR, "newsitem_VALIDATE_MISSING_ROLE", requiredRole.getName());
        }


        for (WorkflowStepValidator validator : selectedStep.getValidators()) {
            try {
                dk.i2m.converge.core.plugin.WorkflowValidator workflowValidator = validator.getValidator();
                workflowValidator.execute(selectedNewsItem, selectedStep, validator);
            } catch (WorkflowValidatorException ex) {
                JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_ERROR, false, ex.getMessage(), new Object[]{});
                this.validWorkflowStep = false;
            }
        }
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public NewsItemActor getSelectedActor() {
        return selectedActor;
    }

    public void setSelectedActor(NewsItemActor selectedActor) {
        this.selectedActor = selectedActor;
    }

    public NewsItemActor getNewActor() {
        return newActor;
    }

    public void setNewActor(NewsItemActor newActor) {
        this.newActor = newActor;
    }

    public Map<String, UserRole> getOutletRoles() {
        Map<String, UserRole> roles = new LinkedHashMap<String, UserRole>();

        for (UserRole role : selectedNewsItem.getOutlet().getRoles()) {
            roles.put(role.getName(), role);
        }
        return roles;
    }

    public Map<String, UserAccount> getRoleUsers() {
        Map<String, UserAccount> users = new LinkedHashMap<String, UserAccount>();

        if (newActor != null && newActor.getRole() != null) {
            List<UserAccount> accounts = userFacade.getMembers(newActor.getRole());
            Collections.sort(accounts, new BeanComparator("fullName"));

            for (UserAccount user : accounts) {
                users.put(user.getFullName(), user);
            }
        }


        return users;
    }

    public DataModel getMetaDataSubjects() {
        return new ListDataModel(metaDataFacade.findConceptByType(Subject.class));
    }

    public DataModel getMetaDataOrganisations() {
        return new ListDataModel(metaDataFacade.findConceptByType(Organisation.class));
    }

    public DataModel getMetaDataLocations() {
        return new ListDataModel(metaDataFacade.findConceptByType(GeoArea.class));
    }

    public DataModel getMetaDataPointsOfInterest() {
        return new ListDataModel(metaDataFacade.findConceptByType(PointOfInterest.class));
    }

    public DataModel getMetaDataPersons() {
        return new ListDataModel(metaDataFacade.findConceptByType(Person.class));
    }

    public Concept getSelectedMetaDataConcept() {
        return selectedMetaDataConcept;
    }

    public void setSelectedMetaDataConcept(Concept selectedMetaDataConcept) {
        this.selectedMetaDataConcept = selectedMetaDataConcept;
    }

    public DataModel getVersions() {
        return versions;
    }

    public void setVersions(DataModel versions) {
        this.versions = versions;
    }

    public WorkflowStateTransition getSelectedWorkflowStateTransition() {
        return selectedWorkflowStateTransition;
    }

    public void setSelectedWorkflowStateTransition(WorkflowStateTransition selectedWorkflowStateTransition) {
        this.selectedWorkflowStateTransition = selectedWorkflowStateTransition;
    }

    /**
     * Gets a {@link Map} of available {@link WorkflowStep}s from the current
     * state of the {@link NewsItem}.
     *
     * @return {@link Map} of available {@link WorkflowStep}s from the current
     *         state of the {@link NewsItem}
     */
    public Map<String, WorkflowStep> getAvailableWorkflowSteps() {
        Map<String, WorkflowStep> steps = new LinkedHashMap<String, WorkflowStep>();

        for (WorkflowStep step : getSelectedNewsItem().getCurrentState().getNextStates()) {
            boolean isValidForUser = !Collections.disjoint(step.getValidFor(), getUser().getUserRoles());
            if (step.isValidForAll() || isValidForUser) {
                steps.put(step.getName(), step);
            }
        }

        return steps;
    }

    /**
     * Gets the selected {@link WorkflowStep}.
     *
     * @return {@link WorkflowStep} selected
     */
    public WorkflowStep getSelectedStep() {
        return selectedStep;
    }

    /**
     * Sets the selected {@link WorkflowStep}. Upon selecting a
     * {@link WorkflowStep}, validation will occur.
     *
     * @param selectedStep
     *          {@link WorkflowStep} selected
     */
    public void setSelectedStep(WorkflowStep selectedStep) {
        this.selectedStep = selectedStep;
    }

    public NewsItemMediaAttachment getSelectedAttachment() {
        return selectedAttachment;
    }

    public void setSelectedAttachment(NewsItemMediaAttachment selectedAttachment) {
        this.selectedAttachment = selectedAttachment;
    }

    /**
     * Determines if the selected {@link WorkflowStep} is valid.
     *
     * @return {@code true} if the selected {@link WorkflowStep} is valid,
     *         otherwise {@code false}
     */
    public boolean isValidWorkflowStep() {
        return validWorkflowStep;
    }

    /**
     * Determines if the current user is the current actor of the
     * {@link NewsItem}.
     *
     * @return {@code true} if the current user is among the current actors of
     *         the {@link NewsItem}, otherwise {@code false}
     */
    public boolean isCurrentActor() {
        boolean currentActor = false;
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

    public boolean isPullbackAvailable() {
        return this.pullbackAvailable;
    }

    public boolean isEditable() {
        return isCurrentActor() && !isReadOnly();
    }

    /**
     * Determines if the news item is locked and thereby read-only or if the
     * user is a part of the actors but not the current actor.
     *
     * @return {@code true} if the news item is locked, otherwise {@code false}
     */
    public boolean isReadOnly() {
        return readOnly || (ContentItemPermission.ACTOR == permission);
    }

    public Map<String, Edition> getOpenEditions() {
        Map<String, Edition> editions = new LinkedHashMap<String, Edition>();
        if (selectedNewsItem.getOutlet() != null) {
            for (Edition edition : outletFacade.findEditionsByStatus(true, selectedNewsItem.getOutlet())) {
                editions.put(edition.getFriendlyName(), edition);
            }
        }
        return editions;
    }

    /**
     * Gets a {@link Map} of active sections for the {@link Outlet}
     * in the selected {@link NewsItemPlacement}.
     * 
     * @return {@link Map} of active sections
     */
    public Map<String, Section> getSections() {
        Map<String, Section> sections = new LinkedHashMap<String, Section>();
        for (Section section : selectedNewsItemPlacement.getOutlet().getActiveSections()) {
            sections.put(section.getFullName(), section);
        }
        return sections;
    }

    private UserAccount getUser() {
        return (UserAccount) JsfUtils.getValueOfValueExpression("#{userSession.user}");
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public DataModel getSearchResults() {
        return searchResults;
    }

    public void setSearchResults(DataModel searchResults) {
        this.searchResults = searchResults;
    }

    private SearchResults lastSearch = new SearchResults();

    public SearchResults getLastSearch() {
        return lastSearch;
    }

    /**
     * Event handler for starting the search.
     *
     * @param event
     *          Event that invoked the handler
     */
    public void onSearch(ActionEvent event) {
        if (!getKeyword().trim().isEmpty()) {
            lastSearch = searchEngine.search(getKeyword(), 0, 50, "type:Media");
            List<SearchResult> results = lastSearch.getHits();

            List<SearchResult> realResults = new ArrayList<SearchResult>();
            for (SearchResult result : results) {
                realResults.add(result);
            }

            if (realResults.isEmpty()) {
                searchResults = new ListDataModel(new ArrayList());
                JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO, false, "No results found with the query: {0}", getKeyword());
            } else {

                for (SearchResult hit : realResults) {
                    hit.setLink(MessageFormat.format(hit.getLink(), new Object[]{JsfUtils.getValueOfValueExpression("#{facesContext.externalContext.request.contextPath}")}));
                }
                searchResults = new ListDataModel(realResults);
            }
        }
    }

    public Long getSelectedMediaItemId() {
        return selectedMediaItemId;
    }

    public void setSelectedMediaItemId(Long selectedMediaItemId) {

        this.selectedMediaItemId = selectedMediaItemId;

        if (this.selectedMediaItemId != null) {
            this.selectedAttachment = new NewsItemMediaAttachment();
            this.selectedAttachment.setNewsItem(selectedNewsItem);

            // TODO: If an item has been indexed in the search engine, but removed from the database, this will throw a DataNotFoundException
            try {
                this.selectedAttachment.setMediaItem(catalogueFacade.findMediaItemById(this.selectedMediaItemId));
                this.selectedAttachment.setCaption(this.selectedAttachment.getMediaItem().getDescription());
            } catch (DataNotFoundException ex) {
                LOG.log(Level.SEVERE, ex.getMessage());
            }
        }
    }

    public void onUseAttachment(ActionEvent event) {
        this.selectedAttachment.setDisplayOrder(this.selectedNewsItem.getNextAssetAttachmentDisplayOrder());
        this.selectedAttachment = newsItemFacade.create(selectedAttachment);
        this.selectedNewsItem.getMediaAttachments().add(selectedAttachment);

        // Update caption in MediaItem
        this.userSubmission.setDescription(selectedAttachment.getCaption());
        this.userSubmission = catalogueFacade.update(userSubmission);

        JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO, "ASSET_ATTACHED_TO_STORY");
        onPreAttachMediaFile(event);
    }

    public void onUpdateAttachment(ActionEvent event) {
        selectedNewsItem.getMediaAttachments().remove(selectedAttachment);
        selectedAttachment = newsItemFacade.update(selectedAttachment);
        selectedNewsItem.getMediaAttachments().add(selectedAttachment);
        JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO, false, "The attachment was updated", null);
    }

    public void onPreAttachMediaFile(ActionEvent event) {
        this.selectedCatalogue = null;
        this.userSubmission = new MediaItem();
        this.userSubmission.setOwner(getUser());
        this.selectedCatalogue = getUser().getDefaultMediaRepository();
        this.selectedAttachment = new NewsItemMediaAttachment();
        this.selectedAttachment.setNewsItem(selectedNewsItem);
        searchResults = new ListDataModel(new ArrayList());
        setKeyword("");
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

    /**
     * Event handler for uploading a new {@link MediaItem}
     * to the {@link dk.i2m.converge.core.content.NewsItem}.
     * 
     * @param event 
     *          Event that invoked the handler
     */
    public void onUploadMediaItem(UploadEvent event) {
        if (event == null) {
            return;
        }

        // Create placeholder (MediaItem)
        userSubmission.setStatus(MediaItemStatus.SELF_UPLOAD);
        userSubmission.setCatalogue(selectedCatalogue);
        if (userSubmission.getId() == null) {
            userSubmission = catalogueFacade.create(userSubmission);
        }

        // Create MediaItemRendition - assume that the uploaded 
        // file is the original rendition.
        org.richfaces.model.UploadItem item = event.getUploadItem();

        if (item.isTempFile()) {
            java.io.File tempFile = item.getFile();

            MediaItemRendition mir = new MediaItemRendition();
            mir.setMediaItem(userSubmission);
            mir.setRendition(selectedCatalogue.getOriginalRendition());
            userSubmission.getRenditions().add(mir);

            String filename = HttpUtils.getFilename(item.getFileName());
            try {
                mir = catalogueFacade.create(tempFile, userSubmission, mir.getRendition(), item.getFileName(), item.getContentType());
            } catch (IOException ioex) {
                JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_ERROR, false, "Could not upload file. " + ioex.getMessage(), null);
            }

            try {
                userSubmission = catalogueFacade.findMediaItemById(userSubmission.getId());
            } catch (DataNotFoundException ex) {
                LOG.log(Level.SEVERE, "Could not find MediaItem #{0}", userSubmission.getId());
            }

            Map<String, String> props = new HashMap<String, String>();

            List<DiscoveredProperty> discoProps = new ArrayList<DiscoveredProperty>();
            for (String s : props.keySet()) {
                discoProps.add(new DiscoveredProperty(s, props.get(s)));
            }

            if (props.containsKey("headline")) {
                userSubmission.setTitle(props.get("headline"));
            }

            if (userSubmission.getTitle().trim().isEmpty()) {
                userSubmission.setTitle(FilenameUtils.getBaseName(filename));
            }

            if (props.containsKey("description")) {
                userSubmission.setDescription(props.get("description"));
            }

//                discovered = new ListDataModel(discoProps);

        } else {
            LOG.log(Level.SEVERE, "RichFaces is not set-up to use tempFiles for storing file uploads");
        }

        userSubmission = catalogueFacade.update(userSubmission);

        JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO, false, "Media item uploaded", null);
    }

    public NewsItemMediaAttachment getDeleteMediaItem() {
        return deleteMediaItem;
    }

    public void setDeleteMediaItem(NewsItemMediaAttachment deleteMediaItem) {
        this.deleteMediaItem = deleteMediaItem;

        if (this.deleteMediaItem != null) {
            newsItemFacade.deleteMediaAttachmentById(this.deleteMediaItem.getId());
            this.selectedNewsItem.getMediaAttachments().remove(this.deleteMediaItem);
        }
    }

    public String getConceptType() {
        return conceptType;
    }

    public void setConceptType(String conceptType) {
        this.conceptType = conceptType;
    }

    public String getNewConcept() {
        return newConcept;
    }

    public void setNewConcept(String newConcept) {
        this.newConcept = newConcept;
    }

    public String getNewConceptDescription() {
        return newConceptDescription;
    }

    public void setNewConceptDescription(String newConceptDescription) {
        this.newConceptDescription = newConceptDescription;
    }

    public String getNewConceptName() {
        return newConceptName;
    }

    public void setNewConceptName(String newConceptName) {
        this.newConceptName = newConceptName;
    }

    public boolean isConceptAdded() {
        return conceptAdded;
    }

    public void setRemoveConcept(Concept concept) {
        this.selectedNewsItem.getConcepts().remove(concept);
    }

    public Date getEditionDate() {
        return editionDate;
    }

    public void setEditionDate(Date editionDate) {
        this.editionDate = editionDate;

        this.editionCandidates = new LinkedHashMap<String, EditionCandidate>();

        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm zzz");

        if (editionDate != null) {

            java.util.Calendar editionCal = java.util.Calendar.getInstance();
//            if (getUser().getTimeZone() != null) {
//                editionCal.setTimeZone(getUser().getTimeZone());
//            }
            editionCal.setTime(editionDate);

            List<EditionCandidate> editions = outletFacade.findEditionCandidatesByDate(getSelectedNewsItemPlacement().getOutlet(), editionCal, showClosedEditions);
            Collections.sort(editions, new BeanComparator("publicationDate"));
            for (EditionCandidate e : editions) {
                String label = "";
                if (e.getPublicationDate() != null) {
                    label = formatter.format(e.getPublicationDate().getTime());
                }
                this.editionCandidates.put(label, e);
            }
        }
    }

    public void onToggleShowClosedEditions(ActionEvent event) {
        setEditionDate(editionDate);
    }

    public boolean isShowClosedEditions() {
        return showClosedEditions;
    }

    public void setShowClosedEditions(boolean showClosedEditions) {
        this.showClosedEditions = showClosedEditions;
    }

    public void onChangePlacementOutlet(ValueChangeEvent event) {
        this.selectedNewsItemPlacement.setOutlet((Outlet) event.getNewValue());
        setEditionDate(getEditionDate());
    }

    /**
     * Gets a {@link Map} of {@link Edition} candidates based on the selected edition date.
     * 
     * @return {@link Map} of {@link Edition} candidates based on the selected edition date
     */
    public Map<String, EditionCandidate> getEditionCandidates() {
        return this.editionCandidates;
    }

    public void onNewPlacement(ActionEvent event) {
        this.selectedNewsItemPlacement = new NewsItemPlacement();
        this.selectedNewsItemPlacement.setNewsItem(selectedNewsItem);
        if (getUser().getDefaultOutlet() != null) {
            this.selectedNewsItemPlacement.setOutlet(getUser().getDefaultOutlet());
        } else {
            this.selectedNewsItemPlacement.setOutlet(selectedNewsItem.getOutlet());
        }

        if (getUser().getDefaultSection() != null && this.selectedNewsItemPlacement.getOutlet() != null && getUser().getDefaultSection().getOutlet().equals(this.selectedNewsItemPlacement.getOutlet())) {
            this.selectedNewsItemPlacement.setSection(getUser().getDefaultSection());
        }

        this.editionCandidates = new LinkedHashMap<String, EditionCandidate>();
        this.editionCandidate = null;
        this.editionDate = null;
    }

    public void onAddPlacement(ActionEvent event) {

        if (getEditionCandidate() == null) {
            JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_ERROR, "OUTLET_PLACEMENT_SELECT_EDITION");
            return;
        }

        if (getEditionCandidate().isExist()) {
            try {
                selectedNewsItemPlacement.setEdition(outletFacade.findEditionById(getEditionCandidate().getEditionId()));
            } catch (DataNotFoundException ex) {
                LOG.log(Level.INFO, "Edition {0} could not be found in the database", getEditionCandidate().getEditionId());
            }
        } else {
            selectedNewsItemPlacement.setEdition(outletFacade.createEdition(getEditionCandidate()));
        }

        selectedNewsItemPlacement = newsItemFacade.createPlacement(selectedNewsItemPlacement);

        if (!selectedNewsItem.getPlacements().contains(selectedNewsItemPlacement)) {
            selectedNewsItem.getPlacements().add(selectedNewsItemPlacement);
        }
    }

    public void onUpdatePlacement(ActionEvent event) {
        if (getEditionCandidate() == null) {
            JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_ERROR, "OUTLET_PLACEMENT_SELECT_EDITION");
            return;
        }

        if (getEditionCandidate().isExist()) {
            try {
                selectedNewsItemPlacement.setEdition(outletFacade.findEditionById(getEditionCandidate().getEditionId()));
            } catch (DataNotFoundException ex) {
                LOG.log(Level.INFO, "Edition {0} does not exist", getEditionCandidate().getEditionId());
            }
        } else {
            selectedNewsItemPlacement.setEdition(outletFacade.createEdition(getEditionCandidate()));
        }

        selectedNewsItemPlacement = newsItemFacade.updatePlacement(selectedNewsItemPlacement);

        if (!selectedNewsItem.getPlacements().contains(selectedNewsItemPlacement)) {
            selectedNewsItem.getPlacements().add(selectedNewsItemPlacement);
        }
    }

    public void onRemovePlacement(ActionEvent event) {
        if (selectedNewsItem.getPlacements().contains(selectedNewsItemPlacement)) {
            selectedNewsItem.getPlacements().remove(selectedNewsItemPlacement);
        }
        newsItemFacade.deletePlacement(selectedNewsItemPlacement);
    }

    public NewsItemPlacement getSelectedNewsItemPlacement() {
        return selectedNewsItemPlacement;
    }

    public void setSelectedNewsItemPlacement(NewsItemPlacement selectedNewsItemPlacement) {
        this.selectedNewsItemPlacement = selectedNewsItemPlacement;
        if (this.selectedNewsItemPlacement != null && this.selectedNewsItemPlacement.getEdition() != null) {
            this.editionCandidate = new EditionCandidate(this.selectedNewsItemPlacement.getEdition());
        }
    }

    public EditionCandidate getEditionCandidate() {
        return editionCandidate;
    }

    public void setEditionCandidate(EditionCandidate editionCandidate) {
        this.editionCandidate = editionCandidate;
    }

    public Catalogue getSelectedCatalogue() {
        return selectedCatalogue;
    }

    public void setSelectedCatalogue(Catalogue selectedCatalogue) {
        this.selectedCatalogue = selectedCatalogue;
    }

    public MediaItem getUserSubmission() {
        return userSubmission;
    }

    public void setUserSubmission(MediaItem userSubmission) {
        this.userSubmission = userSubmission;
    }

    /**
     * Gets the number of columns to display in the grid of attached media items.
     * 
     * @return Number of columns to display in the media attachment grid
     */
    public int getNumberOfMediaAttachmentsColumns() {
        if (selectedNewsItem == null) {
            return 0;
        } else if (selectedNewsItem.getMediaAttachments().size() < 3) {
            return selectedNewsItem.getMediaAttachments().size();
        } else {
            return 3;
        }
    }
}
