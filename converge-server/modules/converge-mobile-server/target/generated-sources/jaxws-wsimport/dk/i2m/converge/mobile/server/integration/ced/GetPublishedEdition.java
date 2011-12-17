
package dk.i2m.converge.mobile.server.integration.ced;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for getPublishedEdition complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="getPublishedEdition">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="editionId" type="{http://www.w3.org/2001/XMLSchema}long" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "getPublishedEdition", propOrder = {
    "editionId"
})
public class GetPublishedEdition {

    protected Long editionId;

    /**
     * Gets the value of the editionId property.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getEditionId() {
        return editionId;
    }

    /**
     * Sets the value of the editionId property.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setEditionId(Long value) {
        this.editionId = value;
    }

}
