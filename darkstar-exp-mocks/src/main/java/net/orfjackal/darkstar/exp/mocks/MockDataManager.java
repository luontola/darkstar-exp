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

import com.sun.sgs.app.DataManager;
import com.sun.sgs.app.ManagedObject;
import com.sun.sgs.app.ManagedObjectRemoval;
import com.sun.sgs.app.ManagedReference;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * @author Esko Luontola
 * @since 17.6.2008
 */
public class MockDataManager implements DataManager {

    private final Map<Object, ManagedReference<?>> refs = new IdentityHashMap<Object, ManagedReference<?>>();
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
        ManagedReference<?> ref = refs.remove(object);
        objects.remove(ref.getId().longValue());
    }

    public void markForUpdate(Object object) {
        // do nothing - the objects are always in memory
    }

    public <T> ManagedReference<T> createReference(T object) {
        ManagedReference<T> ref = (ManagedReference<T>) refs.get(object);
        if (ref == null) {
            ref = new MockManagedReference<T>(nextObjectId++, object);
            refs.put(object, ref);
            objects.put(ref.getId().longValue(), (ManagedObject) object);
        }
        return ref;
    }

    public ManagedObject getObjectById(long oid) {
        return objects.get(oid);
    }
}
