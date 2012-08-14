/*
 * Copyright (C) 2010 - 2012 Interactive Media Management
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later 
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT 
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more 
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package dk.i2m.converge.jsf.beans;

import dk.i2m.converge.core.DataNotFoundException;
import dk.i2m.converge.core.content.AssignmentType;
import dk.i2m.converge.core.content.ContentItem;
import dk.i2m.converge.core.content.ContentItemActor;
import dk.i2m.converge.core.content.ContentItemPermission;
import dk.i2m.converge.core.content.ContentResultSet;
import dk.i2m.converge.core.content.NewsItem;
import dk.i2m.converge.core.content.NewsItemPlacement;
import dk.i2m.converge.core.content.catalogue.Catalogue;
import dk.i2m.converge.core.content.catalogue.MediaItem;
import dk.i2m.converge.core.security.SystemPrivilege;
import dk.i2m.converge.core.security.UserAccount;
import dk.i2m.converge.core.workflow.Outlet;
import dk.i2m.converge.core.workflow.WorkflowState;
import dk.i2m.converge.core.workflow.WorkflowStateTransitionException;
import dk.i2m.converge.ejb.facades.*;
import dk.i2m.converge.jsf.components.tags.DialogSelfAssignment;
import dk.i2m.jsf.CookieNotFoundException;
import static dk.i2m.jsf.JsfUtils.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.event.ActionEvent;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import org.richfaces.component.html.HtmlTree;
import org.richfaces.event.NodeSelectedEvent;
import org.richfaces.model.TreeNode;
import org.richfaces.model.TreeNodeImpl;

/**
 * Backing bean for {@code /Inbox.jspx}.
 *
 * @author Allan Lykke Christensen
 */
public class Inbox {

    private enum ResultType {

        CATALOGUE, CATALOGUE_WITH_STATE, OUTLET, OUTLET_WITH_STATE, MY_ASSIGNMENT
    }
    private static final Logger LOG = Logger.getLogger(Inbox.class.getName());
    private static final int ONE_YEAR = 31276800;
    private static final int PAGE_SIZE = 25;
    @EJB
    private NewsItemFacadeLocal newsItemFacade;
    @EJB
    private CatalogueFacadeLocal catalogueFacade;
    @EJB
    private ContentItemFacadeLocal contentItemFacade;
    /**
     * Placeholder for the selected {@link ContentItem}.
     */
    private ContentItem selectedContentItem;
    /**
     * Placeholder for the permissions for {@link #selectedContentItem}.
     */
    private ContentItemPermission selectedContentItemPermission = ContentItemPermission.UNAUTHORIZED;
    private NewsItem selectedNewsItem;
    private MediaItem selectedMediaItem;
    private ContentResultSet resultSet;
    private DataModel newsItems = null;
    private DataModel mediaItems = null;
    private TreeNode outletsNode = null;
    private TreeNode cataloguesNode = null;
    private String inboxTitle = "";
    private NewsItem duplicateNewsItem;
    private DialogSelfAssignment newAssignment;
    private boolean showNewsItem = true;
    private String newAssignmentType = "tabStory";
    private String createdItemLink;
    private int pagingStart;
    private int pagingRows;
    private String sortBy = "updated";
    private String sortDirection = "desc";
    private ResultType resultType;
    private String contentView;
    private Outlet selectedOutlet;
    private Catalogue selectedCatalogue;
    private WorkflowState selectedWorkflowState;

    /**
     * Creates a new instance of {@link Inbox}.
     */
    public Inbox() {
    }

    @PostConstruct
    public void onInit() {
        resetPaging();
        this.contentView = "list";

        if (fetchSelectedFromCookies()) {
            this.resultType = ResultType.CATALOGUE;
            this.showNewsItem = false;
            fetchViewPreferenceFromCookies();
            loadContentResultSet();
        } else {
            onShowMyAssignments(null);
        }
    }

