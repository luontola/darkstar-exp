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

package net.orfjackal.darkstar.exp.mocks;

import com.sun.sgs.app.*;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Esko Luontola
 * @since 17.6.2008
 */
public class MockAppContextResolver implements AppContextResolver {

    public MockChannelManager channelManager = new MockChannelManager();
    public MockDataManager dataManager = new MockDataManager();
    public MockTaskManager taskManager = new MockTaskManager();
    public final Map<Class<?>, Object> managers = new HashMap<Class<?>, Object>();

    /**
     * @see com.sun.sgs.impl.kernel.KernelContext#getChannelManager()
     */
    public ChannelManager getChannelManager() {
        if (channelManager == null) {
            throw new ManagerNotFoundException("this application is running without a ChannelManager");
        }
        return channelManager;
    }

    /**
     * @see com.sun.sgs.impl.kernel.KernelContext#getDataManager()
     */
    public DataManager getDataManager() {
        if (dataManager == null) {
            throw new ManagerNotFoundException("this application is running without a DataManager");
        }
        return dataManager;
    }

    /**
     * @see com.sun.sgs.impl.kernel.KernelContext#getTaskManager()
     */
    public TaskManager getTaskManager() {
        if (taskManager == null) {
            throw new ManagerNotFoundException("this application is running without a TaskManager");
        }
        return taskManager;
    }

    /**
     * @see com.sun.sgs.impl.kernel.KernelContext#getManager(Class<T>)
     */
    public <T> T getManager(Class<T> type) {
        Object manager = managers.get(type);
        if (manager == null) {
            throw new ManagerNotFoundException("couldn't find manager: " + type.getName());
        }
        return type.cast(manager);
    }
}
