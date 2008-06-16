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
import junit.framework.TestCase;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Esko Luontola
 * @since 25.1.2008
 */
public abstract class TestTransparentReference {

    public static byte[] serializeObject(Object object) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bytes);
        out.writeObject(object);
        out.close();
        return bytes.toByteArray();
    }

    public static Object deserializeObject(byte[] bytes) throws IOException, ClassNotFoundException {
        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes));
        Object object = in.readObject();
        in.close();
        return object;
    }

    public static abstract class WhenCreatingATransparentReference extends TestCase {

        protected TransparentReferenceFactory factory;
        private DummyManagedObject object;
        private Object proxy;

        protected void setUp() throws Exception {
//            MockSGS.init();
            TransparentReferenceImpl.setFactory(factory);
            object = new DummyManagedObject();
            proxy = factory.createTransparentReference(object);
        }

        public void testAProxyShouldBeCreated() {
            assertNotNull(proxy);
        }

        public void testTheProxyShouldNotBeTheObject() {
            assertNotSame(object, proxy);
        }

        public void testTheProxyShouldImplementTheSameInterfacesAsTheObject() {
            assertTrue(DummyInterface.class.isAssignableFrom(proxy.getClass()));
        }

        public void testTheProxyShouldImplementTheSameInterfacesAsTheObjectsSuperclasses() {
            Object subclassProxy = factory.createTransparentReference(new DummyManagedObject() {
            });
            assertTrue(DummyInterface.class.isAssignableFrom(subclassProxy.getClass()));
        }

        public void testTheProxyShouldNotImplementManagedObject() {
            assertFalse(ManagedObject.class.isAssignableFrom(proxy.getClass()));
        }

        public void testTheProxyShouldBeSerializableAndDeserializable() throws IOException, ClassNotFoundException {
            byte[] bytes = serializeObject(proxy);
            Object deserialized = deserializeObject(bytes);
            assertTrue(deserialized instanceof TransparentReference);
        }

        public void testTheProxyShouldContainAManagedReferenceAndNotAManagedObject() throws IOException {
            final List<Object> objects = new ArrayList<Object>();
            ObjectOutputStream out = new ObjectOutputStream(new ByteArrayOutputStream()) {
                {
                    enableReplaceObject(true);
                }

                protected Object replaceObject(Object obj) throws IOException {
                    objects.add(obj);
                    return obj;
                }
            };
            out.writeObject(proxy);
            out.close();

            boolean containsManagedReference = false;
            for (Object o : objects) {
                assertFalse("ManagedObject instance not allowed: " + o.getClass(), (o instanceof ManagedObject));
                if (o instanceof ManagedReference) {
                    containsManagedReference = true;
                }
            }
            assertTrue(containsManagedReference);
        }

        public void testCallingAMethodOnTheProxyShouldCallAMethodOnTheObject() {
            DummyInterface proxy = (DummyInterface) this.proxy;
            assertEquals(1, proxy.dummyMethod());
            assertEquals(1, object.lastValue);
            assertEquals(2, proxy.dummyMethod());
            assertEquals(2, object.lastValue);
        }

        public void testTheProxyShouldShowTheSameExceptionsAsTheObject() {
            DummyManagedObject exceptionThrowing = new DummyManagedObject() {
                public int dummyMethod() {
                    throw new IllegalStateException("foo");
                }
            };
            DummyInterface proxy = (DummyInterface) factory.createTransparentReference(exceptionThrowing);
            try {
                proxy.dummyMethod();
                fail();
            } catch (Exception e) {
                assertEquals(IllegalStateException.class, e.getClass());
                assertEquals("foo", e.getMessage());
            }
        }

        public void testItShouldBePossibleToIdentifyAProxy() {
            assertTrue(proxy instanceof TransparentReference);
            assertFalse(object instanceof TransparentReference);
        }

        public void testItShouldBePossibleToGetTheObjectFromTheProxy() {
            assertSame(object, ((TransparentReference) proxy).getManagedObject());
        }

        public void testItShouldBePossibleToGetTheObjectsTypeFromTheProxy() {
            assertSame(DummyManagedObject.class, ((TransparentReference) proxy).getType());
        }

        public void testItShouldBePossibleToGetTheManagedReferenceFromTheProxy() {
            ManagedReference reference = ((TransparentReference) proxy).getManagedReference();
            assertSame(object, reference.get());
        }
    }

    @SuppressWarnings({"FieldCanBeLocal"})
    public static abstract class WhenManagedObjectsAreConvertedToTransparentReferencesDuringSerialization extends TestCase {

        protected TransparentReferenceFactory factory;
        private DummyManagedObject managedObject;
        private DummyInterface normalObject;
        private SerializationTestObject deserialized;

        protected void setUp() throws Exception {
//            MockSGS.init();
            TransparentReferenceImpl.setFactory(factory);
            managedObject = new DummyManagedObject();
            normalObject = new DummyNormalObject();
            SerializationTestObject object = new SerializationTestObject(managedObject, normalObject);

            ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
            ObjectOutputStream out = new TransparentReferenceConvertingObjectOutputStream(outBytes, factory);
            out.writeObject(object);
            out.close();
            byte[] bytes = outBytes.toByteArray();

            deserialized = (SerializationTestObject) deserializeObject(bytes);
        }

        public void testShouldReplaceManagedObjectsWithProxies() {
            assertTrue(deserialized.manField instanceof TransparentReference);
        }

        public void testShouldIgnoreNormalObjects() {
            assertEquals(normalObject.getClass(), deserialized.aField.getClass());
        }

        public void testShouldSupportArrays() {
            assertSame(normalObject.getClass(), deserialized.aManArray[0].getClass());
            assertTrue(deserialized.aManArray[1] instanceof TransparentReference);
        }

        private static class SerializationTestObject implements Serializable {

            private DummyInterface aField;
            private DummyInterface manField;
            private DummyInterface[] aManArray;

            public SerializationTestObject(DummyManagedObject managedObject, DummyInterface normalObject) {
                aField = normalObject;
                manField = managedObject;
                aManArray = new DummyInterface[]{normalObject, managedObject};
            }
        }

        private static class DummyNormalObject implements DummyInterface, Serializable {
            public int dummyMethod() {
                return 0;
            }
        }
    }

    public static abstract class MarkingTransparentReferencesForUpdate extends TestCase {

        protected TransparentReferenceFactory factory;
        private DummyManagedObject managedObject;
        private DummyInterface proxy;
        private Object normalObject;

        protected void setUp() throws Exception {
//            MockSGS.init();
            TransparentReferenceImpl.setFactory(factory);
            managedObject = new DummyManagedObject();
            proxy = (DummyInterface) factory.createTransparentReference(managedObject);
            normalObject = new Object();
        }

        public void testMarkForUpdateOnManagedObjectShouldUseMarkForUpdate() {
            TransparentReferenceUtil.markForUpdate(managedObject);
            // unable to test, should call: AppContext.getDataManager().markForUpdate(managedObject);
        }

        public void testMarkForUpdateOnTransparentReferenceShouldUseGetForUpdate() {
            TransparentReferenceUtil.markForUpdate(proxy);
            // unable to test, should call: ManagedReference.getForUpdate();
        }

        public void testMarkForUpdateOnNormalObjectShouldDoNothing() {
            TransparentReferenceUtil.markForUpdate(normalObject);
            // unable to test, should call nothing
        }
    }
}
