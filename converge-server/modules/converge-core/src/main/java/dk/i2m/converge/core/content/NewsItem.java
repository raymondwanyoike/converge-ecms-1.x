/*
 * Copyright (C) 2010 - 2011 Interactive Media Management
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
package dk.i2m.converge.core.content;

import dk.i2m.converge.core.calendar.Event;
import dk.i2m.converge.core.content.catalogue.MediaItem;
import dk.i2m.converge.core.metadata.Concept;
import dk.i2m.converge.core.security.UserAccount;
import dk.i2m.converge.core.utils.BeanComparator;
import dk.i2m.converge.core.utils.StringUtils;
import dk.i2m.converge.core.workflow.Edition;
import dk.i2m.converge.core.workflow.Outlet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import javax.persistence.*;

/**
 * {@link NewsItem} written by one or more journalists for publishing in an
 * {@link Edition} of an {@link Outlet}.
 *
 * @author Allan Lykke Christensen
 */
@Entity
@Table(name = "news_item")
@DiscriminatorValue("news_item")
@NamedQueries({
    @NamedQuery(name = NewsItem.FIND_BY_OUTLET, query = "SELECT n FROM NewsItem AS n WHERE n.outlet = :outlet ORDER BY n.created DESC"),
    @NamedQuery(name = NewsItem.FIND_CURRENT_ASSIGNMENTS, query = "SELECT DISTINCT n FROM NewsItem n JOIN n.actors a WHERE n.currentState.showInInbox = true AND n.currentState.workflow.endState <> n.currentState AND n.currentState.workflow.trashState <> n.currentState AND (( a.user = :user AND a.role = n.currentState.actorRole) OR (n.currentState.permission = :permission AND :user MEMBER OF n.currentState.actorRole.userAccounts)) ORDER BY n.created DESC"),
    @NamedQuery(name = NewsItem.VIEW_CURRENT_ASSIGNMENTS, query = "SELECT DISTINCT NEW dk.i2m.converge.core.views.CurrentAssignment(n.id, n.title, n.targetWordCount, n.deadline, n.assignmentBriefing, n.checkedOut, cob.fullName) FROM NewsItem n LEFT JOIN n.checkedOutBy cob JOIN n.actors a WHERE n.currentState.showInInbox = true AND n.currentState.workflow.endState <> n.currentState AND n.currentState.workflow.trashState <> n.currentState AND (( a.user = :user AND a.role = n.currentState.actorRole) OR (n.currentState.permission = :permission AND :user MEMBER OF n.currentState.actorRole.userAccounts)) ORDER BY n.created DESC"),
    @NamedQuery(name = NewsItem.VIEW_OUTLET_BOX, query = "SELECT DISTINCT NEW dk.i2m.converge.core.views.InboxView(n.id, n.title, n.slugline, n.targetWordCount, n.precalculatedWordCount, n.precalculatedCurrentActor, n.currentState.name, n.outlet.title, n.deadline,n.updated,n.checkedOut, cob.fullName, n.assignmentBriefing, n.thumbnailLink) FROM NewsItem n, ContentItemActor a JOIN a.contentItem ci LEFT JOIN n.checkedOutBy cob WHERE n.id=ci.id AND (( a.user = :user) OR (n.currentState.permission = dk.i2m.converge.core.workflow.WorkflowStatePermission.GROUP AND :user MEMBER OF n.currentState.actorRole.userAccounts)) AND n.currentState.workflow.endState <> n.currentState AND n.outlet = :outlet ORDER BY n.updated DESC"),
    @NamedQuery(name = NewsItem.VIEW_OUTLET_BOX_STATE, query = "SELECT DISTINCT NEW dk.i2m.converge.core.views.InboxView(n.id, n.title, n.slugline, n.targetWordCount, n.precalculatedWordCount, n.precalculatedCurrentActor, n.currentState.name, n.outlet.title, n.deadline,n.updated,n.checkedOut, cob.fullName, n.assignmentBriefing, n.thumbnailLink) FROM NewsItem AS n JOIN n.actors AS a LEFT JOIN n.checkedOutBy cob WHERE n.outlet = :outlet AND n.currentState = :state AND (( a.user = :user) OR (n.currentState.permission = dk.i2m.converge.core.workflow.WorkflowStatePermission.GROUP AND :user MEMBER OF n.currentState.actorRole.userAccounts)) ORDER BY n.updated DESC"),
    @NamedQuery(name = NewsItem.FIND_CHECKED_IN_NEWS_ITEM, query = "SELECT n FROM NewsItem AS n WHERE n.id = :id AND n.checkedOut IS NULL"),
    @NamedQuery(name = NewsItem.FIND_ASSIGNMENTS_BY_OUTLET, query = "SELECT n FROM NewsItem AS n WHERE n.currentState.workflow.endState <> n.currentState AND n.currentState.workflow.trashState <> n.currentState AND n.outlet = :outlet AND n.assigned = true ORDER BY n.created DESC"),
    @NamedQuery(name = NewsItem.FIND_BY_OUTLET_AND_STATE, query = "SELECT n FROM NewsItem AS n WHERE n.currentState = :state AND n.outlet = :outlet ORDER BY n.updated DESC"),
    @NamedQuery(name = NewsItem.FIND_BY_OUTLET_AND_STATE_NAME, query = "SELECT n FROM NewsItem AS n WHERE n.currentState.name = :stateName AND n.outlet = :outlet"),
    @NamedQuery(name = NewsItem.FIND_BY_OUTLET_STATE_AND_USER, query = "SELECT DISTINCT ni FROM NewsItem AS ni JOIN ni.actors AS n WHERE ni.outlet = :outlet AND ni.currentState = :state AND (( n.user = :user) OR (ni.currentState.permission = dk.i2m.converge.core.workflow.WorkflowStatePermission.GROUP AND :user MEMBER OF ni.currentState.actorRole.userAccounts)) ORDER BY ni.updated DESC"),
    @NamedQuery(name = NewsItem.FIND_VERSIONS, query = "SELECT n FROM NewsItem AS n WHERE n.versionOf = :newsItem ORDER BY n.updated DESC"),
    @NamedQuery(name = NewsItem.FIND_TRASH, query = "SELECT DISTINCT ni FROM NewsItem AS ni JOIN ni.actors AS n WHERE ni.currentState.workflow.trashState = ni.currentState AND (( n.user = :user) OR (ni.currentState.permission = dk.i2m.converge.core.workflow.WorkflowStatePermission.GROUP AND :user MEMBER OF ni.currentState.actorRole.userAccounts)) ORDER BY ni.updated DESC"),
    @NamedQuery(name = NewsItem.REVOKE_LOCK, query = "UPDATE NewsItem n SET n.checkedOut = NULL, n.checkedOutBy = NULL WHERE n.id = :id"),
    @NamedQuery(name = NewsItem.REVOKE_LOCKS, query = "UPDATE NewsItem n SET n.checkedOut = NULL, n.checkedOutBy = NULL WHERE n.checkedOutBy = :user"),
    @NamedQuery(name = NewsItem.REVOKE_ALL_LOCKS, query = "UPDATE NewsItem n SET n.checkedOut = NULL, n.checkedOutBy = NULL WHERE n.checkedOutBy IS NOT NULL"),
    @NamedQuery(name = NewsItem.FIND_BY_USER_USER_ROLE_AND_DATE, query = "SELECT DISTINCT ni FROM NewsItem AS ni JOIN ni.actors AS a JOIN ni.history AS h WHERE (a.user = :user AND a.role = :userRole AND h.timestamp >= :startDate AND h.timestamp <= :endDate AND h.state = :state) ORDER BY ni.updated DESC"),
    @NamedQuery(name = NewsItem.FIND_SUBMITTED_BY_USER, query = "SELECT DISTINCT ni FROM NewsItem AS ni JOIN ni.actors AS a JOIN ni.history AS h WHERE (h.user = :user AND h.timestamp >= :startDate AND h.timestamp <= :endDate AND h.submitted = true) ORDER BY ni.updated DESC"),
    @NamedQuery(name = NewsItem.FIND_SUBMITTED_BY_PASSIVE_USER, query = "SELECT DISTINCT ni FROM NewsItem AS ni JOIN ni.actors AS a JOIN ni.history AS h WHERE (a.user = :user AND h.timestamp >= :startDate AND h.timestamp <= :endDate AND h.submitted = true) ORDER BY ni.updated DESC"),
    @NamedQuery(name = NewsItem.FIND_SUBMITTED_BY_USER_ROLE, query = "SELECT DISTINCT ni FROM NewsItem AS ni JOIN ni.actors AS a JOIN ni.history AS h WHERE (a.role = :userRole AND h.timestamp >= :startDate AND h.timestamp <= :endDate AND h.submitted = true) ORDER BY ni.updated DESC"),
    @NamedQuery(name = NewsItem.COUNT_BY_USER_AND_OUTLET_AND_WORKFLOW_STATE, query = "SELECT COUNT(DISTINCT ci) FROM ContentItem AS ci, NewsItem AS n LEFT JOIN n.actors AS a WHERE n.id=ci.id AND n.outlet = :" + NewsItem.PARAM_OUTLET + " AND n.currentState = :" + NewsItem.PARAM_WORKFLOW_STATE + " AND (( a.user = :" + NewsItem.PARAM_USER + ") OR (n.currentState.permission = dk.i2m.converge.core.workflow.WorkflowStatePermission.GROUP AND :" + NewsItem.PARAM_USER + " MEMBER OF n.currentState.actorRole.userAccounts))"),
    @NamedQuery(name = NewsItem.COUNT_BY_USER_AND_OUTLET, query = "SELECT COUNT(DISTINCT ci) FROM ContentItem AS ci, NewsItem AS n LEFT JOIN n.actors AS a WHERE n.id=ci.id AND n.outlet = :" + NewsItem.PARAM_OUTLET + " AND (( a.user = :" + NewsItem.PARAM_USER + ") OR (n.currentState.permission = dk.i2m.converge.core.workflow.WorkflowStatePermission.GROUP AND :" + NewsItem.PARAM_USER + " MEMBER OF n.currentState.actorRole.userAccounts))"),
    @NamedQuery(name = NewsItem.COUNT_INBOX, query = "SELECT COUNT(DISTINCT ci) FROM ContentItem ci LEFT JOIN ci.actors a WHERE ci.currentState.showInInbox = true AND ci.currentState.workflow.endState <> ci.currentState AND ci.currentState.workflow.trashState <> ci.currentState AND (( a.user = :" + NewsItem.PARAM_USER + " AND a.role = ci.currentState.actorRole) OR (ci.currentState.permission = dk.i2m.converge.core.workflow.WorkflowStatePermission.GROUP AND :" + NewsItem.PARAM_USER + " MEMBER OF ci.currentState.actorRole.userAccounts))"),
})
public class NewsItem extends ContentItem {

