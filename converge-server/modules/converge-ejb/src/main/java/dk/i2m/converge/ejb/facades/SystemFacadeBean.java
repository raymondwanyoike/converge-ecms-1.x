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
package dk.i2m.converge.ejb.facades;

import dk.i2m.converge.domain.Property;
import dk.i2m.converge.core.Announcement;
import dk.i2m.converge.core.content.Language;
import dk.i2m.converge.core.ConfigurationKey;
import dk.i2m.converge.ejb.services.ConfigurationServiceLocal;
import dk.i2m.converge.ejb.services.DaoServiceLocal;
import dk.i2m.converge.ejb.services.UserServiceLocal;
import dk.i2m.converge.core.plugin.Plugin;
import dk.i2m.converge.core.plugin.PluginManager;
import dk.i2m.converge.ejb.services.DataNotFoundException;
import dk.i2m.converge.ejb.services.TimerServiceLocal;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 * Session bean providing access to information about the system.
 *
 * @author Allan Lykke Christensen
 */
@Stateless
public class SystemFacadeBean implements SystemFacadeLocal {

    private static final Logger LOG = Logger.getLogger(SystemFacadeBean.class.getName());

    @EJB private UserServiceLocal userService;

    @EJB private ConfigurationServiceLocal cfgService;

    @EJB private DaoServiceLocal daoService;

    @EJB private NewsItemFacadeLocal newsItemFacade;

    @EJB private TimerServiceLocal timerService;

    /**
     * Creates a new instance of {@link SystemFacadeBean}.
     */
    public SystemFacadeBean() {
    }

    /**
     * Conducts a sanity check of the system.
     *
     * @return <code>true</code> if the sanity of the system is OK, otherwise
     *         <code>false</code>
     */
    @Override
    public boolean sanityCheck() {
        int userCount = userService.findAll().size();
        LOG.log(Level.INFO, "{0} user {0, choice, 0#accounts|1#account|2#accounts} in the system", userCount);
        getPlugins();

        LOG.log(Level.INFO, "{0} stale {0, choice, 0#locks|1#lock|2#locks} removed", newsItemFacade.revokeAllLocks());

        String userPhotoDirectory = cfgService.getString(ConfigurationKey.WORKING_DIRECTORY) + System.getProperty("file.separator") + "users" + System.getProperty("file.separator");

        LOG.log(Level.INFO, "Checking if user photo directory ({0}) exists", new Object[]{userPhotoDirectory});

        File file = new File(userPhotoDirectory);
        if (!file.exists()) {
            LOG.log(Level.INFO, "{0} does not exist. Creating  directory", new Object[]{userPhotoDirectory});
            file.mkdirs();
        } else {
            LOG.log(Level.INFO, "{0} exists", new Object[]{userPhotoDirectory});
        }

        timerService.startTimers();

        return true;
    }

    /** {@inheritDoc } */
    @Override
    public Map<String, Plugin> getPlugins() {
        return PluginManager.getInstance().getPlugins();
    }

    /** {@inheritDoc } */
    @Override
    public List<Property> getSystemProperties() {
        List<Property> properties = new ArrayList<Property>();
        for (ConfigurationKey cfgKey : ConfigurationKey.values()) {
            if (!(cfgKey.equals(ConfigurationKey.VERSION) || cfgKey.equals(
                    ConfigurationKey.BUILD_TIME) || cfgKey.equals(
                    ConfigurationKey.APPLICATION_NEWSFEED))) {
                String key = cfgKey.name();
                String value = this.cfgService.getString(cfgKey);
                properties.add(new Property(key, value));
            }
        }
        return properties;
    }

    /** {@inheritDoc } */
    @Override
    public void updateSystemProperties(List<Property> properties) {
        for (Property property : properties) {
            this.cfgService.set(ConfigurationKey.valueOf(property.getKey()), property.getValue().replaceAll(",", "\\,"));
        }
    }

    /** {@inheritDoc } */
    @Override
    public String getProperty(ConfigurationKey key) {
        return cfgService.getString(key);
    }

    /** {@inheritDoc } */
    @Override
    public String getApplicationVersion() {
        return cfgService.getLongVersion();
    }

    /** {@inheritDoc } */
    @Override
    public List<Announcement> getAnnouncements() {
        return daoService.findAll(Announcement.class);
    }

    /** {@inheritDoc } */
    @Override
    public List<Announcement> getPublishedAnnouncements() {
        return daoService.findWithNamedQuery(Announcement.FIND_PUBLISHED);
    }

    /** {@inheritDoc } */
    @Override
    public Announcement updateAnnouncement(Announcement announcement) {
        return daoService.update(announcement);
    }

    /** {@inheritDoc } */
    @Override
    public Announcement createAnnouncement(Announcement announcement) {
        return daoService.create(announcement);
    }

    /** {@inheritDoc } */
    @Override
    public void deleteAnnouncement(Long id) {
        daoService.delete(Announcement.class, id);
    }

    /** {@inheritDoc } */
    @Override
    public Announcement findAnnouncementById(Long id) throws DataNotFoundException {
        return daoService.findById(Announcement.class, id);
    }

    /** {@inheritDoc } */
    @Override
    public List<Language> getLanguages() {
        return daoService.findAll(Language.class);
    }

    /** {@inheritDoc } */
    @Override
    public Language updateLanguage(Language language) {
        return daoService.update(language);
    }

    /** {@inheritDoc } */
    @Override
    public Language createLanguage(Language language) {
        return daoService.create(language);
    }

    /** {@inheritDoc } */
    @Override
    public void deleteLanguage(Long id) throws ReferentialIntegrityException {
        // TODO: Check if the language is being used in an Outlet or NewsItem
        daoService.delete(Language.class, id);
    }

    /** {@inheritDoc } */
    @Override
    public Language findLanguageById(Long id) throws DataNotFoundException {
        return daoService.findById(Language.class, id);
    }
}
