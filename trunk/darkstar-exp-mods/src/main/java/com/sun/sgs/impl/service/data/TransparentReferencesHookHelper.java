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

package com.sun.sgs.impl.service.data;

/**
 * @author Esko Luontola
 * @since 14.8.2008
 */
public class TransparentReferencesHookHelper {

    public static void flushModifiedObjects() {
        Context context = getActiveContext();
        if (context != null) {
            context.refs.flushModifiedObjects();
        }
    }

    private static Context getActiveContext() {
        try {
            return DataServiceImpl.getContextNoJoin();
        } catch (RuntimeException e) {
            // no active context, possibly because we are in a non-durable task
            return null;
        }
    }
}
