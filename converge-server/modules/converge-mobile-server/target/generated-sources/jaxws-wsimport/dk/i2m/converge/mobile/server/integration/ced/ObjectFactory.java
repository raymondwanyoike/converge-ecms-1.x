
package dk.i2m.converge.mobile.server.integration.ced;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the dk.i2m.converge.mobile.server.integration.ced package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _Section_QNAME = new QName("http://soap.ws.converge.i2m.dk/", "section");
    private final static QName _GetPublishedEditionResponse_QNAME = new QName("http://soap.ws.converge.i2m.dk/", "getPublishedEditionResponse");
    private final static QName _GetPublishedEdition_QNAME = new QName("http://soap.ws.converge.i2m.dk/", "getPublishedEdition");
    private final static QName _GetOutletResponse_QNAME = new QName("http://soap.ws.converge.i2m.dk/", "getOutletResponse");
    private final static QName _NewsItem_QNAME = new QName("http://soap.ws.converge.i2m.dk/", "newsItem");
    private final static QName _GetOutlet_QNAME = new QName("http://soap.ws.converge.i2m.dk/", "getOutlet");
    private final static QName _Outlet_QNAME = new QName("http://soap.ws.converge.i2m.dk/", "outlet");
    private final static QName _Edition_QNAME = new QName("http://soap.ws.converge.i2m.dk/", "edition");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: dk.i2m.converge.mobile.server.integration.ced
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link Edition }
     * 
     */
    public Edition createEdition() {
        return new Edition();
    }

    /**
     * Create an instance of {@link NewsItem }
     * 
     */
    public NewsItem createNewsItem() {
        return new NewsItem();
    }

    /**
     * Create an instance of {@link GetOutletResponse }
     * 
     */
    public GetOutletResponse createGetOutletResponse() {
        return new GetOutletResponse();
    }

    /**
     * Create an instance of {@link GetPublishedEditionResponse }
     * 
     */
    public GetPublishedEditionResponse createGetPublishedEditionResponse() {
        return new GetPublishedEditionResponse();
    }

    /**
     * Create an instance of {@link MediaItem }
     * 
     */
    public MediaItem createMediaItem() {
        return new MediaItem();
    }

    /**
     * Create an instance of {@link GetOutlet }
     * 
     */
    public GetOutlet createGetOutlet() {
        return new GetOutlet();
    }

    /**
     * Create an instance of {@link Section }
     * 
     */
    public Section createSection() {
        return new Section();
    }

    /**
     * Create an instance of {@link GetPublishedEdition }
     * 
     */
    public GetPublishedEdition createGetPublishedEdition() {
        return new GetPublishedEdition();
    }

    /**
     * Create an instance of {@link Outlet }
     * 
     */
    public Outlet createOutlet() {
        return new Outlet();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Section }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://soap.ws.converge.i2m.dk/", name = "section")
    public JAXBElement<Section> createSection(Section value) {
        return new JAXBElement<Section>(_Section_QNAME, Section.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetPublishedEditionResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://soap.ws.converge.i2m.dk/", name = "getPublishedEditionResponse")
    public JAXBElement<GetPublishedEditionResponse> createGetPublishedEditionResponse(GetPublishedEditionResponse value) {
        return new JAXBElement<GetPublishedEditionResponse>(_GetPublishedEditionResponse_QNAME, GetPublishedEditionResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetPublishedEdition }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://soap.ws.converge.i2m.dk/", name = "getPublishedEdition")
    public JAXBElement<GetPublishedEdition> createGetPublishedEdition(GetPublishedEdition value) {
        return new JAXBElement<GetPublishedEdition>(_GetPublishedEdition_QNAME, GetPublishedEdition.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetOutletResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://soap.ws.converge.i2m.dk/", name = "getOutletResponse")
    public JAXBElement<GetOutletResponse> createGetOutletResponse(GetOutletResponse value) {
        return new JAXBElement<GetOutletResponse>(_GetOutletResponse_QNAME, GetOutletResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link NewsItem }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://soap.ws.converge.i2m.dk/", name = "newsItem")
    public JAXBElement<NewsItem> createNewsItem(NewsItem value) {
        return new JAXBElement<NewsItem>(_NewsItem_QNAME, NewsItem.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetOutlet }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://soap.ws.converge.i2m.dk/", name = "getOutlet")
    public JAXBElement<GetOutlet> createGetOutlet(GetOutlet value) {
        return new JAXBElement<GetOutlet>(_GetOutlet_QNAME, GetOutlet.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Outlet }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://soap.ws.converge.i2m.dk/", name = "outlet")
    public JAXBElement<Outlet> createOutlet(Outlet value) {
        return new JAXBElement<Outlet>(_Outlet_QNAME, Outlet.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Edition }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://soap.ws.converge.i2m.dk/", name = "edition")
    public JAXBElement<Edition> createEdition(Edition value) {
        return new JAXBElement<Edition>(_Edition_QNAME, Edition.class, null, value);
    }

}
