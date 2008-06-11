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

import java.net.NetworkInterface;
import java.nio.channels.Channel;
import java.nio.channels.SelectableChannel;

public enum StandardSocketOption implements SocketOption {

    SO_BROADCAST(Boolean.class),

    SO_KEEPALIVE(Boolean.class),

    SO_SNDBUF(Integer.class),

    SO_RCVBUF(Integer.class),

    SO_REUSEADDR(Boolean.class),

    SO_LINGER(Integer.class),

    IP_TOS(Integer.class),

    IP_MULTICAST_IF(NetworkInterface.class),

    IP_MULTICAST_TTL(Integer.class),

    IP_MULTICAST_LOOP(Boolean.class),

    TCP_NODELAY(Boolean.class);

    private final Class<?> type;

    private StandardSocketOption(Class<?> type) {
        this.type = type;
    }

    public final Class<?> type() {
        return type;
    }
}
