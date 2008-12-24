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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * @author Esko Luontola
 * @since 14.7.2008
 */
public class HookInstaller {

    public static void installHooksFromFile(File file, HookManager manager) throws IOException {
        Properties props = new Properties();
        FileInputStream in = new FileInputStream(file);
        props.load(in);
        in.close();
        installHooksFromProperties(props, manager);
    }

    public static void installHooksFromProperties(Properties props, HookManager manager) {
        String types = props.getProperty(DarkstarExp.HOOKS);
        try {
            for (String type : split(types)) {
                Hook hook = toClass(type).newInstance();
                manager.installHook(hook);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid " + DarkstarExp.HOOKS + " property: " + types, e);
        }
    }

    private static List<String> split(String value) {
        List<String> result = new ArrayList<String>();
        if (value != null) {
            for (String s : value.split("\\s+")) {
                if (!s.equals("")) {
                    result.add(s);
                }
            }
        }
        return result;
    }

    private static Class<? extends Hook> toClass(String value) throws ClassNotFoundException {
        Class<?> type = HookInstaller.class.getClassLoader().loadClass(value);
        return type.asSubclass(Hook.class);
    }
}
