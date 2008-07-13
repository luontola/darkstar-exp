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
import junit.framework.TestCase;
import net.orfjackal.darkstar.exp.mocks.MockAppContext;

/**
 * @author Esko Luontola
 * @since 1.2.2008
 */
public abstract class TestManagedIdentity {

    public abstract static class ManagedIdentityContracts extends TestCase {

        protected TransparentReferenceFactory factory;
        private ManagedObject man1;
        private ManagedObject man2;
        private TransparentReference ref1;
        private TransparentReference ref2;
        private Object obj;

        protected void setUp() throws Exception {
            MockAppContext.install();
            
            man1 = new DummyManagedObject();
            man2 = new DummyManagedObject();
            ref1 = factory.createTransparentReference(man1);
            ref2 = factory.createTransparentReference(man2);
            obj = new Object();
        }

        protected void tearDown() throws Exception {
            MockAppContext.uninstall();
        }

        public void testManagedObjectEqualsManagedObject() {
            assertTrue(ManagedIdentity.equals(man1, man1));
            assertFalse(ManagedIdentity.equals(man1, man2));
        }

        public void testTransparentReferenceEqualsTransparentReference() {
            assertTrue(ManagedIdentity.equals(ref1, ref1));
            assertFalse(ManagedIdentity.equals(ref1, ref2));
        }

        public void testManagedObjectEqualsTransparentReference() {
            assertTrue(ManagedIdentity.equals(man1, ref1));
            assertTrue(ManagedIdentity.equals(ref1, man1));
            assertFalse(ManagedIdentity.equals(man1, ref2));
            assertFalse(ManagedIdentity.equals(ref2, man1));
        }

        public void testManagedObjectEqualsNormalObject() {
            assertFalse(ManagedIdentity.equals(man1, obj));
            assertFalse(ManagedIdentity.equals(obj, man1));
        }

        public void testTransparentReferenceEqualsNormalObject() {
            assertFalse(ManagedIdentity.equals(ref1, obj));
            assertFalse(ManagedIdentity.equals(obj, ref1));
        }

        public void testManagedObjectEqualsNull() {
            assertFalse(ManagedIdentity.equals(man1, null));
            assertFalse(ManagedIdentity.equals(null, man1));
        }

        public void testTransparentReferenceEqualsNull() {
            assertFalse(ManagedIdentity.equals(ref1, null));
            assertFalse(ManagedIdentity.equals(null, ref1));
        }

        public void testNormalObjectEqualsNormalObject() {
            assertTrue(ManagedIdentity.equals(obj, obj));
            assertFalse(ManagedIdentity.equals(obj, new Object()));
        }

        public void testNullEqualsNull() {
            assertTrue(ManagedIdentity.equals(null, null));
        }

        public void testDifferenceManagedObjectsHaveDifferenceHashCode() {
            int hc1 = ManagedIdentity.hashCode(man1);
            int hc2 = ManagedIdentity.hashCode(man2);
            assertFalse(hc1 == hc2);
        }

        public void testDifferenceTransparentReferencesHaveDifferenceHashCode() {
            int hc1 = ManagedIdentity.hashCode(ref1);
            int hc2 = ManagedIdentity.hashCode(ref2);
            assertFalse(hc1 == hc2);
        }

        public void testManagedObjectsAndTransparentReferencesHaveTheSameHashCode() {
            assertEquals(ManagedIdentity.hashCode(man1), ManagedIdentity.hashCode(ref1));
            assertEquals(ManagedIdentity.hashCode(man2), ManagedIdentity.hashCode(ref2));
        }

        public void testEqualsOnProxyShouldNotCallManagedObject() {
            ManagedObject man = new FailingManagedObject();
            TransparentReference proxy = factory.createTransparentReference(man);
            try {
                proxy.equals(man);
            } catch (Exception e) {
                e.printStackTrace();
                fail();
            }
        }

        public void testHashCodeOnProxyShouldNotCallManagedObject() {
            ManagedObject man = new FailingManagedObject();
            TransparentReference proxy = factory.createTransparentReference(man);
            try {
                proxy.hashCode();
            } catch (Exception e) {
                e.printStackTrace();
                fail();
            }
        }

        private class FailingManagedObject extends DummyManagedObject {
            @SuppressWarnings({"EqualsWhichDoesntCheckParameterClass"})
            public boolean equals(Object obj) {
                fail();
                throw new RuntimeException();
            }

            public int hashCode() {
                fail();
                throw new RuntimeException();
            }
        }
    }
}
