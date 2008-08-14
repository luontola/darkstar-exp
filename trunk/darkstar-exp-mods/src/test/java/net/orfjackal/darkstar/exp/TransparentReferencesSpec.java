/*
 * Copyright (c) 2008, Esko Luontola. All Rights Reserved.
 *
 * This file is part of Darkstar EXP.
 *
 * Darkstar EXP is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation and distributed hereunder to you.
 *
 * Darkstar EXP is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.orfjackal.darkstar.exp;

import com.sun.sgs.app.*;
import jdave.Group;
import jdave.Specification;
import jdave.junit4.JDaveRunner;
import net.orfjackal.darkstar.exp.hooks.DarkstarExp;
import net.orfjackal.darkstar.exp.mods.TransparentReferencesHook1;
import net.orfjackal.darkstar.exp.mods.TransparentReferencesHook2;
import net.orfjackal.darkstar.integration.DarkstarServer;
import net.orfjackal.darkstar.integration.DebugClient;
import net.orfjackal.darkstar.integration.util.TempDirectory;
import net.orfjackal.darkstar.integration.util.TimedInterrupt;
import net.orfjackal.darkstar.tref.ManagedIdentity;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

/**
 * @author Esko Luontola
 * @since 14.8.2008
 */
@RunWith(JDaveRunner.class)
@Group({"fast"})
public class TransparentReferencesSpec extends Specification<Object> {

    private static final int TIMEOUT = 5000;
    private static final boolean DEBUG = true;

    private DarkstarServer server;
    private DebugClient client;

    private TempDirectory tempDirectory;
    private Thread testTimeout;

    public void create() throws TimeoutException, InterruptedException {
        tempDirectory = new TempDirectory();
        tempDirectory.create();

        server = new DarkstarServer(tempDirectory.getDirectory());
        server.setAppName("MyAppListener");
        server.setAppListener(MyAppListener.class);
        server.setProperty(DarkstarExp.HOOKS, "" +
                TransparentReferencesHook1.class.getName() + "\n" +
                TransparentReferencesHook2.class.getName());
        server.start();

        server.waitForApplicationReady(TIMEOUT);
        testTimeout = TimedInterrupt.startOnCurrentThread(TIMEOUT);

        client = new DebugClient("localhost", server.getPort());
        client.login();
        String event = client.events.take();
        specify(event, event.startsWith(DebugClient.LOGGED_IN));
    }

    public void destroy() {
        try {
            if (DEBUG) {
                System.out.println("Server Out:");
                System.out.println(server.getSystemOut());
                System.err.println("Server Log:");
                System.err.println(server.getSystemErr());
            }
            client.logout(true);
            testTimeout.interrupt();
            server.shutdown();
        } finally {
            tempDirectory.dispose();
        }
    }

    private void execOnServer(byte command) throws IOException {
        client.send((ByteBuffer) ByteBuffer
                .allocate(1).put(command).flip());
    }


    public class ReferringAManagedObjectKnownByTheDataManager {

        public Object create() throws IOException {
            execOnServer(REFER_KNOWN_MANAGED_OBJECT);
            return null;
        }

        public void duringTheFirstTaskItIsReferredDirectly() throws TimeoutException {
            server.waitUntilSystemOutContains("1: is null: false", TIMEOUT);
            server.waitUntilSystemOutContains("1: is managed: true", TIMEOUT);
            server.waitUntilSystemOutContains("1: foo() returns: FOO", TIMEOUT);
        }

        public void duringTheNextTaskItIsReferredThroughATransparentReference() throws Exception {
            execOnServer(NOOP);
            server.waitUntilSystemOutContains("2: is null: false", TIMEOUT);
            server.waitUntilSystemOutContains("2: is managed: false", TIMEOUT);
            server.waitUntilSystemOutContains("2: foo() returns: FOO", TIMEOUT);
        }
    }

    public class ReferringANewlyCreatedManagedObject {

        public Object create() throws IOException {
            execOnServer(REFER_NEW_MANAGED_OBJECT);
            return null;
        }

