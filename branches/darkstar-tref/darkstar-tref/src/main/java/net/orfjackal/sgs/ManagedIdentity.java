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

import com.sun.sgs.app.AppContext;
import com.sun.sgs.app.ManagedObject;

/**
 * For transparent references to work correctly, all subclasses of {@link ManagedObject} should
 * define their {@link #equals(Object)} and {@link #hashCode()} methods as follows:
 * <pre><code>
 * public boolean equals(Object obj) {
 *     return ManagedIdentity.equals(this, obj);
 * }
 * public int hashCode() {
 *     return ManagedIdentity.hashCode(this);
 * }
 * </code></pre>
 *
 * @author Esko Luontola
 * @since 1.2.2008
 */
public final class ManagedIdentity {

    private ManagedIdentity() {
    }

    public static boolean equals(Object obj1, Object obj2) {
        Object id1 = getManagedIdentity(obj1);
        Object id2 = getManagedIdentity(obj2);
        return safeEquals(id1, id2);
    }

    public static int hashCode(Object obj) {
        Object id = getManagedIdentity(obj);
        return id.hashCode();
    }

    private static Object getManagedIdentity(Object obj) {
        if (obj instanceof TransparentReference) {
            return ((TransparentReference) obj).getManagedReference();
        } else if (obj instanceof ManagedObject) {
            return AppContext.getDataManager().createReference((ManagedObject) obj);
        } else {
            return obj;
        }
    }

    private static boolean safeEquals(Object x, Object y) {
        return x == y || (x != null && x.equals(y));
    }
}
