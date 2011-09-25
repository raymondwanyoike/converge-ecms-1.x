/*
 * Copyright (C) 2010 Interactive Media Management
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

import dk.i2m.converge.core.metadata.Concept;
import dk.i2m.converge.core.security.UserAccount;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;

/**
 * Represents a media item belonging to a {@link MediaRepository}. A
 * {@link MediaItem} could be an image, audio recording or video clip.
 *
 * @author Allan Lykke Christensen
 */
@Entity()
@Table(name = "media_item")
@NamedQueries({
    @NamedQuery(name = MediaItem.FIND_BY_STATUS, query = "SELECT DISTINCT m FROM MediaItem m WHERE m.status = :status ORDER BY m.updated DESC"),
    @NamedQuery(name = MediaItem.FIND_BY_OWNER, query = "SELECT DISTINCT m FROM MediaItem m WHERE m.owner = :owner ORDER BY m.id DESC"),
    @NamedQuery(name = MediaItem.FIND_CURRENT_AS_OWNER, query = "SELECT DISTINCT m FROM MediaItem m JOIN m.mediaRepository mr WHERE mr = :mediaRepository AND m.status <> dk.i2m.converge.core.content.MediaItemStatus.APPROVED AND m.status <> dk.i2m.converge.core.content.MediaItemStatus.REJECTED AND m.owner = :user ORDER BY m.updated DESC"),
    @NamedQuery(name = MediaItem.FIND_CURRENT_AS_EDITOR, query = "SELECT DISTINCT m FROM MediaItem m JOIN m.mediaRepository mr WHERE mr = :mediaRepository AND m.status = dk.i2m.converge.core.content.MediaItemStatus.SUBMITTED AND (:user MEMBER OF mr.editorRole.userAccounts) ORDER BY m.updated DESC"),
    @NamedQuery(name = MediaItem.FIND_BY_OWNER_AND_STATUS, query = "SELECT  DISTINCT m FROM MediaItem m JOIN m.mediaRepository mr WHERE mr = :mediaRepository AND (m.owner = :user OR :user MEMBER OF mr.editorRole.userAccounts) AND m.status = :status ORDER BY m.updated DESC")
})
public class MediaItem implements Serializable {

    public static final String FIND_BY_STATUS = "MediaItem.FindByStatus";

    public static final String FIND_BY_OWNER = "MediaItem.FindByOwner";

    public static final String FIND_CURRENT_AS_OWNER = "MediaItem.FindCurrentAsOwner";

    public static final String FIND_CURRENT_AS_EDITOR = "MediaItem.FindCurrentAsEditor";

