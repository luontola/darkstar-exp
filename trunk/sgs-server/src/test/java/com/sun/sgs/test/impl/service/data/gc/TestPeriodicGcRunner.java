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

package com.sun.sgs.test.impl.service.data.gc;

import com.sun.sgs.impl.service.data.gc.*;
import junit.framework.TestCase;

import java.math.BigInteger;
import java.util.Set;

/**
 * @author Esko Luontola
 * @since 4.1.2009
 */
public class TestPeriodicGcRunner extends TestCase {

    private PeriodicGcRunner gcRunner;
    private DummyGarbageCollector gc;

    protected void setUp() throws Exception {
        gc = new DummyGarbageCollector();
        gcRunner = new PeriodicGcRunner(gc, 2);
    }

    private boolean gcWasRun() {
        try {
            Thread.sleep(10);
            return gc.wasRun;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            gc.wasRun = false;
        }
    }


    public void testGcIsNotRunBeforeTheObjectCreationLimitIsReached() {
        gcRunner.onObjectCreated();
        assertFalse(gcWasRun());
    }

    public void testGcIsRunWhenTheObjectCreationLimitIsReached() {
        gcRunner.onObjectCreated();
        gcRunner.onObjectCreated();
        assertTrue(gcWasRun());
    }

    public void testTheNextGcRunIsAfterTheObjectCreationLimitIsReachedASecondTime() {
        gcRunner.onObjectCreated();
        gcRunner.onObjectCreated();
        assertTrue(gcWasRun());

        gcRunner.onObjectCreated();
        assertFalse(gcWasRun());
        gcRunner.onObjectCreated();
        assertTrue(gcWasRun());
    }


    private static class DummyGarbageCollector implements GarbageCollector {
        public volatile boolean wasRun = false;

        public void runGarbageCollector() throws Exception {
            wasRun = true;
        }

        public void fireObjectModified(BigInteger source, Set<BigInteger> targets) {
        }
    }
}
