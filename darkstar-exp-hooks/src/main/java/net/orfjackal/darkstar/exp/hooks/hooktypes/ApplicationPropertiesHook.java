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

package net.orfjackal.darkstar.exp.hooks.hooktypes;

import net.orfjackal.darkstar.exp.hooks.Hook;

import java.io.File;
import java.util.Properties;

/**
 * @author Esko Luontola
 * @since 14.7.2008
 */
public class ApplicationPropertiesHook implements Hook {

    public void apply(Properties appProps, File appPropsFile) {
        // NOOP
    }
}
