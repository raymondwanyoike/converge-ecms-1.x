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
package dk.i2m.converge.jsf.beans.administrator;

import dk.i2m.commons.BeanComparator;
import dk.i2m.converge.ejb.facades.EntityReferenceException;
import dk.i2m.converge.ejb.facades.OutletFacadeLocal;
import dk.i2m.converge.jsf.beans.BaseBean;
import dk.i2m.converge.core.workflow.Department;
import dk.i2m.converge.core.workflow.EditionPattern;
import dk.i2m.converge.core.workflow.Outlet;
import dk.i2m.converge.core.workflow.OutletEditionAction;
import dk.i2m.converge.core.workflow.OutletEditionActionProperty;
import dk.i2m.converge.core.workflow.Section;
import dk.i2m.converge.ejb.services.DataNotFoundException;
import dk.i2m.jsf.JsfUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.event.ActionEvent;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;

/**
 * Backing bean for <code>/administrator/Outlets.jspx</code>.
 *
 * @author Allan Lykke Christensen
 */
public class Outlets extends BaseBean {

    @EJB private OutletFacadeLocal outletFacade;

    private DataModel outlets = null;

    private String selectedOutletTab = "tabOutlet";

    private Outlet selectedOutlet = null;

    private Department selectedDepartment = null;

    private Section selectedSection = null;

    private OutletEditionActionProperty deleteProperty;

    private EditionPattern selectedEditionPattern;

    private DataModel outletEditionActionProperties = null;

    /**
     * Gets a {@link DataModel} containing the available outlets.
     *
     * @return {@link DataModel} containing the available outlets
     */
    public DataModel getOutlets() {
        if (outlets == null) {
            outlets = new ListDataModel(outletFacade.findAllOutlets());
        }
        return outlets;
    }

    public String getSelectedOutletTab() {
        return selectedOutletTab;
    }

    public void setSelectedOutletTab(String selectedOutletTab) {
        this.selectedOutletTab = selectedOutletTab;
    }

    public Outlet getSelectedOutlet() {
        return selectedOutlet;
    }

    public void setSelectedOutlet(Outlet selectedOutlet) {
        this.selectedOutlet = selectedOutlet;
    }

    public Department getSelectedDepartment() {
        return selectedDepartment;
    }

    public void setSelectedDepartment(Department selectedDepartment) {
        this.selectedDepartment = selectedDepartment;
    }

    public Section getSelectedSection() {
        return selectedSection;
    }

    public void setSelectedSection(Section selectedSection) {
        this.selectedSection = selectedSection;
    }

