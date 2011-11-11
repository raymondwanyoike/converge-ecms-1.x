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
package dk.i2m.converge.ejb.facades;

import dk.i2m.converge.core.content.ContentTag;
import dk.i2m.converge.core.metadata.OpenCalaisMapping;
import dk.i2m.converge.core.security.UserAccount;
import dk.i2m.converge.ejb.services.DataNotFoundException;
import dk.i2m.converge.core.metadata.Subject;
import dk.i2m.converge.ejb.services.DaoServiceLocal;
import dk.i2m.converge.ejb.services.DirectoryException;
import dk.i2m.converge.ejb.services.QueryBuilder;
import dk.i2m.converge.ejb.services.UserNotFoundException;
import dk.i2m.converge.ejb.services.UserServiceLocal;
import dk.i2m.converge.nar.newsml.g2.power.Concept;
import dk.i2m.converge.nar.newsml.g2.power.ConceptNameType;
import dk.i2m.converge.nar.newsml.g2.power.Definition;
import dk.i2m.converge.nar.newsml.g2.power.KnowledgeItem;
import dk.i2m.converge.nar.newsml.g2.power.RelatedConceptType;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

/**
 * Session bean providing a facade for  meta data.
 *
 * @author Allan Lykke Christensen
 */
@Stateless
public class MetaDataFacadeBean implements MetaDataFacadeLocal {

    private static final Logger LOG = Logger.getLogger(MetaDataFacadeBean.class.getName());

    @EJB private DaoServiceLocal daoService;

    @EJB private UserServiceLocal userService;

    @Resource private SessionContext ctx;

    /** {@inheritDoc } */
    @Override
    public dk.i2m.converge.core.metadata.Concept create(dk.i2m.converge.core.metadata.Concept concept) {
        Calendar now = Calendar.getInstance();
        concept.setCreated(now);
        concept.setUpdated(now);
        concept.setCode(concept.getTypeId() + ":" + now.getTimeInMillis());

        try {
            UserAccount updaterUser = userService.findById(ctx.getCallerPrincipal().getName());
            concept.setUpdatedBy(updaterUser);
        } catch (UserNotFoundException ex) {
            // Concept was created through automated service, e.g. OpenCalais
            LOG.log(Level.FINE, "Updating user is unknown {0}", ctx.getCallerPrincipal().getName());
        } catch (DirectoryException ex) {
            LOG.log(Level.SEVERE, "Could not connect to directory server.", ex);
        }

        return daoService.create(concept);
    }

    /** {@inheritDoc } */
    @Override
    public void deleteConcept(Long id) {
        daoService.delete(dk.i2m.converge.core.metadata.Concept.class, id);
    }

    /** {@inheritDoc } */
    @Override
    public void delete(Class clazz, Long id) {
        daoService.delete(clazz, id);
    }

    /** {@inheritDoc } */
    @Override
    public dk.i2m.converge.core.metadata.Concept update(dk.i2m.converge.core.metadata.Concept concept) {
        Calendar now = Calendar.getInstance();
        concept.setUpdated(now);

        try {
            UserAccount updaterUser = userService.findById(ctx.getCallerPrincipal().getName());
            concept.setUpdatedBy(updaterUser);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Unknown user", ex);
        }

        return daoService.update(concept);
    }

    /** {@inheritDoc } */
    @Override
    public List<dk.i2m.converge.core.metadata.Concept> getConcepts() {
        return daoService.findAll(dk.i2m.converge.core.metadata.Concept.class);
    }

    /** {@inheritDoc } */
    @Override
    public List<dk.i2m.converge.core.metadata.Concept> findRecentConcepts(int count) {
        return daoService.findWithNamedQuery(dk.i2m.converge.core.metadata.Concept.FIND_RECENTLY_ADDED, count);
    }

    /** {@inheritDoc } */
    @Override
    public dk.i2m.converge.core.metadata.Concept findConceptById(Long id) throws DataNotFoundException {
        return daoService.findById(dk.i2m.converge.core.metadata.Concept.class, id);
    }

    /** {@inheritDoc } */
    @Override
    public dk.i2m.converge.core.metadata.Concept findConceptByName(String name)
            throws DataNotFoundException {
        Map params = QueryBuilder.with("name", name).parameters();
        return daoService.findObjectWithNamedQuery(
                dk.i2m.converge.core.metadata.Concept.class,
                dk.i2m.converge.core.metadata.Concept.FIND_BY_NAME, params);
    }

