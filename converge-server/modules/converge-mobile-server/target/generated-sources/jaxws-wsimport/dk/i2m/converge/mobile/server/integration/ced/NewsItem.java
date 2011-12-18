
package dk.i2m.converge.mobile.server.integration.ced;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for newsItem complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="newsItem">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="byLine" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="dateLine" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="displayOrder" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="id" type="{http://www.w3.org/2001/XMLSchema}long" minOccurs="0"/>
 *         &lt;element name="media" type="{http://soap.ws.converge.i2m.dk/}mediaItem" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="section" type="{http://soap.ws.converge.i2m.dk/}section" minOccurs="0"/>
 *         &lt;element name="story" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="title" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "newsItem", propOrder = {
    "byLine",
    "dateLine",
    "displayOrder",
    "id",
    "media",
    "section",
    "story",
    "title"
})
public class NewsItem {

    protected String byLine;
    protected String dateLine;
    protected int displayOrder;
    protected Long id;
    @XmlElement(nillable = true)
    protected List<MediaItem> media;
    protected Section section;
    protected String story;
    protected String title;

    /**
     * Gets the value of the byLine property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getByLine() {
        return byLine;
    }

    /**
     * Sets the value of the byLine property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setByLine(String value) {
        this.byLine = value;
    }

    /**
     * Gets the value of the dateLine property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDateLine() {
        return dateLine;
    }

    /**
     * Sets the value of the dateLine property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDateLine(String value) {
        this.dateLine = value;
    }

    /**
     * Gets the value of the displayOrder property.
     * 
     */
    public int getDisplayOrder() {
        return displayOrder;
    }

    /**
     * Sets the value of the displayOrder property.
     * 
     */
    public void setDisplayOrder(int value) {
        this.displayOrder = value;
    }

    /**
     * Gets the value of the id property.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the value of the id property.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setId(Long value) {
        this.id = value;
    }

    /**
     * Gets the value of the media property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the media property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getMedia().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link MediaItem }
     * 
     * 
     */
    public List<MediaItem> getMedia() {
        if (media == null) {
            media = new ArrayList<MediaItem>();
        }
        return this.media;
    }

    /**
     * Gets the value of the section property.
     * 
     * @return
     *     possible object is
     *     {@link Section }
     *     
     */
    public Section getSection() {
        return section;
    }

    /**
     * Sets the value of the section property.
     * 
     * @param value
     *     allowed object is
     *     {@link Section }
     *     
     */
    public void setSection(Section value) {
        this.section = value;
    }

    /**
     * Gets the value of the story property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getStory() {
        return story;
    }

    /**
     * Sets the value of the story property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setStory(String value) {
        this.story = value;
    }

    /**
     * Gets the value of the title property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the value of the title property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTitle(String value) {
        this.title = value;
    }

}
