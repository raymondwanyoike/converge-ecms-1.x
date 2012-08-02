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
package dk.i2m.converge.core.plugin;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Abstract class for implementing a plug-in that can be executed in the
 * {@link JobQueue}.
 *
 * @author Allan Lykke Christensen
 */
public abstract class PluginAction implements Plugin {

    /** 
     * Bundle containing localised plug-in messages. Note, this property must be
     * set in the constructor of implementations. 
     */
    protected ResourceBundle bundle;
    
    /**
     * Executes the {@link JobQueueAction}.
     *
     * @param ctx
     *          PluginContext for accessing services
     * @param itemType
     *          Type of item to process
     * @param itemId
     *          Identifier of item to process
     * @param pluginConfiguration
     *          {@link PluginConfiguration} to be executed
     * @param variables
     *          Variables for assisting the execution
     * @throws PluginActionException
     *          If the {@link PluginAction} failed executing
     */
    public abstract void execute(PluginContext ctx, String itemType, Long itemId,
            PluginConfiguration cfg, Map<String, List<String>> variables) throws
            PluginActionException;

    /**
     * Provides a {@link List} of possible properties for the action.
     *
     * @return {@link List} of possible action properties
     */
    public abstract List<PluginActionPropertyDefinition> getAvailableProperties();

    /**
     * Initialise the bundle used by the plugin.
     * 
     * @param name 
     *          Full class name of the bundle
     */
    protected void setBundle(String name) {
        this.bundle = ResourceBundle.getBundle(name);
    }
    
    /** {@inheritDoc } */
    @Override
    public String getName() {
        return bundle.getString("PLUGIN_NAME");
    }

    /** {@inheritDoc } */
    @Override
    public String getAbout() {
        return bundle.getString("PLUGIN_ABOUT");
    }

    /** {@inheritDoc } */
    @Override
    public String getDescription() {
        return bundle.getString("PLUGIN_DESCRIPTION");
    }

    /** {@inheritDoc } */
    @Override
    public String getVendor() {
        return bundle.getString("PLUGIN_VENDOR");
    }

    /** {@inheritDoc } */
    @Override
    public Date getDate() {
        try {
            SimpleDateFormat format =
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return format.parse(bundle.getString("PLUGIN_BUILD_TIME"));
        } catch (Exception ex) {
            return Calendar.getInstance().getTime();
        }
    }

    /** {@inheritDoc } */
    @Override
    public ResourceBundle getBundle() {
        return bundle;
    }

    /**
     * Obtain an object from the name of a class.
     * 
     * @param className
     *          Full name of the class
     * @return Instantiated object of type {@code className}
     * @throws PluginActionException 
     *          If the class could not be instantiated
     */
    public Object getObjectFromClassName(String className) throws
            PluginActionException {
        try {
            Class c = Class.forName(className);
            Object o = c.newInstance();
            return o;
        } catch (ClassNotFoundException ex) {
            throw new PluginActionException(className + " not found", ex, true);
        } catch (InstantiationException ex) {
            throw new PluginActionException(className
                    + " could not be instantiated", ex, true);
        } catch (IllegalAccessException ex) {
            throw new PluginActionException(className + " could not be accessed",
                    ex, true);
        }
    }
}
