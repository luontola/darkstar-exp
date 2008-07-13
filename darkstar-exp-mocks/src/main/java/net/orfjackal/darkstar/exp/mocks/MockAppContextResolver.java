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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * @author Esko Luontola
 * @since 17.6.2008
 */
public class MockAppContextResolver implements AppContextResolver {

    private static MockAppContextResolver instance;

    public static MockAppContextResolver getInstance() {
        if (instance == null) {
            throw new IllegalStateException("MockAppContextResolver is not installed");
        }
        return instance;
    }

    public static void install() {
        if (instance != null) {
            throw new IllegalStateException("Warning: install before uninstall");
        }
        instance = new MockAppContextResolver();
        AppContext.setContextResolver(instance);
    }

    public static void uninstall() {
        if (instance == null) {
            throw new IllegalStateException("Warning: uninstall before install");
        }
        instance = null;
        AppContext.setContextResolver(null);
    }


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
        private final Map<Long, ManagedObject> objects = new HashMap<Long, ManagedObject>();
        private volatile long nextObjectId = 1;

        public ManagedObject getBinding(String name) {
            throw new UnsupportedOperationException();
        }

        public void setBinding(String name, Object object) {
            throw new UnsupportedOperationException();
        }

        public void removeBinding(String name) {
            throw new UnsupportedOperationException();
        }

        public String nextBoundName(String name) {
            throw new UnsupportedOperationException();
        }

        public void removeObject(Object object) {
            if (!refs.containsKey(object)) {
                throw new IllegalArgumentException("Not in data store: " + object);
            }
            if (object instanceof ManagedObjectRemoval) {
                ManagedObjectRemoval rem = (ManagedObjectRemoval) object;
                rem.removingObject();
            }
            ManagedReference<Object> ref = refs.remove(object);
            objects.remove(ref.getId().longValue());
        }

        public void markForUpdate(Object object) {
            // do nothing - the objects are always in memory
        }

        public <T> ManagedReference<T> createReference(T object) {
            ManagedReference<T> ref = (ManagedReference<T>) refs.get(object);
            if (ref == null) {
                ref = new MockManagedReference<T>(nextObjectId++, object);
                refs.put(object, (ManagedReference<Object>) ref);
                objects.put(ref.getId().longValue(), (ManagedObject) object);
            }
            return ref;
        }
    }


    private static class MockManagedReference<T> implements ManagedReference<T>, Serializable {

        private static final long serialVersionUID = 1L;

        private final long oid;
        private transient ManagedObject object;

        public MockManagedReference(long oid, T object) {
            this.oid = oid;
            this.object = (ManagedObject) object;
        }

        public T get() {
            return (T) object;
        }

        public T getForUpdate() {
            return (T) object;
        }

        public BigInteger getId() {
            return BigInteger.valueOf(oid);
        }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            this.object = getInstance().dataManager.objects.get(oid);
        }
    }
}
