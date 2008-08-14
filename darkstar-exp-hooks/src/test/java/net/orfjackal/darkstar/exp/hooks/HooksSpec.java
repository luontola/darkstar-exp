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

import jdave.Block;
import jdave.Group;
import jdave.Specification;
import jdave.junit4.JDaveRunner;
import static net.orfjackal.darkstar.exp.hooks.DummyHooks.*;
import org.junit.runner.RunWith;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Esko Luontola
 * @since 14.7.2008
 */
@RunWith(JDaveRunner.class)
@Group({"fast"})
public class HooksSpec extends Specification<Object> {

    private HookManager hookManager;

    public void create() throws Exception {
        hookManager = new HookManager();
        Hooks.setHookManager(hookManager);
        Logger.getLogger(HookManager.class.getName()).setLevel(Level.WARNING);
    }

    public void destroy() throws Exception {
        Logger.getLogger(HookManager.class.getName()).setLevel(null);
        Hooks.setHookManager(null);
    }


    public class WhenNoCustomHookHasBeenInstalled {

        private TransformHook hook;

        public Object create() {
            hook = Hooks.get(TransformHook.class);
            return null;
        }

        public void anInstanceOfTheDefaultHookIsReturned() {
            specify(hook.getClass(), should.equal(TransformHook.class));
        }

        public void theInstanceIsCached() {
            TransformHook hook2 = Hooks.get(TransformHook.class);
            specify(hook == hook2);
        }

        public void theHookContainsSomeDefaultBehaviour() {
            specify(hook.transform("foo"), should.equal("foo"));
        }

        public void aCustomHookMayNotBeInstalledAfterTheHookTypeIsUsed() {
            specify(new Block() {
                public void run() throws Throwable {
                    hookManager.installHook(new UpperCaseTransformHook());
                }
            }, should.raise(IllegalArgumentException.class));
        }
    }

    public class WhenACustomHookHasBeenInstalled {

        private UpperCaseTransformHook installed;
        private TransformHook hook;

        public Object create() {
            installed = new UpperCaseTransformHook();
            hookManager.installHook(installed);
            hook = Hooks.get(TransformHook.class);
            return null;
        }

        public void theInstalledCustomHookIsReturned() {
            specify(hook == installed);
        }

        public void theHookContainsSomeCustomBehaviour() {
            specify(hook.transform("foo"), should.equal("FOO"));
        }

        public void aHookOfTheSameTypeMayNotBeInstalled() {
            specify(new Block() {
                public void run() throws Throwable {
                    hookManager.installHook(new LowerCaseTransformHook());
                }
            }, should.raise(IllegalArgumentException.class));
            specify(Hooks.get(TransformHook.class) == installed);
        }

        public void aHookOfAnotherTypeMayBeInstalled() {
            CustomAnotherHook customAnother = new CustomAnotherHook();
            hookManager.installHook(customAnother);
            AnotherHook another = Hooks.get(AnotherHook.class);
            specify(another == customAnother);
        }
    }
}
