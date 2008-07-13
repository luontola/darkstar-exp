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

import com.sun.sgs.app.ManagedObject;
import com.sun.sgs.app.ManagedReference;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.math.BigInteger;

/**
 * @author Esko Luontola
 * @since 17.6.2008
 */
public class MockManagedReference<T> implements ManagedReference<T>, Serializable {

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
        this.object = MockAppContext.getInstance().dataManager.getObjectById(oid);
    }
}
