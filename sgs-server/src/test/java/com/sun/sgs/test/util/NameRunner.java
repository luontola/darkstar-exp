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

package com.sun.sgs.test.util;

import org.junit.runner.Description;

import org.junit.runners.model.InitializationError;

import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;

import org.junit.runners.BlockJUnit4ClassRunner;

/**
 * This is a custom implementation of JUunit4's <code>TestClassRunner</code>
 * that adds support for reporting the name of each test when it starts.
 */
public class NameRunner extends BlockJUnit4ClassRunner {

    private static String testName;

    public NameRunner(Class<?> c) throws InitializationError, Exception {
        super(c);
        filter(new NamedTestFilter());
    }

    public void run(RunNotifier runNotifier) {
        runNotifier.addListener(new RunListenerImpl());
        super.run(runNotifier);
    }

    private class RunListenerImpl extends RunListener {
        public void testStarted(Description description) throws Exception {
            if (description.isTest())
                System.err.println("Testcase: " +
                                   description.getDisplayName());
        }
    }

}
