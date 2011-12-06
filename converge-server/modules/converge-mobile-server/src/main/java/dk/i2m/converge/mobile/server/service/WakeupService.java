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
package dk.i2m.converge.mobile.server.service;

import dk.i2m.converge.mobile.server.FileUtils;
import dk.i2m.converge.mobile.server.ImageUtils;
import dk.i2m.converge.mobile.server.domain.NewsItem;
import dk.i2m.converge.mobile.server.domain.Outlet;
import dk.i2m.converge.mobile.server.integration.ced.MediaItem;
import dk.i2m.converge.mobile.server.integration.ced.OutletService;
import dk.i2m.converge.mobile.server.integration.ced.OutletService_Service;
import dk.i2m.converge.mobile.server.integration.ced.Section;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.imageio.ImageIO;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;

/**
 * RESTful service for initiating the synchronisation with <em>Converge 
 * Editorial</em>. The service is initiated by passing:
 * <ul>
 *     <li>ID of the external outlet</li>
 *     <li>ID of the edition to download</li>
 *     <li>ID of the internal outlet</li>
 *     <li>Key of the internal outlet</li>
 * </li>
 *
 * @author Allan Lykke Christensen
 */
@Stateless
@Path("/wakeup/{eid}/{edition}/{iid}/{key}")
public class WakeupService {

    @Context
    ServletContext context;
    @PersistenceContext(unitName = "cmsPU")
    private EntityManager em;
    private static final Logger LOG = Logger.getLogger(WakeupService.class.getName());
    private static final String SERVICE_URI = "http://soap.ws.converge.i2m.dk/";
    private static final String OUTLET_SERVICE_NAME = "OutletService";

