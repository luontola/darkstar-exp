/*
 * Copyright (c) 2008  Esko Luontola, www.orfjackal.net
 *
 * This program is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License
 * version 2 as published by the Free Software Foundation and
 * distributed hereunder to you.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.sun.sgs.impl.kernel;

import com.perpetual.sgs.mock.MockDataManager;
import com.perpetual.sgs.mock.MockManagedReference;
import com.sun.sgs.app.ManagedObject;
import com.sun.sgs.app.ManagedReference;
import com.sun.sgs.app.NameNotBoundException;
import com.sun.sgs.app.ObjectNotFoundException;

import java.io.Serializable;
import java.util.*;

public class MockDataManagerImpl implements MockDataManager {

    private Map<String,ManagedObject>  boundData = new HashMap<String,ManagedObject>();
    private Set<ManagedObject> allData = new HashSet<ManagedObject>();

    private Map<ManagedObject, ManagedReference> allRefs = new IdentityHashMap<ManagedObject, ManagedReference>();

    public Set<ManagedObject> getAllData() {
        return allData;
    }

    public Map<String, ManagedObject> getBoundData() {
        return boundData;
    }

    public MockDataManagerImpl() {

    }

    public ManagedReference createReference(final ManagedObject object) {
        if (!(object instanceof Serializable)) {
            throw new IllegalArgumentException("Object doesn't implement serializable: " + object.getClass().getCanonicalName());
        }
        // ObjectNotFoundException - how does this occur?
        
        // TODO: contribute this reference identity mod to MockSGS
        ManagedReference ref = allRefs.get(object);
        if (ref == null) {
            ref = new MockManagedReference(object);
            allRefs.put(object, ref);
        }
        return ref;
    }

    @SuppressWarnings("unchecked")
    public <T> T getBinding(final String name, final Class<T> type) {
        T value =  (T)boundData.get(name);
        if (value==null) {
            throw new NameNotBoundException("Couldn't find " + name + " of type " + type.getCanonicalName() );
        }
        if (!allData.contains(value)) {
            throw new ObjectNotFoundException("Couldn't find " + name + " of type  " + type.getCanonicalName());
        }
        return value;
    }


    public void markForUpdate(final ManagedObject object) {
        if (!(object instanceof Serializable)) {
            throw new IllegalArgumentException("Object doesn't implement serializable: " + object.getClass().getCanonicalName());
        }
    }


    public String nextBoundName(final String name) {

        // Get the bound names
        final Set<String> nameSet = boundData.keySet();
        final List<String> sortedNames = new ArrayList<String>(nameSet);

        // Sort them
        Collections.sort(sortedNames);

        boolean match = false;
        for (final String newName : sortedNames) {
            if (match) {
                // This is the next name after we found what we were searching for
                return newName;
            }
            if (newName.equals(name)) {
                match=true;
            }
        }

        return null;
    }

    public void removeBinding(final String name) {
        if (!boundData.containsKey(name)) {
            throw new NameNotBoundException("Name wasn't bound" + name);
        }
        boundData.remove(name);
    }

    public void removeObject(final ManagedObject object) {
        if (!(object instanceof Serializable)) {
            throw new IllegalArgumentException("Object doesn't implement serializable: " + object.getClass().getCanonicalName());
        }

        if (allData.contains(object)) {
            allData.remove(object);
            allRefs.remove(object);
        } else {
            throw new ObjectNotFoundException("Object not found");
        }
    }


    public void setBinding(final String name, final ManagedObject object) {
        if (!(object instanceof Serializable)) {
            throw new IllegalArgumentException("Object doesn't implement serializable: " + object.getClass().getCanonicalName());
        }
        // ObjectNotFoundException - not sure how to handle this?

        boundData.put(name,object);
        allData.add(object);
    }

}