    /**
     * Action listeners for preparing the creation of a new assignment.
     *
     * @param event {@link ActionEvent} that invoked the listener.
     */
    public void onNewAssignment(ActionEvent event) {

        try {
            switch (getUser().getDefaultAssignmentType()) {
                case MEDIA_ITEM:
                    this.newAssignmentType = "tabMedia";
                    break;
                case NEWS_ITEM:
                    this.newAssignmentType = "tabStory";
                    break;
            }
        } catch (NullPointerException ex) {
            // Default assignment type not set
            this.newAssignmentType = "tabStory";
        }

        newAssignment = new DialogSelfAssignment();

        newAssignment.getAssignment().setDeadline(
                java.util.Calendar.getInstance());
        newAssignment.getAssignment().getDeadline().setTimeZone(getUser().
                getTimeZone());
        if (newAssignment.getAssignment().getDeadline().get(
                java.util.Calendar.HOUR_OF_DAY) >= 15) {
            newAssignment.getAssignment().getDeadline().add(
                    java.util.Calendar.DAY_OF_MONTH, 1);
        }
        newAssignment.getAssignment().getDeadline().set(
                java.util.Calendar.HOUR_OF_DAY, 15);
        newAssignment.getAssignment().getDeadline().set(
                java.util.Calendar.MINUTE, 0);
        newAssignment.getAssignment().getDeadline().set(
                java.util.Calendar.SECOND, 0);
        newAssignment.getNewsItem().setOutlet(getUser().getDefaultOutlet());
        if (newAssignment.getNewsItem().getOutlet() != null) {
            newAssignment.getNewsItem().setLanguage(newAssignment.getNewsItem().
                    getOutlet().getLanguage());
        }
        newAssignment.getAssignment().setType(
                getUser().getDefaultAssignmentType());
        newAssignment.getMediaItem().setCatalogue(getUser().
                getDefaultMediaRepository());
        newAssignment.setNextEdition(getUser().isDefaultAddNextEdition());
    }

    /**
     * Event handler for changing the workflow of the selected
     * {@link ContentItem}.
     *
     * @param event Event that invoked the handler
     */
    public void onContentItemWorkflowSelection(ActionEvent event) {
        String wsi = getRequestParameterMap().get("workflow_step_id");
        if (wsi == null) {
            return;
        }

        Long wsId;
        try {
            wsId = Long.valueOf(wsi);
        } catch (NumberFormatException ex) {
            LOG.log(Level.WARNING, "Invalid WorkflowStep id: {0}", wsi);
            return;
        }
        try {
            catalogueFacade.step((MediaItem) getSelectedContentItem(), wsId, false);
            createMessage("frmInbox", FacesMessage.SEVERITY_INFO,
                    Bundle.i18n.name(), "MediaItemDetails_MEDIA_ITEM_SUBMITTED");
            loadContentResultSet();
        } catch (WorkflowStateTransitionValidationException ex) {
            if (ex.isLocalisedMessage()) {
                createMessage("frmInbox", FacesMessage.SEVERITY_ERROR,
                        Bundle.i18n.name(), ex.getMessage(), ex.
                        getLocalisationParameters());
            } else {
                createMessage("frmInbox", FacesMessage.SEVERITY_ERROR,
                        ex.getMessage());
            }
        } catch (WorkflowStateTransitionException ex) {
            createMessage("frmInbox", FacesMessage.SEVERITY_ERROR, ex.
                    getMessage());
        }
    }

