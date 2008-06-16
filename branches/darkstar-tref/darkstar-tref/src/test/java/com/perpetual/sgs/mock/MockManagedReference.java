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

package com.perpetual.sgs.mock;

import com.sun.sgs.app.ManagedObject;
import com.sun.sgs.app.ManagedReference;

import java.io.Serializable;
import java.math.BigInteger;

public class MockManagedReference implements ManagedReference, Serializable {

    private static final long serialVersionUID = 1L;

    private final transient ManagedObject obj;

    public MockManagedReference(ManagedObject obj) {
        this.obj = obj;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> arg0) {
        return (T)obj;
    }

    @SuppressWarnings("unchecked")
    public <T> T getForUpdate(Class<T> arg0) {
        return (T)obj;
    }

    public BigInteger getId() {
        // TODO: contribute this reference identity mod to MockSGS
        return BigInteger.valueOf(System.identityHashCode(obj));
    }
}