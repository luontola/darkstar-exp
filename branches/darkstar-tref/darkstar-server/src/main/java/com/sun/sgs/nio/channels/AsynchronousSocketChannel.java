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
import java.nio.ByteBuffer;
import java.nio.channels.AlreadyConnectedException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ConnectionPendingException;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.UnresolvedAddressException;
import java.nio.channels.UnsupportedAddressTypeException;
import java.util.concurrent.TimeUnit;

import com.sun.sgs.nio.channels.ClosedAsynchronousChannelException;
import com.sun.sgs.nio.channels.spi.AsynchronousChannelProvider;

public abstract class AsynchronousSocketChannel extends AsynchronousChannel
    implements AsynchronousByteChannel, NetworkChannel
{
    protected AsynchronousSocketChannel(AsynchronousChannelProvider provider) {
        super(provider);
    }

    public static AsynchronousSocketChannel open(AsynchronousChannelGroup group)
        throws IOException
    {
        return AsynchronousChannelProvider.provider().openAsynchronousSocketChannel(group);
    }

    public static AsynchronousSocketChannel open() throws IOException {
        return open((AsynchronousChannelGroup)null);
    }

    public abstract AsynchronousSocketChannel bind(SocketAddress local)
                                            throws IOException;

    public abstract AsynchronousSocketChannel setOption(SocketOption name,
        Object value) throws IOException;

    public abstract AsynchronousSocketChannel shutdown(ShutdownType how)
        throws IOException;

    public abstract SocketAddress getConnectedAddress() throws IOException;

    public abstract boolean isConnectionPending();

    public abstract boolean isReadPending();

    public abstract boolean isWritePending();

    public abstract <A> IoFuture<Void, A> connect(SocketAddress remote,
        A attachment,
        CompletionHandler<Void, ? super A> handler);

    public final <A> IoFuture<Void, A> connect(SocketAddress remote, 
        CompletionHandler<Void, ? super A> handler)
    {
        return connect(remote, null, handler);
    }

    public abstract <A> IoFuture<Integer, A> read(ByteBuffer dst,
        long timeout,
        TimeUnit unit,
        A attachment,
        CompletionHandler<Integer, ? super A> handler);

    public final <A> IoFuture<Integer, A> read(ByteBuffer dst,
        A attachment,
        CompletionHandler<Integer, ? super A> handler)
    {
        return read(dst, 0L, TimeUnit.NANOSECONDS, attachment, handler);
    }

    public final <A> IoFuture<Integer, A> read(ByteBuffer dst,
        CompletionHandler<Integer, ? super A> handler)
    {
        return read(dst, 0L, TimeUnit.NANOSECONDS, null, handler);
    }

    public abstract <A> IoFuture<Long, A> read(ByteBuffer[] dsts,
        int offset,
        int length,
        long timeout,
        TimeUnit unit,
        A attachment,
        CompletionHandler<Long, ? super A> handler);

    public abstract <A> IoFuture<Integer, A> write(ByteBuffer src,
        long timeout,
        TimeUnit unit,
        A attachment,
        CompletionHandler<Integer, ? super A> handler);

    public final <A> IoFuture<Integer, A> write(ByteBuffer src,
        A attachment,
        CompletionHandler<Integer, ? super A> handler)
    {
        return write(src, 0L, TimeUnit.NANOSECONDS, attachment, handler);
    }

    public final <A> IoFuture<Integer, A> write(ByteBuffer src,
        CompletionHandler<Integer, ? super A> handler)
    {
        return write(src, 0L, TimeUnit.NANOSECONDS, null, handler);
    }

    public abstract <A> IoFuture<Long, A> write(ByteBuffer[] srcs,
        int offset,
        int length,
        long timeout,
        TimeUnit unit,
        A attachment,
        CompletionHandler<Long, ? super A> handler);
}