    /**
     * Event handler for creating a new assignment.
     *
     * @param event Event that invoked the handler
     */
    public void onAddAssignment(ActionEvent event) {

        if (newAssignmentType.equalsIgnoreCase("tabStory")) {
            newAssignment.getAssignment().setType(AssignmentType.NEWS_ITEM);
        } else {
            newAssignment.getAssignment().setType(AssignmentType.MEDIA_ITEM);
        }

        switch (newAssignment.getAssignment().getType()) {
            case NEWS_ITEM:
                if (newAssignment.getNewsItem().getOutlet() == null) {
                    createMessage("frmInbox",
                            FacesMessage.SEVERITY_ERROR, "i18n",
                            "Inbox_NEWS_ITEM_OUTLET_REQUIRED", new Object[]{});
                    return;
                }

                try {
                    selectedNewsItem = newAssignment.getNewsItem();
                    ContentItemActor nia = new ContentItemActor();
                    nia.setRole(selectedNewsItem.getOutlet().getWorkflow().
                            getStartState().getActorRole());
                    nia.setUser(getUser());
                    nia.setContentItem(selectedNewsItem);
                    selectedNewsItem.getActors().add(nia);
                    selectedNewsItem.setDeadline(newAssignment.getAssignment().
                            getDeadline());

                    if (selectedNewsItem.getOutlet() != null) {
                        selectedNewsItem.setLanguage(selectedNewsItem.getOutlet().
                                getLanguage());
                    }
                    selectedNewsItem.setTitle(newAssignment.getTitle());

                    selectedNewsItem = newsItemFacade.start(selectedNewsItem);
                    this.createdItemLink = "NewsItem.xhtml?id="
                            + selectedNewsItem.getId();

                    if (newAssignment.isNextEdition()) {
                        try {
                            NewsItemPlacement placement = newsItemFacade.
                                    addToNextEdition(selectedNewsItem, getUser().
                                    getDefaultSection());
                            selectedNewsItem = placement.getNewsItem();
                        } catch (DataNotFoundException ex) {
                            LOG.log(Level.INFO, "Could not find next edition");
                        }
                    }

                    createMessage("frmInbox",
                            FacesMessage.SEVERITY_INFO, Bundle.i18n.name(),
                            "Inbox_ASSIGNMENT_CREATED");
                } catch (DuplicateExecutionException ex) {
                    // Double click prevention - stamp in log
                    LOG.log(Level.INFO, ex.getMessage());
                } catch (WorkflowStateTransitionException ex) {
                    createMessage("frmInbox",
                            FacesMessage.SEVERITY_ERROR, Bundle.i18n.name(),
                            "Inbox_ASSIGNMENT_CREATION_ERROR");
                    LOG.log(Level.SEVERE, ex.getMessage(), ex);
                }
                break;

            case MEDIA_ITEM:
                if (newAssignment.getMediaItem().getCatalogue() == null) {
                    createMessage("frmInbox",
                            FacesMessage.SEVERITY_ERROR, Bundle.i18n.name(),
                            "Inbox_MEDIA_ITEM_CATELOGUE_REQUIRED");
                    return;
                }

                newAssignment.getMediaItem().setTitle(newAssignment.getTitle());

                MediaItem item = null;
                try {
                    item = catalogueFacade.create(newAssignment.getMediaItem());
                } catch (WorkflowStateTransitionException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                }

                this.createdItemLink = "MediaItemDetails.xhtml?id="
                        + item.getId();
                createMessage("frmInbox", FacesMessage.SEVERITY_INFO,
                        Bundle.i18n.name(), "Inbox_ASSIGNMENT_CREATED");
                showNewsItem = false;
                break;
        }
    }

    /**
     * Action Listener for removing articles marked as deleted.
     *
     * @param event {@link ActionEvent} that invoked the listener
     */
    public void onEmptyTrash(ActionEvent event) {
        int deleted = newsItemFacade.emptyTrash(getUser().getUsername());
        onShowMyAssignments(event);

        createMessage("frmPage", FacesMessage.SEVERITY_INFO,
                Bundle.i18n.name(), "Inbox_X_ITEMS_DELETED", new Object[]{
                    deleted});
    }

    /**
     * Event handler for showing the current assignments of the user.
     *
     * @param event Event that invoked the handler
     */
    public void onShowMyAssignments(ActionEvent event) {
        resetSelectedCookies();
        resetPaging();

        showNewsItem = true;
        
        this.resultType = ResultType.MY_ASSIGNMENT;
        loadContentResultSet();
        fetchViewPreferenceFromCookies();
        storeSelectedInCookies();
    }

    /**
     * Event handler for handling selection of an {@link Outlet} or
     * {@link Catalogue} folder.
     *
     * @param event Event that invoked the handler
     */
    public void onOutletFolderSelect(NodeSelectedEvent event) {
        LOG.log(Level.FINE, "Selecting outlet folder");

        this.showNewsItem = true;
        this.newsItems = new ListDataModel();

        HtmlTree tree = (HtmlTree) event.getComponent();
        OutletNode node = (OutletNode) tree.getRowData();

        resetPaging();

        if (node.getData() instanceof Outlet) {
            this.resultType = ResultType.OUTLET;
            this.selectedOutlet = (Outlet) node.getData();
        } else if (node.getData() instanceof WorkflowState) {
            this.resultType = ResultType.OUTLET_WITH_STATE;
            this.selectedOutlet = (Outlet) node.getParentData();
            this.selectedWorkflowState = (WorkflowState) node.getData();
        }
        loadContentResultSet();
        fetchViewPreferenceFromCookies();
        storeSelectedInCookies();
    }

