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
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.Channel;
import java.util.concurrent.ExecutionException;

import com.sun.sgs.nio.channels.spi.AsynchronousChannelProvider;

public abstract class AsynchronousChannel implements Channel {

    private final AsynchronousChannelProvider provider;

    protected AsynchronousChannel(AsynchronousChannelProvider provider) {
        if (provider == null)
            throw new NullPointerException("null provider");
        this.provider = provider;
    }

    public final AsynchronousChannelProvider provider() {
        return provider;
    }

    public abstract void close() throws IOException;

}
