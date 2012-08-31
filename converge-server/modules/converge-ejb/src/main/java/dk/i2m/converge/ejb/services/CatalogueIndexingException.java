package dk.i2m.converge.ejb.services;

/**
 *
 * @author Allan Lykke Christensen
 */
public class CatalogueIndexingException extends MediaRepositoryException {

    public CatalogueIndexingException(Throwable cause) {
        super(cause);
    }

    public CatalogueIndexingException(String message, Throwable cause) {
        super(message, cause);
    }

    public CatalogueIndexingException(String message) {
        super(message);
    }

    public CatalogueIndexingException() {
        super();
    }
}
