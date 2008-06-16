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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import com.sun.sgs.nio.channels.spi.AsynchronousChannelProvider;

public abstract class AsynchronousChannelGroup {

    private final AsynchronousChannelProvider provider;

    protected AsynchronousChannelGroup(AsynchronousChannelProvider provider) {
        if (provider == null)
            throw new NullPointerException("null provider");
        this.provider = provider;
    }

    public final AsynchronousChannelProvider provider() {
        return provider;
    }

    public static AsynchronousChannelGroup open(ExecutorService executor)
        throws IOException
    {
        return AsynchronousChannelProvider.provider().
                    openAsynchronousChannelGroup(executor);
    }

    public abstract boolean isShutdown();

    public abstract boolean isTerminated();

    public abstract AsynchronousChannelGroup shutdown();

    public abstract AsynchronousChannelGroup shutdownNow()
        throws IOException;

    public abstract boolean awaitTermination(long timeout, TimeUnit unit)
        throws InterruptedException;

    public static void
    setDefaultUncaughtExceptionHandler(Thread.UncaughtExceptionHandler eh) {
        AsynchronousChannelProvider.provider().setUncaughtExceptionHandler(eh);
    }

    public static Thread.UncaughtExceptionHandler
    getDefaultUncaughtExceptionHandler() {
        return AsynchronousChannelProvider.provider()
                                            .getUncaughtExceptionHandler();
    }
}
