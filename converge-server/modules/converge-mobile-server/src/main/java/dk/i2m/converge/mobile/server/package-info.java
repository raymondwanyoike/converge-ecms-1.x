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

/**
 * Converge Mobile Server (CMOB) is a stand-alone server that is responsible for
 * distributing news to mobile subscribers.<br/><br/>
 * 
 * CMOB interacts with <em>Converge Editorial</em> via the RESTful web service.
 * Every {@link dk.i2m.converge.mobile.server.domain.Outlet} can be set up to a 
 * difference instance of <em>Converge Editorial</em>. CMOB synchronises with
 * <em>Converge Editorial</em> upon accessing the 
 * {@link dk.i2m.converge.mobile.server.service.WakeupService}.
 */
package dk.i2m.converge.mobile.server;
