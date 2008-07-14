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
import net.sf.cglib.proxy.*;

import java.lang.reflect.Method;

/**
 * @author Esko Luontola
 * @since 26.1.2008
 */
public class TransparentReferenceCglibProxyFactory implements TransparentReferenceFactory {

    private final Cache<Class<?>, Factory> cache = new CglibProxyFactoryCache();

    public TransparentReference createTransparentReference(ManagedObject object) {
        Class<?> type = object.getClass();
        ManagedReference reference = AppContext.getDataManager().createReference(object);
        return newProxy(new TransparentReferenceImpl(type, reference));
    }

    public TransparentReference newProxy(TransparentReferenceImpl ref) {
        Factory factory = cache.get(ref.getType());
        return (TransparentReference) factory.newInstance(new Callback[]{
                new ManagedObjectCallback(ref),
                new TransparentReferenceCallback(ref)
        });
    }

    private static class CglibProxyFactoryCache extends Cache<Class<?>, Factory> {

        protected Factory newInstance(Class<?> type) {
            Enhancer e = new Enhancer();
            e.setInterfaces(TransparentReferenceUtil.proxiedInterfaces(type));
            e.setCallbacks(new Callback[]{
                    new ManagedObjectCallback(null),
                    new TransparentReferenceCallback(null)
            });
            e.setCallbackFilter(new TransparentReferenceCallbackFilter());
            return (Factory) e.create();
        }
    }

    private static class ManagedObjectCallback implements LazyLoader {

        private final TransparentReference ref;

        private ManagedObjectCallback(TransparentReference ref) {
            this.ref = ref;
        }

        public Object loadObject() throws Exception {
            return ref.getManagedObject();
        }
    }

    private static class TransparentReferenceCallback implements Dispatcher {

        private final TransparentReference ref;

        public TransparentReferenceCallback(TransparentReference ref) {
            this.ref = ref;
        }

        public Object loadObject() throws Exception {
            return ref;
        }
    }

    private static class TransparentReferenceCallbackFilter implements CallbackFilter {

        private static final int MANAGED_OBJECT = 0;
        private static final int TRANSPARENT_REFERENCE = 1;

        public int accept(Method method) {
            if (TransparentReferenceUtil.delegateToTransparentReference(method)) {
                return TRANSPARENT_REFERENCE;
            } else {
                return MANAGED_OBJECT;
            }
        }
    }
}