    private static final long serialVersionUID = 3L;
    public static final String VIEW_CURRENT_ASSIGNMENTS = "NewsItem.view.currentAssignments";
    public static final String VIEW_OUTLET_BOX = "NewsItem.view.outlet.inbox";
    public static final String VIEW_OUTLET_BOX_STATE = "NewsItem.view.outlet.inbox.state";
    public static final String FIND_CURRENT_ASSIGNMENTS = "NewsItem.findCurrentAssignments";
    public static final String FIND_BY_OUTLET = "NewsItem.findByOutlet";
    public static final String FIND_ASSIGNMENTS_BY_OUTLET = "NewsItem.findAssignmentsByOutlet";
    public static final String FIND_BY_OUTLET_AND_STATE = "NewsItem.findByOutletAndState";
    public static final String FIND_BY_OUTLET_AND_STATE_NAME = "NewsItem.findByOutletAndStateName";
    public static final String FIND_BY_OUTLET_STATE_AND_USER = "NewsItem.findByOutletStateAndUser";
    /**
     * Query for finding all the versions of a given news item arranged by the
     * date they were updated.
     */
    public static final String FIND_VERSIONS = "NewsItem.findVersions";
    /**
     * Query for finding all the news item in the trash.
     */
    public static final String FIND_TRASH = "NewsItem.findTrash";
    /**
     * Query for finding a news item that is checked-in by its unique
     * identifier.
     */
    public static final String FIND_CHECKED_IN_NEWS_ITEM = "NewsItem.findCheckedInNewsItem";
    public static final String FIND_BY_USER_USER_ROLE_AND_DATE = "NewsItem.findByUserUserRoleAndDate";
    public static final String FIND_SUBMITTED_BY_USER = "NewsItem.findBySubmittedUser";
    public static final String FIND_SUBMITTED_BY_PASSIVE_USER = "NewsItem.findBySubmittedPassveUser";
    public static final String FIND_SUBMITTED_BY_USER_ROLE = "NewsItem.findBySubmittedAndUserRole";
    /**
     * Query for revoking the lock of a news item.
     */
    public static final String REVOKE_LOCK = "NewsItem.revokeLock";
    /**
     * Query for revoking the locks of all news item checked out by a given
     * user.
     */
    public static final String REVOKE_LOCKS = "NewsItem.revokeLocks";
    /**
     * Query for revoking the locks of all news item checked out.
     */
    public static final String REVOKE_ALL_LOCKS = "NewsItem.revokeAllLocks";
    /**
     * Query for counting the results from finding items in a given catalogue
     * with a given state for a given user.
     */
    public static final String COUNT_BY_USER_AND_OUTLET_AND_WORKFLOW_STATE = "NewsItem.countByUserAndOutletAndWorkflowState";
    /**
     * Query for counting the results from finding items in a given catalogue
     * for a given user.
     */
    public static final String COUNT_BY_USER_AND_OUTLET = "NewsItem.countByUserAndOutlet";
    /**
     * Query for counting the items in the inbox.
     */
    public static final String COUNT_INBOX = "NewsItem.countInbox";
    /**
     * Query parameter used to specify the user.
     */
    public static final String PARAM_USER = "user";
    /**
     * Query parameter used to specify the catalogue.
     */
    public static final String PARAM_OUTLET = "outlet";
    /**
     * Query parameter used to specify the workflow state.
     */
    public static final String PARAM_WORKFLOW_STATE = "workflowState";
    @Column(name = "slugline")
    private String slugline = "";