    public static final String FIND_BY_OWNER_AND_STATUS = "MediaItem.FindByOwnerAndStatus";

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "assignment_id")
    private Assignment assignment;

    @ManyToOne
    @JoinColumn(name = "media_repository_id")
    private MediaRepository mediaRepository;

    @Column(name = "byline") @Lob
    private String byLine = "";

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private MediaItemStatus status = MediaItemStatus.UNSUBMITTED;

    @Column(name = "filename") @Lob
    private String filename = "";

    @Column(name = "contentType")
    private String contentType = "";

    @ManyToOne
    @JoinColumn(name = "owner")
    private UserAccount owner;

    @Column(name = "editorial_note")
    private String editorialNote = "";

    @Column(name = "title") @Lob
    private String title = "";

    @Column(name = "description") @Lob
    private String description = "";

    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    @Column(name = "media_date")
    private Calendar mediaDate = Calendar.getInstance();

    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    @Column(name = "created")
    private Calendar created = Calendar.getInstance();

    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    @Column(name = "updated")
    private Calendar updated = Calendar.getInstance();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "media_item_concept",
    joinColumns = {@JoinColumn(referencedColumnName = "id", name = "media_item_id", nullable = false)},
    inverseJoinColumns = {@JoinColumn(referencedColumnName = "id", name = "concept_id", nullable = false)})
    private List<Concept> concepts = new ArrayList<Concept>();

    @ManyToOne
    @JoinColumn(name = "parent_id")
    private MediaItem parent = null;

    @ManyToOne
    @JoinColumn(name = "rendition_id")
    private Rendition rendition = null;

    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    private List<MediaItem> versions = new ArrayList<MediaItem>();

    @javax.persistence.Version
    @Column(name = "opt_lock")
    private int versionIdentifier;

    /**
     * Creates a new instance of {@link MediaItem}.
     */
    public MediaItem() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public UserAccount getOwner() {
        return owner;
    }

    public void setOwner(UserAccount owner) {
        this.owner = owner;
    }

    public String getEditorialNote() {
        return editorialNote;
    }

    public void setEditorialNote(String editorialNote) {
        this.editorialNote = editorialNote;
    }

    public MediaRepository getMediaRepository() {
        return mediaRepository;
    }

    public void setMediaRepository(MediaRepository mediaRepository) {
        this.mediaRepository = mediaRepository;
    }

    public List<Concept> getConcepts() {
        return concepts;
    }

    public void setConcepts(List<Concept> concepts) {
        this.concepts = concepts;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public MediaItem getParent() {
        return parent;
    }

    public void setParent(MediaItem parent) {
        this.parent = parent;
    }

    public List<MediaItem> getVersions() {
        return versions;
    }

    public void setVersions(List<MediaItem> versions) {
        this.versions = versions;
    }

    public Assignment getAssignment() {
        return assignment;
    }

    public void setAssignment(Assignment assignment) {
        this.assignment = assignment;
    }

    public Rendition getRendition() {
        return rendition;
    }

    public void setRendition(Rendition rendition) {
        this.rendition = rendition;
    }

    public Calendar getMediaDate() {
        return mediaDate;
    }

    public void setMediaDate(Calendar mediaDate) {
        this.mediaDate = mediaDate;
    }

    public Calendar getUpdated() {
        return updated;
    }

    public void setUpdated(Calendar updated) {
        this.updated = updated;
    }

    public Calendar getCreated() {
        return created;
    }

    public void setCreated(Calendar created) {
        this.created = created;
    }

    public MediaItemStatus getStatus() {
        return status;
    }

    public void setStatus(MediaItemStatus status) {
        this.status = status;
    }

    public String getByLine() {
        return byLine;
    }

    public void setByLine(String byLine) {
        this.byLine = byLine;
    }

    public int getVersionIdentifier() {
        return versionIdentifier;
    }

    public void setVersionIdentifier(int versionIdentifier) {
        this.versionIdentifier = versionIdentifier;
    }

    /**
     * Gets the absolute URL of the {@link MediaItem}. This is
     * dynamically calculated based on the {@link MediaRepository} associated
     * with the {@link MediaItem}.
     *
     * @return Absolute URL of the {@link MediaItem}
     */
    public String getAbsoluteFilename() {
        try {
            return getMediaRepository().getWebAccess() + "/" + URIUtil.encodePath(getFilename(), "UTF-8");
        } catch (URIException ex) {
            return getMediaRepository().getWebAccess() + "/" + getFilename();
        }
    }

    public String getFileLocation() {
        return getMediaRepository().getLocation() + File.separator + getFilename();
    }

    /**
     * Gets the file extension of the {@link MediaItem}.
     *
     * @return File extension of the {@link MediaItem}, or an empty
     *         {@link String} is the extension could not be detected
     */
    public String getExtension() {
        if (getFilename() == null || getFilename().trim().isEmpty()) {
            return "";
        } else {
            int extIndex = getFilename().lastIndexOf(".");
            if (extIndex != getFilename().length()) {
                return getFilename().substring(extIndex + 1);
            } else {
                return "";
            }
        }
    }

    /**
     * Determines if a thumbnail of the item is available. Currently only
     * images provide thumbnails.
     *
     * @return {@code true} if a thumbnail of the item is available.
     */
    public boolean isThumbAvailable() {
        return isFileAttached();
    }

    /**
     * Gets the URL of thumbnail of the item. Note, check if a thumbnail is
     * available before using this method.
     *
     * @return {@link String} containing the URL of the media item thumbnail
     */
    public String getThumbURL() {
        return getMediaRepository().getWebAccess() + "/" + getId() + "-thumb.jpg";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MediaItem other = (MediaItem) obj;
        if (this.id != other.id && (this.id == null || !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 79 * hash + (this.id != null ? this.id.hashCode() : 0);
        return hash;
    }

    public boolean isFileAttached() {
        if (getFilename() == null || getFilename().trim().isEmpty()) {
            return false;
        } else {
            return true;
        }
    }
    
    public boolean isVideo() {
        return isFileAttached() && getContentType().startsWith("video");
    }
    
    public boolean isAudio() {
        return isFileAttached() && getContentType().startsWith("audio");
    }
}
