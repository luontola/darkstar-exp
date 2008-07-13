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

import com.sun.sgs.app.AppContextResolver;
import com.sun.sgs.app.ChannelManager;
import com.sun.sgs.app.DataManager;
import com.sun.sgs.app.TaskManager;

/**
 * @author Esko Luontola
 * @since 17.6.2008
 */
public class MockAppContextResolver implements AppContextResolver {

    // TODO: implement channel and task managers, add support for changing the managers

    public final MockDataManager dataManager = new MockDataManager();

    public ChannelManager getChannelManager() {
        throw new UnsupportedOperationException();
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public TaskManager getTaskManager() {
        throw new UnsupportedOperationException();
    }

    public <T> T getManager(Class<T> type) {
        throw new UnsupportedOperationException();
    }
}
