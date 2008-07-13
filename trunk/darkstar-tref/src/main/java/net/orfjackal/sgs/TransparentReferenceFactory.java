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

package net.orfjackal.sgs;

import com.sun.sgs.app.ManagedObject;

/**
 * @author Esko Luontola
 * @since 26.1.2008
 */
public interface TransparentReferenceFactory {

    /**
     * Creates a proxy for the specified ManagedObject instance, so that the ManagedObject will
     * be referenced by a ManagedReference but the proxy implements all the same interfaces as
     * the specified ManagedObject, excluding the ManagedObject interface. In other words, the
     * objects returned by this method will behave the same as any domain objects, except that
     * you will not need to wrap them in a ManagedReference.
     */
    TransparentReference createTransparentReference(ManagedObject object);

    /**
     * Creates a proxy for a TransparentReference instance which is not yet proxied. This is
     * needed only during deserialization and should not be called elsewhere.
     */
    TransparentReference newProxy(TransparentReferenceImpl reference);
}
