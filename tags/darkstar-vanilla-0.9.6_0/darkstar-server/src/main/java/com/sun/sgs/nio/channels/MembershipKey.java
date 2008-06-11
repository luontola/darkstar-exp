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
import java.net.InetAddress;
import java.net.NetworkInterface;

public abstract class MembershipKey {

    protected MembershipKey() {
        // empty
    }

    public abstract boolean isValid();

    public abstract void drop() throws IOException;

    public abstract MembershipKey block(InetAddress source)
        throws IOException;

    public abstract MembershipKey unblock(InetAddress source)
        throws IOException;

    public abstract MulticastChannel getChannel();

    public abstract InetAddress getGroup();

    public abstract NetworkInterface getNetworkInterface();

    public abstract InetAddress getSourceAddress();
}
