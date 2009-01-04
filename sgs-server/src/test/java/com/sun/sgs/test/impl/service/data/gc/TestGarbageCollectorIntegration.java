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

import com.sun.sgs.app.*;
import com.sun.sgs.auth.Identity;
import com.sun.sgs.impl.service.data.DataServiceImpl;
import com.sun.sgs.impl.service.data.gc.GarbageCollector;
import com.sun.sgs.kernel.TransactionScheduler;
import com.sun.sgs.service.DataService;
import com.sun.sgs.test.util.*;
import junit.framework.TestCase;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.*;

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

    private void assertNodesExist(final boolean expectedExists, final List<BigInteger> nodes) throws Exception {
        txnScheduler.runTask(new TestAbstractKernelRunnable() {
            public void run() throws Exception {
                for (BigInteger id : nodes) {
                    assertEquals("existence of node " + id + " of " + nodes,
                            expectedExists, nodeExists(id));
                }
            }
        }, taskOwner);
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
        assertNodesExist(true, Arrays.asList(
                liveRootId, liveRefId,
                garbageRootId, garbageRefId, garbageCycle1Id, garbageCycle2Id));
    }

    public void testWhenGarbageCollectorIsRunTheLiveNodesExist() throws Exception {
        runGarbageCollector();
        assertNodesExist(true, Arrays.asList(liveRootId, liveRefId));
    }

    public void testWhenGarbageCollectorIsRunTheGarbageRootsAreRemoved() throws Exception {
        runGarbageCollector();
        assertNodesExist(false, Arrays.asList(garbageRootId));
    }

    public void testWhenGarbageCollectorIsRunTheGarbageNodesAreRemoved() throws Exception {
        runGarbageCollector();
        assertNodesExist(false, Arrays.asList(garbageRefId));
    }

    public void testWhenGarbageCollectorIsRunTheGarbageCyclesAreRemoved() throws Exception {
        runGarbageCollector();
        assertNodesExist(false, Arrays.asList(garbageCycle1Id, garbageCycle2Id));
    }


    public void testConcurrentMutatorsDuringGarbageCollection() throws Exception {
        prepareListOfLiveNodes();
        final List<BigInteger> liveNodesCreated = new ArrayList<BigInteger>();
        Thread t = new Thread(new Runnable() {
            public void run() {
                for (int i = 0; i < 10; i++) {
                    createNewLiveNode(liveNodesCreated);
                    Thread.yield();
                }
            }
        });
        t.start();
        runGarbageCollector();
        t.join();
        assertNodesExist(true, liveNodesCreated);
    }

    private void prepareListOfLiveNodes() throws Exception {
        txnScheduler.runTask(new TestAbstractKernelRunnable() {
            public void run() throws Exception {
                dataService.setBinding("liveList", new ListNode());
            }
        }, taskOwner);
    }

    private void createNewLiveNode(final List<BigInteger> liveNodesCreated) {
        try {
            txnScheduler.runTask(new TestAbstractKernelRunnable() {
                public void run() throws Exception {
                    ManagedReference<DummyNode> created = dataService.createReference(new DummyNode());
                    ListNode liveList = (ListNode) dataService.getBinding("liveList");
                    liveList.refs.add(created);
                    liveNodesCreated.add(created.getId());
                }
            }, taskOwner);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private static class DummyNode implements ManagedObject, Serializable {
        public ManagedReference<DummyNode> ref;
    }

    private static class ListNode implements ManagedObject, Serializable {
        public final List<ManagedReference<DummyNode>> refs = new ArrayList<ManagedReference<DummyNode>>();
    }
}
