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

/**
 *
 * @author Allan Lykke Christensen
 */
public class SubscriberNotFound extends Exception {

    public SubscriberNotFound(Throwable cause) {
        super(cause);
    }

    public SubscriberNotFound(String message, Throwable cause) {
        super(message, cause);
    }

    public SubscriberNotFound(String message) {
        super(message);
    }

    public SubscriberNotFound() {
        super();
    }
}
