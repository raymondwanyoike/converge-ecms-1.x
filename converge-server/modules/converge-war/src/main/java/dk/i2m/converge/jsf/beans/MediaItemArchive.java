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

import dk.i2m.converge.core.content.MediaItem;
import dk.i2m.converge.core.security.UserAccount;
import dk.i2m.converge.core.security.UserRole;
import dk.i2m.converge.ejb.facades.MediaDatabaseFacadeLocal;
import dk.i2m.converge.ejb.services.DataNotFoundException;
import dk.i2m.jsf.JsfUtils;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;

/**
 * 
 *
 * @author alc
 */
public class MediaItemArchive {

    private MediaItem selectedMediaItem;

    private Long id;

    @EJB private MediaDatabaseFacadeLocal mediaDatabaseFacade;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;

        if (this.id != null && id != null && selectedMediaItem == null) {
            try {
                selectedMediaItem = mediaDatabaseFacade.findMediaItemById(id);
            } catch (DataNotFoundException ex) {
                Logger.getLogger(MediaItemArchive.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public MediaItem getSelectedMediaItem() {
        return selectedMediaItem;
    }

    public void setSelectedMediaItem(MediaItem selectedMediaItem) {
        this.selectedMediaItem = selectedMediaItem;
    }

    /**
     * Determines if the current user is an editor of the media repository.
     * 
     * @return {@code true} if the current user is an editor of the media repository, otherwise {@code false}
     */
    public boolean isEditor() {
        UserRole editorRole = getSelectedMediaItem().getMediaRepository().getEditorRole();

        if (getUser().getUserRoles().contains(editorRole)) {
            return true;
        } else {
            return false;
        }
    }

    private UserAccount getUser() {
        final String valueExpression = "#{userSession.user}";
        return (UserAccount) JsfUtils.getValueOfValueExpression(valueExpression);
    }
}
