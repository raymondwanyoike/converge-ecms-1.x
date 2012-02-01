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
package dk.i2m.converge.mobile.server.facades;

import dk.i2m.converge.mobile.server.domain.Section;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

/**
 *
 * @author alc
 */
@Stateless
public class SectionFacade extends AbstractFacade<Section> implements
        SectionFacadeLocal {

    @PersistenceContext(unitName = "cmsPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public SectionFacade() {
        super(Section.class);
    }

    @Override
    public Section findByExternalId(Long id) {
        TypedQuery<Section> q = em.createQuery("SELECT s FROM Section s WHERE s.externalId = :externalId", Section.class);
        q.setParameter("externalId", id);
        return q.getSingleResult();
    }
}
