/*
 *  Copyright (C) 2010 Interactive Media Management
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
package dk.i2m.converge.ejb.services;

import dk.i2m.converge.core.ConfigurationKey;
import dk.i2m.converge.core.Notification;
import dk.i2m.converge.core.security.UserAccount;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

/**
 * Stateless session bean providing a service for managing notifications.
 *
 * @author Allan Lykke Christensen
 */
@Stateless
public class NotificationServiceBean implements NotificationServiceLocal {

    @EJB
    private ConfigurationServiceLocal cfgService;
    @EJB
    private DaoServiceLocal daoService;
    @Resource(name = "mail/converge")
    private javax.mail.Session mailSession;

    /**
     * {@inheritDoc }
     */
    @Override
    public void dispatchMail(String to, String from, String subject, String content) {
        try {
            // Define message
            Message message = new MimeMessage(mailSession);
            message.setSentDate(Calendar.getInstance().getTime());
            message.setFrom(new InternetAddress(from));

            if (to != null && !to.trim().isEmpty()) {
                message.addRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            }

            message.setSubject(subject);
            message.setContent(content, "text/html");

            Transport.send(message);
        } catch (AddressException e) {
            Logger.getLogger(NotificationServiceBean.class.getName()).log(Level.SEVERE, e.getMessage());
            Logger.getLogger(NotificationServiceBean.class.getName()).log(Level.FINE, "", e);
        } catch (MessagingException e) {
            Logger.getLogger(NotificationServiceBean.class.getName()).log(Level.SEVERE, e.getMessage());
            Logger.getLogger(NotificationServiceBean.class.getName()).log(Level.FINE, "", e);
        }
    }

    /**
     * Dispatches an e-mail with both HTML and plain text content.
     *
     * @param to E-mail address of the recipient
     * @param from E-mail address of the sender
     * @param subject Subject of the e-mail
     * @param htmlContent HTML content of the e-mail
     * @param plainContent Plain content of the e-mail
     */
    @Override
    public void dispatchMail(String to, String from, String subject, String htmlContent, String plainContent) {
        try {
            // Define message
            Message message = new MimeMessage(mailSession);
            message.setSubject(subject);
            message.setSentDate(Calendar.getInstance().getTime());
            message.setFrom(new InternetAddress(from));

            if (to != null && !to.trim().isEmpty()) {
                message.addRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            }

            Multipart content = new MimeMultipart("alternative");
            MimeBodyPart text = new MimeBodyPart();
            MimeBodyPart html = new MimeBodyPart();
            text.setText(plainContent);
            html.setContent(htmlContent, "text/html");
            content.addBodyPart(text);
            content.addBodyPart(html);
            message.setContent(content);

            Transport.send(message);
        } catch (AddressException e) {
            Logger.getLogger(NotificationServiceBean.class.getName()).log(Level.SEVERE, e.getMessage());
            Logger.getLogger(NotificationServiceBean.class.getName()).log(Level.FINE, "", e);
        } catch (MessagingException e) {
            Logger.getLogger(NotificationServiceBean.class.getName()).log(Level.SEVERE, e.getMessage());
            Logger.getLogger(NotificationServiceBean.class.getName()).log(Level.FINE, "", e);
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Notification create(Notification notification) {
        return daoService.create(notification);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Notification create(UserAccount recipient, String message) {
        Notification notification = new Notification(message, recipient);
        return daoService.create(notification);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void dismiss(Notification notification) {
        daoService.delete(Notification.class, notification.getId());
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void deleteOld(Date date) {
        daoService.executeQuery(Notification.DELETE_OLD,
                QueryBuilder.with(Notification.PARAMETER_DATE, date));
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void deleteOld() {
        int keep = cfgService.getInteger(ConfigurationKey.ACTIVITY_STREAM_KEEP);
        Calendar now = Calendar.getInstance();
        now.roll(Calendar.DAY_OF_MONTH, -keep);
        deleteOld(now.getTime());
    }
}
