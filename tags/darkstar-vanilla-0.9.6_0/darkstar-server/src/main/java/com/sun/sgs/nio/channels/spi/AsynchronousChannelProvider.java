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

package com.sun.sgs.nio.channels.spi;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.ExecutorService;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import com.sun.sgs.nio.channels.AsynchronousChannelGroup;
import com.sun.sgs.nio.channels.AsynchronousDatagramChannel;
import com.sun.sgs.nio.channels.AsynchronousServerSocketChannel;
import com.sun.sgs.nio.channels.AsynchronousSocketChannel;
import com.sun.sgs.nio.channels.ChannelPoolMXBean;
import com.sun.sgs.nio.channels.Channels;
import com.sun.sgs.nio.channels.ManagedChannelFactory;
import com.sun.sgs.nio.channels.ProtocolFamily;
import com.sun.sgs.nio.channels.ShutdownChannelGroupException;

public abstract class AsynchronousChannelProvider {

    private static final Object lock = new Object();

    private static AsynchronousChannelProvider provider = null;

    protected AsynchronousChannelProvider() {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null)
            sm.checkPermission(
                new RuntimePermission("asynchronousChannelProvider"));
    }

    private static boolean loadProviderFromProperty() {
        String cn = System.getProperty(
            "com.sun.sgs.nio.channels.spi.AsynchronousChannelProvider");
        if (cn == null)
            return false;
        try {
            Class<?> c = Class.forName(cn, true,
                ClassLoader.getSystemClassLoader());
            provider = (AsynchronousChannelProvider)c.newInstance();
            return true;
        } catch (ClassNotFoundException x) {
            throw new ExceptionInInitializerError(x);
        } catch (IllegalAccessException x) {
            throw new ExceptionInInitializerError(x);
        } catch (InstantiationException x) {
            throw new ExceptionInInitializerError(x);
        } catch (SecurityException x) {
            throw new ExceptionInInitializerError(x);
        }
    }

    public static AsynchronousChannelProvider provider() {
        synchronized (lock) {
            if (provider != null)
                return provider;
            return AccessController.doPrivileged(
                new PrivilegedAction<AsynchronousChannelProvider>() {
                    public AsynchronousChannelProvider run() {
                        if (loadProviderFromProperty())
                            return provider;
                        provider = com.sun.sgs.impl.nio.DefaultAsyncChannelProvider.create();
                        return provider;
                    }
                });
        }
    }

    public abstract AsynchronousChannelGroup
    openAsynchronousChannelGroup(ExecutorService executor)
        throws IOException;

    public abstract AsynchronousServerSocketChannel
    openAsynchronousServerSocketChannel(AsynchronousChannelGroup group)
        throws IOException;

    public abstract AsynchronousSocketChannel
    openAsynchronousSocketChannel(AsynchronousChannelGroup group)
        throws IOException;

    public abstract AsynchronousDatagramChannel
    openAsynchronousDatagramChannel(ProtocolFamily pf,
                                    AsynchronousChannelGroup group)
        throws IOException;

    public abstract void
    setUncaughtExceptionHandler(Thread.UncaughtExceptionHandler eh);

    public abstract Thread.UncaughtExceptionHandler
    getUncaughtExceptionHandler();
}
