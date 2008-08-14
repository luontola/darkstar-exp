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

import com.sun.sgs.app.AppListener;
import com.sun.sgs.app.ClientSession;
import com.sun.sgs.app.ClientSessionListener;
import com.sun.sgs.app.ManagedObject;
import jdave.Group;
import jdave.Specification;
import jdave.junit4.JDaveRunner;
import net.orfjackal.darkstar.exp.hooks.DarkstarExp;
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
        server.setProperty(DarkstarExp.HOOKS, "");
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


    public class WhenAManagedObjectIsReferredDirectly {

        public Object create() throws IOException {
            execOnServer(CREATE_MANAGED_OBJECT);
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

    // Test Application

    private static final byte NOOP = 0;
    private static final byte CREATE_MANAGED_OBJECT = 1;

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
            if (command == CREATE_MANAGED_OBJECT) {
                field = new FooImpl();
            }
        }

        private void printStatus() {
            System.out.println(step + ": is null: " + (field == null));
            System.out.println(step + ": is managed: " + (field instanceof ManagedObject));
            if (field != null) {
                System.out.println(step + ": foo() returns: " + field.foo());
            }
        }

        public void disconnected(boolean graceful) {
        }
    }

    // Test Interfaces

    private interface Foo {
        String foo();
    }

    private static class FooImpl implements Foo, ManagedObject, Serializable {
        private static final long serialVersionUID = 1L;

        public String foo() {
            return "FOO";
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