    @Column(name = "by_line")
    private String byLine = "";
    @Column(name = "brief")
    @Lob
    private String brief = "";
    @Column(name = "story")
    @Lob
    private String story = "";
    @ManyToOne
    @JoinColumn(name = "language_id")
    private Language language;
    @ManyToOne
    @JoinColumn(name = "assignment_id")
    private Assignment assignment;
    @Column(name = "assigned")
    private boolean assigned = false;
    @ManyToOne
    @JoinColumn(name = "assigned_by")
    private UserAccount assignedBy;
    @Column(name = "assignment_briefing")
    @Lob
    private String assignmentBriefing = "";
    @Column(name = "undisclosed")
    private boolean undisclosedAuthor = false;
    @ManyToOne(fetch = FetchType.EAGER, optional = true)
    @JoinColumn(name = "version_news_item_id")
    private NewsItem versionOf;
    @ManyToOne(fetch = FetchType.EAGER, optional = true)
    @JoinColumn(name = "outlet_id")
    private Outlet outlet;
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "news_item_related",
    joinColumns = {
        @JoinColumn(referencedColumnName = "id", name = "news_item_id", nullable = false)},
    inverseJoinColumns = {
        @JoinColumn(referencedColumnName = "id", name = "related_news_item_id", nullable = false)})
    private List<NewsItem> related = new ArrayList<NewsItem>();
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "news_item_concept",
    joinColumns = {
        @JoinColumn(referencedColumnName = "id", name =
        "news_item_id", nullable = false)},
    inverseJoinColumns = {
        @JoinColumn(referencedColumnName = "id", name =
        "concept_id", nullable = false)})
    private List<Concept> concepts = new ArrayList<Concept>();
    @Column(name = "deadline")
    @Temporal(TemporalType.TIMESTAMP)
    private Calendar deadline = null;
    @ManyToOne(optional = true)
    @JoinColumn(name = "event_id")
    private Event event;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    @Column(name = "checked_out")
    private Calendar checkedOut;
    @ManyToOne
    @JoinColumn(name = "checked_out_by")
    private UserAccount checkedOutBy;
    @Column(name = "target_word_count")
    private Integer targetWordCount = 0;
    @Column(name = "precalc_word_count")
    private Long precalculatedWordCount = 0L;
    @javax.persistence.Version
    @Column(name = "opt_lock")
    private int versionIdentifier;
    @OneToMany(mappedBy = "newsItem", fetch = FetchType.LAZY)
    private List<NewsItemMediaAttachment> mediaAttachments =
            new ArrayList<NewsItemMediaAttachment>();
    @OneToMany(mappedBy = "newsItem", fetch = FetchType.EAGER, cascade =
    CascadeType.ALL)
    private List<NewsItemPlacement> placements =
            new ArrayList<NewsItemPlacement>();
    /**
     * Contains properties of the {@link NewsItem} that may be system or
     * user-related.
     */
    @OneToMany(mappedBy = "newsItem")
    private List<NewsItemProperty> properties =
            new ArrayList<NewsItemProperty>();

    /**
     * Creates a new instance of {@link NewsItem}.
     */
    public NewsItem() {
    }

    /**
     * Gets the slugline of the story.
     *
     * @return Slugline of the story
     */
    public String getSlugline() {
        return slugline;
    }

    /**
     * Sets the slugline of the story.
     *
     * @param slugline Slugline of the story
     */
    public void setSlugline(String slugline) {
        this.slugline = slugline;
    }

    /**
     * Gets the brief of the {@link NewsItem}.
     *
     * @return Brief of the {@link NewsItem}.
     */
    public String getBrief() {
        return brief;
    }

    /**
     * Sets the brief of the {@link NewsItem}.
     *
     * @param brief Brief of the {@link NewsItem}.
     */
    public void setBrief(String brief) {
        this.brief = brief;
    }

    /**
     * Gets the story of the {@link NewsItem}.
     *
     * @return Story of the {@link NewsItem}.
     */
    public String getStory() {
        return story;
    }

    /**
     * Sets the story of the {@link NewsItem}.
     *
     * @param story Story of the {@link NewsItem}.
     */
    public void setStory(String story) {
        this.story = story;
    }

    /**
     * Gets the "by-line" of the story.
     *
     * @return "by-line" of the story
     */
    public String getByLine() {
        return byLine;
    }

    /**
     * Sets the "by-line" of the story.
     *
     * @param byLine "by-line" of the story
     */
    public void setByLine(String byLine) {
        this.byLine = byLine;
    }

    /**
     * Gets the {@link List} of related {@link NewsItem}s.
     *
     * @return {@link List} of related {@link NewsItem}s.
     */
    public List<NewsItem> getRelated() {
        return related;
    }

    /**
     * Sets the {@link List} of related {@link NewsItem}s.
     *
     * @param related {@link List} of related {@link NewsItem}s.
     */
    public void setRelated(List<NewsItem> related) {
        this.related = related;
    }

    /**
     * Gets the user who assigned the news item. If the news item is
     * self-assigned,
     * <code>null</code> is returned.
     *
     * @return User who assigned the news item
     */
    public UserAccount getAssignedBy() {
        return assignedBy;
    }

    /**
     * Sets the user who assigned the news item. If the news item is
     * self-assigned, this value should be
     * <code>null</code>.
     *
     * @param assignedBy User who assigned the news item
     */
    public void setAssignedBy(UserAccount assignedBy) {
        this.assignedBy = assignedBy;
    }

    /**
     * Gets the date and time when the {@link NewsItem} was checked out.
     *
     * @return Date and time when the {@link NewsItem} was checked out. If the
     * {@link NewsItem} is not checked out, {@code null} is returned.
     */
    public Calendar getCheckedOut() {
        return checkedOut;
    }

    /**
     * Sets the check-out date and time of the {@link NewsItem}.
     *
     * @param checkedOut Date and time when the {@link NewsItem} was checked
     * out. If the {@link NewsItem} is not checked out, {@code null} should be
     * set
     */
    public void setCheckedOut(Calendar checkedOut) {
        this.checkedOut = checkedOut;
    }

    public UserAccount getCheckedOutBy() {
        return checkedOutBy;
    }

    public void setCheckedOutBy(UserAccount checkedOutBy) {
        this.checkedOutBy = checkedOutBy;
    }

    /**
     * Gets the {@link Language} of the {@link NewsItem}.
     *
     * @return {@link Language} of the {@link NewsItem}
     */
    public Language getLanguage() {
        return language;
    }

    /**
     * Sets the {@link Language} of the {@link NewsItem}.
     *
     * @param language {@link Language} of the {@link NewsItem}
     */
    public void setLanguage(Language language) {
        this.language = language;
    }

    /**
     * Determines if the {@link NewsItem} is checked out.
     *
     * @return {@code true} if the {@link NewsItem} is checked out, otherwise
     * {@code false}
     */
    public boolean isLocked() {
        if (getCheckedOut() == null) {
            return false;
        } else {
            return true;
        }
    }

    public Outlet getOutlet() {
        return outlet;
    }

    public void setOutlet(Outlet outlet) {
        this.outlet = outlet;
    }

    public boolean isUndisclosedAuthor() {
        return undisclosedAuthor;
    }

    public void setUndisclosedAuthor(boolean undisclosedAuthor) {
        this.undisclosedAuthor = undisclosedAuthor;
    }

    /**
     * Gets the {@link NewsItem} that this {@link NewsItem} is a version of.
     *
     * @return {@link NewsItem} that this is a version of.
     */
    public NewsItem getVersionOf() {
        return versionOf;
    }

    /**
     * Sets the {@link NewsItem} that this {@link NewsItem} is a version of.
     *
     * @param versionOf {@link NewsItem} that this is a version of.
     */
    public void setVersionOf(NewsItem versionOf) {
        this.versionOf = versionOf;
    }

    /**
     * Gets the {@link List} of {@link Concept}s related to the
     * {@link NewsItem}.
     *
     * @return {@link List} of {@link Concept}s related to the {@link NewsItem}.
     */
    public List<Concept> getConcepts() {
        return this.concepts;
    }

    /**
     * Sets the {@link List} of {@link Concept}s related to the
     * {@link NewsItem}.
     *
     * @param concepts {@link List} of {@link Concept}s related to the
     * {@link NewsItem}
     */
    public void setConcepts(List<Concept> concepts) {
        this.concepts = concepts;
    }

    /**
     * Gets the word count of the story.
     *
     * @return Word count of the story
     */
    public long getWordCount() {
        return StringUtils.countWords(StringUtils.stripHtml(this.story));
    }

    /**
     * Gets the target word count of the story. The target word count is set by
     * the assignment creator to guide the writer of the story.
     *
     * @return Target word count of the story
     */
    public Integer getTargetWordCount() {
        return targetWordCount;
    }

    /**
     * Sets the target word count of the story.
     *
     * @param targetWordCount Target number of words for the story
     */
    public void setTargetWordCount(Integer targetWordCount) {
        this.targetWordCount = targetWordCount;
    }

    /**
     * Gets the Lix count of the story. The lix count determine the readability
     * of the story.
     *
     * @return Lix count of the story.
     */
    public long getLixNumber() {
        return StringUtils.lix(StringUtils.stripHtml(this.story));
    }

    /**
     * Gets the deadline of the {@link NewsItem} for the journalist. This value
     * is
     * <code>null</code> if a deadline has not been set.
     *
     * @return Deadline of the {@link NewsItem} for the journalist
     */
    public Calendar getDeadline() {
        return deadline;
    }

    /**
     * Sets the deadline of the {@link NewsItem}. Setting the deadline to
     * <code>null</code> will remove the deadline.
     *
     * @param deadline Deadline to set
     */
    public void setDeadline(Calendar deadline) {
        this.deadline = deadline;
    }

    /**
     * Determines if a deadline has been set.
     *
     * @return <code>true</code> if the deadline has been set, otherwise
     * <code>false</code>
     */
    public boolean isDeadlineSet() {
        return deadline == null;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public boolean isAssigned() {
        return assigned;
    }

    /**
     * Sets the assigned status of the {@link NewsItem}. The status determines
     * if the {@link NewsItem} was assigned to the journalist or if it was
     * self-assigned.
     *
     * @param assigned
     *
     */
    public void setAssigned(boolean assigned) {
        this.assigned = assigned;
    }

    public String getAssignmentBriefing() {
        return assignmentBriefing;
    }

    public void setAssignmentBriefing(String assignmentBriefing) {
        this.assignmentBriefing = assignmentBriefing;
    }

    /**
     * Gets a {@link List} of sorted asset attachments.
     *
     * @return {@link List} of attached assets sorted by
     * {@link NewsItemMediaAttachment#getDisplayOrder()}
     */
    public List<NewsItemMediaAttachment> getMediaAttachments() {
        Collections.sort(mediaAttachments, new BeanComparator("displayOrder"));
        return mediaAttachments;
    }

    public void setMediaAttachments(
            List<NewsItemMediaAttachment> mediaAttachments) {
        this.mediaAttachments = mediaAttachments;
    }

    public Assignment getAssignment() {
        return assignment;
    }

    public void setAssignment(Assignment assignment) {
        this.assignment = assignment;
    }

    /**
     * Gets a {@link List} of all placements for this {@link NewsItem}.
     *
     * @return {@link List} of all placements for this {@link NewsItem}
     */
    public List<NewsItemPlacement> getPlacements() {
        return placements;
    }

    /**
     * Sets a {@link List} of all placements for this {@link NewsItem}.
     *
     * @param placements {@link List} of all placements for this
     * {@link NewsItem}
     */
    public void setPlacements(List<NewsItemPlacement> placements) {
        this.placements = placements;
    }

    /**
     * Gets a {@link Integer} containing the precalculated word count. The word
     * count is precalculated everytime the {@link NewsItem} is saved.
     *
     * @return {@link Integer} containing the precalculated word count
     */
    public Long getPrecalculatedWordCount() {
        return precalculatedWordCount;
    }

    public void setPrecalculatedWordCount(Long precalculatedWordCount) {
        this.precalculatedWordCount = precalculatedWordCount;
    }

    /**
     * Gets a {@link List} of properties that has been stored for this
     * {@link NewsItem}.
     *
     * @return {@link List} of related properties
     */
    public List<NewsItemProperty> getProperties() {
        return properties;
    }

    /**
     * Sets the properties of the {@link NewsItem}.
     *
     * @param properties {@link List} of properties related to the
     * {@link NewsItem}
     */
    public void setProperties(List<NewsItemProperty> properties) {
        this.properties = properties;
    }

    /**
     * Gets the version identifier of the entity. The purpose of the identifier
     * is to implement optimistic locking of the entity.
     *
     * @return Version identifier
     */
    public int getVersionIdentifier() {
        return versionIdentifier;
    }

    /**
     * Determines if the {@link NewsItem} has reached its end state in the
     * workflow.
     *
     * @return {@code true} if the {@link NewsItem} has reached the end state of
     * the workflow, otherwise {@code false}
     */
    public boolean isEndState() {
        // NullPointer check
        if (getCurrentState() == null || getOutlet() == null || getOutlet().
                getWorkflow() == null || getOutlet().getWorkflow().getEndState()
                == null) {
            return false;
        }


        if (getCurrentState().equals(getOutlet().getWorkflow().getEndState())) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Determines if the {@link NewsItem} is at the start state in the workflow.
     *
     * @return {@code true} if the {@link NewsItem} is at the start state of the
     * workflow, otherwise {@code false}
     */
    public boolean isStartState() {
        // NullPointer check
        if (getCurrentState() == null || getOutlet() == null || getOutlet().
                getWorkflow() == null || getOutlet().getWorkflow().getStartState()
                == null) {
            return false;
        }

        if (getCurrentState().equals(getOutlet().getWorkflow().getStartState())) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Determines if the {@link NewsItem} is at the trash state in the workflow.
     *
     * @return {@code true} if the {@link NewsItem} is at the trash state of the
     * workflow, otherwise {@code false}
     */
    public boolean isTrashState() {
        // NullPointer check
        if (getCurrentState() == null || getOutlet() == null || getOutlet().
                getWorkflow() == null || getOutlet().getWorkflow().getTrashState()
                == null) {
            return false;
        }

        if (getCurrentState().equals(getOutlet().getWorkflow().getTrashState())) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Determines if the {@link NewsItem} is an intermediate state in the
     * workflow.
     *
     * @return {@code true} if the {@link NewsItem} is at an intermediate state
     * of the workflow, otherwise {@code false}
     */
    public boolean isIntermediateState() {
        return !isEndState() && !isTrashState();
    }

    /**
     * Gets the next available Display Order for attached assets. The next
     * available display order is the highest display order of existing
     * attachments plus one.
     *
     * @return Next available Display Order for an attached asset.
     */
    public int getNextAssetAttachmentDisplayOrder() {
        int i = 1;
        for (NewsItemMediaAttachment attachment : getMediaAttachments()) {
            if (attachment != null) {
                if (attachment.getDisplayOrder() >= i) {
                    i = attachment.getDisplayOrder() + 1;
                }
            }
        }
        return i;
    }

    /**
     * A {@link NewsItem} is equal to this {@link NewsItem} if their
     * {@link #id}s are equal.
     *
     * @param obj {@link Object} to determine if equal to this {@link NewsItem}
     * @return {@code true} if {@link Object} is a {@link NewsItem} with the
     * same {@link #id} as this {@link NewsItem}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final NewsItem other = (NewsItem) obj;
        if (getId() != other.getId()
                && (getId() == null || !getId().equals(other.getId()))) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + (getId() != null ? getId().hashCode() : 0);
        return hash;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public String toString() {
        return getClass().getName() + "[id=" + getId() + "]";
    }
}