    /**
     * Event handler for handling selection of a {@link Catalogue} folder.
     *
     * @param event Event that invoked the handler
     */
    public void onCatalogueFolderSelect(NodeSelectedEvent event) {
        this.showNewsItem = false;
        this.newsItems = new ListDataModel();

        HtmlTree tree = (HtmlTree) event.getComponent();
        OutletNode node = (OutletNode) tree.getRowData();

        resetPaging();

        if (node.getData() instanceof Catalogue) {
            this.resultType = ResultType.CATALOGUE;
            this.selectedCatalogue = (Catalogue) node.getData();
        } else if (node.getData() instanceof WorkflowState) {
            this.resultType = ResultType.CATALOGUE_WITH_STATE;
            this.selectedCatalogue = (Catalogue) node.getParentData();
            this.selectedWorkflowState = (WorkflowState) node.getData();
        }
        loadContentResultSet();
        fetchViewPreferenceFromCookies();
        storeSelectedInCookies();
    }

    /**
     * Event handler for changing the view of the {@link ContentResultSet}.
     *
     * @param event Event that invoked the handler
     */
    public void onChangeView(ActionEvent event) {
        this.contentView = getRequestParameterMap().get("view");
        storeViewPreferenceInCookies();
    }

    /**
     * Event handler for changing the page of the current
     * {@link ContentResultSet}.
     *
     * @param event Event that invoked the handler
     */
    public void onChangePage(ActionEvent event) {
        String changePage = getRequestParameterMap().get("changePage");
        if (changePage != null) {
            int newPage = Integer.valueOf(changePage);
            this.pagingStart = (newPage - 1)
                    * getResultSet().getResultsPerPage();
            this.pagingRows = getResultSet().getResultsPerPage();
            loadContentResultSet();
        }
    }

    /**
     * Event handler for changing the sort order and direction of
     * {@link ContentResultSet}.
     *
     * @param event Event that invoked the handler
     */
    public void onChangeSorting(ActionEvent event) {
        String newSortBy = getRequestParameterMap().get("sortField");
        if (this.sortBy.equalsIgnoreCase(newSortBy)) {
            if (this.sortDirection.equalsIgnoreCase("desc")) {
                this.sortDirection = "asc";
            } else {
                this.sortDirection = "desc";
            }
        } else {
            this.sortBy = newSortBy;
            this.sortDirection = "asc";
        }
        loadContentResultSet();
    }

    /**
     * Gets the title of the Inbox. The title changes depending on the selected
     * folder/state.
     *
     * @return Title of the inbox
     */
    public String getInboxTitle() {
        return getMessage(Bundle.i18n.name(), "Inbox_INBOX_X",
                new Object[]{inboxTitle});
    }

    public String getNewAssignmentType() {
        return newAssignmentType;
    }

    public void setNewAssignmentType(String newAssignmentType) {
        this.newAssignmentType = newAssignmentType;
    }

    /**
     * Gets a {@link DataModel} containing the user's {@link NewsItem}s.
     *
     * @return {@link DataModel} containing the user's {@link NewsItem}s
     */
    public DataModel getNewsItems() {
        if (newsItems == null) {
            newsItems = new ListDataModel(new ArrayList());
        }
        return newsItems;
    }

    public DataModel getMediaItems() {
        if (mediaItems == null) {
            mediaItems = new ListDataModel(new ArrayList());
        }
        return mediaItems;
    }

    public String getCreatedItemLink() {
        return this.createdItemLink;
    }

    /**
     * Property containing the selected {@link MediaItem}.
     *
     * @return Selected {@link MediaItem} or {@code null} if no item was
     * selected
     */
    public MediaItem getSelectedMediaItem() {
        return selectedMediaItem;
    }

