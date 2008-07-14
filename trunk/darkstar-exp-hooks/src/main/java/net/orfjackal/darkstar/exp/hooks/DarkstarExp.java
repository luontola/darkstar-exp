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
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * @author Esko Luontola
 * @since 14.7.2008
 */
public final class DarkstarExp {
    private static final Logger log = Logger.getLogger(DarkstarExp.class.getName());

    public static final String CONFIG_FILE = "darkstar.exp.config.file";
    public static final String HOOKS = "darkstar.exp.hooks";

    private DarkstarExp() {
    }

    public static void init(Properties appProperties) throws IOException {
        HookManager hookManager = new HookManager();
        if (System.getProperty(HOOKS) != null) {
            log.info("Loading hooks from system property");
            HookInstaller.installHooksFromProperties(System.getProperties(), hookManager);

        } else if (appProperties.getProperty(HOOKS) != null) {
            log.info("Loading hooks from application config");
            HookInstaller.installHooksFromProperties(appProperties, hookManager);

        } else if (System.getProperty(CONFIG_FILE) != null) {
            log.info("Loading hooks from server config");
            HookInstaller.installHooksFromFile(new File(System.getProperty(CONFIG_FILE)), hookManager);

        } else {
            log.warning("Unable to load hooks");
        }
        Hooks.setHookManager(hookManager);
    }
}
