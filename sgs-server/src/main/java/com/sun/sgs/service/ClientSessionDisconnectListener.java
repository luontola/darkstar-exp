/*
 * Copyright 2007-2008 Sun Microsystems, Inc.
 *
 * This file is part of Project Darkstar Server.
 *
 * Project Darkstar Server is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License
 * version 2 as published by the Free Software Foundation and
 * distributed hereunder to you.
 *
 * Project Darkstar Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.sun.sgs.service;

import java.math.BigInteger;

import com.sun.sgs.app.ClientSession;

/**
 * A listener that services may register with the {@link ClientSessionService}
 * to receive notification of session disconnect.
 * 
 * @see ClientSessionService#registerSessionDisconnectListener
 */
public interface ClientSessionDisconnectListener {

    /**
     * Notifies this listener that the session with the given
     * {@code sessionRefId} has disconnected.
     * 
     * @param sessionRefId the ID of the {@code ManagedReference} to the
     *        {@link ClientSession} that disconnected
     */
    void disconnected(BigInteger sessionRefId);
}
