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

package net.orfjackal.sgs;

import com.sun.sgs.app.*;

import java.math.BigInteger;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * @author Esko Luontola
 * @since 17.6.2008
 */
public class MockAppContextResolver implements AppContextResolver {

    private final MockDataManager dataManager = new MockDataManager();

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

    private static class MockDataManager implements DataManager {

        private final Map<Object, ManagedReference<Object>> refs = new IdentityHashMap<Object, ManagedReference<Object>>();

        public ManagedObject getBinding(String name) {
            return null;
        }

        public void setBinding(String name, Object object) {
        }

        public void removeBinding(String name) {
        }

        public String nextBoundName(String name) {
            return null;
        }

        public void removeObject(Object object) {
        }

        public void markForUpdate(Object object) {
        }

        public <T> ManagedReference<T> createReference(T object) {
            ManagedReference<T> ref = (ManagedReference<T>) refs.get(object);
            if (ref == null) {
                ref = new MockManagedReference<T>(object);
                refs.put(object, (ManagedReference<Object>) ref);
            }
            return ref;
        }
    }

    private static class MockManagedReference<T> implements ManagedReference<T> {

        private final T object;

        public MockManagedReference(T object) {
            this.object = object;
        }

        public T get() {
            return object;
        }

        public T getForUpdate() {
            return object;
        }

        public BigInteger getId() {
            return BigInteger.valueOf(System.identityHashCode(object));
        }
    }
}
