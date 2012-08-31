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
package dk.i2m.converge.core.plugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.scannotation.AnnotationDB;

/**
 * Singleton responsible for discovering available {@link Plugin}s.
 *
 * @author Allan Lykke Christensen
 */
public final class PluginManager {

    /**
     * Singleton instance of the PluginManager.
     */
    private static PluginManager instance = null;
    private static final Class[] parameters = new Class[]{URL.class};
    private static final Logger LOG = Logger.getLogger(PluginManager.class.
            getName());
    private Map<String, NewswireDecoder> newswireDecoders =
            new HashMap<String, NewswireDecoder>();
    private Map<String, WorkflowAction> workflowActions =
            new HashMap<String, WorkflowAction>();
    private Map<String, EditionAction> outletActions =
            new HashMap<String, EditionAction>();
    private Map<String, WorkflowValidator> workflowValidators =
            new HashMap<String, WorkflowValidator>();
    private Map<String, CatalogueHook> catalogueActions =
            new HashMap<String, CatalogueHook>();
    private Map<String, NewsItemAction> newsItemActions =
            new HashMap<String, NewsItemAction>();

    private PluginManager() {
        discover();
    }

    /**
     * Gets an instance of the {@link PluginManager}.
     *
     * @return Instance of the {@link PluginManager}
     */
    public static PluginManager getInstance() {
        if (instance == null) {
            instance = new PluginManager();
        }
        return instance;
    }

    public static PluginService createPluginService() {
        addPluginJarsToClasspath();
        return StandardPluginService.getInstance();
    }

