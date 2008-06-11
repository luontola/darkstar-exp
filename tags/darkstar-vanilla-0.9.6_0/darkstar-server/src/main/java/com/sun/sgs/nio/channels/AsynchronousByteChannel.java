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

import java.nio.ByteBuffer;
import java.nio.channels.Channel;

public interface AsynchronousByteChannel extends Channel {

    <A> IoFuture<Integer, A> read(ByteBuffer dst, A attachment,
        CompletionHandler<Integer, ? super A> handler);

    <A> IoFuture<Integer, A> read(ByteBuffer dst,
        CompletionHandler<Integer, ? super A> handler);
    <A> IoFuture<Integer, A> write(ByteBuffer src, A attachment,
        CompletionHandler<Integer, ? super A> handler);

    <A> IoFuture<Integer, A> write(ByteBuffer src,
        CompletionHandler<Integer, ? super A> handler);
}
