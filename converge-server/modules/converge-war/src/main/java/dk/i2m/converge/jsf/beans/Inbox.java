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
import dk.i2m.converge.core.content.NewsItem;
import dk.i2m.converge.core.content.ContentItemActor;
import dk.i2m.converge.core.content.NewsItemPlacement;
import dk.i2m.converge.core.content.ContentResultSet;
import dk.i2m.converge.core.content.catalogue.Catalogue;
import dk.i2m.converge.core.content.catalogue.MediaItem;
import dk.i2m.converge.core.content.catalogue.MediaItemStatus;
import dk.i2m.converge.core.security.SystemPrivilege;
import dk.i2m.converge.core.security.UserAccount;
import dk.i2m.converge.core.views.InboxView;
import dk.i2m.converge.core.workflow.Outlet;
import dk.i2m.converge.core.workflow.WorkflowState;
import dk.i2m.converge.ejb.facades.CatalogueFacadeLocal;
import dk.i2m.converge.ejb.facades.DuplicateExecutionException;
import dk.i2m.converge.ejb.facades.NewsItemFacadeLocal;
import dk.i2m.converge.ejb.facades.WorkflowStateTransitionException;
import dk.i2m.converge.jsf.components.tags.DialogSelfAssignment;
import dk.i2m.jsf.CookieNotFoundException;
import dk.i2m.jsf.JsfUtils;
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

    private static final int ONE_YEAR = 31276800;

    private static final int PAGE_SIZE = 25;

    private static final Logger LOG = Logger.getLogger(Inbox.class.getName());

    private Catalogue selectedCatalogue;

    private WorkflowState selectedWorkflowState;

    private void resetSelectedCookies() {
        JsfUtils.removeCookie("selected-catalogue");
        JsfUtils.removeCookie("selected-catalogue-workflowstate");
        JsfUtils.removeCookie("selected-outlet");
        JsfUtils.removeCookie("selected-outlet-workflowstate");
    }

    private enum ResultType {

        CATALOGUE, CATALOGUE_WITH_STATE, OUTLET, OUTLET_WITH_STATE
    }

    @EJB private NewsItemFacadeLocal newsItemFacade;

    @EJB private CatalogueFacadeLocal catalogueFacade;

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

    private void resetPaging() {
        this.pagingRows = PAGE_SIZE;
        this.pagingStart = 0;
    }

    /**
     * Action listeners for preparing the creation of a new assignment.
     *
     * @param event
     *          {@link ActionEvent} that invoked the listener.
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
     * Event handler for creating a new assignment.
     * 
     * @param event 
     *          Event that invoked the handler
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
                    JsfUtils.createMessage("frmInbox",
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

                    JsfUtils.createMessage("frmInbox",
                            FacesMessage.SEVERITY_INFO, Bundle.i18n.name(),
                            "Inbox_ASSIGNMENT_CREATED");
                } catch (DuplicateExecutionException ex) {
                    // Double click prevention - stamp in log
                    LOG.log(Level.INFO, ex.getMessage());
                } catch (WorkflowStateTransitionException ex) {
                    JsfUtils.createMessage("frmInbox",
                            FacesMessage.SEVERITY_ERROR, Bundle.i18n.name(),
                            "Inbox_ASSIGNMENT_CREATION_ERROR");
                    LOG.log(Level.SEVERE, ex.getMessage(), ex);
                }
                break;

            case MEDIA_ITEM:
                if (newAssignment.getMediaItem().getCatalogue() == null) {
                    JsfUtils.createMessage("frmInbox",
                            FacesMessage.SEVERITY_ERROR, Bundle.i18n.name(),
                            "Inbox_MEDIA_ITEM_CATELOGUE_REQUIRED");
                    return;
                }

                ContentItemActor nia = new ContentItemActor();
                nia.setRole(newAssignment.getMediaItem().getCatalogue().
                        getWorkflow().getStartState().getActorRole());
                nia.setUser(getUser());
                nia.setContentItem(newAssignment.getMediaItem());
                newAssignment.getMediaItem().getActors().add(nia);

                newAssignment.getMediaItem().setStatus(
                        MediaItemStatus.UNSUBMITTED);
                newAssignment.getMediaItem().setTitle(newAssignment.getTitle());
                newAssignment.getMediaItem().setOwner(getUser());
                newAssignment.getMediaItem().setByLine(getUser().getFullName());

                MediaItem item = null;
                try {
                    item = catalogueFacade.create(newAssignment.getMediaItem());
                } catch (WorkflowStateTransitionException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                }

                this.createdItemLink = "MediaItemDetails.xhtml?id="
                        + item.getId();
                JsfUtils.createMessage("frmInbox", FacesMessage.SEVERITY_INFO,
                        Bundle.i18n.name(), "Inbox_ASSIGNMENT_CREATED");
                showNewsItem = false;
                break;
        }
    }

    /**
     * Action Listener for removing articles marked as deleted.
     *
     * @param event 
     *          {@link ActionEvent} that invoked the listener
     */
    public void onEmptyTrash(ActionEvent event) {
        int deleted = newsItemFacade.emptyTrash(getUser().getUsername());
        onShowMyAssignments(event);

        JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO,
                Bundle.i18n.name(), "Inbox_X_ITEMS_DELETED", new Object[]{
                    deleted});
    }

    /**
     * Event handler for showing the current assignments of the user.
     *
     * @param event
     *          Event that invoked the handler
     */
    public void onShowMyAssignments(ActionEvent event) {
        resetSelectedCookies();

        showNewsItem = true;

        List<InboxView> inboxView = newsItemFacade.findInbox(getUser().
                getUsername());
        this.newsItems = new ListDataModel(inboxView);
        this.inboxTitle = JsfUtils.getMessage(Bundle.i18n.name(),
                "Inbox_MY_ASSIGNMENTS_X_ITEMS", new Object[]{newsItems.
                    getRowCount()});
    }

    /**
     * Event handler for handling selection of an {@link Outlet} or 
     * {@link Catalogue} folder.
     *
     * @param event 
     *          Event that invoked the handler
     */
    public void onOutletFolderSelect(NodeSelectedEvent event) {
        final int MAX_STATE_ITEMS = 100;
        showNewsItem = true;
        mediaItems = new ListDataModel();

        HtmlTree tree = (HtmlTree) event.getComponent();
        OutletNode node = (OutletNode) tree.getRowData();

        if (node.getData() instanceof Outlet) {

            Outlet outlet = (Outlet) node.getData();
            newsItems = new ListDataModel(newsItemFacade.findOutletBox(getUser().
                    getUsername(), outlet));

            inboxTitle = JsfUtils.getMessage(Bundle.i18n.name(),
                    "Inbox_OUTLET_STATUS_CURRENT", new Object[]{
                        outlet.getTitle(),
                        newsItems.getRowCount()});
        } else if (node.getData() instanceof WorkflowState) {
            showNewsItem = true;
            mediaItems = new ListDataModel();
            WorkflowState state = (WorkflowState) node.getData();
            Outlet outlet = (Outlet) node.getParentData();

            if (state.equals(outlet.getWorkflow().getEndState())) {
                newsItems =
                        new ListDataModel(newsItemFacade.findOutletBox(getUser().
                        getUsername(), outlet, state, 0, MAX_STATE_ITEMS));
            } else {
                newsItems =
                        new ListDataModel(newsItemFacade.findOutletBox(getUser().
                        getUsername(), outlet, state));
            }

            inboxTitle = JsfUtils.getMessage(Bundle.i18n.name(),
                    "Inbox_OUTLET_STATUS", new Object[]{outlet.getTitle(),
                        state.getName(), newsItems.getRowCount()});
        }
    }

    /**
     * Event handler for handling selection of a {@link Catalogue} folder.
     *
     * @param event 
     *          Event that invoked the handler
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
     * @param event 
     *          Event that invoked the handler
     */
    public void onChangeView(ActionEvent event) {
        this.contentView = JsfUtils.getRequestParameterMap().get("view");
        storeViewPreferenceInCookies();
    }

    /**
     * Event handler for changing the page of the current 
     * {@link ContentResultSet}.
     * 
     * @param event 
     *          Event that invoked the handler
     */
    public void onChangePage(ActionEvent event) {
        String changePage = JsfUtils.getRequestParameterMap().get("changePage");
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
     * @param event 
     *          Event that invoked the handler
     */
    public void onChangeSorting(ActionEvent event) {
        String newSortBy = JsfUtils.getRequestParameterMap().get("sortField");
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
        return JsfUtils.getMessage(Bundle.i18n.name(), "Inbox_INBOX_X",
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
     *         selected
     */
    public MediaItem getSelectedMediaItem() {
        return selectedMediaItem;
    }

    /**
     * Property containing the selected {@link MediaItem}.
     * 
     * @param selectedMediaItem
     *          Selected {@link MediaItem} or {@code null} if no item was
     *          selected
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
                this.inboxTitle = JsfUtils.getMessage(Bundle.i18n.name(),
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
                this.inboxTitle = JsfUtils.getMessage(Bundle.i18n.name(),
                        "Inbox_CATALOGUE_STATUS",
                        new Object[]{this.selectedCatalogue.getName(),
                            this.selectedWorkflowState.getName(),
                            this.resultSet.getNumberOfResults(), this.resultSet.
                            getSearchTimeInSeconds()});
                break;
            default:
                LOG.log(Level.INFO, "Unknown page");
        }
    }

    /**
     * Event handler for updating the state of an {@link MediaItem} from the
     * list of {@link MediaItem}s in a {@link Catalogue} folder.
     *
     * @param item 
     *          {@link MediaItem} to update
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

    public DialogSelfAssignment getNewAssignment() {
        return newAssignment;
    }

    public void setNewAssignment(DialogSelfAssignment newAssignment) {
        this.newAssignment = newAssignment;
    }

    private UserAccount getUser() {
        final String valueExpression = "#{userSession.user}";
        return (UserAccount) JsfUtils.getValueOfValueExpression(valueExpression);
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

    /**
     * Stores the view preference in the cookies stored in the client browser.
     */
    private void storeViewPreferenceInCookies() {
        JsfUtils.addCookie("view-catalogue-" + this.selectedCatalogue.getId(),
                this.contentView, ONE_YEAR);
    }

    /**
     * Store the selected catalogue or outlet + workflow state in the client
     * browser.
     */
    private void storeSelectedInCookies() {
        if (showNewsItem) {
            JsfUtils.removeCookie("selected-catalogue");
            JsfUtils.removeCookie("selected-catalogue-workflowstate");
        } else {
            JsfUtils.removeCookie("selected-outlet");
            JsfUtils.removeCookie("selected-outlet-workflowstate");
            if (this.selectedCatalogue != null) {
                JsfUtils.addCookie("selected-catalogue", ""
                        + this.selectedCatalogue.getId(), ONE_YEAR);
            }
            if (this.selectedWorkflowState != null) {
                JsfUtils.addCookie("selected-catalogue-workflowstate", ""
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
            String catViewCookie = JsfUtils.getCookieValue("view-catalogue-"
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

    private boolean fetchSelectedFromCookies() {
        try {
            String catId = JsfUtils.getCookieValue("selected-catalogue");
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
