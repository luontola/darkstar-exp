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

import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;


/**
 * @author Esko Luontola
 * @since 23.1.2008
 */
public final class TransparentReferenceUtil {

    private TransparentReferenceUtil() {
    }

    /**
     * Marks the object for update if it is a ManagedObject or a TransparentReference. Otherwise does nothing.
     */
    public static void markForUpdate(Object object) {
        if (object instanceof TransparentReference) {
            TransparentReference ref = (TransparentReference) object;
            ref.getManagedReference().getForUpdate();
        } else if (object instanceof ManagedObject) {
            AppContext.getDataManager().markForUpdate((ManagedObject) object);
        }
    }

    /**
     * Used when converting {@link ManagedObject} instances to transparent references during serialization.
     * Needed because {@link ObjectOutputStream#replaceObject} does not check whether the returned objects
     * have a {@code writeReplace()} method.
     */
    public static Object createTransparentReferenceForSerialization(ManagedObject object, TransparentReferenceFactory factory) {
        return factory.createTransparentReference(object).writeReplace();
    }

    /**
     * Tells which interfaces of the provided class should be proxied by all transparent reference implementations.
     */
    public static Class<?>[] proxiedInterfaces(Class<?> aClass) {
        List<Class<?>> results = new ArrayList<Class<?>>();
        for (Class<?> c = aClass; c != null; c = c.getSuperclass()) {
            for (Class<?> anInterface : c.getInterfaces()) {
                assert !TransparentReference.class.equals(anInterface);
                if (!ManagedObject.class.isAssignableFrom(anInterface)) {
                    results.add(anInterface);
                }
            }
        }
        results.add(TransparentReference.class);
        return results.toArray(new Class<?>[results.size()]);
    }

    /**
     * Tests whether the proxy should delegate a method call to the transparent reference instance.
     */
    public static boolean delegateToTransparentReference(Method method) {
        return method.getDeclaringClass().equals(TransparentReference.class)
                || (method.getDeclaringClass().equals(Object.class) && method.getName().equals("equals"))
                || (method.getDeclaringClass().equals(Object.class) && method.getName().equals("hashCode"));
    }
}
