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

package net.orfjackal.darkstar.tref;

import com.sun.sgs.app.ManagedObject;
import com.sun.sgs.app.ManagedReference;

import java.io.Serializable;

/**
 * @author Esko Luontola
 * @since 31.1.2008
 */
public final class TransparentReferenceImpl implements TransparentReference, Serializable {

    private static final long serialVersionUID = 1L;

    private static TransparentReferenceFactory factory = new TransparentReferenceCglibProxyFactory();

    public static void setFactory(TransparentReferenceFactory factory) {
        TransparentReferenceImpl.factory = factory;
    }

    public static TransparentReferenceFactory getFactory() {
        return factory;
    }

    private final ManagedReference reference;
    private final Class<?> type;

    public TransparentReferenceImpl(Class<?> type, ManagedReference reference) {
        this.type = type;
        this.reference = reference;
    }

    public ManagedObject getManagedObject() {
        return (ManagedObject) reference.get();
    }

    public ManagedReference getManagedReference() {
        return reference;
    }

    public Class<?> getType() {
        return type;
    }

    @SuppressWarnings({"EqualsWhichDoesntCheckParameterClass"})
    public boolean equals(Object obj) {
        return ManagedIdentity.equals(this, obj);
    }

    public int hashCode() {
        return ManagedIdentity.hashCode(this);
    }

    /**
     * Proxy will delegate to this method, so that the TransparentReferenceImpl instead
     * of the proxy will be serialized.
     */
    public Object writeReplace() {
        return this;
    }

    /**
     * On deserialization, create a new proxy for this TransparentReferenceImpl.
     */
    protected Object readResolve() {
        return getFactory().newProxy(this);
    }
}
