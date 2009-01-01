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

package com.sun.sgs.test.impl.service.gc;

import com.sun.sgs.app.*;
import com.sun.sgs.auth.Identity;
import com.sun.sgs.impl.service.data.DataServiceImpl;
import com.sun.sgs.kernel.TransactionScheduler;
import com.sun.sgs.service.*;
import com.sun.sgs.test.util.*;
import junit.framework.TestCase;

import java.io.Serializable;
import java.math.BigInteger;

/**
 * @author Esko Luontola
 * @since 1.1.2009
 */
public class TestGarbageCollectorIntegration extends TestCase {

    private SgsTestNode serverNode;
    private TransactionScheduler txnScheduler;
    private DataService dataService;
    private Identity taskOwner;

    private GarbageCollector gc;

    private BigInteger liveRootId;
    private BigInteger liveRefId;
    private BigInteger garbageRootId;
    private BigInteger garbageRefId;
    private BigInteger garbageCycle1Id;
    private BigInteger garbageCycle2Id;

    protected void setUp() throws Exception {
        serverNode = new SgsTestNode("TestGarbageCollectorIntegration", null, null);
        txnScheduler = serverNode.getSystemRegistry().getComponent(TransactionScheduler.class);
        dataService = serverNode.getDataService();
        taskOwner = serverNode.getProxy().getCurrentOwner();

        gc = ((DataServiceImpl) dataService).gc;
        initGraphNoGarbage();
        createGarbage();
    }

    private void initGraphNoGarbage() throws Exception {
        txnScheduler.runTask(new TestAbstractKernelRunnable() {
            public void run() throws Exception {
                DummyNode liveRoot = new DummyNode();
                DummyNode liveRef = new DummyNode();
                liveRoot.ref = dataService.createReference(liveRef);
                dataService.setBinding("live", liveRoot);

                DummyNode garbageRoot = new DummyNode();
                DummyNode garbageRef = new DummyNode();
                DummyNode garbageCycle1 = new DummyNode();
                DummyNode garbageCycle2 = new DummyNode();
                garbageRoot.ref = dataService.createReference(garbageRef);
                garbageRef.ref = dataService.createReference(garbageCycle1);
                garbageCycle1.ref = dataService.createReference(garbageCycle2);
                garbageCycle2.ref = dataService.createReference(garbageCycle1);
                dataService.setBinding("garbage", garbageRoot);

                liveRootId = getId(liveRoot);
                liveRefId = getId(liveRef);
                garbageRootId = getId(garbageRoot);
                garbageRefId = getId(garbageRef);
                garbageCycle1Id = getId(garbageCycle1);
                garbageCycle2Id = getId(garbageCycle2);
            }
        }, taskOwner);
    }

    private void createGarbage() throws Exception {
        txnScheduler.runTask(new TestAbstractKernelRunnable() {
            public void run() throws Exception {
                dataService.removeBinding("garbage");
            }
        }, taskOwner);
    }

    private void runGarbageCollector() throws Exception {
        gc.runGarbageCollector();
    }

    private BigInteger getId(Object obj) {
        return dataService.createReference(obj).getId();
    }

    private boolean nodeExists(BigInteger id) {
        try {
            ManagedReference<?> ref = dataService.createReferenceForId(id);
            ref.get();
            return true;
        } catch (ObjectNotFoundException e) {
            return false;
        }
    }

    protected void tearDown() throws Exception {
        serverNode.shutdown(true);
    }


    public void testBeforeGarbageCollectorIsRunAllNodesExist() throws Exception {
        txnScheduler.runTask(new TestAbstractKernelRunnable() {
            public void run() throws Exception {
                assertTrue(nodeExists(liveRootId));
                assertTrue(nodeExists(liveRefId));
                assertTrue(nodeExists(garbageRootId));
                assertTrue(nodeExists(garbageRefId));
                assertTrue(nodeExists(garbageCycle1Id));
                assertTrue(nodeExists(garbageCycle2Id));
            }
        }, taskOwner);
    }

    public void testWhenGarbageCollectorIsRunTheLiveNodesExist() throws Exception {
        runGarbageCollector();
        txnScheduler.runTask(new TestAbstractKernelRunnable() {
            public void run() throws Exception {
                assertTrue(nodeExists(liveRootId));
                assertTrue(nodeExists(liveRefId));
            }
        }, taskOwner);
    }

    public void testWhenGarbageCollectorIsRunTheGarbageRootsAreRemoved() throws Exception {
        runGarbageCollector();
        txnScheduler.runTask(new TestAbstractKernelRunnable() {
            public void run() throws Exception {
                assertFalse(nodeExists(garbageRootId));
            }
        }, taskOwner);
    }

    public void testWhenGarbageCollectorIsRunTheGarbageNodesAreRemoved() throws Exception {
        runGarbageCollector();
        txnScheduler.runTask(new TestAbstractKernelRunnable() {
            public void run() throws Exception {
                assertFalse(nodeExists(garbageRefId));
            }
        }, taskOwner);
    }

    public void testWhenGarbageCollectorIsRunTheGarbageCyclesAreRemoved() throws Exception {
        runGarbageCollector();
        txnScheduler.runTask(new TestAbstractKernelRunnable() {
            public void run() throws Exception {
                assertFalse(nodeExists(garbageCycle1Id));
                assertFalse(nodeExists(garbageCycle2Id));
            }
        }, taskOwner);
    }

    // TODO: concurrent mutators


    private static class DummyNode implements ManagedObject, Serializable {
        public ManagedReference<DummyNode> ref;
    }
}
