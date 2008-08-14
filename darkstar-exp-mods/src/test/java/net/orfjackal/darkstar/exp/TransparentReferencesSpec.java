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
import net.orfjackal.darkstar.integration.util.TempDirectory;
import net.orfjackal.darkstar.tref.ManagedIdentity;
import org.junit.runner.RunWith;

import java.io.Serializable;
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

    private DarkstarServer server;
    private TempDirectory tempDirectory;

    public void create() throws TimeoutException {
        tempDirectory = new TempDirectory();
        server = new DarkstarServer(tempDirectory.getDirectory());
        server.setAppName("TransparentReferencesTestApp");
        server.setAppListener(TransparentReferencesTestApp.class);
        server.setProperty(DarkstarExp.HOOKS, "");
        server.waitForApplicationReady(TIMEOUT);
    }

    public void destroy() {
        try {
            server.shutdown();
        } finally {
            tempDirectory.dispose();
        }
    }

    public class WithTransparentReferences {

        public Object create() {
            return null;
        }

        public void todo() {
            // TODO
        }
    }


    public static class TransparentReferencesTestApp implements AppListener, Serializable {
        private static final long serialVersionUID = 1L;

        public void initialize(Properties props) {
        }

        public ClientSessionListener loggedIn(ClientSession session) {
            return null;
        }
    }

    public interface Foo {
        String foo();
    }

    public static class FooImpl implements Foo, ManagedObject, Serializable {
        private static final long serialVersionUID = 1202794677805487620L;

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
