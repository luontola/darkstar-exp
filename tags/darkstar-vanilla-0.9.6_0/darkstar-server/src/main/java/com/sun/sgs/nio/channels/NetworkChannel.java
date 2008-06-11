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

package com.sun.sgs.nio.channels;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.Channel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.UnsupportedAddressTypeException;
import java.util.Set;

public interface NetworkChannel extends Channel {

    NetworkChannel bind(SocketAddress local) throws IOException;

    SocketAddress getLocalAddress() throws IOException;

    NetworkChannel setOption(SocketOption name, Object value)
        throws IOException;

    Object getOption(SocketOption name) throws IOException;

    Set<SocketOption> options();
}
