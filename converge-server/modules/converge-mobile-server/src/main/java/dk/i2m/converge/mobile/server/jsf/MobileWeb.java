/*
 *  Copyright (C) 2011 Interactive Media Management. All Rights Reserved.
 * 
 *  NOTICE:  All information contained herein is, and remains the property of 
 *  INTERACTIVE MEDIA MANAGEMENT and its suppliers, if any.  The intellectual 
 *  and technical concepts contained herein are proprietary to INTERACTIVE MEDIA
 *  MANAGEMENT and its suppliers and may be covered by Danish and Foreign 
 *  Patents, patents in process, and are protected by trade secret or copyright 
 *  law. Dissemination of this information or reproduction of this material
 *  is strictly forbidden unless prior written permission is obtained from 
 *  INTERACTIVE MEDIA MANAGEMENT.
 */
package dk.i2m.converge.mobile.server.jsf;

import dk.i2m.converge.mobile.server.domain.Outlet;
import dk.i2m.converge.mobile.server.domain.Section;
import dk.i2m.converge.mobile.server.utils.BeanComparator;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

/**
 *
 * @author Allan Lykke Christensen
 */
@ManagedBean(name = "mobileWeb")
@ViewScoped
public class MobileWeb implements Serializable {

    @PersistenceContext(unitName = "cmsPU")
    private EntityManager em;
    private DataModel<Section> availableCategories;
    private DataModel<Section> subscribedCategories;

    /** Creates a new instance of MobileWeb */
    public MobileWeb() {
    }

    public void reloadAvailableCategories() {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Outlet> cq = cb.createQuery(Outlet.class);
        Root<Outlet> o = cq.from(Outlet.class);
        cq.select(o).where(cb.equal(o.get("id"), getOutletId())).
                where(cb.equal(o.get("key"), getOutletKey()));
        
        List<Outlet> matches = em.createQuery(cq).getResultList();

        if (!matches.isEmpty()) {
            Outlet match = matches.iterator().next();
            Collections.sort(match.getSections(), new BeanComparator("displayOrder", true));
            this.availableCategories = new ListDataModel<Section>(match.getSections());
        }
    }

    public DataModel<Section> getAvailableCategories() {
        if (this.availableCategories == null) {
            reloadAvailableCategories();
        }
        return availableCategories;
    }

    public DataModel<Section> getSubscribedCategories() {
        return subscribedCategories;
    }

    private String getOutletKey() {
        return FacesContext.getCurrentInstance().getExternalContext().getInitParameter("OUTLET_KEY");
    }

    private Long getOutletId() {
        String id = FacesContext.getCurrentInstance().getExternalContext().getInitParameter("OUTLET_ID");
        try {
            return Long.valueOf(id);
        } catch (NumberFormatException ex) {
            return -1L;
        }
    }
}
