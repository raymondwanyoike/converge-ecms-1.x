package dk.i2m.converge.ejb.services;

/**
 *
 * @author Allan Lykke Christensen
 */
public class InvalidCatalogueException extends MediaRepositoryException {

    public InvalidCatalogueException(Throwable cause) {
        super(cause);
    }

    public InvalidCatalogueException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidCatalogueException(String message) {
        super(message);
    }

    public InvalidCatalogueException() {
        super();
    }
}