    /**
     * Property containing the selected {@link MediaItem}.
     *
     * @param selectedMediaItem Selected {@link MediaItem} or {@code null} if no
     * item was selected
     */
    public void setSelectedMediaItem(MediaItem selectedMediaItem) {
        this.selectedMediaItem = selectedMediaItem;
    }

    public void setContentMediaItem(Long id) throws DataNotFoundException {
        this.selectedMediaItem = catalogueFacade.findMediaItemById(id);
    }

    public void setContentNewsItem(Long id) throws DataNotFoundException {
        this.selectedNewsItem = newsItemFacade.findNewsItemById(id);
    }

    public NewsItem getSelectedNewsItem() {
        return selectedNewsItem;
    }

    public void setSelectedNewsItem(NewsItem selectedNewsItem) {
        this.selectedNewsItem = selectedNewsItem;
    }

    public Map<String, Outlet> getPrivilegedOutlets() {
        Map<String, Outlet> outlets = new LinkedHashMap<String, Outlet>();

        for (Outlet outlet : getUser().getPrivilegedOutlets(
                SystemPrivilege.MY_NEWS_ITEMS)) {
            outlets.put(outlet.getTitle(), outlet);
        }

        return outlets;
    }

    /**
     * Gets a {@link TreeNode} containing the {@link Outlet}s privileged to the
     * current {@link UserAccount}.
     *
     * @return {@link TreeNode} of privileged {@link Outlet}s
     */
    public TreeNode getOutletsNode() {
        if (outletsNode == null) {
            outletsNode = new TreeNodeImpl();
            List<Outlet> outlets = getUser().getPrivilegedOutlets(
                    SystemPrivilege.MY_NEWS_ITEMS);

            for (Outlet outlet : outlets) {
                if (outlet.isValid()) {
                    TreeNode node = new TreeNodeImpl();
                    node.setData(new OutletNode(outlet, null, outlet.getClass().
                            getName()));

                    List<WorkflowState> states =
                            outlet.getWorkflow().getStates();

                    for (WorkflowState state : states) {
                        TreeNode subNode = new TreeNodeImpl();
                        subNode.setData(new OutletNode(state, outlet, state.
                                getClass().getName()));
                        node.addChild(state.getId(), subNode);
                    }

                    outletsNode.addChild("O" + outlet.getId(), node);
                }
            }
        }

        return outletsNode;
    }

    /**
     * Gets a {@link TreeNode} containing the {@link Catalogue}s privileged to
     * the current {@link UserAccount}.
     *
     * @return {@link TreeNode} of privileged {@link Catalogue}s
     */
    public TreeNode getCataloguesNode() {
        if (this.cataloguesNode == null) {
            this.cataloguesNode = new TreeNodeImpl();

            List<Catalogue> myCatalogues = getUser().getPrivilegedCatalogues();

            for (Catalogue myCatalogue : myCatalogues) {

                // Add top-level catalogue node
                TreeNode node = new TreeNodeImpl();
                node.setData(new OutletNode(myCatalogue, null,
                        myCatalogue.getClass().getName()));

                List<WorkflowState> states =
                        myCatalogue.getWorkflow().getStates();

                // Add children nodes (workflow states) to the catalogue node
                for (WorkflowState state : states) {
                    TreeNode subNode = new TreeNodeImpl();
                    subNode.setData(new OutletNode(state, myCatalogue, state.
                            getClass().getName()));
                    node.addChild(state.getId(), subNode);
                }

                this.cataloguesNode.addChild("M" + myCatalogue.getId(), node);
            }
        }

        return this.cataloguesNode;
    }

    /**
     * Event handler for updating the state of an {@link MediaItem} from the
     * list of {@link MediaItem}s in a {@link Catalogue} folder.
     *
     * @param item {@link MediaItem} to update
     */
    public void setUpdateMediaItem(MediaItem item) {
        // The media item must be removed and added to keep its version 
        // identifier current. If it is not updated in the datamodel, it will 
        // throw an internal server error upon the second update

        // Remove media item from data model
        ((List<MediaItem>) getMediaItems().getWrappedData()).remove(item);

        // Update the media item in the database
        item = catalogueFacade.update(item);

        // Add the media item back in the data model
        ((List<MediaItem>) getMediaItems().getWrappedData()).add(item);
    }

