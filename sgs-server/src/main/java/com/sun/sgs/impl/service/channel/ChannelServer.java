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

package com.sun.sgs.impl.service.channel;

import java.io.IOException;
import java.rmi.Remote;

/**
 * A remote interface for communicating between peer channel service
 * implementation.
 */
public interface ChannelServer extends Remote {

    /**
     * Notifies this server that it should service the event queue of
     * the channel with the specified {@code channelId}.
     *
     * @param	channelId a channel ID
     * @throws	IOException if a communication problem occurs while
     * 		invoking this method
     */
    void serviceEventQueue(byte[] channelId) throws IOException;

    /**
     * Notifies this server that it should reread the channel
     * membership list of the specified {@code channelId} for
     * sessions connected to this node before processing any other
     * events on the channel.  {@code refresh} requests are sent
     * when a node performs recovery operations for a channel
     * coordinator failure.  When a channel coordinator fails, a
     * {@code join}, {@code leave}, or other event notification may
     * be lost, so any local channel membership information that is
     * cached may be stale and needs to be reread before processing
     * any more events.
     *
     * @param	name a channel name
     * @param	channelId a channel ID
     * @throws	IOException if a communication problem occurs while
     * 		invoking this method
     */
    void refresh(String name, byte[] channelId) throws IOException;

    /**
     * Notifies this server that the locally-connected session with
     * the specified {@code sessionId} has joined the channel with
     * the specified {@code name} and {@code channelId}.
     *
     * @param	name a channel name
     * @param	channelId a channel ID
     * @param	sessionId a session ID
     * @throws	IOException if a communication problem occurs while
     * 		invoking this method
     */
    void join(String name, byte[] channelId, byte[] sessionId)
	throws IOException;

    /**
     * Notifies this server that the locally-connected session with
     * the specified {@code sessionId} has left the channel with the
     * specified {@code channelId}.
     *
     * @param	channelId a channel ID
     * @param	sessionId a session ID
     * @throws	IOException if a communication problem occurs while
     * 		invoking this method
     */
    void leave(byte[] channelId, byte[] sessionId) throws IOException;

    /**
     * Notifies this server that all locally-connected member sessions
     * have left the channel with the specified {@code channelId}.
     *
     * @param	channelId a channel ID
     * @throws	IOException if a communication problem occurs while
     * 		invoking this method
     */
    void leaveAll(byte[] channelId) throws IOException;

    /**
     * Sends the specified message to all locally-connected sessions
     * that are members of the channel with the specified {@code
     * channelId}.
     *
     * @param	channelId a channel ID
     * @param	message a channel message
     * @throws	IOException if a communication problem occurs while
     * 		invoking this method
     */
    void send(byte[] channelId, byte[] message) throws IOException;

    /**
     * Notifies this server that the channel with the specified {@code
     * channelId} is closed.
     *
     * @param	channelId a channel ID
     * @throws	IOException if a communication problem occurs while
     * 		invoking this method
     */
    void close(byte[] channelId) throws IOException;
}
