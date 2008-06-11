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
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NotYetBoundException;
import java.nio.channels.UnsupportedAddressTypeException;
import java.util.concurrent.ExecutionException;

import com.sun.sgs.nio.channels.spi.AsynchronousChannelProvider;

public abstract class AsynchronousServerSocketChannel
    extends AsynchronousChannel implements NetworkChannel
{
    protected AsynchronousServerSocketChannel(AsynchronousChannelProvider provider)
    {
        super(provider);
    }

    public static AsynchronousServerSocketChannel open(
        AsynchronousChannelGroup group) throws IOException
    {
        return AsynchronousChannelProvider.provider()
            .openAsynchronousServerSocketChannel(group);
    }

    public static AsynchronousServerSocketChannel open() throws IOException {
        return open((AsynchronousChannelGroup) null);
    }

    public final AsynchronousServerSocketChannel bind(SocketAddress local)
        throws IOException
    {
        return bind(local, 0);
    }

    public abstract AsynchronousServerSocketChannel bind(SocketAddress local,
        int backlog) throws IOException;

    public abstract AsynchronousServerSocketChannel setOption(
        SocketOption name, Object value) throws IOException;

    public abstract <A> IoFuture<AsynchronousSocketChannel, A> accept(
        A attachment,
        CompletionHandler<AsynchronousSocketChannel, ? super A> handler);

    public final <A> IoFuture<AsynchronousSocketChannel, A> accept(
        CompletionHandler<AsynchronousSocketChannel, ? super A> handler)
    {
        return accept(null, handler);
    }

    public abstract boolean isAcceptPending();
}
