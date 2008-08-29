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

import com.sun.sgs.app.AppContext;
import com.sun.sgs.app.ManagedObject;
import com.sun.sgs.app.ManagedReference;
import net.orfjackal.darkstar.tref.util.Cache;

import java.lang.reflect.*;

/**
 * @author Esko Luontola
 * @since 26.1.2008
 */
public class TransparentReferenceJdkProxyFactory implements TransparentReferenceFactory {

    private final Cache<Class<?>, Constructor<?>> cache = new JdkProxyFactoryCache();

    public TransparentReference createTransparentReference(ManagedObject object) {
        Class<?> type = object.getClass();
        ManagedReference<?> reference = AppContext.getDataManager().createReference(object);
        return newProxy(new TransparentReferenceImpl(type, reference));
    }

    public TransparentReference newProxy(TransparentReferenceImpl ref) {
        try {
            Constructor<?> constructor = cache.get(ref.getType());
            return (TransparentReference) constructor.newInstance(new TransparentReferenceHandler(ref));

        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private static class JdkProxyFactoryCache extends Cache<Class<?>, Constructor<?>> {

        protected Constructor<?> newInstance(Class<?> type) {
            try {
                Class<?>[] interfaces = TransparentReferenceUtil.proxiedInterfaces(type);
                Class<?> proxyClass = Proxy.getProxyClass(type.getClassLoader(), interfaces);
                return proxyClass.getConstructor(InvocationHandler.class);

            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static class TransparentReferenceHandler implements InvocationHandler {

        private final TransparentReference ref;

        public TransparentReferenceHandler(TransparentReference ref) {
            this.ref = ref;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            try {
                if (TransparentReferenceUtil.delegateToTransparentReference(method)) {
                    return method.invoke(ref, args);
                } else {
                    return method.invoke(ref.getManagedObject(), args);
                }
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
        }
    }
}
