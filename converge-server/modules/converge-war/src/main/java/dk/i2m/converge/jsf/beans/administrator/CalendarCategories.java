/*
 *  Copyright (C) 2010 - 2012 Interactive Media Management
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
package dk.i2m.converge.jsf.beans.administrator;

import dk.i2m.converge.core.calendar.EventCategory;
import dk.i2m.converge.ejb.facades.CalendarFacadeLocal;
import dk.i2m.converge.jsf.beans.Bundle;
import dk.i2m.jsf.JsfUtils;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.event.ActionEvent;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;

/**
 * Backing bean for {@code /administrator/CalendarCategories.jspx}.
 *
 * @author Allan Lykke Christensen
 */
public class CalendarCategories {

    @EJB
    private CalendarFacadeLocal calendarFacade;
    private DataModel categories = null;
    private EventCategory selectedCategory;

    /**
     * Event handler for saving a new or existing {@link EventCategory}
     *
     * @param event Event that invoked the handler
     */
    public void onSave(ActionEvent event) {

        if (isAddMode()) {
            selectedCategory = calendarFacade.create(selectedCategory);
            JsfUtils.createMessage("frmEventCategories",
                    FacesMessage.SEVERITY_INFO, Bundle.i18n.name(),
                    "administrator_CalendarCategories_EVENT_CATEGORY_CREATED");
        } else {
            selectedCategory = calendarFacade.update(selectedCategory);
            JsfUtils.createMessage("frmEventCategories",
                    FacesMessage.SEVERITY_INFO, Bundle.i18n.name(),
                    "administrator_CalendarCategories_EVENT_CATEGORY_UPDATED");
        }
        categories = null;
    }

    /**
     * Event handler for preparing the creation of a new {@link EventCategory}
     *
     * @param event Event that invoked the handler
     */
    public void onNew(ActionEvent event) {
        selectedCategory = new EventCategory();
    }

    /**
     * Event handler for deleting an existing {@link EventCategory}
     *
     * @param event Event that invoked the handler
     */
    public void onDelete(ActionEvent event) {
        if (selectedCategory != null) {
            calendarFacade.deleteEventCategory(selectedCategory.getId());
        }
        categories = null;
    }

    /**
     * Gets a {@link DataModel} of available
     * {@link EventCategories EventCategory}
     *
     * @return {@link DataModel} of available
     * {@link EventCategories EventCategory}
     */
    public DataModel getCategories() {
        if (categories == null) {
            categories = new ListDataModel(calendarFacade.findAllCategories());
        }
        return categories;
    }

    /**
     * Gets the currently selected {@link EventCategory}
     *
     * @return Currently selected {@link EventCategory}
     */
    public EventCategory getSelectedCategory() {
        return selectedCategory;
    }

    /**
     * Sets the currently selected {@link EventCategory}
     *
     * @param selectedCategory Currently selected {@link EventCategory}
     */
    public void setSelectedCategory(EventCategory selectedCategory) {
        this.selectedCategory = selectedCategory;
    }

    /**
     * Determine if the selected {@link EventCategory} is being edited.
     *
     * @return {@code true} if the {@link EventCategory} is being edited and
     * {@code false} if the {@link EventCategory} is being new
     */
    public boolean isEditMode() {
        if (selectedCategory == null || selectedCategory.getId() == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Determine if the selected {@link EventCategory} is being added.
     *
     * @return {@code true} if the {@link EventCategory} is being added and
     * {@code false} if the {@link EventCategory} is being edited
     */
    public boolean isAddMode() {
        return !isEditMode();
    }
}