    public NewsItem getDuplicateNewsItem() {
        return duplicateNewsItem;
    }

    public void setDuplicateNewsItem(NewsItem duplicateNewsItem) {
        this.duplicateNewsItem = duplicateNewsItem;

        if (this.duplicateNewsItem != null) {
            onNewAssignment(null);

            newAssignment.getNewsItem().setTitle(
                    getDuplicateNewsItem().getTitle());
            newAssignment.setTitle(getDuplicateNewsItem().getTitle());
            newAssignment.getNewsItem().setBrief(
                    getDuplicateNewsItem().getBrief());
            newAssignment.getNewsItem().setStory(
                    getDuplicateNewsItem().getStory());
            newAssignment.getNewsItem().setVersionOf(getDuplicateNewsItem());
        }
    }

    // -------------------------------------------------------------------------
    // -- PROPERTIES
    // -------------------------------------------------------------------------
    /**
     * Gets the permissions for the selected {@link ContentItem} for the current
     * user.
     *
     * @return Permissions for the selected {@link ContentItem}
     */
    public ContentItemPermission getSelectedContentItemPermission() {
        return selectedContentItemPermission;
    }

    /**
     * Determines if the user is among the current users of the selected
     * {@link ContentItem}.
     *
     * @return {@code true} if the user is among the current users of the
     * selected {@link ContentItem}
     */
    public boolean isCurrentUserOfSelectedContentItem() {
        switch (this.selectedContentItemPermission) {
            case USER:
            case ROLE:
                return true;
            default:
                return false;
        }
    }

    /**
     * Gets the selected {@link ContentItem}.
     *
     * @return Selected {@link ContentItem} or {@code null} if a
     * {@link ContentItem} is not selected
     */
    public ContentItem getSelectedContentItem() {
        return selectedContentItem;
    }

    /**
     * Sets the selected {@link ContentItem}.
     *
     * @param selectedContentItem Selected {@link ContentItem} or {@code null}
     * if a {@link ContentItem} is not selected
     */
    public void setSelectedContentItem(ContentItem selectedContentItem) {
        this.selectedContentItem = selectedContentItem;
    }

