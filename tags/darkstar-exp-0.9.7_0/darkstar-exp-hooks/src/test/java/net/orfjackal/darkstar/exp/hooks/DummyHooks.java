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

package net.orfjackal.darkstar.exp.hooks;

/**
 * @author Esko Luontola
 * @since 14.7.2008
 */
public final class DummyHooks {

    private DummyHooks() {
    }

    public static class TransformHook implements Hook {
        public String transform(String param) {
            return param;
        }
    }

    public static class UpperCaseTransformHook extends TransformHook {
        public String transform(String param) {
            return param.toUpperCase();
        }
    }

    public static class LowerCaseTransformHook extends TransformHook {
        public String transform(String param) {
            return param.toLowerCase();
        }
    }

    public static class AnotherHook implements Hook {
        public void doSomething() {
            // default implementation
        }
    }

    public static class CustomAnotherHook extends AnotherHook {
        public void doSomething() {
            // custom implementation
        }
    }
}
