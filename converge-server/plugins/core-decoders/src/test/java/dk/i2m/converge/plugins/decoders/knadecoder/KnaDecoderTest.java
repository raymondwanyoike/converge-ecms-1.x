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
package dk.i2m.converge.plugins.decoders.knadecoder;

import dk.i2m.converge.core.utils.FileUtils;
import javax.mail.Transport;
import javax.mail.internet.MimeMultipart;
import javax.mail.Multipart;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.InternetAddress;
import javax.mail.Message.RecipientType;
import javax.mail.internet.MimeMessage;
import javax.mail.Session;
import java.util.Properties;
import dk.i2m.converge.core.newswire.NewswireServiceProperty;
import dk.i2m.converge.core.newswire.NewswireItem;
import dk.i2m.converge.core.newswire.NewswireService;
import dk.i2m.converge.core.plugin.PluginContext;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.easymock.EasyMock.*;

/**
 *
 * @author Allan Lykke Christensen
 */
public class KnaDecoderTest {

    public KnaDecoderTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testDecode() throws Exception {
        int EXPECTED_NEWSWIRE_ITEMS = 12;

        PluginContext mockCtx = createMock(PluginContext.class);
        expect(mockCtx.getWorkingDirectory()).andReturn("target/newswiretestdata");
        expect(mockCtx.createNewswireItem(new NewswireItem())).
                andReturn(new NewswireItem()).
                times(EXPECTED_NEWSWIRE_ITEMS);
        replay(mockCtx);

        NewswireService service = new NewswireService();
        service.setDecoderClass(KnaDecoder.class.getName());
        service.setSource("KNA");

        // By including mock-javamail in the test classpath, the connection will be done to a dummy mail in-memory mail server
        service.getProperties().add(new NewswireServiceProperty(service, KnaDecoder.TRANSPORT, KnaDecoder.TRANSPORT_IMAP));
        service.getProperties().add(new NewswireServiceProperty(service, KnaDecoder.TRANSPORT_IMAP_SERVER, "i2m.dk"));
        service.getProperties().add(new NewswireServiceProperty(service, KnaDecoder.TRANSPORT_IMAP_PORT, "143"));
        service.getProperties().add(new NewswireServiceProperty(service, KnaDecoder.TRANSPORT_IMAP_USERNAME, "converge-ecms"));
        service.getProperties().add(new NewswireServiceProperty(service, KnaDecoder.TRANSPORT_IMAP_PASSWORD, "C0nvergeEcm$"));
        service.getProperties().add(new NewswireServiceProperty(service, KnaDecoder.TRANSPORT_IMAP_FOLDER_NEWSWIRE, "INBOX"));
        service.getProperties().add(new NewswireServiceProperty(service, KnaDecoder.TRANSPORT_IMAP_FOLDER_PROCESSED, "processed"));
        service.getProperties().add(new NewswireServiceProperty(service, KnaDecoder.TRANSPORT_IMAP_DELETE_PROCESSED, "false"));

        Properties props = new Properties();
        Session mailSession = Session.getDefaultInstance(props, null);
        mailSession.setDebug(true);

        // Drop mails in the in-memory mailbox (mock-javamail)
        MimeMessage msg = new MimeMessage(mailSession);
        msg.setRecipients(RecipientType.TO, "converge-ecms@i2m.dk");
        msg.setSubject("1608 Kna 1-13");
        msg.setFrom(new InternetAddress("allan@i2m.dk"));

        MimeBodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart.setText(FileUtils.getString(getClass().getResourceAsStream("/dk/i2m/converge/plugins/decoders/knadecoder/1608-kna-1-13.txt")));

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(messageBodyPart);

        msg.setContent(multipart);

        Transport.send(msg);

        List<NewswireItem> results = service.getDecoder().decode(mockCtx, service);
        assertNotNull(results);
        assertEquals("Incorrect results returned", EXPECTED_NEWSWIRE_ITEMS, results.size());

        for (NewswireItem ni : results) {
            System.out.println(ni.getTitle());
        }

    }
}
