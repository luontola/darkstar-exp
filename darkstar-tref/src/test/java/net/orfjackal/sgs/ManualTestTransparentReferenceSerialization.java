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

import java.io.*;

/**
 * Test for whether it is possible to modify the interfaces of serialized classes:
 * <ol>
 * <li>Run step 1 when DummyManagedObject2 <em>does not</em> implement DummyInterface2.</li>
 * <li>Modify DummyManagedObject2 so that it <em>does</em> implement DummyInterface2.</li>
 * <li>Run step 2 and verify that the deserialized class <em>is</em> instanceof DummyInterface2.</li>
 * </ol>
 *
 * @author Esko Luontola
 * @since 26.1.2008
 */
public class ManualTestTransparentReferenceSerialization {

    public static final File FILE = new File(ManualTestTransparentReferenceSerialization.class.getName() + ".ser.tmp");

    public static final TransparentReferenceFactory FACTORY = new TransparentReferenceCglibProxyFactory();
//    public static final TransparentReferenceFactory FACTORY = new TransparentReferenceJdkProxyFactory();

    public static class Step1_Serialize {

        public static void main(String[] args) throws IOException {
            MockAppContextResolver.install();

            TransparentReferenceImpl.setFactory(FACTORY);
            DummyInterface proxy = (DummyInterface) FACTORY.createTransparentReference(new DummyManagedObject2());
            System.out.println("proxy = " + proxy);

            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(FILE));
            out.writeObject(proxy);
            out.close();
            System.out.println("Written to " + FILE.getCanonicalPath());

            MockAppContextResolver.uninstall();
        }
    }

    public static class Step2_Deserialize {
        public static void main(String[] args) throws IOException, ClassNotFoundException {
            MockAppContextResolver.install();

            TransparentReferenceImpl.setFactory(FACTORY);
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(FILE));
            Object o = in.readObject();
            in.close();

            System.out.println("o.getClass() = " + o.getClass());
            System.out.println("isTransparentReference     = " + (o instanceof TransparentReference)
                    + "\t(expected: true)");
            System.out.println("managedObjectFromProxy     = " + ((TransparentReference) o).getManagedObject()
                    + "\t(expected: null when SGS is mocked)");
            System.out.println("instanceof DummyInterface  = " + (o instanceof DummyInterface)
                    + "\t(expected: " + DummyInterface.class.isAssignableFrom(DummyManagedObject2.class) + ")");
            System.out.println("instanceof DummyInterface2 = " + (o instanceof DummyInterface2)
                    + "\t(expected: " + DummyInterface2.class.isAssignableFrom(DummyManagedObject2.class) + ")");

            MockAppContextResolver.uninstall();
        }
    }

    public static class Step3_CleanUp {
        public static void main(String[] args) throws IOException {
            FILE.delete();
            System.out.println("Deleted " + FILE.getCanonicalPath());
        }
    }

    public static class DummyManagedObject2 implements
            DummyInterface,
//            DummyInterface2,
            Serializable, ManagedObject {

        private static final long serialVersionUID = 1L;

        public int dummyMethod() {
            return 0;
        }

        public int dummyMethod2() {
            return 0;
        }

        @SuppressWarnings({"EqualsWhichDoesntCheckParameterClass"})
        public boolean equals(Object obj) {
            return ManagedIdentity.equals(this, obj);
        }

        public int hashCode() {
            return ManagedIdentity.hashCode(this);
        }
    }

    public static interface DummyInterface2 {
        int dummyMethod2();
    }
}