    /** {@inheritDoc } */
    @Override
    public List<Subject> findSubjectsByParent(Subject parent) {
        Map params = QueryBuilder.with("parent", parent).parameters();
        return daoService.findWithNamedQuery(Subject.FIND_BY_PARENT, params);
    }

    @Override
    public List<Subject> findTopLevelSubjects() {
        return daoService.findWithNamedQuery(Subject.FIND_PARENT_SUBJECTS);
    }

    /** {@inheritDoc } */
    @Override
    public dk.i2m.converge.core.metadata.Concept findConceptByCode(String code)
            throws DataNotFoundException {
        Map params = QueryBuilder.with("code", code).parameters();
        return daoService.findObjectWithNamedQuery(
                dk.i2m.converge.core.metadata.Concept.class,
                dk.i2m.converge.core.metadata.Concept.FIND_BY_CODE, params);
    }

    /** {@inheritDoc} */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    @Override
    public int importKnowledgeItem(String xml, String language) {
        //TODO: Allow for importing other items than Subjects

        String languageVariant;
        if (language.indexOf("_") == -1) {
            languageVariant = language.replaceAll("-", "_");
        } else {
            languageVariant = language.replaceAll("_", "-");
        }

        try {
            StringReader reader = new StringReader(xml);
            int imported = 0;
            JAXBContext jaxbContext = JAXBContext.newInstance(
                    KnowledgeItem.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            KnowledgeItem ki = (KnowledgeItem) unmarshaller.unmarshal(reader);

            for (Concept c : ki.getConceptSet().getConcept()) {
                Subject subject;
                String subjectCode = c.getConceptId().getQcode();
                String subjectTitle = "No translation for [" + language + "]";
                String subjectDefinition = "No translation for [" + language
                        + "]";

                // Determine if the subject already exist in the database
                // Note: If a subject already exist in the database it should
                //       be updated rather than ignored
                boolean update = false;
                try {
                    subject = (Subject) findConceptByCode(subjectCode);
                    update = true;
                } catch (DataNotFoundException e) {
                    subject = new Subject();
                    update = false;
                }

                for (ConceptNameType cn : c.getName()) {
                    if (cn.getLang().equalsIgnoreCase(language) || cn.getLang().
                            equalsIgnoreCase(languageVariant)) {
                        subjectTitle = cn.getValue();
                    }
                }

                for (Object obj : c.getDefinitionOrNoteOrFacet()) {
                    if (obj instanceof Definition) {
                        Definition def = (Definition) obj;
                        if (def.getLang().equalsIgnoreCase(language) || def.getLang().equalsIgnoreCase(languageVariant)) {
                            for (Object defContent : def.getContent()) {
                                subjectDefinition = (String) defContent;
                            }
                        }
                    }
                }

                Calendar now = Calendar.getInstance();
                subject.setCreated(now);
                subject.setUpdated(now);

                subject.setName(subjectTitle);
                subject.setDefinition(subjectDefinition);
                subject.setCode(subjectCode);

                boolean child = false;
                String parentCode = "";
                for (Object obj : c.getSameAsOrBroaderOrNarrower()) {
                    JAXBElement element = (JAXBElement) obj;
                    if (element.getName().getLocalPart().equalsIgnoreCase("broader")) {
                        RelatedConceptType related = (RelatedConceptType) element.getValue();
                        child = true;
                        parentCode = related.getQcode();
                    }
                }

                if (child) {
                    try {
                        Subject parent = (Subject) findConceptByCode(parentCode);
                        subject.getBroader().add(parent);
                    } catch (DataNotFoundException dnfe) {
                        LOG.log(Level.WARNING, "Specify broader concept (parent) with qcode: {0} could not be found", parentCode);
                    }
                }

                if (update) {
                    update(subject);
                } else {
                    daoService.create(subject);
                }
                imported++;
            }
            return imported;
        } catch (JAXBException ex) {
            LOG.log(Level.SEVERE, "Could not import KnowledgeItem", ex);
            return 0;
        }
    }

    /** {@inheritDoc} */
    @Override
    public String[] getLanguagesAvailableForImport(String xml) {
        List<String> availableLanguages = new ArrayList<String>();
        try {
            StringReader reader = new StringReader(xml);
            JAXBContext jaxbContext = JAXBContext.newInstance(
                    KnowledgeItem.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            KnowledgeItem ki = (KnowledgeItem) unmarshaller.unmarshal(reader);

            for (Concept c : ki.getConceptSet().getConcept()) {

                for (ConceptNameType cn : c.getName()) {
                    availableLanguages.add(cn.getLang());
                }
                break;
            }
        } catch (JAXBException ex) {
            LOG.log(Level.SEVERE, "Could not import KnowledgeItem", ex);
        }
        return availableLanguages.toArray(new String[availableLanguages.size()]);
    }

    /** {@inheritDoc } */
    @Override
    public List<dk.i2m.converge.core.metadata.Concept> search(String search) {
        Map params = QueryBuilder.with("keyword", "%" + search + "%").parameters();
        return daoService.findWithNamedQuery(dk.i2m.converge.core.metadata.Concept.FIND_BY_NAME_OR_DEFINITION, params);
    }

    /** {@inheritDoc} */
    @Override
    public List<dk.i2m.converge.core.metadata.Concept> findConceptByType(Class type) {
        return daoService.findAll(type);
    }

    @Override
    public List<dk.i2m.converge.core.metadata.Concept> findConceptsByName(String conceptName, Class... types) {
        Map<String, Object> params = QueryBuilder.with("name", conceptName + "%").parameters();
        List<dk.i2m.converge.core.metadata.Concept> conceptMatches = daoService.findWithNamedQuery(dk.i2m.converge.core.metadata.Concept.FIND_BY_LIKE_NAME, params);
        List<dk.i2m.converge.core.metadata.Concept> matches = new ArrayList<dk.i2m.converge.core.metadata.Concept>();

        for (dk.i2m.converge.core.metadata.Concept c : conceptMatches) {

            for (Class type : types) {
                if (c.getClass().equals(type)) {
                    matches.add(c);
//                } else {
//                    logger.log(Level.INFO, "Concept ("+ c.getFullTitle() +") of class {0} is not equal to class {1}", new Object[]{c.getClass().getName(), type.getName()});
                }
            }
        }
        return matches;
    }

    @Override
    public ContentTag findOrCreateContentTag(String name) {
        Map<String, Object> params = QueryBuilder.with("name", name).parameters();

        ContentTag tag;

        try {
            tag = daoService.findObjectWithNamedQuery(ContentTag.class, ContentTag.FIND_BY_NAME, params);
        } catch (DataNotFoundException ex) {
            tag = daoService.create(new ContentTag(name));
        }

        return tag;
    }

    @Override
    public List<ContentTag> findContentTagLikeName(String name) {
        Map<String, Object> params = QueryBuilder.with("name", "%" + name + "%").parameters();
        return daoService.findWithNamedQuery(ContentTag.FIND_LIKE_NAME, params);
    }

    /**
     * Gets all the mappings between Open Calais and Concepts.
     * 
     * @return {@link List} of mappings between Open Calais and Concepts
     */
    @Override
    public List<OpenCalaisMapping> getOpenCalaisMappings() {
        return daoService.findAll(OpenCalaisMapping.class);
    }

    @Override
    public OpenCalaisMapping create(OpenCalaisMapping mapping) {
        return daoService.create(mapping);
    }

    @Override
    public OpenCalaisMapping update(OpenCalaisMapping mapping) {
        return daoService.update(mapping);
    }

    @Override
    public void deleteOpenCalaisMapping(Long id) {
        daoService.delete(OpenCalaisMapping.class, id);
    }

    @Override
    public dk.i2m.converge.core.metadata.Concept findOpenCalaisMapping(String typeGroup, String field, String value) throws DataNotFoundException {
        Map<String, Object> params = QueryBuilder.with("typeGroup", typeGroup).and("field", field).and("value", value).parameters();
        List<OpenCalaisMapping> mappings = daoService.findWithNamedQuery(OpenCalaisMapping.FIND_BY_TYPE_GROUP_FIELD_AND_VALUE, params);

        if (mappings.isEmpty()) {
            throw new DataNotFoundException("No mapping for " + typeGroup + " with field " + field + " equal to " + value);
        }

        OpenCalaisMapping firstMatch = mappings.iterator().next();

        return firstMatch.getConcept();
    }
}
