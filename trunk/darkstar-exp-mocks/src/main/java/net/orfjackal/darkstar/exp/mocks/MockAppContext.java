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

package net.orfjackal.darkstar.exp.mocks;

import com.sun.sgs.app.AppContext;

/**
 * @author Esko Luontola
 * @since 17.6.2008
 */
public class MockAppContext {

    private static MockAppContextResolver instance;

    public static MockAppContextResolver getInstance() {
        if (instance == null) {
            throw new IllegalStateException("MockAppContextResolver is not installed");
        }
        return instance;
    }

    public static void install() {
        if (instance != null) {
            throw new IllegalStateException("Warning: install before uninstall");
        }
        instance = new MockAppContextResolver();
        AppContext.setContextResolver(instance);
    }

    public static void uninstall() {
        if (instance == null) {
            throw new IllegalStateException("Warning: uninstall before install");
        }
        instance = null;
        AppContext.setContextResolver(null);
    }
}
