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
package dk.i2m.converge.jsf.beans.administrator;

import dk.i2m.converge.core.AppVersion;
import dk.i2m.converge.ejb.facades.SystemFacadeLocal;
import dk.i2m.jsf.JsfUtils;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;

/**
 * Backing bean for <code>/administrator/Administration.jspx</code>.
 *
 * @author Allan Lykke Christensen
 */
public class Administration {

    @EJB private SystemFacadeLocal systemFacade;

    private RuntimeMXBean mx = ManagementFactory.getRuntimeMXBean();

    /**
     * Creates a new instance of {@link Administration}.
     */
    public Administration() {
    }

    public List<AppVersion> getAvailableMigration() {
        return systemFacade.getVersionsForMigration();
    }

    public boolean isMigrationAvailable() {
        return !getAvailableMigration().isEmpty();
    }

    public void setMigrateVersion(AppVersion selectedVersion) {
        Logger.getLogger(Administration.class.getName()).log(Level.INFO, "Migration started");
        if (systemFacade.upgrade(selectedVersion)) {
            JsfUtils.createMessage("frmAboutConverge", FacesMessage.SEVERITY_INFO, "MIGRATION_COMPLETED");
        } else {
            JsfUtils.createMessage("frmAboutConverge", FacesMessage.SEVERITY_INFO, "MIGRATION_FAILED");
        }
    }

    public long getTotalMemory() {
        return Runtime.getRuntime().totalMemory();
    }

    public long getUsedMemory() {
        return getTotalMemory() - getFreeMemory();
    }

    public long getMaxMemory() {
        return Runtime.getRuntime().maxMemory();
    }

    public long getFreeMemory() {
        return Runtime.getRuntime().freeMemory();
    }

    public String getOperatingSystem() {
        return System.getProperty("os.name") + " (" + System.getProperty("os.arch") + ") " + System.getProperty("os.version");
    }

    public String getJavaVersion() {
        return System.getProperty("java.vm.name") + " " + System.getProperty("java.vm.version") + " (" + System.getProperty("java.vm.vendor") + ")";
    }

    public String getBootClassPath() {
        return mx.getBootClassPath();
    }

    public String getClassPath() {
        return mx.getClassPath();
    }

    public List<String> getCommandLineArguments() {
        return mx.getInputArguments();
    }

    public Map<String, String> getSystemProperties() {
        return mx.getSystemProperties();
    }

    public Date getStartTime() {
        return new Date(mx.getStartTime());
    }

    /**
     * Gets the up time of the virtual machine in milliseconds.
     * 
     * @return Up time of the VM in ms.
     */
    public long getUpTime() {
        return mx.getUptime();
    }
}
