/*
 * Copyright (C) 2011 Interactive Media Management
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
package dk.i2m.converge.core;

import dk.i2m.converge.core.security.UserAccount;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.*;

/**
 * General purpose log entry from a {@link dk.i2m.converge.core.plugin.Plugin}
 * or content item.
 *
 * @author Allan Lykke Christensen
 */
@Entity
@Table(name = "log_entry")
@NamedQueries({
    @NamedQuery(name = LogEntry.FIND_BY_ORIGIN,
    query =
    "SELECT l FROM LogEntry l WHERE l.origin = :origin AND l.originId = :originId ORDER BY l.date DESC")
})
public class LogEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Query for finding log entries by a given {@code origin} and {@code originId}.
     */
    public static final String FIND_BY_ORIGIN = "LogEntry.findByOrigin";

    public enum Severity {

        SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity")
    private Severity severity;

    @Column(name = "description") @Lob
    private String description = "";

    @Column(name = "log_date")
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date date;

    @Column(name = "origin")
    private String origin = "";

    @Column(name = "origin_id")
    private String originId = "";

    @ManyToOne
    @JoinColumn(name = "actor_id")
    private UserAccount actor;

    public LogEntry() {
    }

    /**
     * Creates a new {@link LogEntry}.
     *
     * @param severity    {@link Severity} of the {@link LogEntry}
     * @param description Description of the {@link LogEntry}
     * @param origin      Origin of the {@link LogEntry}
     * @param originId    Identifier of the origin
     */
    public LogEntry(Severity severity, String description, String origin,
            String originId) {
        this.severity = severity;
        this.description = description;
        this.origin = origin;
        this.originId = originId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getOriginId() {
        return originId;
    }

    public void setOriginId(String originId) {
        this.originId = originId;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public UserAccount getActor() {
        return actor;
    }

    public void setActor(UserAccount actor) {
        this.actor = actor;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof LogEntry)) {
            return false;
        }
        LogEntry other = (LogEntry) object;
        if ((this.id == null && other.id != null) || (this.id != null
                && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "dk.i2m.converge.core.LogEntry[ id=" + id + " ]";
    }
}
