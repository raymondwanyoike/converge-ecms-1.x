/*
 * Copyright (C) 2012 Interactive Media Management
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

import dk.i2m.converge.ejb.facades.SystemFacadeLocal;
import dk.i2m.jsf.JsfUtils;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.servlet.ServletException;

/**
 * Backing bean for the error reporting page {@code Error.jspx}.
 *
 * @author Allan Lykke Christensen
 */
public class Error {

    @EJB
    private SystemFacadeLocal systemFacade;
    /**
     * Description of the error.
     */
    private String errorDescription = "";

    /**
     * Creates a new instance of Error
     */
    public Error() {
    }

    /**
     * Event handler for submitting the error report.
     *
     * @return {@code /Dashboard}
     */
    public String onSubmitErrorReport() {
        String stacktrace = getStackTrace();
        String browserData = FacesContext.getCurrentInstance().
                getExternalContext().getRequestHeaderMap().toString();

        systemFacade.submitErrorReport(getErrorDescription(), stacktrace, browserData);
        JsfUtils.createMessage("frmActivityStream",
                FacesMessage.SEVERITY_INFO,
                Bundle.i18n.name(),
                "Error_ERROR_REPORTED");

        return "/Dashboard";
    }

    /**
     * Event handler for disregarding the error.
     *
     * @return {@code /Dashboard}
     */
    public String onCancel() {
        return "/Dashboard";
    }

    /**
     * Gets the description of the error entered by the user.
     *
     * @return Description of the error
     */
    public String getErrorDescription() {
        return errorDescription;
    }

    /**
     * Sets the description of the error.
     *
     * @param errorDescription Error description
     */
    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }

    /**
     * Gets a string containing the complete stack trace of the
     * {@link Exception}.
     *
     * @return String containing the complete stack trace of the
     * {@link Exception}.
     */
    private String getStackTrace() {
        Map sessionMap = JsfUtils.getSessionMap();

        Throwable ex = (Throwable) sessionMap.get(
                "javax.servlet.error.exception");

        StringWriter writer = new StringWriter();
        PrintWriter pw = new PrintWriter(writer);
        fillStackTrace(ex, pw);

        return writer.toString();
    }

    /**
     * Write the stacktrace of a {@link Throwable} into a {@link PrintWriter}.
     *
     * @param ex {@link Throwable} occurred
     * @param pw {@link PrintWriter} to write the stack trace
     */
    private void fillStackTrace(Throwable ex, PrintWriter pw) {
        if (null == ex) {
            return;
        }

        ex.printStackTrace(pw);

        if (ex instanceof ServletException) {
            Throwable cause = ((ServletException) ex).getRootCause();

            if (null != cause) {
                pw.println("Root Cause:");
                fillStackTrace(cause, pw);
            }
        } else {
            Throwable cause = ex.getCause();

            if (null != cause) {
                pw.println("Cause:");
                fillStackTrace(cause, pw);
            }
        }
    }
}
