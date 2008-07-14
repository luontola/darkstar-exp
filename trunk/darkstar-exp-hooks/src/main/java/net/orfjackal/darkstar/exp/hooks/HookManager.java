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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Esko Luontola
 * @since 14.7.2008
 */
public class HookManager {

    private final ConcurrentMap<Class<?>, Object> hooks = new ConcurrentHashMap<Class<?>, Object>();

    public <T extends Hook> T get(Class<T> type) {
        Object hook = hooks.get(type);
        if (hook == null) {
            // use atomic operations to ensure that only one instance of the hook is ever returned
            installHook(createDefaultHook(type));
            hook = hooks.get(type);
            assert hook != null;
        }
        return type.cast(hook);
    }

    public void installHook(Hook hook) {
        for (Class<?> type = hook.getClass(); Hook.class.isAssignableFrom(type); type = type.getSuperclass()) {
            Object previous = hooks.putIfAbsent(type, hook);
            if (previous != null) {
                throw new IllegalArgumentException("A hook of type " + type + " already installed: " + previous);
            }
        }
    }

    private static <T extends Hook> T createDefaultHook(Class<T> type) {
        try {
            return type.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Unable to create a default hook of type " + type, e);
        }
    }
}
