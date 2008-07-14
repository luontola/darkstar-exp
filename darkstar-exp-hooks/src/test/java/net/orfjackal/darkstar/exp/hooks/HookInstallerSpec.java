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

import jdave.Group;
import jdave.Specification;
import jdave.junit4.JDaveRunner;
import static net.orfjackal.darkstar.exp.hooks.DummyHooks.*;
import org.junit.runner.RunWith;

import java.util.Properties;

/**
 * @author Esko Luontola
 * @since 14.7.2008
 */
@RunWith(JDaveRunner.class)
@Group({"fast"})
public class HookInstallerSpec extends Specification<Object> {

    private Properties props;
    private HookManager manager;

    public void create() throws Exception {
        props = new Properties();
        manager = new HookManager();
    }


    public class WhenNoHooksHaveBeenConfigured {

        public Object create() {
            HookInstaller.installHooksFromProperties(props, manager);
            return null;
        }

        public void noHooksWillBeInstalled() {
            specify(manager.get(TransformHook.class).getClass(),
                    should.equal(TransformHook.class));
            specify(manager.get(AnotherHook.class).getClass(),
                    should.equal(AnotherHook.class));
        }
    }

    public class WhenOneHookHasBeenConfigured {

        public Object create() {
            props.setProperty(HookInstaller.HOOKS_KEY, UpperCaseTransformHook.class.getName());
            HookInstaller.installHooksFromProperties(props, manager);
            return null;
        }

        public void theSpecifiedHookWillBeInstalled() {
            specify(manager.get(TransformHook.class).getClass(),
                    should.equal(UpperCaseTransformHook.class));
            specify(manager.get(AnotherHook.class).getClass(),
                    should.equal(AnotherHook.class));
        }
    }

    public class WhenManyHooksHaveBeenConfigured {

        public Object create() {
            // many hooks separated by whitespace
            String hookList = " " + UpperCaseTransformHook.class.getName()
                    + "\n    " + CustomAnotherHook.class.getName()
                    + "\n";
            props.setProperty(HookInstaller.HOOKS_KEY, hookList);
            HookInstaller.installHooksFromProperties(props, manager);
            return null;
        }

        public void allTheHooksWillBeInstalled() {
            specify(manager.get(TransformHook.class).getClass(),
                    should.equal(UpperCaseTransformHook.class));
            specify(manager.get(AnotherHook.class).getClass(),
                    should.equal(CustomAnotherHook.class));
        }
    }
}
