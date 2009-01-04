/*
 * Copyright (c) 2008-2009, Esko Luontola. All Rights Reserved.
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

package com.sun.sgs.impl.service.data.gc;

import com.sun.sgs.impl.sharedutil.LoggerWrapper;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.*;

/**
 * @author Esko Luontola
 * @since 4.1.2009
 */
public class PeriodicGcRunner {
    private static final LoggerWrapper logger = new LoggerWrapper(Logger.getLogger(PeriodicGcRunner.class.getName()));

    public static final int DEFAULT_GC_RUN_INTERVAL = 10000;
    public static final String GC_RUN_INTERVAL_PROPERTY = PeriodicGcRunner.class.getName() + ".gc.run.interval";

    private final GarbageCollector gc;
    private final int gcRunInterval;
    private final AtomicInteger objectsCreated = new AtomicInteger(0);
    private volatile Thread activeGcRunner;

    public PeriodicGcRunner(GarbageCollector gc, int gcRunInterval) {
        this.gc = gc;
        this.gcRunInterval = gcRunInterval;
    }

    public void onObjectCreated() {
        if (objectsCreated.incrementAndGet() >= gcRunInterval) {
            objectsCreated.set(0);
            startGcRunnerThread();
        }
    }

    private synchronized void startGcRunnerThread() {
        if (activeGcRunner == null) {
            activeGcRunner = new Thread(new GcRunner());
            activeGcRunner.setDaemon(true);
            activeGcRunner.start();
        }
    }

    private class GcRunner implements Runnable {
        public void run() {
            logger.log(Level.INFO, "Garbage collection started");
            try {
                gc.runGarbageCollector();
            } catch (Exception e) {
                logger.logThrow(Level.SEVERE, e, "Failure during garbage collection");
            } finally {
                activeGcRunner = null;
            }
            logger.log(Level.INFO, "Garbage collection finished");
        }
    }
}
