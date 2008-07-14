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

package net.orfjackal.darkstar.exp.mods;

import com.sun.sgs.impl.kernel.StandardProperties;
import net.orfjackal.darkstar.exp.hooks.hooktypes.ApplicationPropertiesHook;

import java.io.File;
import java.util.Properties;

/**
 * Interpret app root as relative to the properties file.
 * <p/>
 * Insert in {@link com.sun.sgs.impl.kernel.Kernel#main(String[])}
 * right before {@code new Kernel(appProperties)}
 *
 * @author Esko Luontola
 * @since 19.6.2008
 */
public class AppRootAsRelativeToAppPropertiesFileHook extends ApplicationPropertiesHook {

    public void apply(Properties appProps, File appPropsFile) {
        String appRootProp = appProps.getProperty(StandardProperties.APP_ROOT);
        File appRoot;
        if (new File(appRootProp).isAbsolute()) {
            appRoot = new File(appRootProp);
        } else {
            File propertiesDir = appPropsFile.getParentFile();
            appRoot = new File(propertiesDir, appRootProp);
        }
        appProps.setProperty(StandardProperties.APP_ROOT, appRoot.getAbsolutePath());
    }
}