        public void duringTheFirstTaskItIsReferredDirectly() throws TimeoutException {
            server.waitUntilSystemOutContains("1: is null: false", TIMEOUT);
            server.waitUntilSystemOutContains("1: is managed: true", TIMEOUT);
            server.waitUntilSystemOutContains("1: foo() returns: FOO", TIMEOUT);
        }

        public void duringTheNextTaskItIsReferredThroughATransparentReference() throws Exception {
            execOnServer(NOOP);
            server.waitUntilSystemOutContains("2: is null: false", TIMEOUT);
            server.waitUntilSystemOutContains("2: is managed: false", TIMEOUT);
            server.waitUntilSystemOutContains("2: foo() returns: FOO", TIMEOUT);
        }
    }

    public class CyclicReference {

        public Object create() throws IOException {
            execOnServer(CRETE_CYCLIC_REFERENCE);
            return null;
        }

        public void duringTheFirstTaskTheIdentitiesAreNotDuplicated() throws TimeoutException {
            server.waitUntilSystemOutContains("1: cycle identity ok: true", TIMEOUT);
        }

        /**
         * This test will cause an indefinite loop in flushing objects unless
         * com.sun.sgs.impl.service.data.ReferenceTable#unregisterObject
         * is modified to unregister the objects only after flushing is finished.
         */
        public void duringTheNextTaskTheIdentitiesAreNotDuplicated() throws Exception {
            execOnServer(NOOP);
            server.waitUntilSystemOutContains("2: cycle identity ok: true", TIMEOUT);
        }
    }

    // Test Application

    private static final byte NOOP = 0;
    private static final byte REFER_KNOWN_MANAGED_OBJECT = 1;
    private static final byte REFER_NEW_MANAGED_OBJECT = 2;
    private static final byte CRETE_CYCLIC_REFERENCE = 3;

    public static class MyAppListener implements AppListener, Serializable {
        private static final long serialVersionUID = 1L;

        public void initialize(Properties props) {
        }

        public ClientSessionListener loggedIn(ClientSession session) {
            return new MyClientSessionListener();
        }
    }

    private static class MyClientSessionListener implements ClientSessionListener, Serializable {
        private static final long serialVersionUID = 1L;

        private int step = 0;
        private Foo field;

        public void receivedMessage(ByteBuffer message) {
            step++;
            exec(message.get());
            printStatus();
        }

        private void exec(int command) {
            if (command == REFER_KNOWN_MANAGED_OBJECT) {
                field = new FooImpl();
                AppContext.getDataManager().createReference(field);
            }
            if (command == REFER_NEW_MANAGED_OBJECT) {
                field = new FooImpl();
            }
            if (command == CRETE_CYCLIC_REFERENCE) {
                field = new FooImpl();
                Foo other = new FooImpl();
                field.setOther(other);
                other.setOther(field);
            }
        }

        private void printStatus() {
            System.out.println("-");
            println("is null", field == null);
            println("is managed", field instanceof ManagedObject);
            if (field != null) {
                println("foo() returns", field.foo());

                if (field.getOther() != null) {
                    boolean ok = field.equals(field.getOther().getOther())
                            && !field.equals(field.getOther());
                    println("cycle identity ok", ok);
                    println("field hashCode", field.hashCode());
                    println("other hashCode", field.getOther().hashCode());
                    println("cycle hashCode", field.getOther().getOther().hashCode());
                }
            }
        }

        private void println(String label, Object value) {
            System.out.println(step + ": " + label + ": " + value);
        }

        public void disconnected(boolean graceful) {
        }
    }

    // Test Interfaces

    private interface Foo {

        String foo();

        Foo getOther();

        void setOther(Foo other);
    }

    private static class FooImpl implements Foo, ManagedObject, Serializable {
        private static final long serialVersionUID = 1L;

        private Foo other;

        public String foo() {
            return "FOO";
        }

        public Foo getOther() {
            return other;
        }

        public void setOther(Foo other) {
            this.other = other;
        }

        @SuppressWarnings({"EqualsWhichDoesntCheckParameterClass"})
        public boolean equals(Object obj) {
            return ManagedIdentity.equals(this, obj);
        }

        public int hashCode() {
            return ManagedIdentity.hashCode(this);
        }
    }
}