    private static void addPluginJarsToClasspath() {
        try {
            //add the plugin directory to classpath
            addDirToClasspath(new File("plugins"));
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Gets a {@link Map} of all discovered plugins where the key is the name of
     * the {@link Plugin} and the value is the instance of the {@link Plugin}
     *
     * @return {@link Map} of discovered plugins
     */
    public Map<String, Plugin> getPlugins() {
        Map<String, Plugin> plugins = new LinkedHashMap<String, Plugin>();
        plugins.putAll(newswireDecoders);
        plugins.putAll(workflowActions);
        plugins.putAll(outletActions);
        plugins.putAll(workflowValidators);
        plugins.putAll(catalogueActions);
        plugins.putAll(newsItemActions);
        return plugins;
    }

    public Map<String, NewswireDecoder> getNewswireDecoders() {
        return newswireDecoders;
    }

    public Map<String, WorkflowAction> getWorkflowActions() {
        return workflowActions;
    }

    public Map<String, EditionAction> getOutletActions() {
        return outletActions;
    }

    public Map<String, WorkflowValidator> getWorkflowValidators() {
        return workflowValidators;
    }

    public Map<String, CatalogueHook> getCatalogueActions() {
        return catalogueActions;
    }

    public Map<String, NewsItemAction> getNewsItemActions() {
        return newsItemActions;
    }

    /**
     * Discovers available plug-ins.
     *
     * @return Number of plug-ins discovered
     */
    public int discover() {
        int discoveredPlugins = 0;
        AnnotationDB db = new AnnotationDB();

        try {
            URLClassLoader cl = (URLClassLoader) getClass().getClassLoader();
            URL[] preClassPaths = cl.getURLs();

            List<URL> postClassPaths = new ArrayList<URL>();
            for (URL url : preClassPaths) {
                URL newURL;
                if (url.toString().startsWith("/")) {
                    newURL = new URL("file:" + url.toString());
                } else {
                    newURL = url;
                }

                try {
                    newURL.openStream();
                    postClassPaths.add(newURL);
                } catch (FileNotFoundException fnfe) {
                    LOG.log(Level.FINEST, "{0} was not found", newURL.toString());
                }
            }

            db.scanArchives(postClassPaths.toArray(
                    new URL[postClassPaths.size()]));

            discoveredPlugins += discoverPlugins(db,
                    dk.i2m.converge.core.annotations.NewswireDecoder.class,
                    newswireDecoders);
            discoveredPlugins += discoverPlugins(db,
                    dk.i2m.converge.core.annotations.WorkflowAction.class,
                    workflowActions);
            discoveredPlugins += discoverPlugins(db,
                    dk.i2m.converge.core.annotations.OutletAction.class,
                    outletActions);
            discoveredPlugins += discoverPlugins(db,
                    dk.i2m.converge.core.annotations.WorkflowValidator.class,
                    workflowValidators);
            discoveredPlugins += discoverPlugins(db,
                    dk.i2m.converge.core.annotations.CatalogueAction.class,
                    catalogueActions);
            discoveredPlugins += discoverPlugins(db,
                    dk.i2m.converge.core.annotations.NewsItemAction.class,
                    newsItemActions);
            LOG.log(Level.INFO,
                    "{0} {0, choice, 0#plugins|1#plugin|2#plugins} discovered",
                    discoveredPlugins);
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "", ex);
        }

        return discoveredPlugins;
    }

    private int discoverPlugins(AnnotationDB db, Class type, Map registry) {
        registry.clear();
        Set<String> entities = db.getAnnotationIndex().get(type.getName());

        if (entities == null) {
            LOG.log(Level.INFO, "No {0} plug-ins found in classpath", type.
                    getName());
        } else {
            for (String clazz : entities) {
                LOG.log(Level.INFO, "Plug-in ''{0}'' found", clazz);
                try {
                    Class c = Class.forName(clazz);
                    Plugin plugin = (Plugin) c.newInstance();
                    registry.put(plugin.getName(), plugin);
                } catch (ClassNotFoundException e) {
                    LOG.log(Level.SEVERE, "", e);
                } catch (InstantiationException e) {
                    LOG.log(Level.SEVERE, "", e);
                } catch (IllegalAccessException e) {
                    LOG.log(Level.SEVERE, "", e);
                }
            }
        }
        return registry.size();
    }

    /**
     * Adds the JAR files in the given directory to the classpath of the
     * application.
     *
     * @param directory Directory to add to the classpath
     * @throws IOException If the directory could not be added to the classpath
     */
    public static void addDirToClasspath(File directory) throws IOException {
        if (directory.exists()) {
            LOG.log(Level.FINE, "Adding directory \"{0}\" to plugin classpath", new Object[]{directory.getAbsoluteFile().getName()});
            File[] files = directory.listFiles();
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                addURL(file.toURI().toURL());
            }
        } else {
            LOG.log(Level.WARNING, "The plug-in directory \"{0}\" does not "
                    + "exist!", new Object[]{directory.getAbsoluteFile().getName()});
            directory.mkdirs();
            LOG.log(Level.WARNING, "Plug-in directory \"{0}\" created", 
                    new Object[]{directory.getAbsoluteFile()});

        }
    }

    /**
     * Adds a URL to the classpath.
     *
     * @param u URL to add to the classpath
     * @throws IOException If the URL could not be added to the classpath
     */
    public static void addURL(URL u) throws IOException {
        URLClassLoader sysLoader = (URLClassLoader) ClassLoader.
                getSystemClassLoader();
        URL urls[] = sysLoader.getURLs();
        for (int i = 0; i < urls.length; i++) {
            if (urls[i].toString().equalsIgnoreCase(u.toString())) {
                LOG.log(Level.INFO, "URL {0} is already in the CLASSPATH", u);
                return;
            }
        }
        Class sysclass = URLClassLoader.class;
        try {
            Method method = sysclass.getDeclaredMethod("addURL", parameters);
            method.setAccessible(true);
            method.invoke(sysLoader, new Object[]{u});
        } catch (Throwable t) {
            t.printStackTrace();
            throw new IOException(
                    "Error, could not add URL to system classloader");
        }
    }
}
