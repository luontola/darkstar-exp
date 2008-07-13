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
import com.sun.sgs.app.ManagedReference;

/**
 * @author Esko Luontola
 * @since 31.1.2008
 */
public interface TransparentReference {

    ManagedObject getManagedObject();

    ManagedReference getManagedReference();

    Class<?> getType();

    /**
     * @see ManagedIdentity#equals(Object, Object)
     */
    boolean equals(Object obj);

    /**
     * @see ManagedIdentity#hashCode(Object)
     */
    int hashCode();

    Object writeReplace();
}