    /**
     * Determines if the {@link Outlet} is in <em>edit</em> or <em>add</em>
     * mode.
     *
     * @return <code>true</code> if the {@link Outlet} is in <em>edit</em> mode
     *         and <code>false</code> if in <em>add</em> mode
     */
    public boolean isOutletEditMode() {
        if (selectedOutlet == null || selectedOutlet.getId() == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Determines if the {@link Outlet} is in <em>add</em> mode.
     *
     * @return <code>true</code> if the {@link Outlet} is in <em>add</em> mode
     *         and <code>false</code> if in <em>edit</em> mode
     */
    public boolean isOutletAddMode() {
        return !isOutletEditMode();
    }

    public boolean isDepartmentEditMode() {
        if (selectedDepartment == null || selectedDepartment.getId() == null) {
            return false;
        } else {
            return true;
        }
    }

    public boolean isDepartmentAddMode() {
        return !isDepartmentEditMode();
    }

    public boolean isSectionEditMode() {
        if (selectedSection == null || selectedSection.getId() == null) {
            return false;
        } else {
            return true;
        }
    }

    public boolean isSectionAddMode() {
        return !isSectionEditMode();
    }

    public void onNewOutlet(ActionEvent event) {
        selectedOutlet = new Outlet();
    }

    public void onRecloseEditions(ActionEvent event) {
        if (selectedOutlet.getId() != null) {
            outletFacade.scheduleActionsOnOutlet(selectedOutlet.getId());
        }
        JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO, "EDITIONS_RECLOSED");
    }

    /**
     * Executes an {@link OutletEditionAction} of all editions of the selected {@link Outlet}.
     * 
     * @param action 
     *          {@link OutletEditionAction} to execute
     */
    public void setExecuteAction(OutletEditionAction action) {
        if (selectedOutlet != null && action != null) {
            outletFacade.scheduleActionOnOutlet(selectedOutlet.getId(), action.getId());
            JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO, "The action has been scheduled to be executed on all editions of the outlet");
        } else {
            JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_ERROR, "Outlet or action missing");
        }
    }

    /**
     * Event handler for saving updates to the {@link Outlet}.
     *
     * @param event
     *          Event that invoked the handler
     */
    public void onSaveOutlet(ActionEvent event) {
        if (isOutletAddMode()) {
            selectedOutlet = outletFacade.createOutlet(selectedOutlet);
            JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO, "OUTLET_CREATED");
        } else {
            selectedOutlet = outletFacade.updateOutlet(selectedOutlet);
            JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO, "OUTLET_UPDATED");
        }
        
        if (!selectedOutlet.isValid()) {
            JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_ERROR, "i18n", "administrator_Outlets_INVALID_OUTLET", null);
        }
        
        this.outlets = null;
    }

    /**
     * Event handler for deleting the {@link Outlet}.
     *
     * @param event
     *          Event that invoked the handler
     */
    public void onDeleteOutlet(ActionEvent event) {
        outletFacade.deleteOutletById(selectedOutlet.getId());
        JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO, "OUTLET_DELETED");
    }

    public void onNewDepartment(ActionEvent event) {
        selectedDepartment = new Department();
        selectedDepartment.setOutlet(selectedOutlet);
    }

    public void onSaveOutletDepartment(ActionEvent event) {

        String frmAttach = "frmOutletDetails";
        if (event.getComponent().getId().equalsIgnoreCase("lnkApplyOutletDepartment")) {
            frmAttach = "frmOutletDepartmentDetails";
        }

        if (isDepartmentAddMode()) {
            selectedDepartment = outletFacade.createDepartment(selectedDepartment);
            JsfUtils.createMessage(frmAttach, FacesMessage.SEVERITY_INFO, "DEPARTMENT_MSG_CREATED");
        } else {
            outletFacade.updateDepartment(selectedDepartment);
            JsfUtils.createMessage(frmAttach, FacesMessage.SEVERITY_INFO, "DEPARTMENT_MSG_UPDATED");
        }
        reloadSelectedOutlet();
    }

    public void onDeleteOutletDepartment(ActionEvent event) {
        outletFacade.deleteDepartment(selectedDepartment.getId());
        JsfUtils.createMessage("frmOutletDetails", FacesMessage.SEVERITY_INFO, "DEPARTMENT_MSG_DELETED");
        reloadSelectedOutlet();
    }

    public void onNewSection(ActionEvent event) {
        selectedSection = new Section();
        selectedSection.setOutlet(selectedOutlet);
    }

    public void onSaveSection(ActionEvent event) {
        if (isSectionAddMode()) {
            selectedSection = outletFacade.createSection(selectedSection);
            JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO, "outlets_SECTION_CREATED");
        } else {
            selectedSection = outletFacade.updateSection(selectedSection);
            JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO, "outlets_SECTION_UPDATED");
        }
        reloadSelectedOutlet();
    }

    public void onDeleteSection(ActionEvent event) {
        try {
            outletFacade.deleteSection(selectedSection.getId());
            JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO, "outlets_SECTION_DELETED");
        } catch (EntityReferenceException ex) {
            JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO, "outlets_SECTION_CANNOT_BE_DELETED_ENTITY_REFERENCE");
        }

        reloadSelectedOutlet();
    }

    private void reloadSelectedOutlet() {
        try {
            selectedOutlet = outletFacade.findOutletById(selectedOutlet.getId());
        } catch (DataNotFoundException ex) {
            Logger.getLogger(Outlets.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public Map<String, Section> getActiveSections() {
        Map<String, Section> activeSections = new LinkedHashMap<String, Section>();

        if (selectedOutlet != null) {
            Collections.sort(selectedOutlet.getSections(), new BeanComparator("fullName"));

            for (Section section : selectedOutlet.getSections()) {
                if (section.isActive()) {
                    activeSections.put(section.getFullName(), section);
                }
            }
        }

        return activeSections;
    }

    private OutletEditionAction selectedOutletEditionAction;

    private OutletEditionActionProperty selectedOutletEditionActionProperty = new OutletEditionActionProperty();

    private String selectedOutletEditionDetailsTab = "";

    public void onNewOutletAction(ActionEvent event) {
        selectedOutletEditionAction = new OutletEditionAction();
        selectedOutletEditionAction.setOutlet(selectedOutlet);
        selectedOutletEditionAction.setExecuteOrder(1);
        // A default action class is required to avoid NullPointerException from JSF
        selectedOutletEditionAction.setActionClass(dk.i2m.converge.plugins.indexedition.IndexEditionAction.class.getName());
    }

    public void onAddActionProperty(ActionEvent event) {
        selectedOutletEditionActionProperty.setOutletEditionAction(selectedOutletEditionAction);
        selectedOutletEditionAction.getProperties().add(selectedOutletEditionActionProperty);
        selectedOutletEditionActionProperty = new OutletEditionActionProperty();
        outletEditionActionProperties = null;
    }

    public OutletEditionAction getSelectedOutletEditionAction() {
        return selectedOutletEditionAction;
    }

    public void setSelectedOutletEditionAction(OutletEditionAction selectedOutletEditionAction) {
        this.selectedOutletEditionAction = selectedOutletEditionAction;
        this.outletEditionActionProperties = null;
    }

    public DataModel getOutletEditionActionProperties() {
        if (outletEditionActionProperties == null) {
            if (this.selectedOutletEditionAction != null) {
                this.outletEditionActionProperties = new ListDataModel(this.selectedOutletEditionAction.getProperties());
            } else {
                this.outletEditionActionProperties = new ListDataModel(new ArrayList());
            }
        }
        return outletEditionActionProperties;
    }

    public String getSelectedOutletEditionDetailsTab() {
        return selectedOutletEditionDetailsTab;
    }

    public void setSelectedOutletEditionDetailsTab(String selectedOutletEditionDetailsTab) {
        this.selectedOutletEditionDetailsTab = selectedOutletEditionDetailsTab;
    }

    public OutletEditionActionProperty getSelectedOutletEditionActionProperty() {
        return selectedOutletEditionActionProperty;
    }

    public void setSelectedOutletEditionActionProperty(OutletEditionActionProperty selectedOutletEditionActionProperty) {
        this.selectedOutletEditionActionProperty = selectedOutletEditionActionProperty;
    }

    public OutletEditionActionProperty getDeleteProperty() {
        return deleteProperty;
    }

    public void setDeleteProperty(OutletEditionActionProperty deleteProperty) {
        this.deleteProperty = deleteProperty;
        selectedOutletEditionAction.getProperties().remove(deleteProperty);
        this.outletEditionActionProperties = null;
    }

    public boolean isActionEditMode() {
        if (selectedOutletEditionAction == null || selectedOutletEditionAction.getId() == null) {
            return false;
        } else {
            return true;
        }

    }

    public boolean isActionAddMode() {
        return !isActionEditMode();
    }

    public void onDeleteOutletAction(ActionEvent event) {
        selectedOutlet.getEditionActions().remove(selectedOutletEditionAction);
        JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO, false, "The action was deleted", null);

    }

    public void onSaveOutletAction(ActionEvent event) {
        if (isActionAddMode()) {
            selectedOutletEditionAction.setOutlet(selectedOutlet);
            selectedOutletEditionAction = outletFacade.createOutletAction(selectedOutletEditionAction);
            JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO, false, "The action was created", null);
        } else {
            selectedOutletEditionAction = outletFacade.updateOutletAction(selectedOutletEditionAction);
            JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO, false, "The action was updated", null);
        }

        // Update the outlet (as it will not have different steps)
        try {
            selectedOutlet = outletFacade.findOutletById(selectedOutlet.getId());
        } catch (DataNotFoundException ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public EditionPattern getSelectedEditionPattern() {
        return selectedEditionPattern;
    }

    public void setSelectedEditionPattern(EditionPattern selectedEditionPattern) {
        this.selectedEditionPattern = selectedEditionPattern;
    }

    public boolean isEditionPatternEditMode() {
        if (selectedEditionPattern == null || selectedEditionPattern.getId() == null) {
            return false;
        } else {
            return true;
        }
    }

    public void onNewEditionPattern(ActionEvent event) {
        selectedEditionPattern = new EditionPattern();
        selectedEditionPattern.setOutlet(selectedOutlet);
    }

    public void onDeleteEditionPattern(ActionEvent event) {
        selectedOutlet.getEditionPatterns().remove(selectedEditionPattern);
        JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO, "outlet_EDITION_PATTERN_DELETED");
    }

    public void onSaveEditionPattern(ActionEvent event) {

        if (isEditionPatternEditMode()) {
            selectedEditionPattern = outletFacade.updateEditionPattern(selectedEditionPattern);
            JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO, "outlet_EDITION_PATTERN_UPDATED");

        } else {
            selectedEditionPattern.setOutlet(selectedOutlet);
            selectedEditionPattern = outletFacade.createEditionPattern(selectedEditionPattern);
            JsfUtils.createMessage("frmPage", FacesMessage.SEVERITY_INFO, "outlet_EDITION_PATTERN_CREATED");
        }

        // Update the outlet (as it will not have different steps)
        try {
            selectedOutlet = outletFacade.findOutletById(selectedOutlet.getId());
        } catch (DataNotFoundException ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
        }

    }
}
