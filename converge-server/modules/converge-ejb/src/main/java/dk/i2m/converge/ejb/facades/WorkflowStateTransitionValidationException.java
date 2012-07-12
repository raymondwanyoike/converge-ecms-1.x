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
package dk.i2m.converge.ejb.facades;

/**
 * {@link WorkflowStateTransitionException} thrown if validation failed prior
 * to executing the workflow state transition.
 *
 * @author Allan Lykke Christensen
 */
public class WorkflowStateTransitionValidationException extends WorkflowStateTransitionException {

    private Object[] localisationParameters = new Object[]{};

    private boolean localisedMessage = false;

    public WorkflowStateTransitionValidationException() {
        super();
    }

    public WorkflowStateTransitionValidationException(String message) {
        super(message);
    }

    public WorkflowStateTransitionValidationException(String message,
            boolean localisedMessage) {
        super(message);
        this.localisedMessage = localisedMessage;
    }

    public WorkflowStateTransitionValidationException(String message,
            Object[] localisationParameters) {
        this(message);
        this.localisationParameters = localisationParameters;
        this.localisedMessage = true;
    }

    public WorkflowStateTransitionValidationException(String message,
            Throwable cause) {
        super(message, cause);
    }

    public WorkflowStateTransitionValidationException(String message,
            Throwable cause, Object[] localisationParameters) {
        this(message, cause);
        this.localisationParameters = localisationParameters;
        this.localisedMessage = true;
    }

    public WorkflowStateTransitionValidationException(Throwable cause) {
        super(cause);
    }

    public Object[] getLocalisationParameters() {
        return localisationParameters;
    }

    public boolean isLocalisedMessage() {
        return localisedMessage;
    }
}
