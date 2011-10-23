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
package dk.i2m.converge.jsf.beans;

import dk.i2m.converge.core.reporting.activity.ActivityReport;
import dk.i2m.converge.core.reporting.activity.UserActivity;
import dk.i2m.converge.core.security.UserRole;
import dk.i2m.converge.core.workflow.WorkflowState;
import dk.i2m.converge.ejb.facades.ReportingFacadeLocal;
import dk.i2m.jsf.JsfUtils;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

/**
 * Backing bean for the Activity Report.
 *
 * @author Allan Lykke Christensen
 */
public class ReportActivity {

    @EJB private ReportingFacadeLocal reportingFacade;

    private UserRole userRole = null;

    private WorkflowState state = null;

    private Date startDate = null;

    private Date endDate = null;

    private DataModel report = new ListDataModel(new ArrayList());

    private UserActivity selectedUserActivity = null;

    private ActivityReport generatedReport = null;

    private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

    public ReportActivity() {
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public UserRole getUserRole() {
        return userRole;
    }

    public void setUserRole(UserRole userRole) {
        this.userRole = userRole;
    }

    public WorkflowState getState() {
        return state;
    }

    public void setState(WorkflowState state) {
        this.state = state;
    }

    public UserActivity getSelectedUserActivity() {
        return selectedUserActivity;
    }

    public void setSelectedUserActivity(UserActivity selectedUserActivity) {
        this.selectedUserActivity = selectedUserActivity;
    }

    public DataModel getReport() {
        return report;
    }

    public void onGenerateReport(ActionEvent event) {
        //this.generatedReport = reportingFacade.generateActivityReport(startDate, endDate, userRole, state);
        this.generatedReport = reportingFacade.generateActivityReport(startDate, endDate, userRole);
        this.report = new ListDataModel(this.generatedReport.getUserActivity());
    }

    /**
     * Determines if a generated report is available.
     * 
     * @return {@code true} if a report is available, otherwise {@code false}
     */
    public boolean isReportAvailable() {
        if (this.generatedReport != null) {
            return true;
        } else {
            return false;
        }
    }

    public void onDownloadXls(ActionEvent event) {

        if (isReportAvailable()) {
            byte[] file = reportingFacade.convertToExcel(generatedReport);
            String filename = "User Activity Report (" + format.format(getStartDate()) + " to " + format.format(getEndDate()) + ").xls";

            HttpServletResponse response = (HttpServletResponse) FacesContext.getCurrentInstance().getExternalContext().getResponse();
            response.setContentType("application/vnd.ms-excel");
            response.setHeader("Content-disposition", "attachment; filename=" + filename);
            try {
                ServletOutputStream out = response.getOutputStream();
                out.write(file);
                out.flush();
                out.close();
            } catch (IOException ex) {
                JsfUtils.createMessage("frmReporting", FacesMessage.SEVERITY_ERROR, "Could not generate Excel report. " + ex.getMessage(), new Object[]{});
            }

            FacesContext faces = FacesContext.getCurrentInstance();
            faces.responseComplete();
        }
    }
}
