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

package com.sun.sgs.test.impl.service.data;

import com.sun.sgs.app.ManagedObject;
import com.sun.sgs.auth.Identity;
import com.sun.sgs.kernel.TransactionScheduler;
import com.sun.sgs.service.DataService;
import com.sun.sgs.test.util.*;
import junit.framework.TestCase;
import net.orfjackal.dimdwarf.api.internal.Entities;

import java.io.Serializable;

/**
 * @author Esko Luontola
 * @since 24.12.2008
 */
public class TestTransparentReferenceIntegration extends TestCase {

    private SgsTestNode serverNode;
    private TransactionScheduler txnScheduler;
    private DataService dataService;
    private Identity taskOwner;

    protected void setUp() throws Exception {
        serverNode = new SgsTestNode("TestTransparentReferenceIntegration", null, null);
        txnScheduler = serverNode.getSystemRegistry().getComponent(TransactionScheduler.class);
        dataService = serverNode.getDataService();
        taskOwner = serverNode.getProxy().getCurrentOwner();
    }

    protected void tearDown() throws Exception {
        serverNode.shutdown(true);
    }


    public void testReferencedManagedObjectsAreReplacedWithProxies() throws Exception {
        txnScheduler.runTask(new TestAbstractKernelRunnable() {
            public void run() throws Exception {
                MyInterface a = new MyObject("A");
                MyInterface b = new MyObject("B");
                a.setOther(b);
                dataService.setBinding("a", a);
                assertEquals("A", a.getName());
                assertEquals("B", b.getName());
                assertIsEntity(a);
                assertIsEntity(b);
            }
        }, taskOwner);
        txnScheduler.runTask(new TestAbstractKernelRunnable() {
            public void run() throws Exception {
                MyInterface a = (MyInterface) dataService.getBinding("a");
                MyInterface b = (MyInterface) a.getOther();
                assertEquals("A", a.getName());
                assertEquals("B", b.getName());
                assertIsEntity(a);
                assertIsProxy(b);
            }
        }, taskOwner);
    }

    public void testCyclicManagedObjectGraphsMustNotCauseInfiniteRecursionOnSerialization() throws Exception {
        txnScheduler.runTask(new TestAbstractKernelRunnable() {
            public void run() throws Exception {
                MyInterface a = new MyObject("A");
                MyInterface b = new MyObject("B");
                a.setOther(b);
                b.setOther(a);
                dataService.setBinding("a", a);
            }
        }, taskOwner);
    }


    private static void assertIsEntity(Object obj) {
        assertTrue(Entities.isEntity(obj));
        assertFalse(Entities.isTransparentReference(obj));
    }

    private static void assertIsProxy(Object obj) {
        assertFalse(Entities.isEntity(obj));
        assertTrue(Entities.isTransparentReference(obj));
    }


    private static class MyObject implements MyInterface, ManagedObject, Serializable {

        private final String name;
        private Object other;

        public MyObject(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public Object getOther() {
            return other;
        }

        public void setOther(Object other) {
            this.other = other;
        }
    }

    private interface MyInterface {

        String getName();

        Object getOther();

        void setOther(Object other);
    }
}
