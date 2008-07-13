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

package com.sun.sgs.app;

import com.sun.sgs.impl.kernel.ContextResolver;

/**
 * @author Esko Luontola
 * @since 17.6.2008
 */
public class AppContextResolverImpl implements AppContextResolver {

    public ChannelManager getChannelManager() {
        return ContextResolver.getChannelManager();
    }

    public DataManager getDataManager() {
        return ContextResolver.getDataManager();
    }

    public TaskManager getTaskManager() {
        return ContextResolver.getTaskManager();
    }

    public <T> T getManager(Class<T> type) {
        return ContextResolver.getManager(type);
    }
}