    /**
     * Sets the ID of the {@link ContentItem} to select. Upon setting the ID the
     * selected {@link ContentItem} will be set.
     *
     * @param id Unique identifier of the {@link ContentItem} to set
     */
    public void setSelectedContentItemId(Long id) {
        try {
            this.selectedContentItem = contentItemFacade.findContentItemById(id);
            this.selectedContentItemPermission = contentItemFacade.findContentItemPermissionById(id, getUser().getUsername());
        } catch (DataNotFoundException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

    public DialogSelfAssignment getNewAssignment() {
        return newAssignment;
    }

    public void setNewAssignment(DialogSelfAssignment newAssignment) {
        this.newAssignment = newAssignment;
    }

    public boolean isShowNewsItem() {
        return showNewsItem;
    }

    public boolean isShowMediaItem() {
        return !showNewsItem;
    }

    public int getPagingRows() {
        return pagingRows;
    }

    public void setPagingRows(int pagingRows) {
        this.pagingRows = pagingRows;
    }

    public int getPagingStart() {
        return pagingStart;
    }

    public void setPagingStart(int pagingStart) {
        this.pagingStart = pagingStart;
    }

    public ContentResultSet getResultSet() {
        return resultSet;
    }

    public void setResultSet(ContentResultSet resultSet) {
        this.resultSet = resultSet;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortDirection() {
        return sortDirection;
    }

    public void setSortDirection(String sortDirection) {
        this.sortDirection = sortDirection;
    }

    public String getContentView() {
        return contentView;
    }

    // -------------------------------------------------------------------------
    // -- HELPERS
    // -------------------------------------------------------------------------
    /**
     * Loads the {@link ContentResultSet} into the view based on the state
     * properties.
     */
    private void loadContentResultSet() {
        switch (this.resultType) {
            case CATALOGUE:
                // Query for items in this catalogue and state
                this.resultSet =
                        catalogueFacade.findMediaItemsByCatalogue(getUser().
                        getUsername(), this.selectedCatalogue, this.pagingStart,
                        this.pagingRows, this.sortBy, this.sortDirection);
                this.mediaItems = new ListDataModel(this.resultSet.getHits());

                // Update inbox title
                this.inboxTitle = getMessage(Bundle.i18n.name(),
                        "Inbox_CATALOGUE_STATUS_CURRENT",
                        new Object[]{this.selectedCatalogue.getName(),
                            this.resultSet.getNumberOfResults(), this.resultSet.
                            getSearchTimeInSeconds()});
                break;
            case CATALOGUE_WITH_STATE:
                this.resultSet =
                        catalogueFacade.findMediaItemsByWorkflowState(getUser().
                        getUsername(), this.selectedCatalogue,
                        this.selectedWorkflowState, this.pagingStart,
                        this.pagingRows, this.sortBy, this.sortDirection);
                this.mediaItems = new ListDataModel(this.resultSet.getHits());
                this.inboxTitle = getMessage(Bundle.i18n.name(),
                        "Inbox_CATALOGUE_STATUS",
                        new Object[]{this.selectedCatalogue.getName(),
                            this.selectedWorkflowState.getName(),
                            this.resultSet.getNumberOfResults(), this.resultSet.
                            getSearchTimeInSeconds()});
                break;
            case OUTLET:
                this.resultSet =
                        newsItemFacade.findNewsItemsByOutlet(
                        getUser().getUsername(), 
                        this.selectedOutlet,
                        this.pagingStart,
                        this.pagingRows, 
                        this.sortBy, 
                        this.sortDirection);
                this.newsItems = new ListDataModel(this.resultSet.getHits());
                        //newsItemFacade.findOutletBox(getUser().getUsername(), this.selectedOutlet));
                this.inboxTitle = getMessage(Bundle.i18n.name(),
                        "Inbox_OUTLET_STATUS_CURRENT",
                        new Object[]{
                            this.selectedOutlet.getTitle(),
                            this.resultSet.getNumberOfResults(),
                            this.resultSet.getSearchTimeInSeconds()});
                break;
            case OUTLET_WITH_STATE:
                this.resultSet =
                        newsItemFacade.findNewsItemsByWorkflowState(
                        getUser().getUsername(), 
                        this.selectedOutlet,
                        this.selectedWorkflowState,
                        this.pagingStart,
                        this.pagingRows, 
                        this.sortBy, 
                        this.sortDirection);
                this.newsItems = new ListDataModel(this.resultSet.getHits());
                        //newsItemFacade.findOutletBox(getUser().getUsername(), this.selectedOutlet));
                this.inboxTitle = getMessage(Bundle.i18n.name(),
                        "Inbox_OUTLET_STATUS",
                        new Object[]{
                            this.selectedOutlet.getTitle(),
                            this.selectedWorkflowState.getName(),
                            this.resultSet.getNumberOfResults(),
                            this.resultSet.getSearchTimeInSeconds()});
                break;
            case MY_ASSIGNMENT:
                this.resultSet = newsItemFacade.findInbox(
                        getUser().getUsername(), 
                        this.pagingStart,
                        this.pagingRows, 
                        this.sortBy, 
                        this.sortDirection);
                this.newsItems = new ListDataModel(this.resultSet.getHits());
                this.inboxTitle = getMessage(Bundle.i18n.name(),
                    "Inbox_MY_ASSIGNMENTS_X_ITEMS", new Object[]{
                        this.resultSet.getNumberOfResults(),
                        this.resultSet.getSearchTimeInSeconds()});
                break;
            default:
                LOG.log(Level.INFO, "Unknown page");
        }
    }

    /**
     * Stores the view preference in the cookies stored in the client browser.
     */
    private void storeViewPreferenceInCookies() {
        addCookie("view-catalogue-" + this.selectedCatalogue.getId(),
                this.contentView, ONE_YEAR);
    }

    /**
     * Store the selected catalogue or outlet + workflow state in the client
     * browser.
     */
    private void storeSelectedInCookies() {
        if (showNewsItem) {
            removeCookie("selected-catalogue");
            removeCookie("selected-catalogue-workflowstate");
        } else {
            removeCookie("selected-outlet");
            removeCookie("selected-outlet-workflowstate");
            if (this.selectedCatalogue != null) {
                addCookie("selected-catalogue", ""
                        + this.selectedCatalogue.getId(), ONE_YEAR);
            }
            if (this.selectedWorkflowState != null) {
                addCookie("selected-catalogue-workflowstate", ""
                        + this.selectedWorkflowState.getId(), ONE_YEAR);
            }
        }
    }

    /**
     * Fetches the view preference from the cookies stored in the client
     * browser.
     */
    private void fetchViewPreferenceFromCookies() {
        try {
            if (this.selectedCatalogue == null) {
                return;
            }
            String catViewCookie = getCookieValue("view-catalogue-"
                    + this.selectedCatalogue.getId());
            if (catViewCookie.equalsIgnoreCase("grid")) {
                this.contentView = "grid";
            } else {
                this.contentView = "list";
            }
        } catch (CookieNotFoundException ex) {
            // View preference for the selected catalogue was not available
        }
    }

    /**
     * Fetches the <em>selected-</em> cookies from the client browser and
     * initialises the necessary properties on the bean.
     *
     * @return {@code true} if preferences were fetched from the client browser,
     * otherwise {@code false}
     */
    private boolean fetchSelectedFromCookies() {
        try {
            String catId = getCookieValue("selected-catalogue");
            this.selectedCatalogue = catalogueFacade.findCatalogueById(Long.
                    valueOf(catId));
            return true;
        } catch (CookieNotFoundException ex) {
            // Selected catalogue cookie was not available
        } catch (DataNotFoundException ex) {
            // Catalogue doesn't exist
        }
        return false;
    }

    /**
     * Reset the <em>selected-</em> cookies.
     */
    private void resetSelectedCookies() {
        removeCookie("selected-catalogue");
        removeCookie("selected-catalogue-workflowstate");
        removeCookie("selected-outlet");
        removeCookie("selected-outlet-workflowstate");
    }

    /**
     * Fetches the current user from the {@code userSession} bean.
     *
     * @return {@link UserAccount} of the current user
     */
    private UserAccount getUser() {
        final String valueExpression = "#{userSession.user}";
        return (UserAccount) getValueOfValueExpression(valueExpression);
    }

    /**
     * Resets the paging panel.
     */
    private void resetPaging() {
        this.pagingRows = PAGE_SIZE;
        this.pagingStart = 0;
    }

    // -------------------------------------------------------------------------
    // -- INNER CLASSES 
    // -- TODO: REFACTOR INTO STAND-ALONE CLASSES
    // -------------------------------------------------------------------------
    public class OutletNode {

        private Object data;
        private Object parentData;
        private String type;

        public OutletNode(Object data, Object parentData, String type) {
            this.data = data;
            this.parentData = parentData;
            this.type = type;
        }

        public Object getParentData() {
            return parentData;
        }

        public void setParentData(Object parentData) {
            this.parentData = parentData;
        }

        public Object getData() {
            return data;
        }

        public void setData(Object data) {
            this.data = data;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final OutletNode other = (OutletNode) obj;
            if (this.data != other.data && (this.data == null || !this.data.
                    equals(other.data))) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 53 * hash + (this.data != null ? this.data.hashCode() : 0);
            return hash;
        }
    }

    public class MediaRepositoryNode {

        private Object data;
        private Object parentData;
        private String type;

        public MediaRepositoryNode(Object data, Object parentData, String type) {
            this.data = data;
            this.parentData = parentData;
            this.type = type;
        }

        public Object getParentData() {
            return parentData;
        }

        public void setParentData(Object parentData) {
            this.parentData = parentData;
        }

        public Object getData() {
            return data;
        }

        public void setData(Object data) {
            this.data = data;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final MediaRepositoryNode other = (MediaRepositoryNode) obj;
            if (this.data != other.data && (this.data == null || !this.data.
                    equals(other.data))) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 53 * hash + (this.data != null ? this.data.hashCode() : 0);
            return hash;
        }
    }
}