    /**
     * Initiates the download of a given edition on <em>Converge Editorial</em>.
     * 
     * @param externalId
     *          Unique identifier of the Outlet on <em>Converge Editorial</em>
     * @param internalId
     *          Internal identifier of the Outlet on <em>Converge Mobile 
     *          Server</em>
     * @param internalKey
     *          Internal key of the Outlet on <em>Converge Mobile Server</em>
     * @param editionId
     *          Unique identifier of the Edition to download on <em>Converge 
     *          Editorial</em>
     * @return Empty string
     */
    @GET
    public String handleGET(@PathParam(value = "eid") String externalId, @PathParam(value = "iid") String internalId, @PathParam(value = "key") String internalKey, @PathParam(value = "edition") String editionId) {
        try {

            LOG.log(Level.INFO, "Synchronisation of External Outlet {0} with Internal Outlet {1} with key {2}", new Object[]{externalId, internalId, internalKey});

            Outlet outlet = em.find(Outlet.class, Long.valueOf(internalId));

            if (!outlet.getKey().equals(internalKey)) {
                LOG.log(Level.WARNING, "Invalid key provided by invoker {0}, expecting {1}", new Object[]{internalKey, outlet.getKey()});
                return "";
            }

            Long externalOutletId = Long.valueOf(outlet.getExternalId());
            Long externalEditionId = Long.valueOf(editionId);

            Authenticator.setDefault(new MyAuthenticator(outlet.getExternalUid(), outlet.getExternalPwd()));
            URL url = new URL(outlet.getExternalUrl());
            QName qname = new QName(SERVICE_URI, OUTLET_SERVICE_NAME);
            Service service = OutletService_Service.create(url, qname);
            OutletService os = service.getPort(OutletService.class);
            dk.i2m.converge.mobile.server.integration.ced.Outlet externalOutlet = os.getOutlet(externalOutletId);

            System.out.println(externalOutlet.getTitle());
            for (Section s : externalOutlet.getSections()) {
                if (isNewSection(s.getId())) {
                    dk.i2m.converge.mobile.server.domain.Section section = new dk.i2m.converge.mobile.server.domain.Section();
                    section.setTitle(s.getTitle());
                    section.setExternalId(s.getId());
                    section.setDisplayOrder(1);
                    LOG.log(Level.INFO, "Adding new section {0}", new Object[]{s.getTitle()});
                    em.persist(section);
                    outlet.getSections().add(section);
                    em.merge(outlet);
                }
            }

            dk.i2m.converge.mobile.server.integration.ced.Edition externalEdition = os.getPublishedEdition(externalEditionId);
            em.createQuery("UPDATE NewsItem ni " + "SET ni.available = ?1").setParameter(1, false).executeUpdate();

            String imgLocation = context.getInitParameter("IMG_LOCATION");
            String imgUrl = context.getInitParameter("IMG_URL");

            for (dk.i2m.converge.mobile.server.integration.ced.NewsItem item : externalEdition.getItems()) {
                if (!isNewsItemAvailable(item.getId())) {
                    NewsItem newsItem = new NewsItem();
                    newsItem.setExternalId(item.getId());
                    newsItem.setHeadline(item.getTitle());
                    newsItem.setStory(item.getStory());
                    newsItem.setAvailable(true);
                    newsItem.setDateline(item.getDateLine());
                    newsItem.setByline(item.getByLine());
                    newsItem.setDisplayOrder(item.getDisplayOrder());


                    // Generate thumbs
                    if (!item.getMedia().isEmpty()) {
                        MediaItem mediaItem = item.getMedia().iterator().next();
                        URL mediaItemUrl = new URL(mediaItem.getUrl());

                        try {
                            String thumbLocation = imgLocation + "/" + mediaItem.getId() + "-thumb.jpg";
                            String storyLocation = imgLocation + "/" + mediaItem.getId() + ".jpg";
                            String thumbPNGLocation = imgLocation + "/" + mediaItem.getId() + "-thumb.png";
                            String storyPNGLocation = imgLocation + "/" + mediaItem.getId() + ".png";

                            byte[] thumb = ImageUtils.generateThumbnail(getBytesFromUrl(mediaItemUrl), 48, 48, 100);
                            FileUtils.writeToFile(thumb, thumbLocation);

                            byte[] storyImg = ImageUtils.generateThumbnail(getBytesFromUrl(mediaItemUrl), 200, 100, 100);
                            FileUtils.writeToFile(storyImg, storyLocation);

                            convertJpgToPng(thumbLocation, thumbPNGLocation);
                            convertJpgToPng(storyLocation, storyPNGLocation);

                            File f = new File(thumbLocation);
                            if (f.delete()) {
                                LOG.log(Level.INFO, "{0} deleted", thumbLocation);
                            } else {
                                LOG.log(Level.INFO, "{0} could not be deleted", thumbLocation);
                            }

                            f = new File(storyLocation);
                            if (f.delete()) {
                                LOG.log(Level.INFO, "{0} deleted", storyLocation);
                            } else {
                                LOG.log(Level.INFO, "{0} could not be deleted", storyLocation);
                            }

                            newsItem.setThumbUrl(imgUrl + "/" + mediaItem.getId() + "-thumb.png");
                            newsItem.setImgUrl(imgUrl + "/" + mediaItem.getId() + ".png");
                        } catch (Exception e) {
                            LOG.log(Level.WARNING, "Could not generate thumb");
                        }

                    } else {
                        // No media items - use categoy image
                        //newsItem.setThumbUrl(imgUrl + "/empty-thumb.png");
                        //newsItem.setImgUrl(imgUrl + "/empty.png");
                        newsItem.setThumbUrl(imgUrl + "/category/" + item.getSection().getId() + "-thumb.png");
                        newsItem.setImgUrl(imgUrl + "/category/" + item.getSection().getId() + ".png");
                    }

                    newsItem.setSection(findSectionByExternalId(item.getSection().getId()));
                    LOG.log(Level.INFO, "Adding new story {0} in {1}", new Object[]{newsItem.getHeadline(), newsItem.getSection().getTitle()});
                    em.persist(newsItem);
                } else {
                    em.createQuery("UPDATE NewsItem ni SET ni.available = ?1 WHERE ni.externalId = ?2").setParameter(1, true).setParameter(2, item.getId()).executeUpdate();
                }
            }

        } catch (MalformedURLException ex) {
            Logger.getLogger(WakeupService.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "";
    }

    private boolean isUrlAvailable(String url) {
        try {
            URL test = new URL(url);
            test.openConnection();
            return true;
        } catch (IOException ex) {
            Logger.getLogger(WakeupService.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }

    static class MyAuthenticator extends Authenticator {

        private String username = "";
        private String password = "";

        public MyAuthenticator(String username, String password) {
            this.username = username;
            this.password = password;
        }

        @Override
        public PasswordAuthentication getPasswordAuthentication() {
            return (new PasswordAuthentication(this.username, this.password.toCharArray()));
        }
    }

    private boolean isNewSection(Long section) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery cq = cb.createQuery();
        Root subs = cq.from(dk.i2m.converge.mobile.server.domain.Section.class);
        cq.select(subs).where(cb.equal(subs.get("externalId"), section));

        List<dk.i2m.converge.mobile.server.domain.Section> matches = em.createQuery(cq).getResultList();


        return matches.isEmpty();
    }

    private dk.i2m.converge.mobile.server.domain.Section findSectionByExternalId(Long id) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery cq = cb.createQuery();
        Root subs = cq.from(dk.i2m.converge.mobile.server.domain.Section.class);
        cq.select(subs).where(cb.equal(subs.get("externalId"), id));

        List<dk.i2m.converge.mobile.server.domain.Section> matches = em.createQuery(cq).getResultList();


        if (matches.isEmpty()) {
            return null;
        } else {
            return matches.iterator().next();
        }
    }

    private dk.i2m.converge.mobile.server.domain.NewsItem findNewsItemByExternalId(Long id) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery cq = cb.createQuery();
        Root subs = cq.from(dk.i2m.converge.mobile.server.domain.NewsItem.class);
        cq.select(subs).where(cb.equal(subs.get("externalId"), id));

        List<dk.i2m.converge.mobile.server.domain.NewsItem> matches = em.createQuery(cq).getResultList();


        if (matches.isEmpty()) {
            return null;
        } else {
            return matches.iterator().next();
        }
    }

    private boolean isNewsItemAvailable(Long externalId) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery cq = cb.createQuery();
        Root subs = cq.from(dk.i2m.converge.mobile.server.domain.NewsItem.class);
        cq.select(subs).where(cb.equal(subs.get("externalId"), externalId));

        List<dk.i2m.converge.mobile.server.domain.NewsItem> matches = em.createQuery(cq).getResultList();


        return !matches.isEmpty();
    }

    private byte[] getBytesFromUrl(URL url) {
        ByteArrayOutputStream bais = new ByteArrayOutputStream();
        InputStream is = null;
        try {
            is = url.openStream();
            byte[] byteChunk = new byte[4096]; // Or whatever size you want to read in at a time.
            int n;

            while ((n = is.read(byteChunk)) > 0) {
                bais.write(byteChunk, 0, n);
            }
        } catch (IOException e) {
            System.err.printf("Failed while reading bytes from %s: %s", url.toExternalForm(), e.getMessage());
            e.printStackTrace();
            // Perform any other exception handling that's appropriate.
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
        }
        return bais.toByteArray();
    }

    public byte[] convertJpgToPng(String inputFile, String outputFile) {
        try {
            // this reads a jpeg from a inputFile
            BufferedImage bufferedImage = ImageIO.read(new File(inputFile));

            // this writes the bufferedImage back to outputFile
            ImageIO.write(bufferedImage, "png", new File(outputFile));

            ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", byteArrayOut);
            return byteArrayOut.toByteArray();


        } catch (IOException ex) {
            Logger.getLogger(WakeupService.class.getName()).log(Level.SEVERE, null, ex);
        }
        return new byte[0];
    }
}
